package com.example.domain.reminder

import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import com.example.data.local.dao.ReminderDao
import com.example.data.local.dao.TaskDao
import com.example.data.local.entity.ReminderEntity
import com.example.data.local.entity.TaskStatus

interface ReminderScheduler {
  fun ensureNotificationChannels()

  fun canScheduleExactAlarms(): Boolean = true

  fun clearTriggered(reminderId: String) {}

  suspend fun schedule(reminder: ReminderEntity, taskTitle: String)

  suspend fun cancel(reminderId: String, notificationId: Int)

  suspend fun reconcileAll()

  suspend fun reconcileMissedOnSystemEvent() {
    reconcileAll()
  }
}

class LocalReminderScheduler(
  private val context: Context,
  private val reminderDao: ReminderDao,
  private val taskDao: TaskDao,
) : ReminderScheduler {

  override fun ensureNotificationChannels() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
      val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
      val audioAttributes =
        AudioAttributes.Builder()
          .setUsage(AudioAttributes.USAGE_NOTIFICATION)
          .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
          .build()

      val taskChannel =
        NotificationChannel(
          CHANNEL_TASK_REMINDERS,
          "Task Reminders",
          NotificationManager.IMPORTANCE_HIGH,
        ).apply {
          description = "Scheduled alerts and reminders for tasks"
          enableLights(true)
          enableVibration(true)
          vibrationPattern = longArrayOf(0L, 350L, 180L, 350L)
          lockscreenVisibility = Notification.VISIBILITY_PUBLIC
          setSound(soundUri, audioAttributes)
        }
      val focusChannel =
        NotificationChannel(
          CHANNEL_FOCUS_TIMER,
          "Focus Timer",
          NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
          description = "Focus session completion alerts"
        }
      manager.createNotificationChannel(taskChannel)
      manager.createNotificationChannel(focusChannel)
    }
  }

  override fun canScheduleExactAlarms(): Boolean {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return true
    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return false
    return alarmManager.canScheduleExactAlarms()
  }

  override fun clearTriggered(reminderId: String) {
    ReminderTriggerRegistry.clear(reminderId, context)
  }

  override suspend fun schedule(reminder: ReminderEntity, taskTitle: String) {
    ensureNotificationChannels()
    if (!reminder.isEnabled || reminder.isDismissed) {
      cancel(reminder.id, reminder.notificationId)
      return
    }
    val now = System.currentTimeMillis()
    if (reminder.triggerAt < now - SAVE_GRACE_WINDOW_MS) return
    val effectiveTriggerAt = maxOf(reminder.triggerAt, now + 500L)

    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
    val pendingIntent =
      buildPendingIntent(
        reminderId = reminder.id,
        taskId = reminder.taskId,
        taskTitle = taskTitle,
        notificationId = reminder.notificationId,
        triggerAt = reminder.triggerAt,
      )
    try {
      when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && alarmManager.canScheduleExactAlarms() -> {
          alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            effectiveTriggerAt,
            pendingIntent,
          )
        }
        Build.VERSION.SDK_INT in Build.VERSION_CODES.M until Build.VERSION_CODES.S -> {
          alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            effectiveTriggerAt,
            pendingIntent,
          )
        }
        Build.VERSION.SDK_INT < Build.VERSION_CODES.M -> {
          alarmManager.setExact(
            AlarmManager.RTC_WAKEUP,
            effectiveTriggerAt,
            pendingIntent,
          )
        }
        else -> {
          alarmManager.setAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            effectiveTriggerAt,
            pendingIntent,
          )
        }
      }
    } catch (_: SecurityException) {
      try {
        alarmManager.setAndAllowWhileIdle(
          AlarmManager.RTC_WAKEUP,
          effectiveTriggerAt,
          pendingIntent,
        )
      } catch (_: SecurityException) {
        // Gracefully ignore when system restricts alarms
      }
    }
  }

  override suspend fun cancel(reminderId: String, notificationId: Int) {
    ReminderTriggerRegistry.clear(reminderId, context)
    InAppAlertHub.dismiss(reminderId)
    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
    val intent =
      Intent(context, ReminderReceiver::class.java).apply {
        action = ACTION_TRIGGER_REMINDER
        data = reminderUri(reminderId)
        putExtra(EXTRA_REMINDER_ID, reminderId)
      }
    val pendingIntent =
      PendingIntent.getBroadcast(
        context,
        notificationId,
        intent,
        PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE,
      )
    if (pendingIntent != null) {
      alarmManager.cancel(pendingIntent)
      pendingIntent.cancel()
    }
  }

  override suspend fun reconcileAll() {
    reconcileInternal(deliverMissedAlerts = false)
  }

  override suspend fun reconcileMissedOnSystemEvent() {
    reconcileInternal(deliverMissedAlerts = true)
  }

  private suspend fun reconcileInternal(deliverMissedAlerts: Boolean) {
    ensureNotificationChannels()
    val activeReminders = reminderDao.getActiveReminders()
    val now = System.currentTimeMillis()
    for (reminder in activeReminders) {
      val task = taskDao.getTaskById(reminder.taskId)
      if (task == null || task.status == TaskStatus.COMPLETED || task.status == TaskStatus.CANCELLED) {
        cancel(reminder.id, reminder.notificationId)
        continue
      }

      if (reminder.triggerAt >= now - SAVE_GRACE_WINDOW_MS) {
        if (!ReminderTriggerRegistry.hasTriggered(context, reminder.id, reminder.triggerAt)) {
          schedule(reminder, task.title)
        }
      } else if (
        deliverMissedAlerts &&
          reminder.triggerAt >= now - MISSED_ALERT_RECOVERY_WINDOW_MS &&
          ReminderTriggerRegistry.markTriggeredIfNew(reminder.id, reminder.triggerAt, context)
      ) {
        ReminderNotificationPoster.postTaskAlertNotification(
          context = context,
          reminderId = reminder.id,
          taskId = task.id,
          taskTitle = task.title,
          notificationId = reminder.notificationId,
          triggerAt = reminder.triggerAt,
        )
        InAppAlertHub.enqueue(
          ActiveAlertEvent(
            reminderId = reminder.id,
            taskId = task.id,
            taskTitle = task.title,
            notificationId = reminder.notificationId,
            triggerAt = reminder.triggerAt,
          )
        )
      }
    }
  }

  private fun buildPendingIntent(
    reminderId: String,
    taskId: String,
    taskTitle: String,
    notificationId: Int,
    triggerAt: Long,
  ): PendingIntent {
    val intent =
      Intent(context, ReminderReceiver::class.java).apply {
        action = ACTION_TRIGGER_REMINDER
        data = reminderUri(reminderId)
        putExtra(EXTRA_REMINDER_ID, reminderId)
        putExtra(EXTRA_TASK_ID, taskId)
        putExtra(EXTRA_TASK_TITLE, taskTitle)
        putExtra(EXTRA_NOTIFICATION_ID, notificationId)
        putExtra(EXTRA_TRIGGER_AT, triggerAt)
      }
    return PendingIntent.getBroadcast(
      context,
      notificationId,
      intent,
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )
  }

  private fun reminderUri(reminderId: String): Uri = Uri.parse("cadence://reminder/$reminderId")

  companion object {
    private const val SAVE_GRACE_WINDOW_MS = 2 * 60_000L
    private const val MISSED_ALERT_RECOVERY_WINDOW_MS = 24 * 60 * 60_000L
    const val CHANNEL_TASK_REMINDERS = "cadence_task_reminders_v2"
    const val CHANNEL_FOCUS_TIMER = "cadence_focus_timer"
    const val ACTION_TRIGGER_REMINDER = "com.example.ACTION_TRIGGER_REMINDER"
    const val ACTION_COMPLETE_TASK = "com.example.ACTION_COMPLETE_TASK"
    const val ACTION_SNOOZE_REMINDER = "com.example.ACTION_SNOOZE_REMINDER"
    const val EXTRA_REMINDER_ID = "extra_reminder_id"
    const val EXTRA_TASK_ID = "extra_task_id"
    const val EXTRA_TASK_TITLE = "extra_task_title"
    const val EXTRA_NOTIFICATION_ID = "extra_notification_id"
    const val EXTRA_TRIGGER_AT = "extra_trigger_at"
  }
}
