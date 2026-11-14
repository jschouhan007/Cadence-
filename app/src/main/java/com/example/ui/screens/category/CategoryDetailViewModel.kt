package com.example.ui.screens.category

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.entity.CategoryEntity
import com.example.data.local.entity.CategoryViewMode
import com.example.data.local.entity.TaskStatus
import com.example.data.local.entity.TaskWithDetails
import com.example.data.repository.CategoryRepository
import com.example.data.repository.TaskRepository
import com.example.domain.model.CategoryDeleteStrategy
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class CategoryDetailUiState(
  val isLoading: Boolean = true,
  val category: CategoryEntity? = null,
  val tasks: List<TaskWithDetails> = emptyList(),
  val otherCategories: List<CategoryEntity> = emptyList(),
  val viewMode: CategoryViewMode = CategoryViewMode.LIST,
  val isReorderMode: Boolean = false,
) {
  val completedCount: Int
    get() = tasks.count { it.task.status == TaskStatus.COMPLETED }

  val completionPercent: Int
    get() = if (tasks.isEmpty()) 0 else ((completedCount * 100f) / tasks.size).toInt()
}

class CategoryDetailViewModel(
  private val categoryId: String,
  private val categoryRepository: CategoryRepository,
  private val taskRepository: TaskRepository,
) : ViewModel() {

  private val reorderMode = MutableStateFlow(false)

  val uiState: StateFlow<CategoryDetailUiState> =
    combine(
        categoryRepository.observeCategoryById(categoryId),
        taskRepository.observeAllActiveTasks(),
        categoryRepository.observeActiveCategories(),
        reorderMode,
      ) { category, allTasks, activeCategories, isReorder ->
        val catTasks =
          allTasks
            .filter { it.task.categoryId == categoryId }
            .sortedBy { it.task.sortOrder }
        val mode =
          CategoryViewMode.entries.find { it.name == category?.preferredViewMode }
            ?: CategoryViewMode.LIST

        CategoryDetailUiState(
          isLoading = false,
          category = category,
          tasks = catTasks,
          otherCategories = activeCategories.filter { it.id != categoryId },
          viewMode = mode,
          isReorderMode = isReorder,
        )
      }
      .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = CategoryDetailUiState(),
      )

  fun setViewMode(mode: CategoryViewMode) {
    if (mode != CategoryViewMode.LIST) reorderMode.value = false
    viewModelScope.launch { categoryRepository.setPreferredViewMode(categoryId, mode) }
  }

  fun toggleReorderMode() {
    reorderMode.value = !reorderMode.value
  }

  fun moveTask(taskId: String, moveUp: Boolean) {
    val ids = uiState.value.tasks.map { it.task.id }.toMutableList()
    val index = ids.indexOf(taskId)
    if (index == -1) return
    val target = if (moveUp) index - 1 else index + 1
    if (target !in ids.indices) return
    val item = ids.removeAt(index)
    ids.add(target, item)
    viewModelScope.launch { taskRepository.reorderTasks(ids) }
  }

  fun toggleTaskCompletion(taskId: String) {
    viewModelScope.launch { taskRepository.toggleTaskCompletion(taskId) }
  }

  fun updateTaskStatus(taskId: String, status: TaskStatus) {
    viewModelScope.launch { taskRepository.updateTaskStatus(taskId, status) }
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

  fun updateCategory(name: String, description: String, iconKey: String, colorHex: Long) {
    viewModelScope.launch {
      categoryRepository.updateCategory(categoryId, name, description, iconKey, colorHex)
    }
  }

  fun deleteCategorySafe(
    strategy: CategoryDeleteStrategy,
    targetCategoryId: String?,
    onDeleted: () -> Unit,
  ) {
    viewModelScope.launch {
      categoryRepository.deleteCategorySafe(categoryId, strategy, targetCategoryId)
      onDeleted()
    }
  }
}
