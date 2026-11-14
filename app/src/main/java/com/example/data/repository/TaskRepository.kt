package com.example.data.repository

import androidx.room.withTransaction
import com.example.data.local.CadenceDatabase
import com.example.data.local.dao.ReminderDao
import com.example.data.local.dao.TagDao
import com.example.data.local.dao.TaskDao
import com.example.data.local.dao.TaskHistoryDao
import com.example.data.local.entity.RecurrenceFrequency
import com.example.data.local.entity.ReminderEntity
import com.example.data.local.entity.SubtaskEntity
import com.example.data.local.entity.TagEntity
import com.example.data.local.entity.TaskEntity
import com.example.data.local.entity.TaskHistoryEntity
import com.example.data.local.entity.TaskPriority
import com.example.data.local.entity.TaskStatus
import com.example.data.local.entity.TaskTagCrossRef
import com.example.data.local.entity.TaskWithDetails
import com.example.domain.model.TaskDraft
import com.example.domain.reminder.InAppAlertHub
import com.example.domain.reminder.ReminderScheduler
import com.example.domain.reminder.ReminderTriggerRegistry
import java.util.UUID
import kotlinx.coroutines.flow.Flow

class TaskRepository(
  private val db: CadenceDatabase,
  private val taskDao: TaskDao,
  private val tagDao: TagDao,
  private val reminderDao: ReminderDao,
  private val historyDao: TaskHistoryDao,
  private val reminderScheduler: ReminderScheduler,
  private val onTasksChanged: () -> Unit = {},
) {
  fun observeAllActiveTasks(): Flow<List<TaskWithDetails>> =
    taskDao.observeAllActiveTasksWithDetails()

  fun observeAllTasks(): Flow<List<TaskWithDetails>> = taskDao.observeAllTasksWithDetails()

  fun observeTaskDetails(taskId: String): Flow<TaskWithDetails?> =
    taskDao.observeTaskWithDetails(taskId)

  fun observeTaskHistory(taskId: String): Flow<List<TaskHistoryEntity>> =
    historyDao.observeHistoryForTask(taskId)

  fun observeAllTags(): Flow<List<TagEntity>> = tagDao.observeAllTags()

  fun observeAllReminders(): Flow<List<ReminderEntity>> = reminderDao.observeAllReminders()

  fun searchTasks(query: String): Flow<List<TaskWithDetails>> = taskDao.searchTasks(query.trim())

  suspend fun createQuickTask(
    title: String,
    categoryId: String? = null,
    priority: TaskPriority = TaskPriority.MEDIUM,
    dueAt: Long? = null,
    isAllDay: Boolean = true,
    estimatedMinutes: Int? = null,
    reminderAt: Long? = null,
  ): String {
    val trimmed = title.trim()
    require(trimmed.isNotEmpty()) { "Task title cannot be empty" }
    val now = System.currentTimeMillis()
    val id = UUID.randomUUID().toString()
    val sortOrder = taskDao.getNextSortOrder()
    val entity =
      TaskEntity(
        id = id,
        title = trimmed,
        categoryId = categoryId,
        priority = priority,
        status = TaskStatus.TODO,
        dueAt = dueAt,
        isAllDay = isAllDay,
        estimatedMinutes = estimatedMinutes,
        sortOrder = sortOrder,
        createdAt = now,
        updatedAt = now,
      )
    var reminderEntity: ReminderEntity? = null
    db.withTransaction {
      taskDao.upsertTask(entity)
      if (reminderAt != null) {
        val remId = UUID.randomUUID().toString()
        val notificationId = (remId.hashCode() and 0x7FFFFFFF).coerceAtLeast(1)
        reminderEntity =
          ReminderEntity(
            id = remId,
            taskId = id,
            triggerAt = reminderAt,
            offsetMinutes = -1,
            isEnabled = true,
            isDismissed = false,
            notificationId = notificationId,
            type = ReminderEntity.TYPE_CUSTOM_TIME,
          ).also { reminderDao.upsertReminder(it) }
      }
      historyDao.insertHistory(
        TaskHistoryEntity(
          id = UUID.randomUUID().toString(),
          taskId = id,
          action = "CREATED",
          summary = if (reminderAt != null) "Created task with scheduled alert" else "Created task",
          timestamp = now,
        )
      )
    }
    reminderEntity?.let { reminderScheduler.schedule(it, trimmed) }
    onTasksChanged()
    return id
  }

  suspend fun saveTask(draft: TaskDraft): String {
    val trimmed = draft.title.trim()
    require(trimmed.isNotEmpty()) { "Task title cannot be empty" }
    val now = System.currentTimeMillis()
    val existing = draft.id?.let { taskDao.getTaskById(it) }
    val id = existing?.id ?: UUID.randomUUID().toString()
    val sortOrder = existing?.sortOrder ?: taskDao.getNextSortOrder()
    val completedAt =
      when {
        draft.status == TaskStatus.COMPLETED && existing?.completedAt == null -> now
        draft.status != TaskStatus.COMPLETED -> null
        else -> existing?.completedAt
      }
    val entity =
      TaskEntity(
        id = id,
        title = trimmed,
        description = draft.description.trim(),
        categoryId = draft.categoryId,
        priority = draft.priority,
        status = draft.status,
        startAt = draft.startAt,
        dueAt = draft.dueAt,
        isAllDay = draft.isAllDay,
        estimatedMinutes = draft.estimatedMinutes,
        actualMinutes = existing?.actualMinutes ?: 0,
        sortOrder = sortOrder,
        notes = draft.notes.trim(),
        recurrenceFrequency = draft.recurrenceFrequency,
        recurrenceInterval = draft.recurrenceInterval.coerceAtLeast(1),
        completedAt = completedAt,
        archivedAt = existing?.archivedAt,
        createdAt = existing?.createdAt ?: now,
        updatedAt = now,
      )

    db.withTransaction {
      taskDao.upsertTask(entity)
      historyDao.insertHistory(
        TaskHistoryEntity(
          id = UUID.randomUUID().toString(),
          taskId = id,
          action = if (existing == null) "CREATED" else "UPDATED",
          summary = if (existing == null) "Created task" else "Updated task details",
          timestamp = now,
        )
      )
    }
    reminderScheduler.reconcileAll()
    onTasksChanged()
    return id
  }

  suspend fun toggleTaskCompletion(taskId: String): Boolean {
    val existing = taskDao.getTaskById(taskId) ?: return false
    val now = System.currentTimeMillis()
    val markCompleted = existing.status != TaskStatus.COMPLETED
    val newStatus = if (markCompleted) TaskStatus.COMPLETED else TaskStatus.TODO
    val completedAt = if (markCompleted) now else null
    db.withTransaction {
      taskDao.updateTaskStatus(
        taskId = taskId,
        status = newStatus,
        completedAt = completedAt,
        updatedAt = now,
      )
      historyDao.insertHistory(
        TaskHistoryEntity(
          id = UUID.randomUUID().toString(),
          taskId = taskId,
          action = if (markCompleted) "COMPLETED" else "REOPENED",
          summary = if (markCompleted) "Marked completed" else "Reopened task",
          timestamp = now,
        )
      )
    }
    reminderScheduler.reconcileAll()
    onTasksChanged()
    return markCompleted
  }

  suspend fun updateTaskStatus(taskId: String, status: TaskStatus) {
    val existing = taskDao.getTaskById(taskId) ?: return
    if (existing.status == status) return
    val now = System.currentTimeMillis()
    val completedAt = if (status == TaskStatus.COMPLETED) now else null
    db.withTransaction {
      taskDao.updateTaskStatus(taskId, status, completedAt, now)
      historyDao.insertHistory(
        TaskHistoryEntity(
          id = UUID.randomUUID().toString(),
          taskId = taskId,
          action = "STATUS_CHANGED",
          summary = "Status changed to ${status.label}",
          timestamp = now,
        )
      )
    }
    reminderScheduler.reconcileAll()
    onTasksChanged()
  }

  suspend fun moveTaskToCategory(taskId: String, targetCategoryId: String?) {
    val now = System.currentTimeMillis()
    db.withTransaction {
      taskDao.moveSingleTaskToCategory(taskId, targetCategoryId, now)
      historyDao.insertHistory(
        TaskHistoryEntity(
          id = UUID.randomUUID().toString(),
          taskId = taskId,
          action = "MOVED_CATEGORY",
          summary = if (targetCategoryId == null) "Moved to Inbox" else "Moved to another category",
          timestamp = now,
        )
      )
    }
    onTasksChanged()
  }

  suspend fun rescheduleTask(taskId: String, dueAt: Long?, isAllDay: Boolean = true) {
    val existing = taskDao.getTaskById(taskId) ?: return
    val now = System.currentTimeMillis()
    db.withTransaction {
      taskDao.updateTask(existing.copy(dueAt = dueAt, isAllDay = isAllDay, updatedAt = now))
      historyDao.insertHistory(
        TaskHistoryEntity(
          id = UUID.randomUUID().toString(),
          taskId = taskId,
          action = "RESCHEDULED",
          summary = if (dueAt == null) "Cleared due date" else "Updated due date",
          timestamp = now,
        )
      )
    }
    reminderScheduler.reconcileAll()
    onTasksChanged()
  }

  suspend fun reorderTasks(orderedTaskIds: List<String>) {
    val now = System.currentTimeMillis()
    db.withTransaction {
      orderedTaskIds.forEachIndexed { index, id ->
        taskDao.updateTaskSortOrder(taskId = id, sortOrder = index, updatedAt = now)
      }
    }
    onTasksChanged()
  }

  suspend fun duplicateTask(taskId: String): String? {
    val details = taskDao.getTaskWithDetails(taskId) ?: return null
    val now = System.currentTimeMillis()
    val newId = UUID.randomUUID().toString()
    val sortOrder = taskDao.getNextSortOrder()
    db.withTransaction {
      taskDao.upsertTask(
        details.task.copy(
          id = newId,
          title = "${details.task.title} (Copy)",
          status = TaskStatus.TODO,
          completedAt = null,
          sortOrder = sortOrder,
          createdAt = now,
          updatedAt = now,
        )
      )
      details.subtasks.forEachIndexed { idx, sub ->
        taskDao.upsertSubtask(
          sub.copy(
            id = UUID.randomUUID().toString(),
            taskId = newId,
            isCompleted = false,
            sortOrder = idx,
          )
        )
      }
      details.tags.forEach { tag ->
        tagDao.attachTagToTask(TaskTagCrossRef(taskId = newId, tagId = tag.id))
      }
      historyDao.insertHistory(
        TaskHistoryEntity(
          id = UUID.randomUUID().toString(),
          taskId = newId,
          action = "CREATED",
          summary = "Duplicated from \"${details.task.title}\"",
          timestamp = now,
        )
      )
    }
    return newId
  }

  suspend fun deleteTask(taskId: String) {
    val details = taskDao.getTaskWithDetails(taskId)
    details?.reminders?.forEach { reminder ->
      reminderScheduler.cancel(reminder.id, reminder.notificationId)
    }
    taskDao.deleteTaskById(taskId)
    onTasksChanged()
  }

  // Subtasks
  suspend fun addSubtask(taskId: String, title: String) {
    val trimmed = title.trim()
    if (trimmed.isEmpty()) return
    val nextOrder = taskDao.getNextSubtaskSortOrder(taskId)
    taskDao.upsertSubtask(
      SubtaskEntity(
        id = UUID.randomUUID().toString(),
        taskId = taskId,
        title = trimmed,
        isCompleted = false,
        sortOrder = nextOrder,
      )
    )
  }

  suspend fun toggleSubtask(subtaskId: String, isCompleted: Boolean) {
    taskDao.setSubtaskCompleted(subtaskId, isCompleted)
  }

  suspend fun deleteSubtask(subtaskId: String) {
    taskDao.deleteSubtaskById(subtaskId)
  }

  // Tags
  suspend fun addTagToTask(taskId: String, tagName: String, colorHex: Long = 0xFF1E5F74) {
    val trimmed = tagName.trim()
    if (trimmed.isEmpty()) return
    val existing = tagDao.findByName(trimmed)
    val tag =
      existing
        ?: TagEntity(
          id = UUID.randomUUID().toString(),
          name = trimmed,
          colorHex = colorHex,
        ).also { tagDao.insertTagIgnore(it) }
    tagDao.attachTagToTask(TaskTagCrossRef(taskId = taskId, tagId = tag.id))
  }

  suspend fun removeTagFromTask(taskId: String, tagId: String) {
    tagDao.detachTagFromTask(taskId, tagId)
  }

  // Reminders
  suspend fun addReminder(
    taskId: String,
    triggerAt: Long,
    offsetMinutes: Int,
    type: String = ReminderEntity.TYPE_TASK_DUE,
    isEnabled: Boolean = true,
  ) {
    val task = taskDao.getTaskById(taskId) ?: return
    val id = UUID.randomUUID().toString()
    val notificationId = (id.hashCode() and 0x7FFFFFFF).coerceAtLeast(1)
    val entity =
      ReminderEntity(
        id = id,
        taskId = taskId,
        triggerAt = triggerAt,
        offsetMinutes = offsetMinutes,
        isEnabled = isEnabled,
        isDismissed = false,
        notificationId = notificationId,
        type = type,
      )
    reminderDao.upsertReminder(entity)
    if (isEnabled) {
      reminderScheduler.schedule(entity, task.title)
    }
    onTasksChanged()
  }

  suspend fun addCustomReminder(taskId: String, triggerAt: Long, isEnabled: Boolean = true) {
    addReminder(
      taskId = taskId,
      triggerAt = triggerAt,
      offsetMinutes = -1,
      type = ReminderEntity.TYPE_CUSTOM_TIME,
      isEnabled = isEnabled,
    )
  }

  suspend fun updateReminderTime(
    reminder: ReminderEntity,
    newTriggerAt: Long,
    newOffsetMinutes: Int = -1,
    newType: String = ReminderEntity.TYPE_CUSTOM_TIME,
  ) {
    val updated =
      reminder.copy(
        triggerAt = newTriggerAt,
        offsetMinutes = newOffsetMinutes,
        type = newType,
        isEnabled = true,
        isDismissed = false,
      )
    ReminderTriggerRegistry.clear(reminder.id)
    reminderScheduler.clearTriggered(reminder.id)
    InAppAlertHub.dismiss(reminder.id)
    reminderDao.upsertReminder(updated)
    val task = taskDao.getTaskById(reminder.taskId) ?: return
    reminderScheduler.schedule(updated, task.title)
    onTasksChanged()
  }

  suspend fun toggleReminder(reminder: ReminderEntity, enabled: Boolean) {
    reminderDao.setReminderEnabled(reminder.id, enabled)
    val task = taskDao.getTaskById(reminder.taskId) ?: return
    if (enabled) {
      ReminderTriggerRegistry.clear(reminder.id)
      reminderScheduler.clearTriggered(reminder.id)
      reminderScheduler.schedule(reminder.copy(isEnabled = true), task.title)
    } else {
      reminderScheduler.cancel(reminder.id, reminder.notificationId)
    }
    onTasksChanged()
  }

  suspend fun dismissReminder(reminder: ReminderEntity) {
    reminderDao.setReminderDismissed(reminder.id, true)
    reminderScheduler.cancel(reminder.id, reminder.notificationId)
    onTasksChanged()
  }

  suspend fun snoozeReminder(reminder: ReminderEntity, minutes: Int) {
    val newTrigger = System.currentTimeMillis() + minutes * 60_000L
    ReminderTriggerRegistry.clear(reminder.id)
    reminderScheduler.clearTriggered(reminder.id)
    InAppAlertHub.dismiss(reminder.id)
    reminderDao.snoozeReminder(reminder.id, newTrigger)
    val task = taskDao.getTaskById(reminder.taskId) ?: return
    reminderScheduler.schedule(
      reminder.copy(triggerAt = newTrigger, isDismissed = false, isEnabled = true),
      task.title,
    )
    onTasksChanged()
  }

  suspend fun deleteReminder(reminder: ReminderEntity) {
    reminderScheduler.cancel(reminder.id, reminder.notificationId)
    reminderDao.deleteReminderById(reminder.id)
    onTasksChanged()
  }
}
