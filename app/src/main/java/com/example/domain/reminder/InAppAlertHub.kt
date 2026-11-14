package com.example.domain.reminder

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.CadenceApp
import com.example.MainActivity
import com.example.R
import com.example.domain.time.DateTimeUtils
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class ActiveAlertEvent(
  val reminderId: String,
  val taskId: String,
  val taskTitle: String,
  val notificationId: Int,
  val triggerAt: Long,
)

object ReminderWakeLock {
  fun acquire(context: Context, tag: String, timeoutMs: Long = 20_000L): PowerManager.WakeLock? {
    val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager ?: return null
    return runCatching {
      powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, tag).apply {
        setReferenceCounted(false)
        acquire(timeoutMs)
      }
    }.getOrNull()
  }

  fun release(wakeLock: PowerManager.WakeLock?) {
    runCatching {
      if (wakeLock?.isHeld == true) {
        wakeLock.release()
      }
    }
  }
}

object ReminderTriggerRegistry {
  private const val PREFS_NAME = "cadence_triggered_reminders"
  private val triggeredAtByReminder = ConcurrentHashMap<String, Long>()

  fun hasTriggered(context: Context?, reminderId: String, triggerAt: Long): Boolean {
    if (reminderId.isBlank()) return false
    if (triggeredAtByReminder[reminderId] == triggerAt) return true
    if (context != null) {
      val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
      if (prefs.contains(reminderId) && prefs.getLong(reminderId, Long.MIN_VALUE) == triggerAt) {
        triggeredAtByReminder[reminderId] = triggerAt
        return true
      }
    }
    return false
  }

  fun markTriggeredIfNew(
    reminderId: String,
    triggerAt: Long,
    context: Context? = null,
  ): Boolean {
    if (reminderId.isBlank()) return true
    synchronized(this) {
      if (hasTriggered(context, reminderId, triggerAt)) {
        return false
      }
      triggeredAtByReminder[reminderId] = triggerAt
      context
        ?.applicationContext
        ?.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        ?.edit()
        ?.putLong(reminderId, triggerAt)
        ?.apply()
      return true
    }
  }

  fun clear(reminderId: String, context: Context? = null) {
    triggeredAtByReminder.remove(reminderId)
    context
      ?.applicationContext
      ?.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
      ?.edit()
      ?.remove(reminderId)
      ?.apply()
  }

  fun clearAll(context: Context? = null) {
    triggeredAtByReminder.clear()
    context
      ?.applicationContext
      ?.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
      ?.edit()
      ?.clear()
      ?.apply()
  }
}

object InAppAlertHub {
  private val _activeAlerts = MutableStateFlow<List<ActiveAlertEvent>>(emptyList())
  val activeAlerts: StateFlow<List<ActiveAlertEvent>> = _activeAlerts.asStateFlow()

  fun enqueue(event: ActiveAlertEvent) {
    _activeAlerts.update { current ->
      val filtered = current.filterNot { it.reminderId == event.reminderId }
      filtered + event
    }
  }

  fun dismiss(reminderId: String) {
    _activeAlerts.update { current -> current.filterNot { it.reminderId == reminderId } }
  }

  fun clearAll() {
    _activeAlerts.value = emptyList()
  }
}

object ReminderNotificationPoster {
  fun hasNotificationPermission(context: Context): Boolean {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
    return ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
      PackageManager.PERMISSION_GRANTED
  }

  fun ensureTaskReminderChannel(context: Context) {
    val container = (context.applicationContext as? CadenceApp)?.container
    if (container != null) {
      container.reminderScheduler.ensureNotificationChannels()
      return
    }
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
      val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
      val audioAttributes =
        AudioAttributes.Builder()
          .setUsage(AudioAttributes.USAGE_NOTIFICATION)
          .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
          .build()
      val taskChannel =
        NotificationChannel(
          LocalReminderScheduler.CHANNEL_TASK_REMINDERS,
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
      manager.createNotificationChannel(taskChannel)
    }
  }

  fun postTaskAlertNotification(
    context: Context,
    reminderId: String?,
    taskId: String?,
    taskTitle: String,
    notificationId: Int,
    triggerAt: Long,
  ): Boolean {
    if (!hasNotificationPermission(context)) return false
    ensureTaskReminderChannel(context)

    val openIntent =
      Intent(context, MainActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        if (!taskId.isNullOrBlank()) {
          data = Uri.parse("cadence://task/$taskId")
          putExtra(LocalReminderScheduler.EXTRA_TASK_ID, taskId)
        }
      }
    val openPendingIntent =
      PendingIntent.getActivity(
        context,
        notificationId,
        openIntent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
      )

    val contentText =
      if (triggerAt > 0L) "Scheduled alert · ${DateTimeUtils.formatTime(triggerAt)}"
      else "Scheduled task alert"

    val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

    val builder =
      NotificationCompat.Builder(context, LocalReminderScheduler.CHANNEL_TASK_REMINDERS)
        .setSmallIcon(R.drawable.ic_notification_alarm)
        .setContentTitle(taskTitle)
        .setContentText(contentText)
        .setStyle(NotificationCompat.BigTextStyle().bigText(contentText))
        .setCategory(NotificationCompat.CATEGORY_REMINDER)
        .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
        .setPriority(NotificationCompat.PRIORITY_MAX)
        .setDefaults(NotificationCompat.DEFAULT_ALL)
        .setSound(soundUri)
        .setVibrate(longArrayOf(0L, 350L, 180L, 350L))
        .setWhen(if (triggerAt > 0L) triggerAt else System.currentTimeMillis())
        .setShowWhen(true)
        .setOnlyAlertOnce(false)
        .setContentIntent(openPendingIntent)
        .setAutoCancel(true)

    if (!taskId.isNullOrBlank()) {
      val completeIntent =
        Intent(context, ReminderReceiver::class.java).apply {
          action = LocalReminderScheduler.ACTION_COMPLETE_TASK
          data = Uri.parse("cadence://reminder-action/${reminderId ?: taskId}/complete")
          putExtra(LocalReminderScheduler.EXTRA_TASK_ID, taskId)
          putExtra(LocalReminderScheduler.EXTRA_REMINDER_ID, reminderId)
          putExtra(LocalReminderScheduler.EXTRA_NOTIFICATION_ID, notificationId)
        }
      val completePendingIntent =
        PendingIntent.getBroadcast(
          context,
          notificationId + 10_000,
          completeIntent,
          PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
      builder.addAction(0, "Mark done", completePendingIntent)
    }

    if (!reminderId.isNullOrBlank()) {
      val snoozeIntent =
        Intent(context, ReminderReceiver::class.java).apply {
          action = LocalReminderScheduler.ACTION_SNOOZE_REMINDER
          data = Uri.parse("cadence://reminder-action/$reminderId/snooze")
          putExtra(LocalReminderScheduler.EXTRA_REMINDER_ID, reminderId)
          putExtra(LocalReminderScheduler.EXTRA_NOTIFICATION_ID, notificationId)
        }
      val snoozePendingIntent =
        PendingIntent.getBroadcast(
          context,
          notificationId + 20_000,
          snoozeIntent,
          PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
      builder.addAction(0, "Snooze 10m", snoozePendingIntent)
    }

    NotificationManagerCompat.from(context).notify(notificationId, builder.build())
    return true
  }
}
