package com.example.ui.navigation

import kotlinx.serialization.Serializable

@Serializable object OnboardingRoute

@Serializable object MainGraphRoute

@Serializable object TodayRoute

@Serializable object TasksRoute

@Serializable object CalendarRoute

@Serializable object ProgressRoute

@Serializable data class CategoryDetailRoute(val categoryId: String)

@Serializable
data class TaskDetailRoute(
  val taskId: String? = null,
  val initialTitle: String = "",
  val initialCategoryId: String? = null,
  val initialPriorityName: String = "MEDIUM",
  val initialDueAt: Long? = null,
  val initialIsAllDay: Boolean = true,
  val initialReminderAt: Long? = null,
)

@Serializable object SearchRoute

@Serializable object SettingsRoute

@Serializable object ReminderCentreRoute

@Serializable data class FocusModeRoute(val taskId: String? = null)
