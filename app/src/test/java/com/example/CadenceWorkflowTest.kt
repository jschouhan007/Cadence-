package com.example

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.CadenceDatabase
import com.example.data.local.entity.ReminderEntity
import com.example.data.local.entity.TaskPriority
import com.example.data.local.entity.TaskStatus
import com.example.data.repository.CategoryRepository
import com.example.data.repository.TaskRepository
import com.example.domain.model.CategoryDeleteStrategy
import com.example.domain.reminder.ReminderScheduler
import com.example.domain.time.DateTimeUtils
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class CadenceWorkflowTest {

  @get:Rule val composeTestRule = createAndroidComposeRule<MainActivity>()

  private class FakeReminderScheduler : ReminderScheduler {
    override fun ensureNotificationChannels() {}

    override suspend fun schedule(reminder: ReminderEntity, taskTitle: String) {}

    override suspend fun cancel(reminderId: String, notificationId: Int) {}

    override suspend fun reconcileAll() {}
  }

  @Test
  fun `room database task and category CRUD and safe deletion persist accurately`() = runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val db = CadenceDatabase.createInMemory(context)
    try {
      val scheduler = FakeReminderScheduler()
      val categoryRepo = CategoryRepository(db, db.categoryDao(), db.taskDao())
      val taskRepo =
        TaskRepository(
          db = db,
          taskDao = db.taskDao(),
          tagDao = db.tagDao(),
          reminderDao = db.reminderDao(),
          historyDao = db.taskHistoryDao(),
          reminderScheduler = scheduler,
        )

      val catId = categoryRepo.createCategory("College", "Labs & exams", "school", 0xFF1E5F74)
      val taskId =
        taskRepo.createQuickTask(
          title = "Compiler Lab 3",
          categoryId = catId,
          priority = TaskPriority.HIGH,
          dueAt = DateTimeUtils.endOfDay(System.currentTimeMillis()),
        )

      taskRepo.addSubtask(taskId, "Write lexer tests")
      taskRepo.addTagToTask(taskId, "cs401")

      val loaded = taskRepo.observeTaskDetails(taskId).first()
      assertNotNull(loaded)
      assertEquals("Compiler Lab 3", loaded?.task?.title)
      assertEquals(1, loaded?.subtasks?.size)
      assertEquals(1, loaded?.tags?.size)

      val nowCompleted = taskRepo.toggleTaskCompletion(taskId)
      assertTrue(nowCompleted)
      val afterToggle = taskRepo.observeTaskDetails(taskId).first()
      assertEquals(TaskStatus.COMPLETED, afterToggle?.task?.status)

      // Safe delete category moves task to Inbox (categoryId = null) instead of deleting it
      categoryRepo.deleteCategorySafe(catId, CategoryDeleteStrategy.MOVE_TO_INBOX)
      val afterDelete = taskRepo.observeTaskDetails(taskId).first()
      assertNotNull(afterDelete)
      assertNull(afterDelete?.task?.categoryId)
    } finally {
      db.close()
    }
  }

  private fun ensureOnTodayScreen() {
    composeTestRule.waitUntil(timeoutMillis = 5_000) {
      composeTestRule.onAllNodes(hasTestTag("onboarding_skip_button")).fetchSemanticsNodes().isNotEmpty() ||
        composeTestRule.onAllNodes(hasTestTag("nav_today")).fetchSemanticsNodes().isNotEmpty()
    }
    if (composeTestRule.onAllNodes(hasTestTag("onboarding_skip_button")).fetchSemanticsNodes().isNotEmpty()) {
      composeTestRule.onNodeWithTag("onboarding_skip_button").assertIsDisplayed().performClick()
      composeTestRule.waitUntil(timeoutMillis = 5_000) {
        composeTestRule.onAllNodes(hasTestTag("nav_today")).fetchSemanticsNodes().isNotEmpty()
      }
    }
  }

  @Test
  fun `onboarding to today quick add task and bottom navigation workflow`() {
    ensureOnTodayScreen()

    // Today dashboard is displayed with bottom navigation
    composeTestRule.onNodeWithTag("nav_today").assertIsDisplayed()
    composeTestRule.onNodeWithTag("nav_tasks").assertIsDisplayed()
    composeTestRule.onNodeWithTag("nav_calendar").assertIsDisplayed()
    composeTestRule.onNodeWithTag("nav_progress").assertIsDisplayed()

    // Quick add a task from FAB
    composeTestRule.onNodeWithTag("fab_add_task").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("quick_add_title_input").performTextInput("Review architecture plan")
    composeTestRule.onNodeWithTag("quick_add_save_button").performScrollTo().performClick()
    composeTestRule.waitForIdle()

    composeTestRule.waitUntil(timeoutMillis = 5_000) {
      composeTestRule
        .onAllNodes(hasTestTag("quick_add_save_button"))
        .fetchSemanticsNodes()
        .isEmpty()
    }
    composeTestRule.waitUntil(timeoutMillis = 5_000) {
      composeTestRule
        .onAllNodes(hasText("1 remaining today", substring = true))
        .fetchSemanticsNodes()
        .isNotEmpty()
    }
    composeTestRule
      .onNodeWithTag("today_task_list")
      .performScrollToNode(hasText("Review architecture plan"))
    composeTestRule.onNodeWithText("Review architecture plan").assertIsDisplayed()

    // Navigate across primary tabs
    composeTestRule.onNodeWithTag("nav_tasks").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("tasks_tab_categories").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("add_category_button").assertIsDisplayed()

    composeTestRule.onNodeWithTag("nav_calendar").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("calendar_scope_week").performClick()
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag("nav_progress").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithText("0 of 1 tasks completed").assertIsDisplayed()
    composeTestRule
      .onNodeWithTag("progress_list")
      .performScrollToNode(hasText("Completed in Last 7 Days"))
    composeTestRule.onNodeWithText("Completed in Last 7 Days").assertIsDisplayed()

    // Switching back to Tasks restores its selected Categories sub-tab via saveState/restoreState
    composeTestRule.onNodeWithTag("nav_tasks").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("add_category_button").assertIsDisplayed()

    // System Back from any non-start primary tab pops directly back to TodayRoute
    composeTestRule.runOnUiThread {
      composeTestRule.activity.onBackPressedDispatcher.onBackPressed()
    }
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("nav_today").assertIsDisplayed()
    composeTestRule.onNodeWithTag("today_task_list").assertIsDisplayed()
    composeTestRule
      .onNodeWithTag("today_task_list")
      .performScrollToNode(hasText("1 remaining today", substring = true))
    composeTestRule.onNodeWithText("1 remaining today", substring = true).assertIsDisplayed()
  }

  @Test
  fun `task created with custom alert time schedules and updates alarm`() = runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val db = CadenceDatabase.createInMemory(context)
    try {
      val scheduledCalls = mutableListOf<Pair<Long, String>>()
      val scheduler =
        object : ReminderScheduler {
          override fun ensureNotificationChannels() {}

          override suspend fun schedule(reminder: ReminderEntity, taskTitle: String) {
            scheduledCalls.add(reminder.triggerAt to taskTitle)
          }

          override suspend fun cancel(reminderId: String, notificationId: Int) {}

          override suspend fun reconcileAll() {}
        }
      val taskRepo =
        TaskRepository(
          db = db,
          taskDao = db.taskDao(),
          tagDao = db.tagDao(),
          reminderDao = db.reminderDao(),
          historyDao = db.taskHistoryDao(),
          reminderScheduler = scheduler,
        )

      val alertTime = System.currentTimeMillis() + 2 * 60 * 60_000L
      val taskId =
        taskRepo.createQuickTask(
          title = "Take evening medication",
          priority = TaskPriority.HIGH,
          dueAt = DateTimeUtils.endOfDay(System.currentTimeMillis()),
          reminderAt = alertTime,
        )

      val loaded = taskRepo.observeTaskDetails(taskId).first()
      assertNotNull(loaded)
      assertEquals(1, loaded?.reminders?.size)
      val reminder = loaded!!.reminders.first()
      assertEquals(ReminderEntity.TYPE_CUSTOM_TIME, reminder.type)
      assertEquals(alertTime, reminder.triggerAt)
      assertTrue(scheduledCalls.any { it.first == alertTime && it.second == "Take evening medication" })

      val updatedAlertTime = alertTime + 30 * 60_000L
      taskRepo.updateReminderTime(reminder, updatedAlertTime)
      val reloaded = taskRepo.observeTaskDetails(taskId).first()
      assertEquals(updatedAlertTime, reloaded?.reminders?.first()?.triggerAt)
      assertTrue(scheduledCalls.any { it.first == updatedAlertTime && it.second == "Take evening medication" })
    } finally {
      db.close()
    }
  }

  @Test
  fun `quick add task with alert time carries into task detail and persists`() {
    ensureOnTodayScreen()
    composeTestRule.waitUntil(timeoutMillis = 5_000) {
      composeTestRule.onAllNodes(hasTestTag("fab_add_task")).fetchSemanticsNodes().isNotEmpty()
    }

    composeTestRule.onNodeWithTag("fab_add_task").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("quick_add_title_input").performTextInput("Call dentist")
    composeTestRule.onNodeWithTag("quick_add_alert_pick_time").performScrollTo().performClick()
    composeTestRule.waitForIdle()

    // AlertTimePickerDialog opens with "Set Alert" confirm button
    composeTestRule.onNodeWithTag("alert_picker_date_tomorrow").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("alert_picker_confirm").performClick()
    composeTestRule.waitForIdle()

    // Alert summary is shown in Quick Add sheet
    composeTestRule.onNodeWithTag("quick_add_alert_summary").performScrollTo().assertIsDisplayed()

    // Transition to Full Details and verify pending alert carried over
    composeTestRule.onNodeWithTag("quick_add_more_options_button").performScrollTo().performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("task_detail_save").assertIsDisplayed()
    composeTestRule.onNodeWithText("Tap to change time", substring = true).performScrollTo().assertIsDisplayed()

    // Save task and confirm it appears on Tasks
    composeTestRule.onNodeWithTag("task_detail_save").performClick()
    composeTestRule.waitUntil(timeoutMillis = 5_000) {
      composeTestRule.onAllNodes(hasTestTag("today_task_list")).fetchSemanticsNodes().isNotEmpty()
    }
    composeTestRule.onNodeWithTag("nav_tasks").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.waitUntil(timeoutMillis = 5_000) {
      composeTestRule.onAllNodes(hasText("Call dentist")).fetchSemanticsNodes().isNotEmpty()
    }
    composeTestRule.onNodeWithText("Call dentist").assertIsDisplayed()
  }
}
