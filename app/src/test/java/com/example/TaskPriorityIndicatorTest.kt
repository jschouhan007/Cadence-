package com.example

import androidx.compose.foundation.layout.Column
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import com.example.data.local.entity.TaskEntity
import com.example.data.local.entity.TaskPriority
import com.example.data.local.entity.TaskStatus
import com.example.data.local.entity.TaskWithDetails
import com.example.ui.components.TaskRowCard
import com.example.ui.theme.CadenceTheme
import com.example.ui.theme.ThemeMode
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class TaskPriorityIndicatorTest {

  @get:Rule val composeTestRule = createComposeRule()

  @Test
  fun `task list cards display sunlit studio priority strips badges and signal meters for all priority levels`() {
    val items =
      listOf(
        TaskPriority.HIGH to "high_1",
        TaskPriority.MEDIUM to "med_1",
        TaskPriority.LOW to "low_1",
      ).map { (priority, id) ->
        TaskWithDetails(
          task =
            TaskEntity(
              id = id,
              title = "${priority.label} task item",
              priority = priority,
              status = TaskStatus.TODO,
            ),
          category = null,
          subtasks = emptyList(),
          tags = emptyList(),
          reminders = emptyList(),
        )
      }

    composeTestRule.setContent {
      CadenceTheme(themeMode = ThemeMode.LIGHT) {
        Column {
          items.forEach { item ->
            TaskRowCard(
              item = item,
              onClick = {},
              onToggleComplete = {},
            )
          }
        }
      }
    }

    composeTestRule.onNodeWithTag("task_priority_strip_high_1", useUnmergedTree = true).assertIsDisplayed()
    composeTestRule.onNodeWithTag("task_priority_badge_high_1", useUnmergedTree = true).assertIsDisplayed()
    composeTestRule.onNodeWithContentDescription("High priority", useUnmergedTree = true).assertIsDisplayed()
    composeTestRule.onNodeWithText("High").assertIsDisplayed()

    composeTestRule.onNodeWithTag("task_priority_strip_med_1", useUnmergedTree = true).assertIsDisplayed()
    composeTestRule.onNodeWithTag("task_priority_badge_med_1", useUnmergedTree = true).assertIsDisplayed()
    composeTestRule.onNodeWithContentDescription("Medium priority", useUnmergedTree = true).assertIsDisplayed()
    composeTestRule.onNodeWithText("Medium").assertIsDisplayed()

    composeTestRule.onNodeWithTag("task_priority_strip_low_1", useUnmergedTree = true).assertIsDisplayed()
    composeTestRule.onNodeWithTag("task_priority_badge_low_1", useUnmergedTree = true).assertIsDisplayed()
    composeTestRule.onNodeWithContentDescription("Low priority", useUnmergedTree = true).assertIsDisplayed()
    composeTestRule.onNodeWithText("Low").assertIsDisplayed()
  }
}
