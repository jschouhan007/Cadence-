package com.example.widget

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Alarm
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
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
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.CadenceApp
import com.example.MainActivity
import com.example.data.local.entity.TaskPriority
import com.example.data.local.entity.TaskStatus
import com.example.data.local.entity.TaskWithDetails
import com.example.data.preferences.UserPreferences
import com.example.domain.reminder.LocalReminderScheduler
import com.example.domain.time.DateTimeUtils
import com.example.ui.components.CategoryPill
import com.example.ui.components.PriorityBadge
import com.example.ui.theme.CadenceTheme
import com.example.ui.theme.CadenceThemeTokens
import kotlinx.coroutines.launch

enum class SnippetFilter(val label: String) {
  ALL("To Do"),
  TODAY("Today"),
  OVERDUE("Overdue"),
}

class TaskSnippetBubbleActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    val container = (application as CadenceApp).container

    setContent {
      val prefs by
        container.preferencesRepository.preferences.collectAsStateWithLifecycle(
          initialValue = UserPreferences()
        )
      val allTasks by
        container.taskRepository.observeAllActiveTasks().collectAsStateWithLifecycle(
          initialValue = emptyList()
        )
      val scope = rememberCoroutineScope()

      CadenceTheme(
        themeMode = prefs.themeMode,
        dynamicColor = prefs.useDynamicColor,
      ) {
        FloatingTaskSnippetContent(
          allActiveTasks = allTasks,
          onToggleComplete = { taskId ->
            scope.launch { container.taskRepository.toggleTaskCompletion(taskId) }
          },
          onQuickAddTask = { title ->
            scope.launch {
              container.taskRepository.createQuickTask(
                title = title,
                dueAt = DateTimeUtils.endOfDay(System.currentTimeMillis()),
                isAllDay = true,
              )
            }
          },
          onOpenTask = { taskId ->
            val openIntent =
              Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra(LocalReminderScheduler.EXTRA_TASK_ID, taskId)
              }
            startActivity(openIntent)
            finish()
          },
          onOpenApp = {
            val openIntent =
              Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
              }
            startActivity(openIntent)
            finish()
          },
          onDismiss = { finish() },
        )
      }
    }
  }
}

