package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
  tableName = "task_history",
  foreignKeys =
    [
      ForeignKey(
        entity = TaskEntity::class,
        parentColumns = ["id"],
        childColumns = ["taskId"],
        onDelete = ForeignKey.CASCADE,
      )
    ],
  indices = [Index("taskId"), Index("timestamp")],
)
data class TaskHistoryEntity(
  @PrimaryKey val id: String,
  val taskId: String,
  val action: String,
  val summary: String,
  val timestamp: Long = System.currentTimeMillis(),
)

@Entity(
  tableName = "focus_sessions",
  foreignKeys =
    [
      ForeignKey(
        entity = TaskEntity::class,
        parentColumns = ["id"],
        childColumns = ["taskId"],
        onDelete = ForeignKey.SET_NULL,
      )
    ],
  indices = [Index("taskId"), Index("startedAt"), Index("status")],
)
data class FocusSessionEntity(
  @PrimaryKey val id: String,
  val taskId: String? = null,
  val taskTitleSnapshot: String = "",
  val presetName: String = "Pomodoro",
  val plannedMinutes: Int = 25,
  val startedAt: Long = System.currentTimeMillis(),
  val endedAt: Long? = null,
  val pausedAccumulatedMillis: Long = 0L,
  val pausedAt: Long? = null,
  val status: String = "RUNNING",
)

@Entity(tableName = "recurrence_rules")
data class RecurrenceRuleEntity(
  @PrimaryKey val id: String,
  val frequency: RecurrenceFrequency = RecurrenceFrequency.NONE,
  val interval: Int = 1,
  val selectedDaysCsv: String = "",
  val endAt: Long? = null,
  val maxOccurrences: Int? = null,
)
