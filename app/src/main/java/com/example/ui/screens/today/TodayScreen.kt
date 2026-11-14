package com.example.ui.screens.today

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.CheckCircleOutline
import androidx.compose.material.icons.rounded.CreateNewFolder
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.Today
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.entity.TaskWithDetails
import com.example.domain.time.GreetingPeriod
import com.example.ui.components.CadenceTopBar
import com.example.ui.components.CategoryEditorDialog
import com.example.ui.components.EmptyStateView
import com.example.ui.components.MoveCategoryDialog
import com.example.ui.components.ProgressRing
import com.example.ui.components.TaskRowCard
import com.example.ui.theme.CadenceThemeTokens
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.Spacing
import kotlinx.coroutines.launch

@Composable
fun TodayScreen(
  viewModel: TodayViewModel,
  snackbarHostState: SnackbarHostState,
  onOpenQuickAdd: () -> Unit,
  onOpenTaskDetail: (String) -> Unit,
  onOpenCategoryDetail: (String) -> Unit,
  onOpenSearch: () -> Unit,
  onOpenFocus: (String?) -> Unit,
  onOpenReminders: () -> Unit,
  onOpenSettings: () -> Unit,
  onOpenCalendarTab: () -> Unit,
) {
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()
  val scope = rememberCoroutineScope()
  val semantic = CadenceThemeTokens.colors

  var showCreateCategoryDialog by remember { mutableStateOf(false) }
  var taskToMoveCategory by remember { mutableStateOf<TaskWithDetails?>(null) }
  var showCompletedToday by remember { mutableStateOf(false) }

  val greetingText =
    when (uiState.greetingPeriod) {
      GreetingPeriod.MORNING -> "Good morning"
      GreetingPeriod.AFTERNOON -> "Good afternoon"
      GreetingPeriod.EVENING -> "Good evening"
    }

  Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
    CadenceTopBar(
      title = greetingText,
      subtitle = uiState.formattedDate,
      reminderBadgeCount = uiState.reminderBadgeCount,
      onSearchClick = onOpenSearch,
      onFocusClick = { onOpenFocus(null) },
      onRemindersClick = onOpenReminders,
      onSettingsClick = onOpenSettings,
    )

    if (uiState.isLoading) {
      Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
      }
    } else {
      Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        LazyColumn(
          modifier = Modifier.widthIn(max = Spacing.maxContentWidth).fillMaxSize().testTag("today_task_list"),
          contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 104.dp),
          verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
          // Sunlit Studio Bento Rhythm Hero Card
          item(key = "daily_progress_card") {
            Surface(
              shape = RoundedCornerShape(24.dp),
              color = Color.Transparent,
              border = BorderStroke(1.2.dp, MaterialTheme.colorScheme.outlineVariant),
              tonalElevation = 1.dp,
            ) {
              Column(
                modifier =
                  Modifier.fillMaxWidth()
                    .background(
                      Brush.linearGradient(
                        colors = listOf(semantic.heroStart, semantic.cardElevated, semantic.heroEnd)
                      )
                    )
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
              ) {
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                  val pct = (uiState.progressFraction * 100).toInt()
                  ProgressRing(
                    progress = uiState.progressFraction,
                    centerLabel = "$pct%",
                    subLabel = "${uiState.completedTodayCount} done",
                    indicatorColor = MaterialTheme.colorScheme.primary,
                    secondaryIndicatorColor = semantic.success,
                  )
                  Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Surface(
                      shape = RoundedCornerShape(9.dp),
                      color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                    ) {
                      Text(
                        text = "DAILY GOAL · ${uiState.dailyGoal} TASKS",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                      )
                    }
                    Text(
                      text = "Today's Rhythm",
                      style = MaterialTheme.typography.titleLarge,
                      fontWeight = FontWeight.Bold,
                      color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                      text =
                        when {
                          uiState.remainingTodayCount == 0 && uiState.completedTodayCount > 0 ->
                            "All scheduled tasks for today are complete. Enjoy your breathing room."
                          uiState.remainingTodayCount == 0 ->
                            "Your day is open. Capture a task or plan your next priority."
                          else ->
                            "${uiState.completedTodayCount} completed · ${uiState.remainingTodayCount} remaining today (Goal: ${uiState.dailyGoal})"
                        },
                      style = MaterialTheme.typography.bodySmall,
                      color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                  }
                }

                // 2x2 Tactile Bento Stat Grid
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                  ) {
                    BentoStatTile(
                      label = "Overdue",
                      count = uiState.overdueTasks.size,
                      icon = Icons.Rounded.WarningAmber,
                      tint =
                        if (uiState.overdueTasks.isNotEmpty()) semantic.overdue
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                      bg =
                        if (uiState.overdueTasks.isNotEmpty()) semantic.overdueContainer
                        else MaterialTheme.colorScheme.surfaceContainerLow,
                      modifier = Modifier.weight(1f),
                    )
                    BentoStatTile(
                      label = "Due Today",
                      count = uiState.dueTodayTasks.size,
                      icon = Icons.Rounded.Today,
                      tint = MaterialTheme.colorScheme.primary,
                      bg = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f),
                      modifier = Modifier.weight(1f),
                    )
                  }
                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                  ) {
                    BentoStatTile(
                      label = "Upcoming",
                      count = uiState.upcomingTasks.size,
                      icon = Icons.Rounded.Schedule,
                      tint = semantic.marigold,
                      bg = semantic.marigoldContainer.copy(alpha = 0.85f),
                      modifier = Modifier.weight(1f),
                    )
                    BentoStatTile(
                      label = "Done Today",
                      count = uiState.completedTodayCount,
                      icon = Icons.Rounded.CheckCircle,
                      tint = semantic.success,
                      bg = semantic.successContainer.copy(alpha = 0.85f),
                      modifier = Modifier.weight(1f),
                    )
                  }
                }
              }
            }
          }

          // Quick Actions Studio Dock
          item(key = "quick_actions_strip") {
            Row(
              modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
              horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
              QuickActionPill(
                label = "Add task",
                icon = Icons.Rounded.Add,
                tint = MaterialTheme.colorScheme.primary,
                container = MaterialTheme.colorScheme.primaryContainer,
                onClick = onOpenQuickAdd,
                testTag = "today_quick_add_chip",
              )
              QuickActionPill(
                label = "New category",
                icon = Icons.Rounded.CreateNewFolder,
                tint = semantic.marigold,
                container = semantic.marigoldContainer,
                onClick = { showCreateCategoryDialog = true },
                testTag = "today_new_category_chip",
              )
              QuickActionPill(
                label =
                  if (uiState.activeFocusSession != null) "Resume focus"
                  else "Start focus",
                icon = Icons.Rounded.Timer,
                tint = MaterialTheme.colorScheme.secondary,
                container = MaterialTheme.colorScheme.secondaryContainer,
                onClick = { onOpenFocus(uiState.dueTodayTasks.firstOrNull()?.task?.id) },
                testTag = "today_focus_chip",
              )
              QuickActionPill(
                label = "Calendar",
                icon = Icons.Rounded.CalendarMonth,
                tint = semantic.success,
                container = semantic.successContainer,
                onClick = onOpenCalendarTab,
                testTag = "today_calendar_chip",
              )
            }
          }

          // Empty state when nothing overdue, due today, or upcoming
          if (
            uiState.overdueTasks.isEmpty() &&
              uiState.dueTodayTasks.isEmpty() &&
              uiState.upcomingTasks.isEmpty() &&
              uiState.completedTodayTasks.isEmpty()
          ) {
            item(key = "today_empty_state") {
              EmptyStateView(
                icon = Icons.Rounded.WbSunny,
                title = "Clear horizon today",
                message = "Add tasks for today, capture ideas in Inbox, or start a distraction-free focus session.",
                actionLabel = "Add a Task",
                onAction = onOpenQuickAdd,
                testTag = "today_empty_state",
              )
            }
          }

          // Overdue Section (Never hidden)
          if (uiState.overdueTasks.isNotEmpty()) {
            item(key = "header_overdue") {
              SectionHeader(
                title = "Overdue",
                count = uiState.overdueTasks.size,
                accentColor = semantic.overdue,
              )
            }
            items(uiState.overdueTasks, key = { "overdue_${it.task.id}" }) { item ->
              TaskRowCard(
                item = item,
                onClick = { onOpenTaskDetail(item.task.id) },
                onToggleComplete = {
                  viewModel.toggleTaskCompletion(item.task.id) { completed ->
                    scope.launch {
                      val res =
                        snackbarHostState.showSnackbar(
                          message = if (completed) "Completed \"${item.task.title}\"" else "Reopened task",
                          actionLabel = "Undo",
                          duration = SnackbarDuration.Short,
                        )
                      if (res == SnackbarResult.ActionPerformed) {
                        viewModel.toggleTaskCompletion(item.task.id)
                      }
                    }
                  }
                },
                onCategoryClick = onOpenCategoryDetail,
                onStartFocus = { onOpenFocus(item.task.id) },
                onMoveCategory = { taskToMoveCategory = item },
                onDuplicate = { viewModel.duplicateTask(item.task.id) },
                onDelete = { viewModel.deleteTask(item.task.id) },
              )
            }
          }

          // Due Today Section
          if (uiState.dueTodayTasks.isNotEmpty()) {
            item(key = "header_due_today") {
              SectionHeader(
                title = "My Day · Prioritized",
                count = uiState.dueTodayTasks.size,
                accentColor = MaterialTheme.colorScheme.primary,
              )
            }
            items(uiState.dueTodayTasks, key = { "today_${it.task.id}" }) { item ->
              TaskRowCard(
                item = item,
                onClick = { onOpenTaskDetail(item.task.id) },
                onToggleComplete = {
                  viewModel.toggleTaskCompletion(item.task.id) { completed ->
                    scope.launch {
                      val res =
                        snackbarHostState.showSnackbar(
                          message = if (completed) "Completed \"${item.task.title}\"" else "Reopened task",
                          actionLabel = "Undo",
                          duration = SnackbarDuration.Short,
                        )
                      if (res == SnackbarResult.ActionPerformed) {
                        viewModel.toggleTaskCompletion(item.task.id)
                      }
                    }
                  }
                },
                onCategoryClick = onOpenCategoryDetail,
                onStartFocus = { onOpenFocus(item.task.id) },
                onMoveCategory = { taskToMoveCategory = item },
                onDuplicate = { viewModel.duplicateTask(item.task.id) },
                onDelete = { viewModel.deleteTask(item.task.id) },
              )
            }
          } else if (uiState.completedTodayTasks.isNotEmpty() && uiState.overdueTasks.isEmpty()) {
            item(key = "all_done_banner") {
              Surface(
                shape = RoundedCornerShape(18.dp),
                color = semantic.successContainer,
                border = BorderStroke(1.dp, semantic.success.copy(alpha = 0.35f)),
              ) {
                Row(
                  modifier = Modifier.fillMaxWidth().padding(16.dp),
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                  Icon(Icons.Rounded.CheckCircleOutline, contentDescription = null, tint = semantic.success)
                  Text(
                    text = "You've wrapped up everything scheduled for today.",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                  )
                }
              }
            }
          }

          // Upcoming Section
          if (uiState.upcomingTasks.isNotEmpty()) {
            item(key = "header_upcoming") {
              SectionHeader(
                title = "Upcoming",
                count = uiState.upcomingTasks.size,
                accentColor = semantic.marigold,
              )
            }
            items(uiState.upcomingTasks, key = { "upcoming_${it.task.id}" }) { item ->
              TaskRowCard(
                item = item,
                onClick = { onOpenTaskDetail(item.task.id) },
                onToggleComplete = { viewModel.toggleTaskCompletion(item.task.id) },
                onCategoryClick = onOpenCategoryDetail,
                onStartFocus = { onOpenFocus(item.task.id) },
                onMoveCategory = { taskToMoveCategory = item },
                onDuplicate = { viewModel.duplicateTask(item.task.id) },
                onDelete = { viewModel.deleteTask(item.task.id) },
              )
            }
          }

          // Completed Today Collapsible
          if (uiState.completedTodayTasks.isNotEmpty()) {
            item(key = "header_completed_today") {
              Surface(
                modifier =
                  Modifier.fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .clickable { showCompletedToday = !showCompletedToday },
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLow,
              ) {
                Row(
                  modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                  Text(
                    text = "Completed today (${uiState.completedTodayTasks.size})",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                  )
                  Icon(
                    imageVector =
                      if (showCompletedToday) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                    contentDescription = if (showCompletedToday) "Collapse completed" else "Expand completed",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                  )
                }
              }
            }
            item(key = "completed_today_list") {
              androidx.compose.animation.AnimatedVisibility(visible = showCompletedToday) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                  uiState.completedTodayTasks.forEach { item ->
                    TaskRowCard(
                      item = item,
                      onClick = { onOpenTaskDetail(item.task.id) },
                      onToggleComplete = { viewModel.toggleTaskCompletion(item.task.id) },
                      onCategoryClick = onOpenCategoryDetail,
                      onDelete = { viewModel.deleteTask(item.task.id) },
                    )
                  }
                }
              }
            }
          }
        }
      }
    }
  }

  if (showCreateCategoryDialog) {
    CategoryEditorDialog(
      onDismiss = { showCreateCategoryDialog = false },
      onSave = { name, desc, iconKey, colorHex ->
        viewModel.createCategory(name, desc, iconKey, colorHex)
      },
    )
  }

  taskToMoveCategory?.let { moving ->
    MoveCategoryDialog(
      categories = uiState.categories,
      currentCategoryId = moving.task.categoryId,
      onDismiss = { taskToMoveCategory = null },
      onSelectCategory = { targetId -> viewModel.moveTaskToCategory(moving.task.id, targetId) },
    )
  }
}

