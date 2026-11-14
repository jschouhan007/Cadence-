package com.example.data.local

import androidx.room.TypeConverter
import com.example.data.local.entity.RecurrenceFrequency
import com.example.data.local.entity.TaskPriority
import com.example.data.local.entity.TaskStatus

class CadenceTypeConverters {
  @TypeConverter
  fun fromPriority(value: TaskPriority): String = value.name

  @TypeConverter
  fun toPriority(value: String): TaskPriority =
    TaskPriority.entries.find { it.name == value } ?: TaskPriority.MEDIUM

  @TypeConverter
  fun fromStatus(value: TaskStatus): String = value.name

  @TypeConverter
  fun toStatus(value: String): TaskStatus =
    TaskStatus.entries.find { it.name == value } ?: TaskStatus.TODO

  @TypeConverter
  fun fromRecurrence(value: RecurrenceFrequency): String = value.name

  @TypeConverter
  fun toRecurrence(value: String): RecurrenceFrequency =
    RecurrenceFrequency.entries.find { it.name == value } ?: RecurrenceFrequency.NONE
}
