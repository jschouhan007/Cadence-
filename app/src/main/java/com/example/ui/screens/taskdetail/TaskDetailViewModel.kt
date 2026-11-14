package com.example.ui.screens.taskdetail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.entity.CategoryEntity
import com.example.data.local.entity.RecurrenceFrequency
import com.example.data.local.entity.ReminderEntity
import com.example.data.local.entity.SubtaskEntity
import com.example.data.local.entity.TagEntity
import com.example.data.local.entity.TaskHistoryEntity
import com.example.data.local.entity.TaskPriority
import com.example.data.local.entity.TaskStatus
import com.example.data.repository.CategoryRepository
import com.example.data.repository.TaskRepository
import com.example.domain.model.PendingReminderDraft
import com.example.domain.model.TaskDraft
import java.util.UUID
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class TaskDetailUiState(
  val isLoading: Boolean = true,
  val taskId: String? = null,
  val draft: TaskDraft = TaskDraft(),
  val categories: List<CategoryEntity> = emptyList(),
  val subtasks: List<SubtaskEntity> = emptyList(),
  val tags: List<TagEntity> = emptyList(),
  val allTags: List<TagEntity> = emptyList(),
  val reminders: List<ReminderEntity> = emptyList(),
  val pendingReminders: List<PendingReminderDraft> = emptyList(),
  val history: List<TaskHistoryEntity> = emptyList(),
  val actualMinutes: Int = 0,
  val isSaving: Boolean = false,
) {
  val isNewTask: Boolean
    get() = taskId == null
}

