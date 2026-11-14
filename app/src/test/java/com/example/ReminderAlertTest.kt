package com.example

import android.Manifest
import android.app.AlarmManager
import android.app.Application
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.CadenceDatabase
import com.example.data.local.entity.ReminderEntity
import com.example.data.local.entity.TaskEntity
import com.example.data.local.entity.TaskStatus
import com.example.domain.reminder.InAppAlertHub
import com.example.domain.reminder.LocalReminderScheduler
import com.example.domain.reminder.ReminderReceiver
import com.example.domain.reminder.ReminderTriggerRegistry
import com.example.domain.reminder.ReminderWakeLock
import com.example.domain.time.DateTimeUtils
import java.util.Calendar
import java.util.TimeZone
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowAlarmManager

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ReminderAlertTest {

  private lateinit var context: Application

  @Before
  fun setup() {
    context = ApplicationProvider.getApplicationContext()
    ReminderTriggerRegistry.clearAll(context)
    InAppAlertHub.clearAll()
    Shadows.shadowOf(context).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)
  }

  @After
  fun tearDown() {
    ReminderTriggerRegistry.clearAll(context)
    InAppAlertHub.clearAll()
  }

  @Test
  fun `scheduler registers exact alarm and receiver posts notification plus in-app alert`() = runBlocking {
    val db = CadenceDatabase.createInMemory(context)
    try {
      val scheduler = LocalReminderScheduler(context, db.reminderDao(), db.taskDao())
      val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
      val shadowAlarmManager = Shadows.shadowOf(alarmManager)
      ShadowAlarmManager.setCanScheduleExactAlarms(true)

      val triggerAt = System.currentTimeMillis() + 60_000L
      val task = TaskEntity(id = "t_alert_1", title = "Submit project demo", status = TaskStatus.TODO)
      val reminder =
        ReminderEntity(
          id = "r_alert_1",
          taskId = task.id,
          triggerAt = triggerAt,
          offsetMinutes = -1,
          isEnabled = true,
          isDismissed = false,
          notificationId = 4242,
          type = ReminderEntity.TYPE_CUSTOM_TIME,
        )
      db.taskDao().upsertTask(task)
      db.reminderDao().upsertReminder(reminder)

      scheduler.schedule(reminder, task.title)

      val scheduled = shadowAlarmManager.nextScheduledAlarm
      assertNotNull(scheduled)
      assertEquals(triggerAt, scheduled!!.triggerAtMs)

      val fireIntent =
        Intent(context, ReminderReceiver::class.java).apply {
          action = LocalReminderScheduler.ACTION_TRIGGER_REMINDER
          putExtra(LocalReminderScheduler.EXTRA_REMINDER_ID, reminder.id)
          putExtra(LocalReminderScheduler.EXTRA_TASK_ID, task.id)
          putExtra(LocalReminderScheduler.EXTRA_TASK_TITLE, task.title)
          putExtra(LocalReminderScheduler.EXTRA_NOTIFICATION_ID, reminder.notificationId)
          putExtra(LocalReminderScheduler.EXTRA_TRIGGER_AT, triggerAt)
        }
      ReminderReceiver().onReceive(context, fireIntent)

      val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
      val posted = Shadows.shadowOf(notificationManager).allNotifications
      assertTrue(posted.isNotEmpty())
      val notif = posted.first()
      assertEquals(LocalReminderScheduler.CHANNEL_TASK_REMINDERS, notif.channelId)
      assertEquals("Submit project demo", notif.extras.getString("android.title"))
      assertEquals(2, notif.actions.size)

      val inApp = InAppAlertHub.activeAlerts.value
      assertEquals(1, inApp.size)
      assertEquals("Submit project demo", inApp.first().taskTitle)

      // Duplicate broadcast for same triggerAt is ignored even after clearing in-memory cache
      ReminderTriggerRegistry.clearAll(context = null)
      assertTrue(ReminderTriggerRegistry.hasTriggered(context, reminder.id, triggerAt))
      assertFalse(ReminderTriggerRegistry.markTriggeredIfNew(reminder.id, triggerAt, context))
    } finally {
      db.close()
    }
  }

  @Test
  fun `cancel removes registered alarm and reconcileMissedOnSystemEvent delivers missed boot alerts`() = runBlocking {
    val db = CadenceDatabase.createInMemory(context)
    try {
      val scheduler = LocalReminderScheduler(context, db.reminderDao(), db.taskDao())
      val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
      val shadowAlarmManager = Shadows.shadowOf(alarmManager)
      ShadowAlarmManager.setCanScheduleExactAlarms(true)

      val futureAt = System.currentTimeMillis() + 120_000L
      val task = TaskEntity(id = "t_cancel_1", title = "Take vitamins", status = TaskStatus.TODO)
      val futureReminder =
        ReminderEntity(
          id = "r_cancel_1",
          taskId = task.id,
          triggerAt = futureAt,
          offsetMinutes = -1,
          isEnabled = true,
          isDismissed = false,
          notificationId = 5151,
          type = ReminderEntity.TYPE_CUSTOM_TIME,
        )
      db.taskDao().upsertTask(task)
      db.reminderDao().upsertReminder(futureReminder)

      scheduler.schedule(futureReminder, task.title)
      assertNotNull(shadowAlarmManager.nextScheduledAlarm)

      scheduler.cancel(futureReminder.id, futureReminder.notificationId)
      assertNull(shadowAlarmManager.nextScheduledAlarm)

      // Simulate a reminder missed 10 minutes ago while device was off
      val missedAt = System.currentTimeMillis() - 10 * 60_000L
      val missedReminder =
        ReminderEntity(
          id = "r_missed_1",
          taskId = task.id,
          triggerAt = missedAt,
          offsetMinutes = -1,
          isEnabled = true,
          isDismissed = false,
          notificationId = 6161,
          type = ReminderEntity.TYPE_CUSTOM_TIME,
        )
      db.reminderDao().upsertReminder(missedReminder)

      scheduler.reconcileMissedOnSystemEvent()

      val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
      val posted = Shadows.shadowOf(notificationManager).allNotifications
      assertTrue(posted.any { it.extras.getString("android.title") == "Take vitamins" })
    } finally {
      db.close()
    }
  }

  @Test
  fun `wake lock helper acquires and releases safely`() {
    val wakeLock = ReminderWakeLock.acquire(context, "Cadence:UnitTestWakeLock", 2_000L)
    assertNotNull(wakeLock)
    assertTrue(wakeLock!!.isHeld)
    ReminderWakeLock.release(wakeLock)
    assertFalse(wakeLock.isHeld)
  }

  @Test
  fun `utc date picker roundtrip preserves local calendar day across timezones`() {
    val originalTz = TimeZone.getDefault()
    try {
      for (zoneId in listOf("America/Los_Angeles", "Asia/Kolkata", "UTC")) {
        TimeZone.setDefault(TimeZone.getTimeZone(zoneId))
        val localCal =
          Calendar.getInstance().apply {
            clear()
            set(2026, Calendar.NOVEMBER, 14, 15, 30, 0)
          }
        val utcMidnight = DateTimeUtils.localDateToUtcMidnight(localCal.timeInMillis)
        val restoredLocalStart = DateTimeUtils.utcMidnightToLocalStartOfDay(utcMidnight)
        val restoredCal = Calendar.getInstance().apply { timeInMillis = restoredLocalStart }
        assertEquals(2026, restoredCal.get(Calendar.YEAR))
        assertEquals(Calendar.NOVEMBER, restoredCal.get(Calendar.MONTH))
        assertEquals(14, restoredCal.get(Calendar.DAY_OF_MONTH))
      }
    } finally {
      TimeZone.setDefault(originalTz)
    }
  }
}