@Composable
fun FloatingTaskSnippetContent(
  allActiveTasks: List<TaskWithDetails>,
  onToggleComplete: (String) -> Unit,
  onQuickAddTask: (String) -> Unit,
  onOpenTask: (String) -> Unit,
  onOpenApp: () -> Unit,
  onDismiss: () -> Unit,
) {
  var selectedFilter by remember { mutableStateOf(SnippetFilter.ALL) }
  var quickTitle by remember { mutableStateOf("") }
  val now = System.currentTimeMillis()
  val semantic = CadenceThemeTokens.colors

  val snapshotTasks = allActiveTasks.toList()
  val pendingTasks =
    remember(snapshotTasks) {
      snapshotTasks
        .filter { it.task.status != TaskStatus.COMPLETED && it.task.status != TaskStatus.CANCELLED }
        .sortedWith(
          compareBy<TaskWithDetails>(
            { !DateTimeUtils.isOverdue(it.task.dueAt, it.task.isAllDay, now) },
            { it.task.dueAt == null },
            { it.task.dueAt ?: Long.MAX_VALUE },
            { -it.task.priority.weight },
            { it.task.sortOrder },
          )
        )
    }

  val overdueCount =
    remember(pendingTasks) {
      pendingTasks.count { DateTimeUtils.isOverdue(it.task.dueAt, it.task.isAllDay, now) }
    }
  val todayCount =
    remember(pendingTasks) {
      pendingTasks.count { DateTimeUtils.isDueToday(it.task.dueAt, now) }
    }

  val displayedTasks =
    remember(pendingTasks, selectedFilter) {
      when (selectedFilter) {
        SnippetFilter.ALL -> pendingTasks
        SnippetFilter.TODAY -> pendingTasks.filter { DateTimeUtils.isDueToday(it.task.dueAt, now) }
        SnippetFilter.OVERDUE ->
          pendingTasks.filter { DateTimeUtils.isOverdue(it.task.dueAt, it.task.isAllDay, now) }
      }
    }

  Box(modifier = Modifier.fillMaxSize()) {
    Box(
      modifier =
        Modifier.fillMaxSize()
          .clickable(
            indication = null,
            interactionSource = remember { MutableInteractionSource() },
            onClick = onDismiss,
          )
    )

    BoxWithConstraints(
      modifier =
        Modifier.fillMaxSize()
          .safeDrawingPadding()
          .imePadding()
          .padding(horizontal = 16.dp, vertical = 18.dp),
      contentAlignment = Alignment.Center,
    ) {
      val maxListHeight = (maxHeight * 0.56f).coerceIn(180.dp, 420.dp)

      Surface(
        modifier =
          Modifier.widthIn(max = 440.dp)
            .fillMaxWidth()
            .testTag("floating_snippet_card")
            .pointerInput(Unit) { detectTapGestures {} },
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.background,
        tonalElevation = 8.dp,
        shadowElevation = 20.dp,
        border = BorderStroke(1.2.dp, MaterialTheme.colorScheme.outlineVariant),
      ) {
        Column(modifier = Modifier.fillMaxWidth()) {
          // Sunlit header wash
          Column(
            modifier =
              Modifier.fillMaxWidth()
                .background(Brush.horizontalGradient(listOf(semantic.heroStart, semantic.heroEnd)))
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
              Box(
                modifier =
                  Modifier.size(40.dp)
                    .clip(CircleShape)
                    .background(
                      Brush.linearGradient(
                        listOf(
                          MaterialTheme.colorScheme.primary,
                          MaterialTheme.colorScheme.secondary,
                        )
                      )
                    ),
                contentAlignment = Alignment.Center,
              ) {
                Icon(
                  imageVector = Icons.Rounded.CheckCircle,
                  contentDescription = null,
                  tint = Color.White,
                  modifier = Modifier.size(22.dp),
                )
              }

              Column(modifier = Modifier.weight(1f)) {
                Text(
                  text = "Tasks To Do",
                  style = MaterialTheme.typography.titleMedium,
                  fontWeight = FontWeight.Bold,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
                )
                Text(
                  text =
                    if (pendingTasks.isEmpty()) "All tasks complete"
                    else "${pendingTasks.size} pending · Scroll to review",
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
                )
              }

              IconButton(
                onClick = onOpenApp,
                modifier = Modifier.testTag("floating_snippet_open_app"),
              ) {
                Icon(
                  imageVector = Icons.AutoMirrored.Rounded.OpenInNew,
                  contentDescription = "Open full app",
                )
              }
              IconButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("floating_snippet_close"),
              ) {
                Icon(
                  imageVector = Icons.Rounded.Close,
                  contentDescription = "Close snippet",
                )
              }
            }

            // Filter strip
            Row(
              modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
              horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
              FilterChip(
                selected = selectedFilter == SnippetFilter.ALL,
                onClick = { selectedFilter = SnippetFilter.ALL },
                label = { Text("To Do (${pendingTasks.size})", maxLines = 1, softWrap = false) },
                shape = RoundedCornerShape(12.dp),
                colors =
                  FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                  ),
                modifier = Modifier.testTag("snippet_filter_all"),
              )
              FilterChip(
                selected = selectedFilter == SnippetFilter.TODAY,
                onClick = { selectedFilter = SnippetFilter.TODAY },
                label = { Text("Today ($todayCount)", maxLines = 1, softWrap = false) },
                shape = RoundedCornerShape(12.dp),
                colors =
                  FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                  ),
                modifier = Modifier.testTag("snippet_filter_today"),
              )
              FilterChip(
                selected = selectedFilter == SnippetFilter.OVERDUE,
                onClick = { selectedFilter = SnippetFilter.OVERDUE },
                label = { Text("Overdue ($overdueCount)", maxLines = 1, softWrap = false) },
                shape = RoundedCornerShape(12.dp),
                colors =
                  FilterChipDefaults.filterChipColors(
                    selectedContainerColor = semantic.overdue,
                    selectedLabelColor = Color.White,
                  ),
                modifier = Modifier.testTag("snippet_filter_overdue"),
              )
            }
          }

          HorizontalDivider(color = semantic.subtleDivider)

          Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            // Scrollable task list
            if (displayedTasks.isEmpty()) {
              Box(
                modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
                contentAlignment = Alignment.Center,
              ) {
                Text(
                  text =
                    when (selectedFilter) {
                      SnippetFilter.ALL -> "No tasks remaining. Add one below!"
                      SnippetFilter.TODAY -> "Nothing scheduled for today."
                      SnippetFilter.OVERDUE -> "No overdue tasks."
                    },
                  style = MaterialTheme.typography.bodyMedium,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
              }
            } else {
              LazyColumn(
                modifier =
                  Modifier.fillMaxWidth()
                    .weight(1f, fill = false)
                    .heightIn(max = maxListHeight)
                    .testTag("floating_snippet_list"),
                contentPadding = PaddingValues(vertical = 2.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
              ) {
                items(displayedTasks, key = { it.task.id }) { item ->
                  SnippetTaskRow(
                    item = item,
                    onToggleComplete = { onToggleComplete(item.task.id) },
                    onClick = { onOpenTask(item.task.id) },
                  )
                }
              }
            }

            HorizontalDivider(color = semantic.subtleDivider)

            // Inline quick capture
            Row(
              modifier = Modifier.fillMaxWidth(),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
              OutlinedTextField(
                value = quickTitle,
                onValueChange = { quickTitle = it },
                placeholder = { Text("Quick add task for today...", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.weight(1f).testTag("floating_snippet_quick_input"),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions =
                  KeyboardActions(
                    onDone = {
                      if (quickTitle.isNotBlank()) {
                        onQuickAddTask(quickTitle.trim())
                        quickTitle = ""
                      }
                    }
                  ),
              )
              FilledIconButton(
                onClick = {
                  if (quickTitle.isNotBlank()) {
                    onQuickAddTask(quickTitle.trim())
                    quickTitle = ""
                  }
                },
                enabled = quickTitle.isNotBlank(),
                shape = RoundedCornerShape(14.dp),
                colors =
                  IconButtonDefaults.filledIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                  ),
                modifier = Modifier.size(50.dp).testTag("floating_snippet_quick_add"),
              ) {
                Icon(Icons.Rounded.Add, contentDescription = "Add task")
              }
            }
          }
        }
      }
    }
  }
}

