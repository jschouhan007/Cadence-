package com.example.ui.screens.reminders

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.entity.ReminderEntity
import com.example.data.local.entity.TaskStatus
import com.example.data.local.entity.TaskWithDetails
import com.example.data.repository.TaskRepository
import com.example.domain.time.DateTimeUtils
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ReminderDisplayItem(
  val reminder: ReminderEntity,
  val task: TaskWithDetails,
)

data class ReminderCentreUiState(
  val isLoading: Boolean = true,
  val activeReminders: List<ReminderDisplayItem> = emptyList(),
  val dismissedReminders: List<ReminderDisplayItem> = emptyList(),
  val overdueTasks: List<TaskWithDetails> = emptyList(),
)

class ReminderCentreViewModel(
  private val taskRepository: TaskRepository,
) : ViewModel() {

  val uiState: StateFlow<ReminderCentreUiState> =
    combine(
        taskRepository.observeAllReminders(),
        taskRepository.observeAllActiveTasks(),
      ) { reminders, tasks ->
        val now = System.currentTimeMillis()
        val taskMap = tasks.associateBy { it.task.id }
        val paired =
          reminders.mapNotNull { rem ->
            val task = taskMap[rem.taskId] ?: return@mapNotNull null
            ReminderDisplayItem(rem, task)
          }
        val active =
          paired.filter {
            !it.reminder.isDismissed &&
              it.task.task.status != TaskStatus.COMPLETED &&
              it.task.task.status != TaskStatus.CANCELLED
          }
        val dismissed =
          paired.filter {
            it.reminder.isDismissed || it.task.task.status == TaskStatus.COMPLETED
          }
        val overdue =
          tasks.filter {
            it.task.status != TaskStatus.COMPLETED &&
              it.task.status != TaskStatus.CANCELLED &&
              DateTimeUtils.isOverdue(it.task.dueAt, it.task.isAllDay, now)
          }

        ReminderCentreUiState(
          isLoading = false,
          activeReminders = active,
          dismissedReminders = dismissed,
          overdueTasks = overdue,
        )
      }
      .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ReminderCentreUiState(),
      )

  fun snooze(reminder: ReminderEntity, minutes: Int) {
    viewModelScope.launch { taskRepository.snoozeReminder(reminder, minutes) }
  }

  fun dismiss(reminder: ReminderEntity) {
    viewModelScope.launch { taskRepository.dismissReminder(reminder) }
  }

  fun toggleEnabled(reminder: ReminderEntity, enabled: Boolean) {
    viewModelScope.launch { taskRepository.toggleReminder(reminder, enabled) }
  }

  fun toggleTaskCompletion(taskId: String) {
    viewModelScope.launch { taskRepository.toggleTaskCompletion(taskId) }
  }
}
