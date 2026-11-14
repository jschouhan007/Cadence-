package com.example.ui.screens.today

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.entity.CategoryEntity
import com.example.data.local.entity.FocusSessionEntity
import com.example.data.local.entity.TaskPriority
import com.example.data.local.entity.TaskStatus
import com.example.data.local.entity.TaskWithDetails
import com.example.data.preferences.UserPreferencesRepository
import com.example.data.repository.CategoryRepository
import com.example.data.repository.FocusRepository
import com.example.data.repository.TaskRepository
import com.example.domain.time.DateTimeUtils
import com.example.domain.time.GreetingPeriod
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class TodayUiState(
  val isLoading: Boolean = true,
  val greetingPeriod: GreetingPeriod = DateTimeUtils.greetingPeriod(),
  val formattedDate: String = DateTimeUtils.formatFullDate(System.currentTimeMillis()),
  val dailyGoal: Int = 5,
  val completedTodayCount: Int = 0,
  val remainingTodayCount: Int = 0,
  val overdueTasks: List<TaskWithDetails> = emptyList(),
  val dueTodayTasks: List<TaskWithDetails> = emptyList(),
  val upcomingTasks: List<TaskWithDetails> = emptyList(),
  val completedTodayTasks: List<TaskWithDetails> = emptyList(),
  val categories: List<CategoryEntity> = emptyList(),
  val activeFocusSession: FocusSessionEntity? = null,
  val reminderBadgeCount: Int = 0,
) {
  val totalTodayScope: Int
    get() = (completedTodayCount + remainingTodayCount).coerceAtLeast(dailyGoal)

  val progressFraction: Float
    get() {
      val denom = (completedTodayCount + remainingTodayCount).takeIf { it > 0 } ?: dailyGoal
      return if (denom <= 0) 0f else (completedTodayCount.toFloat() / denom.toFloat()).coerceIn(0f, 1f)
    }
}

class TodayViewModel(
  private val taskRepository: TaskRepository,
  private val categoryRepository: CategoryRepository,
  private val preferencesRepository: UserPreferencesRepository,
  private val focusRepository: FocusRepository,
) : ViewModel() {

  val uiState: StateFlow<TodayUiState> =
    combine(
        taskRepository.observeAllActiveTasks(),
        categoryRepository.observeActiveCategories(),
        preferencesRepository.preferences,
        focusRepository.observeActiveSession(),
      ) { tasks, categories, prefs, activeFocus ->
        val now = System.currentTimeMillis()
        val activeTasks =
          tasks.filter {
            it.task.status != TaskStatus.COMPLETED && it.task.status != TaskStatus.CANCELLED
          }
        val priorityComparator =
          compareByDescending<TaskWithDetails> { it.task.priority.weight }
            .thenBy { it.task.dueAt ?: Long.MAX_VALUE }
            .thenBy { it.task.sortOrder }

        val overdue =
          activeTasks
            .filter { DateTimeUtils.isOverdue(it.task.dueAt, it.task.isAllDay, now) }
            .sortedWith(priorityComparator)

        val dueToday =
          activeTasks
            .filter {
              !DateTimeUtils.isOverdue(it.task.dueAt, it.task.isAllDay, now) &&
                DateTimeUtils.isDueToday(it.task.dueAt, now)
            }
            .sortedWith(priorityComparator)

        val upcoming =
          activeTasks
            .filter { DateTimeUtils.isUpcoming(it.task.dueAt, now) }
            .sortedWith(compareBy<TaskWithDetails> { it.task.dueAt ?: Long.MAX_VALUE }.thenByDescending { it.task.priority.weight })
            .take(6)

        val completedToday =
          tasks
            .filter {
              it.task.status == TaskStatus.COMPLETED &&
                it.task.completedAt != null &&
                DateTimeUtils.isSameDay(it.task.completedAt, now)
            }
            .sortedByDescending { it.task.completedAt }

        val upcomingRemindersCount =
          tasks.sumOf { item ->
            if (item.task.status == TaskStatus.COMPLETED || item.task.status == TaskStatus.CANCELLED) 0
            else item.reminders.count { it.isEnabled && !it.isDismissed }
          }

        TodayUiState(
          isLoading = false,
          greetingPeriod = DateTimeUtils.greetingPeriod(now),
          formattedDate = DateTimeUtils.formatFullDate(now),
          dailyGoal = prefs.dailyTaskGoal,
          completedTodayCount = completedToday.size,
          remainingTodayCount = overdue.size + dueToday.size,
          overdueTasks = overdue,
          dueTodayTasks = dueToday,
          upcomingTasks = upcoming,
          completedTodayTasks = completedToday,
          categories = categories,
          activeFocusSession = activeFocus,
          reminderBadgeCount = upcomingRemindersCount + overdue.size,
        )
      }
      .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = TodayUiState(),
      )

  fun toggleTaskCompletion(taskId: String, onToggled: (Boolean) -> Unit = {}) {
    viewModelScope.launch {
      val nowCompleted = taskRepository.toggleTaskCompletion(taskId)
      onToggled(nowCompleted)
    }
  }

  fun createQuickTask(
    title: String,
    categoryId: String?,
    priority: TaskPriority,
    dueAt: Long?,
    isAllDay: Boolean,
  ) {
    viewModelScope.launch {
      taskRepository.createQuickTask(title, categoryId, priority, dueAt, isAllDay)
    }
  }

  fun createCategory(name: String, description: String, iconKey: String, colorHex: Long) {
    viewModelScope.launch { categoryRepository.createCategory(name, description, iconKey, colorHex) }
  }

  fun moveTaskToCategory(taskId: String, targetCategoryId: String?) {
    viewModelScope.launch { taskRepository.moveTaskToCategory(taskId, targetCategoryId) }
  }

  fun duplicateTask(taskId: String) {
    viewModelScope.launch { taskRepository.duplicateTask(taskId) }
  }

  fun deleteTask(taskId: String) {
    viewModelScope.launch { taskRepository.deleteTask(taskId) }
  }
}
