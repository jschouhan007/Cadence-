package com.example.domain.reminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationManagerCompat
import com.example.CadenceApp
import com.example.data.local.entity.TaskStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

class ReminderReceiver : BroadcastReceiver() {
  override fun onReceive(context: Context, intent: Intent) {
    when (intent.action) {
      LocalReminderScheduler.ACTION_TRIGGER_REMINDER -> handleTriggerReminder(context, intent)
      LocalReminderScheduler.ACTION_COMPLETE_TASK -> handleCompleteTask(context, intent)
      LocalReminderScheduler.ACTION_SNOOZE_REMINDER -> handleSnoozeReminder(context, intent)
    }
  }

  private fun handleTriggerReminder(context: Context, intent: Intent) {
    val taskId = intent.getStringExtra(LocalReminderScheduler.EXTRA_TASK_ID)
    val reminderId = intent.getStringExtra(LocalReminderScheduler.EXTRA_REMINDER_ID)
    val taskTitle = intent.getStringExtra(LocalReminderScheduler.EXTRA_TASK_TITLE) ?: "Task reminder"
    val notificationId = intent.getIntExtra(LocalReminderScheduler.EXTRA_NOTIFICATION_ID, 1001)
    val triggerAt = intent.getLongExtra(LocalReminderScheduler.EXTRA_TRIGGER_AT, 0L)

    val wakeLock = ReminderWakeLock.acquire(context, "Cadence:ReminderTriggerWakeLock", 15_000L)
    try {
      if (!reminderId.isNullOrBlank()) {
        val isNew = ReminderTriggerRegistry.markTriggeredIfNew(reminderId, triggerAt, context)
        if (!isNew) return
      }

      ReminderNotificationPoster.postTaskAlertNotification(
        context = context,
        reminderId = reminderId,
        taskId = taskId,
        taskTitle = taskTitle,
        notificationId = notificationId,
        triggerAt = triggerAt,
      )

      if (!reminderId.isNullOrBlank() && !taskId.isNullOrBlank()) {
        InAppAlertHub.enqueue(
          ActiveAlertEvent(
            reminderId = reminderId,
            taskId = taskId,
            taskTitle = taskTitle,
            notificationId = notificationId,
            triggerAt = triggerAt,
          )
        )
      }
    } finally {
      ReminderWakeLock.release(wakeLock)
    }
  }

  private fun handleCompleteTask(context: Context, intent: Intent) {
    val taskId = intent.getStringExtra(LocalReminderScheduler.EXTRA_TASK_ID) ?: return
    val reminderId = intent.getStringExtra(LocalReminderScheduler.EXTRA_REMINDER_ID)
    val notificationId = intent.getIntExtra(LocalReminderScheduler.EXTRA_NOTIFICATION_ID, 1001)
    NotificationManagerCompat.from(context).cancel(notificationId)
    if (!reminderId.isNullOrBlank()) {
      InAppAlertHub.dismiss(reminderId)
    }

    val container = (context.applicationContext as? CadenceApp)?.container ?: return
    val wakeLock = ReminderWakeLock.acquire(context, "Cadence:ReminderCompleteWakeLock", 20_000L)
    val pendingResult = goAsync()
    CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
      try {
        val existing = container.taskRepository.observeTaskDetails(taskId).firstOrNull()
        if (existing != null && existing.task.status != TaskStatus.COMPLETED) {
          container.taskRepository.toggleTaskCompletion(taskId)
        }
      } finally {
        ReminderWakeLock.release(wakeLock)
        pendingResult?.finish()
      }
    }
  }

  private fun handleSnoozeReminder(context: Context, intent: Intent) {
    val reminderId = intent.getStringExtra(LocalReminderScheduler.EXTRA_REMINDER_ID) ?: return
    val notificationId = intent.getIntExtra(LocalReminderScheduler.EXTRA_NOTIFICATION_ID, 1001)
    NotificationManagerCompat.from(context).cancel(notificationId)
    InAppAlertHub.dismiss(reminderId)
    ReminderTriggerRegistry.clear(reminderId, context)

    val container = (context.applicationContext as? CadenceApp)?.container ?: return
    val wakeLock = ReminderWakeLock.acquire(context, "Cadence:ReminderSnoozeWakeLock", 20_000L)
    val pendingResult = goAsync()
    CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
      try {
        val reminder = container.database.reminderDao().getReminderById(reminderId)
        if (reminder != null) {
          container.taskRepository.snoozeReminder(reminder, 10)
        }
      } finally {
        ReminderWakeLock.release(wakeLock)
        pendingResult?.finish()
      }
    }
  }
}
