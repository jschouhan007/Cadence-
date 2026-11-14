package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
  tableName = "categories",
  indices = [Index("sortOrder"), Index("isArchived")],
)
data class CategoryEntity(
  @PrimaryKey val id: String,
  val name: String,
  val description: String = "",
  val iconKey: String = "folder",
  val colorHex: Long = 0xFF1E5F74,
  val sortOrder: Int = 0,
  val isPinned: Boolean = false,
  val isArchived: Boolean = false,
  val preferredViewMode: String = CategoryViewMode.LIST.name,
  val createdAt: Long = System.currentTimeMillis(),
  val updatedAt: Long = System.currentTimeMillis(),
)
