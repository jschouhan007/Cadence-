package com.example

import android.content.Context
import android.os.Looper
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTouchInput
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.CadenceDatabase
import com.example.data.local.entity.ReminderEntity
import com.example.data.local.entity.SortDirection
import com.example.data.local.entity.TaskFilterSection
import com.example.data.local.entity.TaskPriority
import com.example.data.local.entity.TaskSortOption
import com.example.data.repository.CategoryRepository
import com.example.data.repository.TaskRepository
import com.example.domain.model.TaskDraft
import com.example.domain.reminder.ReminderScheduler
import com.example.ui.screens.tasks.TasksScreen
import com.example.ui.screens.tasks.TasksUiState
import com.example.ui.screens.tasks.TasksViewModel
import com.example.ui.theme.CadenceTheme
import com.example.ui.theme.ThemeMode
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class TasksSortAndReorderTest {

  @get:Rule val composeTestRule = createComposeRule()

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
  fun `tasksViewModel sorts by due date priority and title in ascending and descending order`() = runBlocking {
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
      val baseTime = 1_800_000_000_000L
      taskRepo.saveTask(
        TaskDraft(
          title = "Bravo Task",
          priority = TaskPriority.LOW,
          dueAt = baseTime + 20_000L,
        )
      )
      taskRepo.saveTask(
        TaskDraft(
          title = "Alpha Task",
          priority = TaskPriority.URGENT,
          dueAt = null,
        )
      )
      taskRepo.saveTask(
        TaskDraft(
          title = "Charlie Task",
          priority = TaskPriority.HIGH,
          dueAt = baseTime + 10_000L,
        )
      )

      val vm = TasksViewModel(taskRepo, categoryRepo)
      val collectJob = launch { vm.uiState.collect() }

      awaitUiState(vm) { !it.isLoading && it.tasks.size == 3 }

      // Title ascending (default for TITLE_ASC) -> Alpha, Bravo, Charlie
      vm.setSortOption(TaskSortOption.TITLE_ASC)
      val titleAsc =
        awaitUiState(vm) {
          it.sortOption == TaskSortOption.TITLE_ASC && it.sortDirection == SortDirection.ASCENDING
        }
      assertEquals(listOf("Alpha Task", "Bravo Task", "Charlie Task"), titleAsc.tasks.map { it.task.title })

      // Toggle Title descending -> Charlie, Bravo, Alpha
      vm.toggleSortDirection()
      val titleDesc =
        awaitUiState(vm) {
          it.sortOption == TaskSortOption.TITLE_ASC && it.sortDirection == SortDirection.DESCENDING
        }
      assertEquals(listOf("Charlie Task", "Bravo Task", "Alpha Task"), titleDesc.tasks.map { it.task.title })

      // Priority descending (default for PRIORITY) -> URGENT (Alpha), HIGH (Charlie), LOW (Bravo)
      vm.setSortOption(TaskSortOption.PRIORITY)
      val prioDesc =
        awaitUiState(vm) {
          it.sortOption == TaskSortOption.PRIORITY && it.sortDirection == SortDirection.DESCENDING
        }
      assertEquals(listOf("Alpha Task", "Charlie Task", "Bravo Task"), prioDesc.tasks.map { it.task.title })

      // Toggle Priority ascending -> LOW (Bravo), HIGH (Charlie), URGENT (Alpha)
      vm.toggleSortDirection()
      val prioAsc =
        awaitUiState(vm) {
          it.sortOption == TaskSortOption.PRIORITY && it.sortDirection == SortDirection.ASCENDING
        }
      assertEquals(listOf("Bravo Task", "Charlie Task", "Alpha Task"), prioAsc.tasks.map { it.task.title })

      // Due date ascending (default for DUE_DATE; nulls last) -> Charlie (+10s), Bravo (+20s), Alpha (null)
      vm.setSortOption(TaskSortOption.DUE_DATE)
      val dueAsc =
        awaitUiState(vm) {
          it.sortOption == TaskSortOption.DUE_DATE && it.sortDirection == SortDirection.ASCENDING
        }
      assertEquals(listOf("Charlie Task", "Bravo Task", "Alpha Task"), dueAsc.tasks.map { it.task.title })

      // Toggle Due date descending (nulls still last) -> Bravo (+20s), Charlie (+10s), Alpha (null)
      vm.toggleSortDirection()
      val dueDesc =
        awaitUiState(vm) {
          it.sortOption == TaskSortOption.DUE_DATE && it.sortDirection == SortDirection.DESCENDING
        }
      assertEquals(listOf("Bravo Task", "Charlie Task", "Alpha Task"), dueDesc.tasks.map { it.task.title })

      collectJob.cancel()
    } finally {
      db.close()
    }
  }

  @Test
  fun `commitDragReorder persists same-priority order inside Priority sort and switches to Manual when crossing weights`() =
    runBlocking {
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
        val high1 = taskRepo.saveTask(TaskDraft(title = "High One", priority = TaskPriority.HIGH))
        val high2 = taskRepo.saveTask(TaskDraft(title = "High Two", priority = TaskPriority.HIGH))
        val low1 = taskRepo.saveTask(TaskDraft(title = "Low One", priority = TaskPriority.LOW))

        val vm = TasksViewModel(taskRepo, categoryRepo)
        val collectJob = launch { vm.uiState.collect() }

        vm.setSortOption(TaskSortOption.PRIORITY)
        awaitUiState(vm) {
          !it.isLoading &&
            it.sortOption == TaskSortOption.PRIORITY &&
            it.tasks.map { t -> t.task.id } == listOf(high1, high2, low1)
        }

        // Swap High Two above High One (preserves DESC priority weight invariant -> stays in PRIORITY)
        vm.commitDragReorder(listOf(high2, high1, low1))
        val sameBucketState =
          awaitUiState(vm) { it.tasks.map { t -> t.task.id } == listOf(high2, high1, low1) }
        assertEquals(TaskSortOption.PRIORITY, sameBucketState.sortOption)
        assertEquals(listOf("High Two", "High One", "Low One"), sameBucketState.tasks.map { it.task.title })

        // Drag Low One to the very top (crosses priority weights -> switches to MANUAL ASC and persists)
        vm.commitDragReorder(listOf(low1, high2, high1))
        val manualSwitched =
          awaitUiState(vm) {
            it.sortOption == TaskSortOption.MANUAL &&
              it.sortDirection == SortDirection.ASCENDING &&
              it.tasks.map { t -> t.task.id } == listOf(low1, high2, high1)
          }
        assertEquals(listOf("Low One", "High Two", "High One"), manualSwitched.tasks.map { it.task.title })

        collectJob.cancel()
      } finally {
        db.close()
      }
    }

  @Test
  fun `tasksScreen sort toggle and press-and-drag gesture reorder tasks`() = runBlocking {
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
      val firstId = taskRepo.saveTask(TaskDraft(title = "First Card", priority = TaskPriority.HIGH))
      val secondId = taskRepo.saveTask(TaskDraft(title = "Second Card", priority = TaskPriority.LOW))
      val vm = TasksViewModel(taskRepo, categoryRepo)
      vm.setSection(TaskFilterSection.INBOX)

      composeTestRule.setContent {
        CadenceTheme(themeMode = ThemeMode.LIGHT) {
          val snackbarHostState = remember { SnackbarHostState() }
          TasksScreen(
            viewModel = vm,
            snackbarHostState = snackbarHostState,
            onOpenQuickAdd = {},
            onOpenTaskDetail = {},
            onOpenCategoryDetail = {},
            onOpenSearch = {},
            onOpenFocus = {},
            onOpenReminders = {},
            onOpenSettings = {},
          )
        }
      }

      composeTestRule.waitUntil(timeoutMillis = 5_000) {
        vm.uiState.value.tasks.size == 2
      }
      composeTestRule.onNodeWithTag("tasks_sort_menu_chip").assertIsDisplayed()
      composeTestRule.onNodeWithTag("tasks_sort_direction_toggle").assertIsDisplayed()

      // Switch sort option to Title via dropdown menu, then toggle Asc <-> Desc
      composeTestRule.onNodeWithTag("tasks_sort_menu_chip").performClick()
      composeTestRule.onNodeWithTag("tasks_sort_option_title_asc").performClick()
      composeTestRule.waitUntil(timeoutMillis = 5_000) {
        vm.uiState.value.sortOption == TaskSortOption.TITLE_ASC &&
          vm.uiState.value.sortDirection == SortDirection.ASCENDING
      }
      composeTestRule.onNodeWithText("Asc").assertIsDisplayed()

      composeTestRule.onNodeWithTag("tasks_sort_direction_toggle").performClick()
      composeTestRule.waitUntil(timeoutMillis = 5_000) {
        vm.uiState.value.sortDirection == SortDirection.DESCENDING &&
          vm.uiState.value.tasks.map { it.task.title } == listOf("Second Card", "First Card")
      }
      composeTestRule.onNodeWithText("Desc").assertIsDisplayed()

      // Scroll to make both cards visible and press-and-drag First Card up over Second Card
      composeTestRule.onNodeWithTag("tasks_list").performScrollToNode(hasText("First Card"))
      composeTestRule.onNodeWithTag("task_card_$firstId").assertIsDisplayed()
      composeTestRule.onNodeWithTag("task_card_$firstId").performTouchInput {
        down(center)
        advanceEventTime(800L)
        moveBy(Offset(0f, -260f))
        advanceEventTime(100L)
        up()
      }

      composeTestRule.waitUntil(timeoutMillis = 5_000) {
        vm.uiState.value.sortOption == TaskSortOption.MANUAL &&
          vm.uiState.value.tasks.map { it.task.id } == listOf(firstId, secondId)
      }
      assertEquals(listOf("First Card", "Second Card"), vm.uiState.value.tasks.map { it.task.title })
    } finally {
      db.close()
    }
  }
}
