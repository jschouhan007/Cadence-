package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "tags", indices = [Index(value = ["name"], unique = true)])
data class TagEntity(
  @PrimaryKey val id: String,
  val name: String,
  val colorHex: Long = 0xFF1E5F74,
)

@Entity(
  tableName = "task_tags",
  primaryKeys = ["taskId", "tagId"],
  foreignKeys =
    [
      ForeignKey(
        entity = TaskEntity::class,
        parentColumns = ["id"],
        childColumns = ["taskId"],
        onDelete = ForeignKey.CASCADE,
      ),
      ForeignKey(
        entity = TagEntity::class,
        parentColumns = ["id"],
        childColumns = ["tagId"],
        onDelete = ForeignKey.CASCADE,
      ),
    ],
  indices = [Index("taskId"), Index("tagId")],
)
data class TaskTagCrossRef(
  val taskId: String,
  val tagId: String,
)
