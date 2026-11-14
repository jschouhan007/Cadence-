package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.DriveFileMove
import androidx.compose.material.icons.rounded.Alarm
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Checklist
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.PlayCircleOutline
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.local.entity.RecurrenceFrequency
import com.example.data.local.entity.TaskPriority
import com.example.data.local.entity.TaskStatus
import com.example.data.local.entity.TaskWithDetails
import com.example.domain.time.DateTimeUtils
import com.example.ui.theme.CadenceThemeTokens
import com.example.ui.theme.MotionDuration

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TaskRowCard(
  item: TaskWithDetails,
  onClick: () -> Unit,
  onToggleComplete: () -> Unit,
  modifier: Modifier = Modifier,
  onCategoryClick: ((String) -> Unit)? = null,
  onStartFocus: (() -> Unit)? = null,
  onDuplicate: (() -> Unit)? = null,
  onMoveCategory: (() -> Unit)? = null,
  onDelete: (() -> Unit)? = null,
  showReorderControls: Boolean = false,
  canMoveUp: Boolean = false,
  canMoveDown: Boolean = false,
  onMoveUp: (() -> Unit)? = null,
  onMoveDown: (() -> Unit)? = null,
  isDragging: Boolean = false,
) {
  val task = item.task
  val isCompleted = task.status == TaskStatus.COMPLETED
  val isOverdue = !isCompleted && DateTimeUtils.isOverdue(task.dueAt, task.isAllDay)
  val semantic = CadenceThemeTokens.colors
  val haptic = LocalHapticFeedback.current
  var menuExpanded by remember { mutableStateOf(false) }

  val (priorityColor, priorityContainer) =
    when (task.priority) {
      TaskPriority.URGENT -> semantic.urgent to semantic.urgentContainer
      TaskPriority.HIGH -> semantic.high to semantic.highContainer
      TaskPriority.MEDIUM -> semantic.medium to semantic.mediumContainer
      TaskPriority.LOW -> semantic.low to semantic.lowContainer
    }
  val stripColor =
    if (isCompleted) semantic.success.copy(alpha = 0.45f)
    else priorityColor

  val cardAlpha = if (isCompleted) 0.72f else 1f
  val cardBackground =
    when {
      isCompleted -> semantic.cardElevated
      task.priority == TaskPriority.URGENT -> lerp(semantic.cardElevated, semantic.urgentContainer, 0.24f)
      task.priority == TaskPriority.HIGH -> lerp(semantic.cardElevated, semantic.highContainer, 0.14f)
      else -> semantic.cardElevated
    }
  val borderColor =
    when {
      isDragging -> MaterialTheme.colorScheme.primary
      isCompleted -> MaterialTheme.colorScheme.outlineVariant
      isOverdue -> semantic.overdue.copy(alpha = 0.55f)
      task.priority == TaskPriority.URGENT -> semantic.urgent.copy(alpha = 0.40f)
      task.priority == TaskPriority.HIGH -> semantic.high.copy(alpha = 0.32f)
      task.priority == TaskPriority.MEDIUM -> semantic.medium.copy(alpha = 0.22f)
      else -> MaterialTheme.colorScheme.outlineVariant
    }

  Surface(
    modifier =
      modifier
        .fillMaxWidth()
        .alpha(cardAlpha)
        .testTag("task_card_${task.id}")
        .clip(RoundedCornerShape(18.dp))
        .clickable(onClick = onClick),
    shape = RoundedCornerShape(18.dp),
    color = cardBackground,
    border = BorderStroke(if (isDragging) 2.dp else 1.2.dp, borderColor),
    tonalElevation = if (isDragging) 6.dp else if (isCompleted) 0.dp else 1.dp,
    shadowElevation = if (isDragging) 8.dp else 0.dp,
  ) {
    Row(
      modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min),
      verticalAlignment = Alignment.Top,
    ) {
      // Tactile Sunlit Studio Priority Accent Bar
      Box(
        modifier =
          Modifier.padding(start = 8.dp, top = 12.dp, bottom = 12.dp)
            .width(4.5.dp)
            .fillMaxHeight()
            .clip(RoundedCornerShape(4.dp))
            .background(stripColor)
            .testTag("task_priority_strip_${task.id}")
      )

      Row(
        modifier = Modifier.weight(1f).padding(start = 6.dp, end = 8.dp, top = 12.dp, bottom = 12.dp),
        verticalAlignment = Alignment.Top,
      ) {
        val uncheckedFill =
          if (!isCompleted && (task.priority == TaskPriority.URGENT || task.priority == TaskPriority.HIGH)) {
            priorityContainer.copy(alpha = 0.45f)
          } else {
            Color.Transparent
          }
        val checkBg by
          animateColorAsState(
            targetValue = if (isCompleted) semantic.success else uncheckedFill,
            animationSpec = tween(MotionDuration.Fast),
            label = "taskCheckBg",
          )
        val checkBorder by
          animateColorAsState(
            targetValue =
              when {
                isCompleted -> semantic.success
                isOverdue -> semantic.overdue
                else -> priorityColor
              },
            animationSpec = tween(MotionDuration.Fast),
            label = "taskCheckBorder",
          )
        val checkScale by
          animateFloatAsState(
            targetValue = if (isCompleted) 1.05f else 1f,
            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
            label = "taskCheckScale",
          )

        Box(
          modifier =
            Modifier.minimumInteractiveComponentSize()
              .testTag("task_check_${task.id}")
              .clip(CircleShape)
              .clickable {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onToggleComplete()
              },
          contentAlignment = Alignment.Center,
        ) {
          Box(
            modifier =
              Modifier.size(24.dp)
                .scale(checkScale)
                .clip(CircleShape)
                .background(checkBg)
                .border(width = 2.dp, color = checkBorder, shape = CircleShape),
            contentAlignment = Alignment.Center,
          ) {
            if (isCompleted) {
              Icon(
                imageVector = Icons.Rounded.Check,
                contentDescription = "Mark task incomplete",
                tint = Color.White,
                modifier = Modifier.size(15.dp),
              )
            }
          }
        }

        Spacer(modifier = Modifier.width(6.dp))

        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
          Text(
            text = task.title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            textDecoration = if (isCompleted) TextDecoration.LineThrough else TextDecoration.None,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
          )

          if (task.description.isNotBlank()) {
            Text(
              text = task.description,
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              maxLines = 2,
              overflow = TextOverflow.Ellipsis,
            )
          }

          FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
          ) {
            PriorityBadge(
              priority = task.priority,
              compact = true,
              modifier = Modifier.testTag("task_priority_badge_${task.id}"),
            )

            item.category?.let { cat ->
              CategoryPill(
                name = cat.name,
                iconKey = cat.iconKey,
                colorHex = cat.colorHex,
                onClick = onCategoryClick?.let { cb -> { cb(cat.id) } },
              )
            }

            DateTimeUtils.formatDueLabel(task.dueAt, task.isAllDay)?.let { dueText ->
              val dueBg =
                if (isOverdue) semantic.overdueContainer
                else MaterialTheme.colorScheme.surfaceContainerLow
              val dueFg =
                if (isOverdue) semantic.overdue
                else MaterialTheme.colorScheme.onSurfaceVariant
              StatusPill(
                label = if (isOverdue) "Overdue · $dueText" else dueText,
                icon = Icons.Rounded.Schedule,
                containerColor = dueBg,
                contentColor = dueFg,
              )
            }

            if (task.estimatedMinutes != null && task.estimatedMinutes > 0) {
              StatusPill(
                label = "${task.estimatedMinutes}m",
                icon = Icons.Rounded.Schedule,
                containerColor = semantic.marigoldContainer.copy(alpha = 0.7f),
                contentColor = semantic.marigold,
              )
            }

            if (item.subtasks.isNotEmpty()) {
              val done = item.subtasks.count { it.isCompleted }
              StatusPill(
                label = "$done/${item.subtasks.size}",
                icon = Icons.Rounded.Checklist,
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }

            val activeReminders =
              item.reminders.filter { it.isEnabled && !it.isDismissed }.sortedBy { it.triggerAt }
            if (activeReminders.isNotEmpty()) {
              val nextAlert = activeReminders.first()
              val alertText =
                if (activeReminders.size == 1) {
                  DateTimeUtils.formatDateTimeShort(nextAlert.triggerAt)
                } else {
                  "${DateTimeUtils.formatDateTimeShort(nextAlert.triggerAt)} (+${activeReminders.size - 1})"
                }
              StatusPill(
                label = alertText,
                icon = Icons.Rounded.Alarm,
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
              )
            }

            if (task.recurrenceFrequency != RecurrenceFrequency.NONE) {
              StatusPill(
                label = task.recurrenceFrequency.label,
                icon = Icons.Rounded.Repeat,
                containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.7f),
                contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
              )
            }
          }
        }

        if (showReorderControls) {
          Column(horizontalAlignment = Alignment.CenterHorizontally) {
            IconButton(
              onClick = { onMoveUp?.invoke() },
              enabled = canMoveUp,
              modifier = Modifier.size(40.dp),
            ) {
              Icon(
                imageVector = Icons.Rounded.ArrowUpward,
                contentDescription = "Move task up",
                modifier = Modifier.size(18.dp),
              )
            }
            IconButton(
              onClick = { onMoveDown?.invoke() },
              enabled = canMoveDown,
              modifier = Modifier.size(40.dp),
            ) {
              Icon(
                imageVector = Icons.Rounded.ArrowDownward,
                contentDescription = "Move task down",
                modifier = Modifier.size(18.dp),
              )
            }
          }
        } else if (
          onStartFocus != null || onDuplicate != null || onMoveCategory != null || onDelete != null
        ) {
          Box {
            IconButton(
              onClick = { menuExpanded = true },
              modifier = Modifier.testTag("task_menu_${task.id}"),
            ) {
              Icon(
                imageVector = Icons.Rounded.MoreVert,
                contentDescription = "Task options",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
            DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
              if (onStartFocus != null) {
                DropdownMenuItem(
                  text = { Text("Focus on task") },
                  leadingIcon = { Icon(Icons.Rounded.PlayCircleOutline, contentDescription = null) },
                  onClick = {
                    menuExpanded = false
                    onStartFocus()
                  },
                )
              }
              if (onMoveCategory != null) {
                DropdownMenuItem(
                  text = { Text("Move category") },
                  leadingIcon = { Icon(Icons.AutoMirrored.Rounded.DriveFileMove, contentDescription = null) },
                  onClick = {
                    menuExpanded = false
                    onMoveCategory()
                  },
                )
              }
              if (onDuplicate != null) {
                DropdownMenuItem(
                  text = { Text("Duplicate") },
                  leadingIcon = { Icon(Icons.Rounded.ContentCopy, contentDescription = null) },
                  onClick = {
                    menuExpanded = false
                    onDuplicate()
                  },
                )
              }
              if (onDelete != null) {
                DropdownMenuItem(
                  text = { Text("Delete", color = MaterialTheme.colorScheme.error) },
                  leadingIcon = {
                    Icon(
                      Icons.Rounded.DeleteOutline,
                      contentDescription = null,
                      tint = MaterialTheme.colorScheme.error,
                    )
                  },
                  onClick = {
                    menuExpanded = false
                    onDelete()
                  },
                )
              }
            }
          }
        }
      }
    }
  }
}
