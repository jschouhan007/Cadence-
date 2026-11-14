package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entity.CategoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryDao {
  @Query("SELECT * FROM categories WHERE isArchived = 0 ORDER BY isPinned DESC, sortOrder ASC, createdAt ASC")
  fun observeActiveCategories(): Flow<List<CategoryEntity>>

  @Query("SELECT * FROM categories ORDER BY isArchived ASC, isPinned DESC, sortOrder ASC, createdAt ASC")
  fun observeAllCategories(): Flow<List<CategoryEntity>>

  @Query("SELECT * FROM categories WHERE id = :id")
  fun observeCategoryById(id: String): Flow<CategoryEntity?>

  @Query("SELECT * FROM categories WHERE id = :id")
  suspend fun getCategoryById(id: String): CategoryEntity?

  @Query("SELECT COUNT(*) FROM categories")
  suspend fun getCategoryCount(): Int

  @Query("SELECT COALESCE(MAX(sortOrder), -1) + 1 FROM categories")
  suspend fun getNextSortOrder(): Int

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsertCategory(category: CategoryEntity)

  @Insert(onConflict = OnConflictStrategy.IGNORE)
  suspend fun insertAllIgnore(categories: List<CategoryEntity>)

  @Update
  suspend fun updateCategory(category: CategoryEntity)

  @Update
  suspend fun updateCategories(categories: List<CategoryEntity>)

  @Query("UPDATE categories SET sortOrder = :sortOrder, updatedAt = :updatedAt WHERE id = :id")
  suspend fun updateSortOrder(id: String, sortOrder: Int, updatedAt: Long)

  @Query("DELETE FROM categories WHERE id = :id")
  suspend fun deleteCategoryById(id: String)
}
