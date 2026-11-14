package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.local.entity.FocusSessionEntity
import com.example.data.local.entity.ReminderEntity
import com.example.data.local.entity.TagEntity
import com.example.data.local.entity.TaskHistoryEntity
import com.example.data.local.entity.TaskTagCrossRef
import kotlinx.coroutines.flow.Flow

@Dao
interface TagDao {
  @Query("SELECT * FROM tags ORDER BY name ASC")
  fun observeAllTags(): Flow<List<TagEntity>>

  @Insert(onConflict = OnConflictStrategy.IGNORE)
  suspend fun insertTagIgnore(tag: TagEntity)

  @Query("SELECT * FROM tags WHERE LOWER(name) = LOWER(:name) LIMIT 1")
  suspend fun findByName(name: String): TagEntity?

  @Insert(onConflict = OnConflictStrategy.IGNORE)
  suspend fun attachTagToTask(crossRef: TaskTagCrossRef)

  @Query("DELETE FROM task_tags WHERE taskId = :taskId AND tagId = :tagId")
  suspend fun detachTagFromTask(taskId: String, tagId: String)

  @Query("DELETE FROM task_tags WHERE taskId = :taskId")
  suspend fun clearTagsForTask(taskId: String)
}

@Dao
interface ReminderDao {
  @Query("SELECT * FROM reminders ORDER BY triggerAt ASC")
  fun observeAllReminders(): Flow<List<ReminderEntity>>

  @Query("SELECT * FROM reminders WHERE taskId = :taskId ORDER BY triggerAt ASC")
  fun observeRemindersForTask(taskId: String): Flow<List<ReminderEntity>>

  @Query("SELECT * FROM reminders WHERE isEnabled = 1 AND isDismissed = 0 ORDER BY triggerAt ASC")
  suspend fun getActiveReminders(): List<ReminderEntity>

  @Query("SELECT * FROM reminders WHERE id = :reminderId LIMIT 1")
  suspend fun getReminderById(reminderId: String): ReminderEntity?

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsertReminder(reminder: ReminderEntity)

  @Query("UPDATE reminders SET isEnabled = :enabled WHERE id = :reminderId")
  suspend fun setReminderEnabled(reminderId: String, enabled: Boolean)

  @Query("UPDATE reminders SET isDismissed = :dismissed WHERE id = :reminderId")
  suspend fun setReminderDismissed(reminderId: String, dismissed: Boolean)

  @Query("UPDATE reminders SET triggerAt = :newTriggerAt, isDismissed = 0 WHERE id = :reminderId")
  suspend fun snoozeReminder(reminderId: String, newTriggerAt: Long)

  @Query("DELETE FROM reminders WHERE id = :reminderId")
  suspend fun deleteReminderById(reminderId: String)

  @Query("DELETE FROM reminders WHERE taskId = :taskId")
  suspend fun deleteRemindersForTask(taskId: String)
}

@Dao
interface TaskHistoryDao {
  @Query("SELECT * FROM task_history WHERE taskId = :taskId ORDER BY timestamp DESC")
  fun observeHistoryForTask(taskId: String): Flow<List<TaskHistoryEntity>>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertHistory(entry: TaskHistoryEntity)
}

@Dao
interface FocusSessionDao {
  @Query("SELECT * FROM focus_sessions ORDER BY startedAt DESC")
  fun observeAllSessions(): Flow<List<FocusSessionEntity>>

  @Query("SELECT * FROM focus_sessions WHERE status IN ('RUNNING', 'PAUSED') ORDER BY startedAt DESC LIMIT 1")
  fun observeActiveSession(): Flow<FocusSessionEntity?>

  @Query("SELECT * FROM focus_sessions WHERE id = :sessionId")
  suspend fun getSessionById(sessionId: String): FocusSessionEntity?

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsertSession(session: FocusSessionEntity)
}
