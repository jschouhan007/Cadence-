package com.example

import android.app.Application
import com.example.data.local.CadenceDatabase
import com.example.data.local.entity.TaskStatus
import com.example.data.preferences.UserPreferencesRepository
import com.example.data.repository.CategoryRepository
import com.example.data.repository.FocusRepository
import com.example.data.repository.TaskRepository
import com.example.domain.reminder.ActiveAlertEvent
import com.example.domain.reminder.InAppAlertHub
import com.example.domain.reminder.LocalReminderScheduler
import com.example.domain.reminder.ReminderNotificationPoster
import com.example.domain.reminder.ReminderScheduler
import com.example.domain.reminder.ReminderTriggerRegistry
import com.example.domain.sync.CloudSyncGateway
import com.example.domain.sync.LocalOnlySyncGateway
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class AppContainer(application: Application) {
  val database: CadenceDatabase by lazy { CadenceDatabase.create(application) }

  val preferencesRepository: UserPreferencesRepository by lazy {
    UserPreferencesRepository(application)
  }

  val reminderScheduler: ReminderScheduler by lazy {
    LocalReminderScheduler(
      context = application,
      reminderDao = database.reminderDao(),
      taskDao = database.taskDao(),
    )
  }

  val categoryRepository: CategoryRepository by lazy {
    CategoryRepository(
      db = database,
      categoryDao = database.categoryDao(),
      taskDao = database.taskDao(),
    )
  }

  val taskRepository: TaskRepository by lazy {
    TaskRepository(
      db = database,
      taskDao = database.taskDao(),
      tagDao = database.tagDao(),
      reminderDao = database.reminderDao(),
      historyDao = database.taskHistoryDao(),
      reminderScheduler = reminderScheduler,
      onTasksChanged = { com.example.widget.CadenceBubbleWidgetProvider.notifyDataChanged(application) },
    )
  }

  val focusRepository: FocusRepository by lazy {
    FocusRepository(
      db = database,
      focusSessionDao = database.focusSessionDao(),
      taskDao = database.taskDao(),
    )
  }

  val cloudSyncGateway: CloudSyncGateway by lazy { LocalOnlySyncGateway() }
}

class CadenceApp : Application() {
  lateinit var container: AppContainer
    private set

  private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

  override fun onCreate() {
    super.onCreate()
    container = AppContainer(this)
    appScope.launch {
      container.reminderScheduler.reconcileMissedOnSystemEvent()
    }
    startForegroundAlertWatcher()
  }

  private fun startForegroundAlertWatcher() {
    appScope.launch {
      container.database.reminderDao().observeAllReminders().collectLatest { reminders ->
        while (true) {
          val now = System.currentTimeMillis()
          val active =
            reminders
              .filter { it.isEnabled && !it.isDismissed }
              .sortedBy { it.triggerAt }

          val dueWindowStart = now - 15 * 60_000L
          val dueNow = active.filter { it.triggerAt in dueWindowStart..now }
          for (rem in dueNow) {
            val task = container.database.taskDao().getTaskById(rem.taskId)
            if (task != null && task.status != TaskStatus.COMPLETED && task.status != TaskStatus.CANCELLED) {
              if (ReminderTriggerRegistry.markTriggeredIfNew(rem.id, rem.triggerAt, this@CadenceApp)) {
                val wakeLock =
                  com.example.domain.reminder.ReminderWakeLock.acquire(
                    this@CadenceApp,
                    "Cadence:ForegroundWatcherWakeLock",
                    5_000L,
                  )
                try {
                  ReminderNotificationPoster.postTaskAlertNotification(
                    context = this@CadenceApp,
                    reminderId = rem.id,
                    taskId = task.id,
                    taskTitle = task.title,
                    notificationId = rem.notificationId,
                    triggerAt = rem.triggerAt,
                  )
                  InAppAlertHub.enqueue(
                    ActiveAlertEvent(
                      reminderId = rem.id,
                      taskId = task.id,
                      taskTitle = task.title,
                      notificationId = rem.notificationId,
                      triggerAt = rem.triggerAt,
                    )
                  )
                } finally {
                  com.example.domain.reminder.ReminderWakeLock.release(wakeLock)
                }
              }
            }
          }

          val nextFuture = active.firstOrNull { it.triggerAt > now } ?: break
          val waitMs = (nextFuture.triggerAt - System.currentTimeMillis()).coerceAtLeast(250L)
          delay(waitMs)
        }
      }
    }
  }
}
