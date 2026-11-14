package com.example.data.local.entity

enum class TaskPriority(val label: String, val weight: Int) {
  LOW("Low", 1),
  MEDIUM("Medium", 2),
  HIGH("High", 3),
  URGENT("Urgent", 4),
}

enum class TaskStatus(val label: String) {
  TODO("To do"),
  IN_PROGRESS("In progress"),
  COMPLETED("Completed"),
  CANCELLED("Cancelled"),
}

enum class RecurrenceFrequency(val label: String) {
  NONE("Does not repeat"),
  DAILY("Daily"),
  WEEKLY("Weekly"),
  MONTHLY("Monthly"),
  YEARLY("Yearly"),
}

enum class CategoryViewMode(val label: String) {
  LIST("List"),
  BOARD("Board"),
  CALENDAR("Calendar"),
}

enum class SortDirection(val label: String, val shortLabel: String) {
  ASCENDING("Ascending", "Asc"),
  DESCENDING("Descending", "Desc"),
}

enum class TaskSortOption(val label: String) {
  MANUAL("Manual"),
  DUE_DATE("Due date"),
  PRIORITY("Priority"),
  CREATED_DESC("Created"),
  TITLE_ASC("Title"),
}

enum class TaskFilterSection(val label: String) {
  INBOX("Inbox"),
  TODAY("Today"),
  UPCOMING("Upcoming"),
  OVERDUE("Overdue"),
  ALL("All active"),
  COMPLETED("Completed"),
}
