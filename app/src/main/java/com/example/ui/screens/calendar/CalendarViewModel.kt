package com.example.ui.screens.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.entity.CategoryEntity
import com.example.data.local.entity.TaskPriority
import com.example.data.local.entity.TaskStatus
import com.example.data.local.entity.TaskWithDetails
import com.example.data.repository.CategoryRepository
import com.example.data.repository.TaskRepository
import com.example.domain.time.DateTimeUtils
import java.util.Calendar
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class CalendarViewScope(val label: String) {
  MONTH("Month"),
  WEEK("Week"),
  DAY("Day"),
}

private data class CalendarControlState(
  val scope: CalendarViewScope = CalendarViewScope.MONTH,
  val selectedDateMillis: Long = DateTimeUtils.startOfDay(System.currentTimeMillis()),
  val displayedYear: Int = Calendar.getInstance().get(Calendar.YEAR),
  val displayedMonth: Int = Calendar.getInstance().get(Calendar.MONTH),
  val filterCategoryId: String? = null,
  val filterPriority: TaskPriority? = null,
)

data class CalendarUiState(
  val isLoading: Boolean = true,
  val scope: CalendarViewScope = CalendarViewScope.MONTH,
  val selectedDateMillis: Long = DateTimeUtils.startOfDay(System.currentTimeMillis()),
  val monthTitle: String = "",
  val monthGridDays: List<Long?> = emptyList(),
  val weekDays: List<Long> = emptyList(),
  val tasksForSelectedDay: List<TaskWithDetails> = emptyList(),
  val unscheduledTasks: List<TaskWithDetails> = emptyList(),
  val allFilteredTasks: List<TaskWithDetails> = emptyList(),
  val categories: List<CategoryEntity> = emptyList(),
  val filterCategoryId: String? = null,
  val filterPriority: TaskPriority? = null,
)

class CalendarViewModel(
  private val taskRepository: TaskRepository,
  private val categoryRepository: CategoryRepository,
) : ViewModel() {

  private val controls = MutableStateFlow(CalendarControlState())

  val uiState: StateFlow<CalendarUiState> =
    combine(
        taskRepository.observeAllActiveTasks(),
        categoryRepository.observeActiveCategories(),
        controls,
      ) { tasks, categories, ctrl ->
        val filtered =
          tasks.filter { item ->
            val catOk = ctrl.filterCategoryId == null || item.task.categoryId == ctrl.filterCategoryId
            val prioOk = ctrl.filterPriority == null || item.task.priority == ctrl.filterPriority
            catOk && prioOk
          }

        val dayTasks =
          filtered
            .filter { item ->
              item.task.dueAt != null && DateTimeUtils.isSameDay(item.task.dueAt, ctrl.selectedDateMillis)
            }
            .sortedWith(
              compareBy<TaskWithDetails> { it.task.isAllDay }
                .thenBy { it.task.dueAt ?: Long.MAX_VALUE }
                .thenByDescending { it.task.priority.weight }
            )

        val unscheduled =
          filtered.filter {
            it.task.dueAt == null &&
              it.task.status != TaskStatus.COMPLETED &&
              it.task.status != TaskStatus.CANCELLED
          }

        CalendarUiState(
          isLoading = false,
          scope = ctrl.scope,
          selectedDateMillis = ctrl.selectedDateMillis,
          monthTitle = DateTimeUtils.formatMonthYear(ctrl.displayedYear, ctrl.displayedMonth),
          monthGridDays = DateTimeUtils.getDaysInMonthGrid(ctrl.displayedYear, ctrl.displayedMonth),
          weekDays = DateTimeUtils.getWeekDays(ctrl.selectedDateMillis),
          tasksForSelectedDay = dayTasks,
          unscheduledTasks = unscheduled,
          allFilteredTasks = filtered,
          categories = categories,
          filterCategoryId = ctrl.filterCategoryId,
          filterPriority = ctrl.filterPriority,
        )
      }
      .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = CalendarUiState(),
      )

  fun setScope(scope: CalendarViewScope) {
    controls.update { it.copy(scope = scope) }
  }

  fun selectDay(dayMillis: Long) {
    val cal = Calendar.getInstance().apply { timeInMillis = dayMillis }
    controls.update {
      it.copy(
        selectedDateMillis = DateTimeUtils.startOfDay(dayMillis),
        displayedYear = cal.get(Calendar.YEAR),
        displayedMonth = cal.get(Calendar.MONTH),
      )
    }
  }

  fun shiftMonth(delta: Int) {
    controls.update { ctrl ->
      val cal = Calendar.getInstance().apply {
        set(Calendar.YEAR, ctrl.displayedYear)
        set(Calendar.MONTH, ctrl.displayedMonth)
        set(Calendar.DAY_OF_MONTH, 1)
        add(Calendar.MONTH, delta)
      }
      ctrl.copy(
        displayedYear = cal.get(Calendar.YEAR),
        displayedMonth = cal.get(Calendar.MONTH),
      )
    }
  }

  fun jumpToToday() {
    val now = System.currentTimeMillis()
    val cal = Calendar.getInstance().apply { timeInMillis = now }
    controls.update {
      it.copy(
        selectedDateMillis = DateTimeUtils.startOfDay(now),
        displayedYear = cal.get(Calendar.YEAR),
        displayedMonth = cal.get(Calendar.MONTH),
      )
    }
  }

  fun setCategoryFilter(categoryId: String?) {
    controls.update { it.copy(filterCategoryId = categoryId) }
  }

  fun setPriorityFilter(priority: TaskPriority?) {
    controls.update { it.copy(filterPriority = priority) }
  }

  fun toggleTaskCompletion(taskId: String) {
    viewModelScope.launch { taskRepository.toggleTaskCompletion(taskId) }
  }

  fun rescheduleTask(taskId: String, newDayMillis: Long) {
    viewModelScope.launch {
      taskRepository.rescheduleTask(taskId, DateTimeUtils.endOfDay(newDayMillis), isAllDay = true)
    }
  }

  fun deleteTask(taskId: String) {
    viewModelScope.launch { taskRepository.deleteTask(taskId) }
  }
}
