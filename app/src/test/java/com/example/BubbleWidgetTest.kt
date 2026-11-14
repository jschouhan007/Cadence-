package com.example

import android.appwidget.AppWidgetManager
import android.content.Context
import android.os.Bundle
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.entity.TaskEntity
import com.example.data.local.entity.TaskPriority
import com.example.data.local.entity.TaskStatus
import com.example.data.local.entity.TaskWithDetails
import com.example.domain.time.DateTimeUtils
import com.example.ui.theme.CadenceTheme
import com.example.widget.CadenceBubbleWidgetProvider
import com.example.widget.FloatingTaskSnippetContent
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
class BubbleWidgetTest {

  @get:Rule val composeTestRule = createComposeRule()

  @Test
  fun `floating task snippet scrolls through tasks and supports completion and quick add`() {
    val now = System.currentTimeMillis()
    val sampleTasks =
      mutableStateListOf<TaskWithDetails>().apply {
        for (i in 1..12) {
          add(
            TaskWithDetails(
              task =
                TaskEntity(
                  id = "task_$i",
                  title = "Snippet Task #$i",
                  priority = if (i <= 2) TaskPriority.HIGH else TaskPriority.MEDIUM,
                  status = TaskStatus.TODO,
                  dueAt = DateTimeUtils.endOfDay(now),
                  isAllDay = true,
                  sortOrder = i,
                ),
              category = null,
              subtasks = emptyList(),
              tags = emptyList(),
              reminders = emptyList(),
            )
          )
        }
      }

    var openedTaskId: String? = null
    var dismissed = false

    composeTestRule.setContent {
      CadenceTheme {
        FloatingTaskSnippetContent(
          allActiveTasks = sampleTasks,
          onToggleComplete = { id -> sampleTasks.removeAll { it.task.id == id } },
          onQuickAddTask = { title ->
            sampleTasks.add(
              0,
              TaskWithDetails(
                task =
                  TaskEntity(
                    id = "quick_1",
                    title = title,
                    status = TaskStatus.TODO,
                    dueAt = DateTimeUtils.endOfDay(now),
                  ),
                category = null,
                subtasks = emptyList(),
                tags = emptyList(),
                reminders = emptyList(),
              ),
            )
          },
          onOpenTask = { openedTaskId = it },
          onOpenApp = {},
          onDismiss = { dismissed = true },
        )
      }
    }

    composeTestRule.onNodeWithTag("floating_snippet_card").assertIsDisplayed()
    composeTestRule.onNodeWithText("12 pending · Scroll to review").assertIsDisplayed()

    // Scroll down the floating snippet list to verify items further down
    composeTestRule
      .onNodeWithTag("floating_snippet_list")
      .performScrollToNode(hasText("Snippet Task #11"))
    composeTestRule.onNodeWithText("Snippet Task #11").assertIsDisplayed()

    // Complete a task right inside the snippet
    composeTestRule.onNodeWithTag("snippet_check_task_11").performClick()
    composeTestRule.waitForIdle()
    assertEquals(11, sampleTasks.size)

    // Quick add a task from the floating snippet footer
    composeTestRule.onNodeWithTag("floating_snippet_quick_input").performTextInput("Buy batteries")
    composeTestRule.onNodeWithTag("floating_snippet_quick_add").performClick()
    composeTestRule.waitForIdle()
    assertTrue(sampleTasks.any { it.task.title == "Buy batteries" })

    // Tap a task to open it
    composeTestRule
      .onNodeWithTag("floating_snippet_list")
      .performScrollToNode(hasText("Buy batteries"))
    composeTestRule.onNodeWithText("Buy batteries").performClick()
    assertEquals("quick_1", openedTaskId)

    // Close button dismisses floating snippet
    composeTestRule.onNodeWithTag("floating_snippet_close").performClick()
    assertTrue(dismissed)
  }

  @Test
  fun `bubble widget provider updates compact and expanded views without errors`() = runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val app = context.applicationContext as CadenceApp
    app.container.taskRepository.createQuickTask(title = "Widget sync task")

    val manager = AppWidgetManager.getInstance(context)
    val shadowManager = Shadows.shadowOf(manager)
    val widgetId = shadowManager.createWidget(CadenceBubbleWidgetProvider::class.java, R.layout.widget_bubble_compact)
    manager.updateAppWidgetOptions(
      widgetId,
      Bundle().apply {
        putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 60)
        putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 60)
      },
    )

    CadenceBubbleWidgetProvider.updateWidgetsNow(context, manager, intArrayOf(widgetId))
    val compactViews = shadowManager.getViewFor(widgetId)
    assertTrue(compactViews != null)

    // Resize widget larger and verify expanded scrollable layout renders cleanly
    manager.updateAppWidgetOptions(
      widgetId,
      Bundle().apply {
        putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 240)
        putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 180)
      },
    )
    CadenceBubbleWidgetProvider.updateWidgetsNow(context, manager, intArrayOf(widgetId))
    val expandedViews = shadowManager.getViewFor(widgetId)
    assertTrue(expandedViews != null)
  }
}
