package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.local.dao.CategoryDao
import com.example.data.local.dao.FocusSessionDao
import com.example.data.local.dao.ReminderDao
import com.example.data.local.dao.TagDao
import com.example.data.local.dao.TaskDao
import com.example.data.local.dao.TaskHistoryDao
import com.example.data.local.entity.CategoryEntity
import com.example.data.local.entity.FocusSessionEntity
import com.example.data.local.entity.RecurrenceRuleEntity
import com.example.data.local.entity.ReminderEntity
import com.example.data.local.entity.SubtaskEntity
import com.example.data.local.entity.TagEntity
import com.example.data.local.entity.TaskEntity
import com.example.data.local.entity.TaskHistoryEntity
import com.example.data.local.entity.TaskTagCrossRef

@Database(
  entities =
    [
      CategoryEntity::class,
      TaskEntity::class,
      SubtaskEntity::class,
      ReminderEntity::class,
      RecurrenceRuleEntity::class,
      TagEntity::class,
      TaskTagCrossRef::class,
      TaskHistoryEntity::class,
      FocusSessionEntity::class,
    ],
  version = 1,
  exportSchema = false,
)
@TypeConverters(CadenceTypeConverters::class)
abstract class CadenceDatabase : RoomDatabase() {
  abstract fun categoryDao(): CategoryDao

  abstract fun taskDao(): TaskDao

  abstract fun tagDao(): TagDao

  abstract fun reminderDao(): ReminderDao

  abstract fun taskHistoryDao(): TaskHistoryDao

  abstract fun focusSessionDao(): FocusSessionDao

  companion object {
    const val DATABASE_NAME = "cadence_productivity.db"

    // Future-ready migration scaffold
    val MIGRATIONS: Array<Migration> = arrayOf()

    fun create(context: Context): CadenceDatabase =
      Room.databaseBuilder(
          context.applicationContext,
          CadenceDatabase::class.java,
          DATABASE_NAME,
        )
        .addMigrations(*MIGRATIONS)
        .addCallback(
          object : Callback() {
            override fun onOpen(db: SupportSQLiteDatabase) {
              super.onOpen(db)
              db.execSQL("PRAGMA foreign_keys=ON;")
            }
          }
        )
        .build()

    fun createInMemory(context: Context): CadenceDatabase =
      Room.inMemoryDatabaseBuilder(context.applicationContext, CadenceDatabase::class.java)
        .allowMainThreadQueries()
        .build()
  }
}