class TaskDetailViewModel(
  initialTaskId: String?,
  initialTitle: String,
  initialCategoryId: String?,
  initialPriority: TaskPriority,
  initialDueAt: Long?,
  initialIsAllDay: Boolean = true,
  initialReminderAt: Long? = null,
  private val taskRepository: TaskRepository,
  private val categoryRepository: CategoryRepository,
) : ViewModel() {

  private val activeTaskId = MutableStateFlow(initialTaskId)
  private val draftState =
    MutableStateFlow(
      TaskDraft(
        id = initialTaskId,
        title = initialTitle,
        categoryId = initialCategoryId,
        priority = initialPriority,
        dueAt = initialDueAt,
        isAllDay = initialIsAllDay,
      )
    )
  private val pendingRemindersState =
    MutableStateFlow(
      if (initialTaskId == null && initialReminderAt != null) {
        listOf(
          PendingReminderDraft(
            tempId = UUID.randomUUID().toString(),
            triggerAt = initialReminderAt,
            offsetMinutes = -1,
            type = ReminderEntity.TYPE_CUSTOM_TIME,
            isEnabled = true,
          )
        )
      } else {
        emptyList()
      }
    )
  private val isInitialized = MutableStateFlow(initialTaskId == null)
  private val isSaving = MutableStateFlow(false)

  init {
    if (initialTaskId != null) {
      viewModelScope.launch {
        val existing = taskRepository.observeTaskDetails(initialTaskId).firstOrNull()
        if (existing != null) {
          draftState.value =
            TaskDraft(
              id = existing.task.id,
              title = existing.task.title,
              description = existing.task.description,
              categoryId = existing.task.categoryId,
              priority = existing.task.priority,
              status = existing.task.status,
              startAt = existing.task.startAt,
              dueAt = existing.task.dueAt,
              isAllDay = existing.task.isAllDay,
              estimatedMinutes = existing.task.estimatedMinutes,
              notes = existing.task.notes,
              recurrenceFrequency = existing.task.recurrenceFrequency,
              recurrenceInterval = existing.task.recurrenceInterval,
            )
        }
        isInitialized.value = true
      }
    }
  }

  private val detailsFlow =
    combine(
      activeTaskId,
      taskRepository.observeAllTasks(),
    ) { id, allTasks ->
      if (id == null) null else allTasks.find { it.task.id == id }
    }

  val uiState: StateFlow<TaskDetailUiState> =
    combine(
        draftState,
        categoryRepository.observeActiveCategories(),
        taskRepository.observeAllTags(),
        detailsFlow,
        isInitialized,
      ) { draft, categories, allTags, details, initialized ->
        TaskDetailUiState(
          isLoading = !initialized,
          taskId = activeTaskId.value,
          draft = draft,
          categories = categories,
          subtasks = details?.subtasks?.sortedBy { it.sortOrder }.orEmpty(),
          tags = details?.tags.orEmpty(),
          allTags = allTags,
          reminders = details?.reminders?.sortedBy { it.triggerAt }.orEmpty(),
          history = emptyList(),
          actualMinutes = details?.task?.actualMinutes ?: 0,
          isSaving = isSaving.value,
        )
      }
      .combine(pendingRemindersState) { state, pending ->
        state.copy(pendingReminders = pending.sortedBy { it.triggerAt })
      }
      .combine(
        taskRepository.observeTaskHistory(initialTaskId ?: "__none__")
      ) { state, initialHistory ->
        state.copy(history = initialHistory)
      }
      .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = TaskDetailUiState(),
      )

  fun updateTitle(title: String) {
    draftState.update { it.copy(title = title) }
  }

  fun updateDescription(description: String) {
    draftState.update { it.copy(description = description) }
  }

  fun updateCategory(categoryId: String?) {
    draftState.update { it.copy(categoryId = categoryId) }
  }

  fun updatePriority(priority: TaskPriority) {
    draftState.update { it.copy(priority = priority) }
  }

  fun updateStatus(status: TaskStatus) {
    draftState.update { it.copy(status = status) }
  }

  fun updateStartAt(startAt: Long?) {
    draftState.update { it.copy(startAt = startAt) }
  }

  fun updateDueAt(dueAt: Long?, isAllDay: Boolean = draftState.value.isAllDay) {
    draftState.update { it.copy(dueAt = dueAt, isAllDay = isAllDay) }
  }

  fun updateAllDay(isAllDay: Boolean) {
    draftState.update { it.copy(isAllDay = isAllDay) }
  }

  fun updateEstimatedMinutes(minutes: Int?) {
    draftState.update { it.copy(estimatedMinutes = minutes) }
  }

  fun updateNotes(notes: String) {
    draftState.update { it.copy(notes = notes) }
  }

  fun updateRecurrence(frequency: RecurrenceFrequency) {
    draftState.update { it.copy(recurrenceFrequency = frequency) }
  }

  fun saveTask(onSaved: (String) -> Unit = {}) {
    val currentDraft = draftState.value
    if (currentDraft.title.isBlank() || isSaving.value) return
    isSaving.value = true
    viewModelScope.launch {
      val savedId = taskRepository.saveTask(currentDraft.copy(id = activeTaskId.value))
      val pendingToPersist = pendingRemindersState.value
      if (pendingToPersist.isNotEmpty()) {
        pendingToPersist.forEach { pending ->
          taskRepository.addReminder(
            taskId = savedId,
            triggerAt = pending.triggerAt,
            offsetMinutes = pending.offsetMinutes,
            type = pending.type,
            isEnabled = pending.isEnabled,
          )
        }
        pendingRemindersState.value = emptyList()
      }
      activeTaskId.value = savedId
      draftState.update { it.copy(id = savedId) }
      isSaving.value = false
      onSaved(savedId)
    }
  }

  private suspend fun ensureTaskSavedForChildItem(): String? {
    val existingId = activeTaskId.value
    if (existingId != null) return existingId
    val draft = draftState.value
    if (draft.title.isBlank()) return null
    val savedId = taskRepository.saveTask(draft)
    val pendingToPersist = pendingRemindersState.value
    if (pendingToPersist.isNotEmpty()) {
      pendingToPersist.forEach { pending ->
        taskRepository.addReminder(
          taskId = savedId,
          triggerAt = pending.triggerAt,
          offsetMinutes = pending.offsetMinutes,
          type = pending.type,
          isEnabled = pending.isEnabled,
        )
      }
      pendingRemindersState.value = emptyList()
    }
    activeTaskId.value = savedId
    draftState.update { it.copy(id = savedId) }
    return savedId
  }

  fun addSubtask(title: String) {
    if (title.isBlank()) return
    viewModelScope.launch {
      val id = ensureTaskSavedForChildItem() ?: return@launch
      taskRepository.addSubtask(id, title)
    }
  }

  fun toggleSubtask(subtaskId: String, isCompleted: Boolean) {
    viewModelScope.launch { taskRepository.toggleSubtask(subtaskId, isCompleted) }
  }

  fun deleteSubtask(subtaskId: String) {
    viewModelScope.launch { taskRepository.deleteSubtask(subtaskId) }
  }

  fun addTag(tagName: String) {
    if (tagName.isBlank()) return
    viewModelScope.launch {
      val id = ensureTaskSavedForChildItem() ?: return@launch
      taskRepository.addTagToTask(id, tagName)
    }
  }

  fun removeTag(tagId: String) {
    val id = activeTaskId.value ?: return
    viewModelScope.launch { taskRepository.removeTagFromTask(id, tagId) }
  }

  fun addReminderOffset(offsetMinutes: Int) {
    val baseTime = draftState.value.dueAt ?: (System.currentTimeMillis() + 60 * 60_000L)
    val triggerAt = (baseTime - offsetMinutes * 60_000L).coerceAtLeast(System.currentTimeMillis() + 60_000L)
    val existingId = activeTaskId.value
    if (existingId == null) {
      pendingRemindersState.update { current ->
        current +
          PendingReminderDraft(
            tempId = UUID.randomUUID().toString(),
            triggerAt = triggerAt,
            offsetMinutes = offsetMinutes,
            type = ReminderEntity.TYPE_TASK_DUE,
            isEnabled = true,
          )
      }
      return
    }
    viewModelScope.launch {
      taskRepository.addReminder(existingId, triggerAt, offsetMinutes, ReminderEntity.TYPE_TASK_DUE)
    }
  }

  fun addCustomReminder(triggerAt: Long) {
    val existingId = activeTaskId.value
    if (existingId == null) {
      pendingRemindersState.update { current ->
        current +
          PendingReminderDraft(
            tempId = UUID.randomUUID().toString(),
            triggerAt = triggerAt,
            offsetMinutes = -1,
            type = ReminderEntity.TYPE_CUSTOM_TIME,
            isEnabled = true,
          )
      }
      return
    }
    viewModelScope.launch {
      taskRepository.addCustomReminder(existingId, triggerAt)
    }
  }

  fun updateReminderTime(reminder: ReminderEntity, newTriggerAt: Long) {
    viewModelScope.launch {
      taskRepository.updateReminderTime(reminder, newTriggerAt)
    }
  }

  fun updatePendingReminderTime(tempId: String, newTriggerAt: Long) {
    pendingRemindersState.update { list ->
      list.map {
        if (it.tempId == tempId) {
          it.copy(
            triggerAt = newTriggerAt,
            offsetMinutes = -1,
            type = ReminderEntity.TYPE_CUSTOM_TIME,
            isEnabled = true,
          )
        } else it
      }
    }
  }

  fun togglePendingReminder(tempId: String, enabled: Boolean) {
    pendingRemindersState.update { list ->
      list.map { if (it.tempId == tempId) it.copy(isEnabled = enabled) else it }
    }
  }

  fun deletePendingReminder(tempId: String) {
    pendingRemindersState.update { list -> list.filterNot { it.tempId == tempId } }
  }

  fun toggleReminder(reminder: ReminderEntity, enabled: Boolean) {
    viewModelScope.launch { taskRepository.toggleReminder(reminder, enabled) }
  }

  fun deleteReminder(reminder: ReminderEntity) {
    viewModelScope.launch { taskRepository.deleteReminder(reminder) }
  }

  fun duplicateTask(onDuplicated: (String) -> Unit) {
    val id = activeTaskId.value ?: return
    viewModelScope.launch {
      val newId = taskRepository.duplicateTask(id)
      if (newId != null) onDuplicated(newId)
    }
  }

  fun deleteTask(onDeleted: () -> Unit) {
    val id = activeTaskId.value ?: return
    viewModelScope.launch {
      taskRepository.deleteTask(id)
      onDeleted()
    }
  }
}