@Composable
private fun SnippetTaskRow(
  item: TaskWithDetails,
  onToggleComplete: () -> Unit,
  onClick: () -> Unit,
) {
  val task = item.task
  val isOverdue = DateTimeUtils.isOverdue(task.dueAt, task.isAllDay)
  val semantic = CadenceThemeTokens.colors
  val haptic = LocalHapticFeedback.current
  val nextAlert = item.reminders.filter { it.isEnabled && !it.isDismissed }.minByOrNull { it.triggerAt }

  val accentColor =
    item.category?.let { Color(it.colorHex) }
      ?: when (task.priority) {
        TaskPriority.URGENT -> semantic.urgent
        TaskPriority.HIGH -> semantic.high
        TaskPriority.MEDIUM -> semantic.medium
        TaskPriority.LOW -> semantic.low
      }

  Surface(
    modifier =
      Modifier.fillMaxWidth()
        .testTag("snippet_task_${task.id}")
        .clip(RoundedCornerShape(16.dp))
        .clickable(onClick = onClick),
    shape = RoundedCornerShape(16.dp),
    color = semantic.cardElevated,
    border =
      BorderStroke(
        1.2.dp,
        if (isOverdue) semantic.overdue.copy(alpha = 0.4f)
        else MaterialTheme.colorScheme.outlineVariant,
      ),
  ) {
    Row(
      modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Box(
        modifier =
          Modifier.width(4.dp)
            .fillMaxHeight()
            .background(if (isOverdue) semantic.overdue else accentColor)
      )

      Row(
        modifier = Modifier.weight(1f).padding(horizontal = 10.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        Box(
          modifier =
            Modifier.minimumInteractiveComponentSize()
              .testTag("snippet_check_${task.id}")
              .clip(CircleShape)
              .clickable {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onToggleComplete()
              },
          contentAlignment = Alignment.Center,
        ) {
          Box(
            modifier =
              Modifier.size(22.dp)
                .clip(CircleShape)
                .border(
                  width = 2.dp,
                  color = if (isOverdue) semantic.overdue else accentColor.copy(alpha = 0.7f),
                  shape = CircleShape,
                )
          )
        }

        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
          Text(
            text = task.title,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
          )

          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
          ) {
            PriorityBadge(priority = task.priority, compact = true)

            item.category?.let { cat ->
              CategoryPill(name = cat.name, iconKey = cat.iconKey, colorHex = cat.colorHex)
            }

            DateTimeUtils.formatDueLabel(task.dueAt, task.isAllDay)?.let { dueText ->
              Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                Icon(
                  imageVector = Icons.Rounded.Schedule,
                  contentDescription = null,
                  modifier = Modifier.size(12.dp),
                  tint = if (isOverdue) semantic.overdue else MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                  text = dueText,
                  style = MaterialTheme.typography.labelSmall,
                  color = if (isOverdue) semantic.overdue else MaterialTheme.colorScheme.onSurfaceVariant,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
                )
              }
            }

            nextAlert?.let { rem ->
              Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                Icon(
                  imageVector = Icons.Rounded.Alarm,
                  contentDescription = null,
                  modifier = Modifier.size(12.dp),
                  tint = MaterialTheme.colorScheme.primary,
                )
                Text(
                  text = DateTimeUtils.formatTime(rem.triggerAt),
                  style = MaterialTheme.typography.labelSmall,
                  color = MaterialTheme.colorScheme.primary,
                  maxLines = 1,
                )
              }
            }
          }
        }
      }
    }
  }
}
