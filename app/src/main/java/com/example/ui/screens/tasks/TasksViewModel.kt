package com.example.ui.screens.tasks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.entity.CategoryEntity
import com.example.data.local.entity.SortDirection
import com.example.data.local.entity.TaskFilterSection
import com.example.data.local.entity.TaskPriority
import com.example.data.local.entity.TaskSortOption
import com.example.data.local.entity.TaskStatus
import com.example.data.local.entity.TaskWithDetails
import com.example.data.repository.CategoryRepository
import com.example.data.repository.TaskRepository
import com.example.domain.model.CategoryDeleteStrategy
import com.example.domain.model.CategoryWithStats
import com.example.domain.time.DateTimeUtils
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class TasksTabMode(val label: String) {
  TASKS("Tasks"),
  CATEGORIES("Categories"),
}

private data class TasksFilterState(
  val tabMode: TasksTabMode = TasksTabMode.TASKS,
  val section: TaskFilterSection = TaskFilterSection.INBOX,
  val selectedPriority: TaskPriority? = null,
  val sortOption: TaskSortOption = TaskSortOption.MANUAL,
  val sortDirection: SortDirection = SortDirection.ASCENDING,
  val isReorderMode: Boolean = false,
  val searchQuery: String = "",
)

data class TasksUiState(
  val isLoading: Boolean = true,
  val tabMode: TasksTabMode = TasksTabMode.TASKS,
  val section: TaskFilterSection = TaskFilterSection.INBOX,
  val selectedPriority: TaskPriority? = null,
  val sortOption: TaskSortOption = TaskSortOption.MANUAL,
  val sortDirection: SortDirection = SortDirection.ASCENDING,
  val isReorderMode: Boolean = false,
  val searchQuery: String = "",
  val tasks: List<TaskWithDetails> = emptyList(),
  val allSectionMatchCount: Int = 0,
  val inboxCount: Int = 0,
  val categoriesWithStats: List<CategoryWithStats> = emptyList(),
  val activeCategories: List<CategoryEntity> = emptyList(),
  val reminderBadgeCount: Int = 0,
)

