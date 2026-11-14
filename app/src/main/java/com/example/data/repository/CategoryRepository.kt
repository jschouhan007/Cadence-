package com.example.data.repository

import androidx.room.withTransaction
import com.example.data.local.CadenceDatabase
import com.example.data.local.dao.CategoryDao
import com.example.data.local.dao.TaskDao
import com.example.data.local.entity.CategoryEntity
import com.example.data.local.entity.CategoryViewMode
import com.example.data.local.entity.TaskStatus
import com.example.domain.model.CategoryDeleteStrategy
import com.example.domain.model.CategoryWithStats
import com.example.domain.model.StarterCategoryTemplates
import java.util.UUID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

class CategoryRepository(
  private val db: CadenceDatabase,
  private val categoryDao: CategoryDao,
  private val taskDao: TaskDao,
) {
  fun observeActiveCategories(): Flow<List<CategoryEntity>> = categoryDao.observeActiveCategories()

  fun observeAllCategories(): Flow<List<CategoryEntity>> = categoryDao.observeAllCategories()

  fun observeCategoryById(id: String): Flow<CategoryEntity?> = categoryDao.observeCategoryById(id)

  fun observeActiveCategoriesWithStats(): Flow<List<CategoryWithStats>> =
    combine(categoryDao.observeActiveCategories(), taskDao.observeAllActiveTasksWithDetails()) {
      categories,
      tasks ->
      val tasksByCategory = tasks.groupBy { it.task.categoryId }
      categories.map { category ->
        val catTasks = tasksByCategory[category.id].orEmpty()
        val completed = catTasks.count { it.task.status == TaskStatus.COMPLETED }
        val active = catTasks.count { it.task.status != TaskStatus.COMPLETED && it.task.status != TaskStatus.CANCELLED }
        CategoryWithStats(
          category = category,
          totalTasks = catTasks.size,
          activeTasks = active,
          completedTasks = completed,
        )
      }
    }

  suspend fun createCategory(
    name: String,
    description: String = "",
    iconKey: String = "folder",
    colorHex: Long = 0xFF1E5F74,
  ): String {
    val trimmed = name.trim()
    require(trimmed.isNotEmpty()) { "Category name cannot be empty" }
    val now = System.currentTimeMillis()
    val id = UUID.randomUUID().toString()
    val nextOrder = categoryDao.getNextSortOrder()
    categoryDao.upsertCategory(
      CategoryEntity(
        id = id,
        name = trimmed,
        description = description.trim(),
        iconKey = iconKey,
        colorHex = colorHex,
        sortOrder = nextOrder,
        createdAt = now,
        updatedAt = now,
      )
    )
    return id
  }

  suspend fun updateCategory(
    categoryId: String,
    name: String,
    description: String,
    iconKey: String,
    colorHex: Long,
  ) {
    val existing = categoryDao.getCategoryById(categoryId) ?: return
    val trimmed = name.trim()
    if (trimmed.isEmpty()) return
    categoryDao.updateCategory(
      existing.copy(
        name = trimmed,
        description = description.trim(),
        iconKey = iconKey,
        colorHex = colorHex,
        updatedAt = System.currentTimeMillis(),
      )
    )
  }

  suspend fun togglePin(categoryId: String) {
    val existing = categoryDao.getCategoryById(categoryId) ?: return
    categoryDao.updateCategory(
      existing.copy(isPinned = !existing.isPinned, updatedAt = System.currentTimeMillis())
    )
  }

  suspend fun setArchived(categoryId: String, archived: Boolean) {
    val existing = categoryDao.getCategoryById(categoryId) ?: return
    categoryDao.updateCategory(
      existing.copy(isArchived = archived, updatedAt = System.currentTimeMillis())
    )
  }

  suspend fun setPreferredViewMode(categoryId: String, viewMode: CategoryViewMode) {
    val existing = categoryDao.getCategoryById(categoryId) ?: return
    categoryDao.updateCategory(
      existing.copy(preferredViewMode = viewMode.name, updatedAt = System.currentTimeMillis())
    )
  }

  suspend fun reorderCategories(orderedIds: List<String>) {
    val now = System.currentTimeMillis()
    db.withTransaction {
      orderedIds.forEachIndexed { index, id ->
        categoryDao.updateSortOrder(id = id, sortOrder = index, updatedAt = now)
      }
    }
  }

  suspend fun deleteCategorySafe(
    categoryId: String,
    strategy: CategoryDeleteStrategy,
    targetCategoryId: String? = null,
  ) {
    val now = System.currentTimeMillis()
    db.withTransaction {
      when (strategy) {
        CategoryDeleteStrategy.MOVE_TO_INBOX -> {
          taskDao.moveTasksToCategory(fromCategoryId = categoryId, targetCategoryId = null, updatedAt = now)
        }
        CategoryDeleteStrategy.MOVE_TO_CATEGORY -> {
          taskDao.moveTasksToCategory(
            fromCategoryId = categoryId,
            targetCategoryId = targetCategoryId,
            updatedAt = now,
          )
        }
        CategoryDeleteStrategy.DELETE_TASKS -> {
          taskDao.deleteTasksByCategoryId(categoryId)
        }
      }
      categoryDao.deleteCategoryById(categoryId)
    }
  }

  suspend fun seedStarterCategoriesIfEmpty(selectedKeys: List<String>) {
    if (selectedKeys.isEmpty()) return
    if (categoryDao.getCategoryCount() > 0) return
    val now = System.currentTimeMillis()
    val templates = StarterCategoryTemplates.filter { it.key in selectedKeys }
    val entities =
      templates.mapIndexed { index, tpl ->
        CategoryEntity(
          id = "cat_${tpl.key}",
          name = tpl.name,
          description = tpl.description,
          iconKey = tpl.iconKey,
          colorHex = tpl.colorHex,
          sortOrder = index,
          createdAt = now,
          updatedAt = now,
        )
      }
    categoryDao.insertAllIgnore(entities)
  }
}
