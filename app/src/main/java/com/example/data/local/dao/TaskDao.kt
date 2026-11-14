package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.data.local.entity.SubtaskEntity
import com.example.data.local.entity.TaskEntity
import com.example.data.local.entity.TaskStatus
import com.example.data.local.entity.TaskWithDetails
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {
  @Transaction
  @Query("SELECT * FROM tasks WHERE archivedAt IS NULL ORDER BY sortOrder ASC, createdAt DESC")
  fun observeAllActiveTasksWithDetails(): Flow<List<TaskWithDetails>>

  @Transaction
  @Query("SELECT * FROM tasks ORDER BY sortOrder ASC, createdAt DESC")
  fun observeAllTasksWithDetails(): Flow<List<TaskWithDetails>>

  @Transaction
  @Query("SELECT * FROM tasks WHERE id = :taskId")
  fun observeTaskWithDetails(taskId: String): Flow<TaskWithDetails?>

  @Transaction
  @Query("SELECT * FROM tasks WHERE id = :taskId")
  suspend fun getTaskWithDetails(taskId: String): TaskWithDetails?

  @Query("SELECT * FROM tasks WHERE id = :taskId")
  suspend fun getTaskById(taskId: String): TaskEntity?

  @Transaction
  @Query(
    """
    SELECT * FROM tasks
    WHERE archivedAt IS NULL AND status NOT IN ('COMPLETED', 'CANCELLED')
    ORDER BY
      CASE WHEN dueAt IS NULL THEN 1 ELSE 0 END ASC,
      dueAt ASC,
      sortOrder ASC
    """
  )
  suspend fun getPendingTasksWithDetails(): List<TaskWithDetails>

  @Query("SELECT COALESCE(MAX(sortOrder), -1) + 1 FROM tasks")
  suspend fun getNextSortOrder(): Int

  @Transaction
  @Query(
    """
    SELECT * FROM tasks
    WHERE archivedAt IS NULL
      AND (
        title LIKE '%' || :query || '%'
        OR description LIKE '%' || :query || '%'
        OR notes LIKE '%' || :query || '%'
        OR id IN (
          SELECT tt.taskId FROM task_tags tt
          INNER JOIN tags tg ON tt.tagId = tg.id
          WHERE tg.name LIKE '%' || :query || '%'
        )
        OR categoryId IN (
          SELECT c.id FROM categories c
          WHERE c.name LIKE '%' || :query || '%'
        )
      )
    ORDER BY
      CASE WHEN status = 'COMPLETED' THEN 1 ELSE 0 END ASC,
      updatedAt DESC
    """
  )
  fun searchTasks(query: String): Flow<List<TaskWithDetails>>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsertTask(task: TaskEntity)

  @Update
  suspend fun updateTask(task: TaskEntity)

  @Query("UPDATE tasks SET sortOrder = :sortOrder, updatedAt = :updatedAt WHERE id = :taskId")
  suspend fun updateTaskSortOrder(taskId: String, sortOrder: Int, updatedAt: Long)

  @Query("UPDATE tasks SET categoryId = :targetCategoryId, updatedAt = :updatedAt WHERE categoryId = :fromCategoryId")
  suspend fun moveTasksToCategory(fromCategoryId: String, targetCategoryId: String?, updatedAt: Long)

  @Query("UPDATE tasks SET categoryId = :targetCategoryId, updatedAt = :updatedAt WHERE id = :taskId")
  suspend fun moveSingleTaskToCategory(taskId: String, targetCategoryId: String?, updatedAt: Long)

  @Query("UPDATE tasks SET status = :status, completedAt = :completedAt, updatedAt = :updatedAt WHERE id = :taskId")
  suspend fun updateTaskStatus(taskId: String, status: TaskStatus, completedAt: Long?, updatedAt: Long)

  @Query("DELETE FROM tasks WHERE categoryId = :categoryId")
  suspend fun deleteTasksByCategoryId(categoryId: String)

  @Query("DELETE FROM tasks WHERE id = :taskId")
  suspend fun deleteTaskById(taskId: String)

  // Subtasks
  @Query("SELECT * FROM subtasks WHERE taskId = :taskId ORDER BY sortOrder ASC")
  fun observeSubtasksForTask(taskId: String): Flow<List<SubtaskEntity>>

  @Query("SELECT COALESCE(MAX(sortOrder), -1) + 1 FROM subtasks WHERE taskId = :taskId")
  suspend fun getNextSubtaskSortOrder(taskId: String): Int

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsertSubtask(subtask: SubtaskEntity)

  @Query("UPDATE subtasks SET isCompleted = :isCompleted WHERE id = :subtaskId")
  suspend fun setSubtaskCompleted(subtaskId: String, isCompleted: Boolean)

  @Query("DELETE FROM subtasks WHERE id = :subtaskId")
  suspend fun deleteSubtaskById(subtaskId: String)

  @Query("DELETE FROM subtasks WHERE taskId = :taskId")
  suspend fun deleteSubtasksForTask(taskId: String)
}