@OptIn(ExperimentalCoroutinesApi::class)
class TasksViewModel(
  private val taskRepository: TaskRepository,
  private val categoryRepository: CategoryRepository,
) : ViewModel() {

  private val filterState = MutableStateFlow(TasksFilterState())

  private val searchedTasksFlow =
    filterState
      .map { it.searchQuery.trim() }
      .distinctUntilChanged()
      .flatMapLatest { trimmedQuery ->
        if (trimmedQuery.isBlank()) {
          taskRepository.observeAllActiveTasks()
        } else {
          taskRepository.searchTasks(trimmedQuery)
        }
      }

  val uiState: StateFlow<TasksUiState> =
    combine(
        taskRepository.observeAllActiveTasks(),
        searchedTasksFlow,
        categoryRepository.observeActiveCategoriesWithStats(),
        categoryRepository.observeActiveCategories(),
        filterState,
      ) { allTasks, candidateTasks, catStats, activeCategories, filters ->
        val now = System.currentTimeMillis()
        val trimmedQuery = filters.searchQuery.trim()
        val isSearching = trimmedQuery.isNotBlank()

        val inboxCount =
          allTasks.count {
            it.task.categoryId == null &&
              it.task.status != TaskStatus.COMPLETED &&
              it.task.status != TaskStatus.CANCELLED
          }

        val queryMatchedTasks =
          if (!isSearching) {
            candidateTasks
          } else {
            candidateTasks.filter { item ->
              item.task.title.contains(trimmedQuery, ignoreCase = true) ||
                item.task.description.contains(trimmedQuery, ignoreCase = true)
            }
          }

        val sectionFiltered =
          queryMatchedTasks.filter { item ->
            val task = item.task
            val isActive = task.status != TaskStatus.COMPLETED && task.status != TaskStatus.CANCELLED
            when (filters.section) {
              TaskFilterSection.INBOX -> isActive && task.categoryId == null
              TaskFilterSection.TODAY ->
                isActive && task.dueAt != null && DateTimeUtils.isDueToday(task.dueAt, now)
              TaskFilterSection.UPCOMING ->
                isActive && task.dueAt != null && DateTimeUtils.isUpcoming(task.dueAt, now)
              TaskFilterSection.OVERDUE ->
                isActive && DateTimeUtils.isOverdue(task.dueAt, task.isAllDay, now)
              TaskFilterSection.ALL -> isActive
              TaskFilterSection.COMPLETED -> task.status == TaskStatus.COMPLETED
            }
          }

        val priorityFiltered =
          if (filters.selectedPriority == null) sectionFiltered
          else sectionFiltered.filter { it.task.priority == filters.selectedPriority }

        val asc = filters.sortDirection == SortDirection.ASCENDING
        val sorted =
          when (filters.sortOption) {
            TaskSortOption.MANUAL ->
              if (asc) priorityFiltered.sortedBy { it.task.sortOrder }
              else priorityFiltered.sortedByDescending { it.task.sortOrder }
            TaskSortOption.DUE_DATE ->
              if (asc) {
                priorityFiltered.sortedWith(
                  compareBy<TaskWithDetails> { it.task.dueAt ?: Long.MAX_VALUE }
                    .thenByDescending { it.task.priority.weight }
                    .thenBy { it.task.sortOrder }
                )
              } else {
                priorityFiltered.sortedWith(
                  compareBy<TaskWithDetails> { it.task.dueAt == null }
                    .thenByDescending { it.task.dueAt ?: Long.MIN_VALUE }
                    .thenByDescending { it.task.priority.weight }
                    .thenBy { it.task.sortOrder }
                )
              }
            TaskSortOption.PRIORITY ->
              if (asc) {
                priorityFiltered.sortedWith(
                  compareBy<TaskWithDetails> { it.task.priority.weight }
                    .thenBy { it.task.sortOrder }
                    .thenBy { it.task.dueAt ?: Long.MAX_VALUE }
                )
              } else {
                priorityFiltered.sortedWith(
                  compareByDescending<TaskWithDetails> { it.task.priority.weight }
                    .thenBy { it.task.sortOrder }
                    .thenBy { it.task.dueAt ?: Long.MAX_VALUE }
                )
              }
            TaskSortOption.CREATED_DESC ->
              if (asc) {
                priorityFiltered.sortedBy { it.task.createdAt }
              } else {
                priorityFiltered.sortedByDescending { it.task.createdAt }
              }
            TaskSortOption.TITLE_ASC ->
              if (asc) {
                priorityFiltered.sortedWith(
                  compareBy<TaskWithDetails> { it.task.title.lowercase() }.thenBy { it.task.sortOrder }
                )
              } else {
                priorityFiltered.sortedWith(
                  compareByDescending<TaskWithDetails> { it.task.title.lowercase() }
                    .thenBy { it.task.sortOrder }
                )
              }
          }

        val allSectionMatchCount =
          if (!isSearching) 0
          else {
            queryMatchedTasks.count { item ->
              val isActive =
                item.task.status != TaskStatus.COMPLETED && item.task.status != TaskStatus.CANCELLED
              val priorityOk =
                filters.selectedPriority == null || item.task.priority == filters.selectedPriority
              isActive && priorityOk
            }
          }

        val badgeCount =
          allTasks.count {
            it.task.status != TaskStatus.COMPLETED &&
              it.task.status != TaskStatus.CANCELLED &&
              DateTimeUtils.isOverdue(it.task.dueAt, it.task.isAllDay, now)
          }

        TasksUiState(
          isLoading = false,
          tabMode = filters.tabMode,
          section = filters.section,
          selectedPriority = filters.selectedPriority,
          sortOption = filters.sortOption,
          sortDirection = filters.sortDirection,
          isReorderMode = filters.isReorderMode && !isSearching,
          searchQuery = filters.searchQuery,
          tasks = sorted,
          allSectionMatchCount = allSectionMatchCount,
          inboxCount = inboxCount,
          categoriesWithStats = catStats,
          activeCategories = activeCategories,
          reminderBadgeCount = badgeCount,
        )
      }
      .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = TasksUiState(),
      )

  fun setTabMode(mode: TasksTabMode) {
    filterState.update { it.copy(tabMode = mode, isReorderMode = false) }
  }

  fun setSection(section: TaskFilterSection) {
    filterState.update { it.copy(section = section) }
  }

  fun setSearchQuery(query: String) {
    filterState.update {
      it.copy(
        searchQuery = query,
        isReorderMode = if (query.isNotBlank()) false else it.isReorderMode,
      )
    }
  }

  fun clearSearchQuery() {
    setSearchQuery("")
  }

  fun setPriorityFilter(priority: TaskPriority?) {
    filterState.update { it.copy(selectedPriority = priority) }
  }

  fun setSortOption(option: TaskSortOption) {
    filterState.update { current ->
      val defaultDir =
        when (option) {
          TaskSortOption.PRIORITY, TaskSortOption.CREATED_DESC -> SortDirection.DESCENDING
          TaskSortOption.MANUAL, TaskSortOption.DUE_DATE, TaskSortOption.TITLE_ASC -> SortDirection.ASCENDING
        }
      current.copy(
        sortOption = option,
        sortDirection =
          if (current.sortOption == option) current.sortDirection else defaultDir,
        isReorderMode = if (option != TaskSortOption.MANUAL) false else current.isReorderMode,
      )
    }
  }

  fun setSortDirection(direction: SortDirection) {
    filterState.update { it.copy(sortDirection = direction) }
  }

  fun toggleSortDirection() {
    filterState.update { current ->
      val next =
        if (current.sortDirection == SortDirection.ASCENDING) SortDirection.DESCENDING
        else SortDirection.ASCENDING
      current.copy(sortDirection = next)
    }
  }

  fun toggleReorderMode() {
    filterState.update {
      if (it.searchQuery.isNotBlank()) it
      else {
        it.copy(
          sortOption = TaskSortOption.MANUAL,
          sortDirection = SortDirection.ASCENDING,
          isReorderMode = !it.isReorderMode,
        )
      }
    }
  }

  fun commitDragReorder(orderedTaskIds: List<String>) {
    if (orderedTaskIds.size <= 1) return
    val currentState = uiState.value
    val taskWeightMap = currentState.tasks.associate { it.task.id to it.task.priority.weight }
    val shouldSwitchToManual =
      when (currentState.sortOption) {
        TaskSortOption.MANUAL -> currentState.sortDirection != SortDirection.ASCENDING
        TaskSortOption.PRIORITY -> {
          val weights = orderedTaskIds.mapNotNull { taskWeightMap[it] }
          val preservesPriorityOrder =
            if (currentState.sortDirection == SortDirection.DESCENDING) {
              weights.zipWithNext().all { (a, b) -> a >= b }
            } else {
              weights.zipWithNext().all { (a, b) -> a <= b }
            }
          !preservesPriorityOrder
        }
        TaskSortOption.DUE_DATE, TaskSortOption.CREATED_DESC, TaskSortOption.TITLE_ASC -> true
      }

    if (shouldSwitchToManual) {
      filterState.update {
        it.copy(
          sortOption = TaskSortOption.MANUAL,
          sortDirection = SortDirection.ASCENDING,
        )
      }
    }
    viewModelScope.launch { taskRepository.reorderTasks(orderedTaskIds) }
  }

  fun moveTask(taskId: String, moveUp: Boolean) {
    val list = uiState.value.tasks.map { it.task.id }.toMutableList()
    val index = list.indexOf(taskId)
    if (index == -1) return
    val target = if (moveUp) index - 1 else index + 1
    if (target !in list.indices) return
    val item = list.removeAt(index)
    list.add(target, item)
    viewModelScope.launch { taskRepository.reorderTasks(list) }
  }

  fun toggleTaskCompletion(taskId: String, onDone: (Boolean) -> Unit = {}) {
    viewModelScope.launch {
      val completed = taskRepository.toggleTaskCompletion(taskId)
      onDone(completed)
    }
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

  fun createCategory(name: String, description: String, iconKey: String, colorHex: Long) {
    viewModelScope.launch { categoryRepository.createCategory(name, description, iconKey, colorHex) }
  }

  fun updateCategory(
    categoryId: String,
    name: String,
    description: String,
    iconKey: String,
    colorHex: Long,
  ) {
    viewModelScope.launch {
      categoryRepository.updateCategory(categoryId, name, description, iconKey, colorHex)
    }
  }

  fun togglePinCategory(categoryId: String) {
    viewModelScope.launch { categoryRepository.togglePin(categoryId) }
  }

  fun archiveCategory(categoryId: String) {
    viewModelScope.launch { categoryRepository.setArchived(categoryId, true) }
  }

  fun moveCategory(categoryId: String, moveUp: Boolean) {
    val ids = uiState.value.categoriesWithStats.map { it.category.id }.toMutableList()
    val index = ids.indexOf(categoryId)
    if (index == -1) return
    val target = if (moveUp) index - 1 else index + 1
    if (target !in ids.indices) return
    val item = ids.removeAt(index)
    ids.add(target, item)
    viewModelScope.launch { categoryRepository.reorderCategories(ids) }
  }

  fun deleteCategorySafe(
    categoryId: String,
    strategy: CategoryDeleteStrategy,
    targetCategoryId: String?,
  ) {
    viewModelScope.launch {
      categoryRepository.deleteCategorySafe(categoryId, strategy, targetCategoryId)
    }
  }
}
