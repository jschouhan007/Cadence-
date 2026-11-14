package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
  tableName = "reminders",
  foreignKeys =
    [
      ForeignKey(
        entity = TaskEntity::class,
        parentColumns = ["id"],
        childColumns = ["taskId"],
        onDelete = ForeignKey.CASCADE,
      )
    ],
  indices = [Index("taskId"), Index("triggerAt"), Index("isEnabled")],
)
data class ReminderEntity(
  @PrimaryKey val id: String,
  val taskId: String,
  val triggerAt: Long,
  val offsetMinutes: Int = 0,
  val isEnabled: Boolean = true,
  val isDismissed: Boolean = false,
  val notificationId: Int,
  val type: String = TYPE_TASK_DUE,
) {
  companion object {
    const val TYPE_TASK_DUE = "TASK_DUE"
    const val TYPE_CUSTOM_TIME = "CUSTOM_TIME"
  }
}