@Composable
private fun BentoStatTile(
  label: String,
  count: Int,
  icon: ImageVector,
  tint: Color,
  bg: Color,
  modifier: Modifier = Modifier,
) {
  Surface(
    modifier = modifier,
    shape = RoundedCornerShape(16.dp),
    color = bg,
    border = BorderStroke(1.dp, tint.copy(alpha = 0.22f)),
  ) {
    Row(
      modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      Box(
        modifier =
          Modifier.size(32.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(tint.copy(alpha = 0.16f)),
        contentAlignment = Alignment.Center,
      ) {
        Icon(
          imageVector = icon,
          contentDescription = null,
          tint = tint,
          modifier = Modifier.size(17.dp),
        )
      }
      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = count.toString(),
          style = MaterialTheme.typography.titleMedium.copy(
            fontFamily = JetBrainsMonoFamily,
            fontFeatureSettings = "tnum",
          ),
          fontWeight = FontWeight.Bold,
          color = tint,
          maxLines = 1,
        )
        Text(
          text = label,
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
      }
    }
  }
}

@Composable
private fun QuickActionPill(
  label: String,
  icon: ImageVector,
  tint: Color,
  container: Color,
  onClick: () -> Unit,
  testTag: String,
) {
  Surface(
    modifier =
      Modifier.clip(RoundedCornerShape(14.dp))
        .clickable(onClick = onClick)
        .testTag(testTag),
    shape = RoundedCornerShape(14.dp),
    color = container.copy(alpha = 0.72f),
    border = BorderStroke(1.dp, tint.copy(alpha = 0.28f)),
  ) {
    Row(
      modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
      Icon(
        imageVector = icon,
        contentDescription = null,
        tint = tint,
        modifier = Modifier.size(17.dp),
      )
      Text(
        text = label,
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurface,
        maxLines = 1,
        softWrap = false,
      )
    }
  }
}

@Composable
private fun SectionHeader(title: String, count: Int, accentColor: Color) {
  Row(
    modifier = Modifier.fillMaxWidth().padding(top = 6.dp, bottom = 2.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(8.dp),
  ) {
    Box(
      modifier =
        Modifier.size(width = 6.dp, height = 18.dp)
          .clip(CircleShape)
          .background(accentColor)
    )
    Text(
      text = title,
      style = MaterialTheme.typography.titleMedium,
      fontWeight = FontWeight.Bold,
      color = MaterialTheme.colorScheme.onBackground,
      modifier = Modifier.weight(1f),
    )
    Text(
      text = "$count",
      style = MaterialTheme.typography.labelMedium.copy(
        fontFamily = JetBrainsMonoFamily,
        fontFeatureSettings = "tnum",
      ),
      fontWeight = FontWeight.Bold,
      color = accentColor,
      modifier =
        Modifier.clip(RoundedCornerShape(9.dp))
          .background(accentColor.copy(alpha = 0.14f))
          .padding(horizontal = 9.dp, vertical = 3.dp),
    )
  }
}
