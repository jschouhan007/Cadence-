package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
  tableName = "tasks",
  foreignKeys =
    [
      ForeignKey(
        entity = CategoryEntity::class,
        parentColumns = ["id"],
        childColumns = ["categoryId"],
        onDelete = ForeignKey.SET_NULL,
      )
    ],
  indices =
    [
      Index("categoryId"),
      Index("status"),
      Index("priority"),
      Index("dueAt"),
      Index("sortOrder"),
      Index("archivedAt"),
    ],
)
data class TaskEntity(
  @PrimaryKey val id: String,
  val title: String,
  val description: String = "",
  val categoryId: String? = null,
  val priority: TaskPriority = TaskPriority.MEDIUM,
  val status: TaskStatus = TaskStatus.TODO,
  val startAt: Long? = null,
  val dueAt: Long? = null,
  val isAllDay: Boolean = true,
  val estimatedMinutes: Int? = null,
  val actualMinutes: Int = 0,
  val sortOrder: Int = 0,
  val notes: String = "",
  val recurrenceFrequency: RecurrenceFrequency = RecurrenceFrequency.NONE,
  val recurrenceInterval: Int = 1,
  val recurrenceRuleId: String? = null,
  val completedAt: Long? = null,
  val archivedAt: Long? = null,
  val createdAt: Long = System.currentTimeMillis(),
  val updatedAt: Long = System.currentTimeMillis(),
)
