package com.example.domain.model

import com.example.data.local.entity.CategoryEntity
import com.example.data.local.entity.TaskWithDetails

data class CategoryWithStats(
  val category: CategoryEntity,
  val totalTasks: Int,
  val activeTasks: Int,
  val completedTasks: Int,
) {
  val completionRatio: Float
    get() = if (totalTasks == 0) 0f else completedTasks.toFloat() / totalTasks.toFloat()

  val completionPercent: Int
    get() = (completionRatio * 100).toInt()
}

enum class CategoryDeleteStrategy {
  MOVE_TO_INBOX,
  MOVE_TO_CATEGORY,
  DELETE_TASKS,
}

data class StarterCategoryTemplate(
  val key: String,
  val name: String,
  val description: String,
  val iconKey: String,
  val colorHex: Long,
)

val StarterCategoryTemplates =
  listOf(
    StarterCategoryTemplate(
      key = "college",
      name = "College",
      description = "Assignments, exams & labs",
      iconKey = "school",
      colorHex = 0xFF2357E8,
    ),
    StarterCategoryTemplate(
      key = "personal",
      name = "Personal",
      description = "Errands & daily life",
      iconKey = "person",
      colorHex = 0xFF10A37F,
    ),
    StarterCategoryTemplate(
      key = "projects",
      name = "Projects",
      description = "Builds, code & research",
      iconKey = "code",
      colorHex = 0xFFFF5D47,
    ),
    StarterCategoryTemplate(
      key = "learning",
      name = "Learning",
      description = "Courses, reading & skills",
      iconKey = "book",
      colorHex = 0xFF7C3AED,
    ),
    StarterCategoryTemplate(
      key = "fitness",
      name = "Fitness",
      description = "Workouts & movement",
      iconKey = "fitness",
      colorHex = 0xFFE5890A,
    ),
    StarterCategoryTemplate(
      key = "finance",
      name = "Finance",
      description = "Bills, budgets & payments",
      iconKey = "wallet",
      colorHex = 0xFF0284C7,
    ),
  )

data class ReminderItemUi(
  val reminderId: String,
  val taskId: String,
  val taskTitle: String,
  val categoryName: String?,
  val categoryColorHex: Long?,
  val triggerAt: Long,
  val offsetMinutes: Int,
  val isEnabled: Boolean,
  val isDismissed: Boolean,
  val isTaskCompleted: Boolean,
)

data class DailyCompletionPoint(
  val dayMillis: Long,
  val shortDayLabel: String,
  val completedCount: Int,
)

data class TaskDraft(
  val id: String? = null,
  val title: String = "",
  val description: String = "",
  val categoryId: String? = null,
  val priority: com.example.data.local.entity.TaskPriority = com.example.data.local.entity.TaskPriority.MEDIUM,
  val status: com.example.data.local.entity.TaskStatus = com.example.data.local.entity.TaskStatus.TODO,
  val startAt: Long? = null,
  val dueAt: Long? = null,
  val isAllDay: Boolean = true,
  val estimatedMinutes: Int? = null,
  val notes: String = "",
  val recurrenceFrequency: com.example.data.local.entity.RecurrenceFrequency = com.example.data.local.entity.RecurrenceFrequency.NONE,
  val recurrenceInterval: Int = 1,
)

data class PendingReminderDraft(
  val tempId: String,
  val triggerAt: Long,
  val offsetMinutes: Int,
  val type: String,
  val isEnabled: Boolean = true,
)

fun TaskWithDetails.completedSubtaskCount(): Int = subtasks.count { it.isCompleted }
