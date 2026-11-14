package com.example

import android.content.Context
import android.os.Looper
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.CadenceDatabase
import com.example.data.local.entity.ReminderEntity
import com.example.data.local.entity.TaskFilterSection
import com.example.data.local.entity.TaskPriority
import com.example.data.local.entity.TaskStatus
import com.example.data.repository.CategoryRepository
import com.example.data.repository.TaskRepository
import com.example.domain.model.TaskDraft
import com.example.domain.reminder.ReminderScheduler
import com.example.ui.screens.tasks.TasksUiState
import com.example.ui.screens.tasks.TasksViewModel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class TasksLocalSearchTest {

  @get:Rule val composeTestRule = createAndroidComposeRule<MainActivity>()

  private class NoOpReminderScheduler : ReminderScheduler {
    override fun ensureNotificationChannels() {}

    override suspend fun schedule(reminder: ReminderEntity, taskTitle: String) {}

    override suspend fun cancel(reminderId: String, notificationId: Int) {}

    override suspend fun reconcileAll() {}
  }

  private fun awaitUiState(
    vm: TasksViewModel,
    timeoutMs: Long = 5_000L,
    predicate: (TasksUiState) -> Boolean,
  ): TasksUiState {
    val deadline = System.currentTimeMillis() + timeoutMs
    while (System.currentTimeMillis() < deadline) {
      Shadows.shadowOf(Looper.getMainLooper()).idle()
      val current = vm.uiState.value
      if (predicate(current)) return current
      Thread.sleep(20)
    }
    Shadows.shadowOf(Looper.getMainLooper()).idle()
    return vm.uiState.value
  }

  @Test
  fun `tasksViewModel filters in real time by title and description via Room searchTasks`() = runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val db = CadenceDatabase.createInMemory(context)
    try {
      val categoryRepo = CategoryRepository(db, db.categoryDao(), db.taskDao())
      val taskRepo =
        TaskRepository(
          db = db,
          taskDao = db.taskDao(),
          tagDao = db.tagDao(),
          reminderDao = db.reminderDao(),
          historyDao = db.taskHistoryDao(),
          reminderScheduler = NoOpReminderScheduler(),
        )
      val catId = categoryRepo.createCategory("Study", "", "school", 0xFF2357E8)

      taskRepo.saveTask(
        TaskDraft(
          title = "Compiler Architecture",
          description = "Build recursive descent parser for lab",
          categoryId = null,
          priority = TaskPriority.HIGH,
        )
      )
      taskRepo.saveTask(
        TaskDraft(
          title = "Grocery Run",
          description = "Pick up oat milk and espresso beans",
          categoryId = catId,
          priority = TaskPriority.MEDIUM,
        )
      )
      taskRepo.saveTask(
        TaskDraft(
          title = "Evening Stretch",
          description = "20 minute mobility flow",
          categoryId = null,
          priority = TaskPriority.LOW,
        )
      )

      val vm = TasksViewModel(taskRepo, categoryRepo)
      val collectJob = launch { vm.uiState.collect() }

      val initial = awaitUiState(vm) { !it.isLoading && it.tasks.size == 2 }
      assertEquals(2, initial.tasks.size)

      vm.setSearchQuery("compiler")
      val byTitle =
        awaitUiState(vm) {
          it.searchQuery == "compiler" && it.tasks.map { t -> t.task.title } == listOf("Compiler Architecture")
        }
      assertEquals(listOf("Compiler Architecture"), byTitle.tasks.map { it.task.title })

      vm.setSection(TaskFilterSection.ALL)
      vm.setSearchQuery("espresso")
      val byDesc =
        awaitUiState(vm) {
          it.searchQuery == "espresso" && it.tasks.map { t -> t.task.title } == listOf("Grocery Run")
        }
      assertEquals(listOf("Grocery Run"), byDesc.tasks.map { it.task.title })

      vm.clearSearchQuery()
      val cleared = awaitUiState(vm) { it.searchQuery.isEmpty() && it.tasks.size == 3 }
      assertEquals(3, cleared.tasks.size)

      collectJob.cancel()
    } finally {
      db.close()
    }
  }

  @Test
  fun `tasks screen inline search bar filters tasks by title and description and clears`() = runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val app = context.applicationContext as CadenceApp
    app.container.taskRepository.saveTask(
      TaskDraft(
        title = "Quarterly Budget",
        description = "Reconcile travel receipts and invoices",
        priority = TaskPriority.HIGH,
        status = TaskStatus.TODO,
      )
    )
    app.container.taskRepository.saveTask(
      TaskDraft(
        title = "Dentist Checkup",
        description = "Confirm 3pm appointment",
        priority = TaskPriority.MEDIUM,
        status = TaskStatus.TODO,
      )
    )

    composeTestRule.waitUntil(timeoutMillis = 5_000) {
      composeTestRule.onAllNodes(hasTestTag("onboarding_skip_button")).fetchSemanticsNodes().isNotEmpty() ||
        composeTestRule.onAllNodes(hasTestTag("nav_tasks")).fetchSemanticsNodes().isNotEmpty()
    }
    if (composeTestRule.onAllNodes(hasTestTag("onboarding_skip_button")).fetchSemanticsNodes().isNotEmpty()) {
      composeTestRule.onNodeWithTag("onboarding_skip_button").performClick()
      composeTestRule.waitUntil(timeoutMillis = 5_000) {
        composeTestRule.onAllNodes(hasTestTag("nav_tasks")).fetchSemanticsNodes().isNotEmpty()
      }
    }

    composeTestRule.onNodeWithTag("nav_tasks").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("tasks_search_input").assertIsDisplayed()

    composeTestRule.onNodeWithTag("tasks_search_input").performTextInput("invoices")
    composeTestRule.waitUntil(timeoutMillis = 5_000) {
      composeTestRule.onAllNodes(hasText("Quarterly Budget")).fetchSemanticsNodes().isNotEmpty() &&
        composeTestRule.onAllNodes(hasText("Dentist Checkup")).fetchSemanticsNodes().isEmpty()
    }
    composeTestRule.onNodeWithTag("tasks_list").performScrollToNode(hasText("Quarterly Budget"))
    composeTestRule.onNodeWithText("Quarterly Budget").assertIsDisplayed()
    composeTestRule.onNodeWithTag("tasks_search_result_banner").assertIsDisplayed()

    composeTestRule.onNodeWithTag("tasks_search_clear").performClick()
    composeTestRule.waitUntil(timeoutMillis = 5_000) {
      composeTestRule.onAllNodes(hasText("Dentist Checkup")).fetchSemanticsNodes().isNotEmpty()
    }
    assertTrue(composeTestRule.onAllNodes(hasText("Quarterly Budget")).fetchSemanticsNodes().isNotEmpty())
  }
}
