package com.example.ui.screens.progress

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.entity.FocusSessionEntity
import com.example.data.local.entity.TaskStatus
import com.example.data.preferences.UserPreferencesRepository
import com.example.data.repository.CategoryRepository
import com.example.data.repository.FocusRepository
import com.example.data.repository.TaskRepository
import com.example.domain.model.CategoryWithStats
import com.example.domain.model.DailyCompletionPoint
import com.example.domain.time.DateTimeUtils
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class ProgressUiState(
  val isLoading: Boolean = true,
  val totalTasks: Int = 0,
  val completedTasks: Int = 0,
  val pendingTasks: Int = 0,
  val overdueTasks: Int = 0,
  val completionPercent: Int = 0,
  val totalFocusMinutesThisWeek: Int = 0,
  val activeDayStreak: Int = 0,
  val showStreaks: Boolean = true,
  val lastSevenDays: List<DailyCompletionPoint> = emptyList(),
  val categoryStats: List<CategoryWithStats> = emptyList(),
  val recentFocusSessions: List<FocusSessionEntity> = emptyList(),
)

class ProgressViewModel(
  taskRepository: TaskRepository,
  categoryRepository: CategoryRepository,
  focusRepository: FocusRepository,
  preferencesRepository: UserPreferencesRepository,
) : ViewModel() {

  val uiState: StateFlow<ProgressUiState> =
    combine(
        taskRepository.observeAllActiveTasks(),
        categoryRepository.observeActiveCategoriesWithStats(),
        focusRepository.observeAllSessions(),
        preferencesRepository.preferences,
      ) { tasks, catStats, focusSessions, prefs ->
        val now = System.currentTimeMillis()
        val completed = tasks.count { it.task.status == TaskStatus.COMPLETED }
        val active = tasks.filter { it.task.status != TaskStatus.COMPLETED && it.task.status != TaskStatus.CANCELLED }
        val overdue = active.count { DateTimeUtils.isOverdue(it.task.dueAt, it.task.isAllDay, now) }
        val pct = if (tasks.isEmpty()) 0 else ((completed * 100f) / tasks.size).toInt()

        val dayFmt = SimpleDateFormat("EEE", Locale.getDefault())
        val last7 =
          (6 downTo 0).map { daysAgo ->
            val dayMillis = DateTimeUtils.startOfDay(DateTimeUtils.addDays(now, -daysAgo))
            val count =
              tasks.count { item ->
                item.task.status == TaskStatus.COMPLETED &&
                  item.task.completedAt != null &&
                  DateTimeUtils.isSameDay(item.task.completedAt, dayMillis)
              }
            DailyCompletionPoint(
              dayMillis = dayMillis,
              shortDayLabel = dayFmt.format(Date(dayMillis)).take(2),
              completedCount = count,
            )
          }

        // Non-judgmental streak: consecutive days (including today if >0, or ending yesterday)
        var streak = 0
        for (daysAgo in 0..30) {
          val dayMillis = DateTimeUtils.startOfDay(DateTimeUtils.addDays(now, -daysAgo))
          val hasCompletion =
            tasks.any {
              it.task.status == TaskStatus.COMPLETED &&
                it.task.completedAt != null &&
                DateTimeUtils.isSameDay(it.task.completedAt, dayMillis)
            }
          if (hasCompletion) {
            streak++
          } else if (daysAgo == 0) {
            continue
          } else {
            break
          }
        }

        val weekStart = DateTimeUtils.startOfDay(DateTimeUtils.addDays(now, -6))
        val focusMinutesWeek =
          focusSessions
            .filter { it.startedAt >= weekStart && (it.status == "COMPLETED" || it.status == "STOPPED") }
            .sumOf { session ->
              val end = session.endedAt ?: session.startedAt
              val activeMs = (end - session.startedAt - session.pausedAccumulatedMillis).coerceAtLeast(0L)
              (activeMs / 60_000L).toInt()
            }

        ProgressUiState(
          isLoading = false,
          totalTasks = tasks.size,
          completedTasks = completed,
          pendingTasks = active.size,
          overdueTasks = overdue,
          completionPercent = pct,
          totalFocusMinutesThisWeek = focusMinutesWeek,
          activeDayStreak = streak,
          showStreaks = prefs.showStreaksInProgress,
          lastSevenDays = last7,
          categoryStats = catStats,
          recentFocusSessions = focusSessions.take(5),
        )
      }
      .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ProgressUiState(),
      )
}
