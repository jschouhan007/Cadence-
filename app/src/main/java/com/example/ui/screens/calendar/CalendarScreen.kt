package com.example.ui.screens.calendar

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Event
import androidx.compose.material.icons.rounded.FilterList
import androidx.compose.material.icons.rounded.Today
import androidx.compose.material3.AssistChip
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.entity.TaskPriority
import com.example.data.local.entity.TaskStatus
import com.example.data.local.entity.TaskWithDetails
import com.example.domain.time.DateTimeUtils
import com.example.ui.components.CadenceTopBar
import com.example.ui.components.EmptyStateView
import com.example.ui.components.StudioSegmentedTabs
import com.example.ui.components.StudioTabOption
import com.example.ui.components.TaskRowCard
import com.example.ui.components.TopDockActionIcon
import com.example.ui.theme.CadenceThemeTokens
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.Spacing
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(
  viewModel: CalendarViewModel,
  onOpenQuickAddForDate: (Long) -> Unit,
  onOpenTaskDetail: (String) -> Unit,
  onOpenCategoryDetail: (String) -> Unit,
  onOpenSearch: () -> Unit,
  onOpenFocus: (String?) -> Unit,
  onOpenReminders: () -> Unit,
  onOpenSettings: () -> Unit,
) {
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()
  val semantic = CadenceThemeTokens.colors
  val todayMillis = remember { System.currentTimeMillis() }

  var showCategoryMenu by remember { mutableStateOf(false) }
  var showPriorityMenu by remember { mutableStateOf(false) }
  var reschedulingTask by remember { mutableStateOf<TaskWithDetails?>(null) }

  Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
    CadenceTopBar(
      title = "Schedule",
      subtitle = DateTimeUtils.formatFullDate(uiState.selectedDateMillis),
      onSearchClick = onOpenSearch,
      onFocusClick = { onOpenFocus(null) },
      onRemindersClick = onOpenReminders,
      onSettingsClick = onOpenSettings,
      extraActions = {
        TopDockActionIcon(
          icon = Icons.Rounded.Today,
          contentDescription = "Jump to today",
          tint = MaterialTheme.colorScheme.secondary,
          onClick = viewModel::jumpToToday,
        )
      },
    )

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
      LazyColumn(
        modifier = Modifier.widthIn(max = Spacing.maxContentWidth).fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 104.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        item(key = "calendar_scope_row") {
          StudioSegmentedTabs(
            options =
              CalendarViewScope.entries.map { sc ->
                StudioTabOption(
                  value = sc,
                  label = sc.label,
                  testTag = "calendar_scope_${sc.name.lowercase()}",
                )
              },
            selectedValue = uiState.scope,
            onSelect = { viewModel.setScope(it) },
          )
        }

        // Filters row
        item(key = "calendar_filters") {
          Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            Box {
              val activeCat = uiState.categories.find { it.id == uiState.filterCategoryId }
              AssistChip(
                onClick = { showCategoryMenu = true },
                label = { Text(activeCat?.name ?: "All categories") },
                leadingIcon = { Icon(Icons.Rounded.FilterList, contentDescription = null, modifier = Modifier.size(16.dp)) },
              )
              DropdownMenu(expanded = showCategoryMenu, onDismissRequest = { showCategoryMenu = false }) {
                DropdownMenuItem(
                  text = { Text("All categories") },
                  onClick = {
                    viewModel.setCategoryFilter(null)
                    showCategoryMenu = false
                  },
                )
                uiState.categories.forEach { cat ->
                  DropdownMenuItem(
                    text = { Text(cat.name) },
                    onClick = {
                      viewModel.setCategoryFilter(cat.id)
                      showCategoryMenu = false
                    },
                  )
                }
              }
            }

            Box {
              AssistChip(
                onClick = { showPriorityMenu = true },
                label = { Text(uiState.filterPriority?.label ?: "All priorities") },
              )
              DropdownMenu(expanded = showPriorityMenu, onDismissRequest = { showPriorityMenu = false }) {
                DropdownMenuItem(
                  text = { Text("All priorities") },
                  onClick = {
                    viewModel.setPriorityFilter(null)
                    showPriorityMenu = false
                  },
                )
                TaskPriority.entries.forEach { prio ->
                  DropdownMenuItem(
                    text = { Text(prio.label) },
                    onClick = {
                      viewModel.setPriorityFilter(prio)
                      showPriorityMenu = false
                    },
                  )
                }
              }
            }

            AssistChip(
              onClick = { onOpenQuickAddForDate(DateTimeUtils.endOfDay(uiState.selectedDateMillis)) },
              label = { Text("Add for ${DateTimeUtils.formatShortDate(uiState.selectedDateMillis)}") },
              leadingIcon = { Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(16.dp)) },
            )
          }
        }

        // Month, Week, or Day Surface
        when (uiState.scope) {
          CalendarViewScope.MONTH -> {
            item(key = "month_grid") {
              Surface(
                shape = RoundedCornerShape(22.dp),
                color = semantic.cardElevated,
                border = BorderStroke(1.2.dp, MaterialTheme.colorScheme.outlineVariant),
                tonalElevation = 1.dp,
              ) {
                Column(modifier = Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                  ) {
                    IconButton(onClick = { viewModel.shiftMonth(-1) }) {
                      Icon(Icons.AutoMirrored.Rounded.KeyboardArrowLeft, contentDescription = "Previous month")
                    }
                    Text(
                      text = uiState.monthTitle,
                      style = MaterialTheme.typography.titleMedium,
                      fontWeight = FontWeight.Bold,
                    )
                    IconButton(onClick = { viewModel.shiftMonth(1) }) {
                      Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = "Next month")
                    }
                  }

                  Row(modifier = Modifier.fillMaxWidth()) {
                    listOf("S", "M", "T", "W", "T", "F", "S").forEach { dow ->
                      Text(
                        text = dow,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f),
                      )
                    }
                  }

                  val rows = uiState.monthGridDays.chunked(7)
                  rows.forEach { week ->
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                      week.forEach { dayMillis ->
                        if (dayMillis == null) {
                          Spacer(modifier = Modifier.weight(1f).heightIn(min = 46.dp))
                        } else {
                          val cal = Calendar.getInstance().apply { timeInMillis = dayMillis }
                          val dayNum = cal.get(Calendar.DAY_OF_MONTH)
                          val isSelected = DateTimeUtils.isSameDay(dayMillis, uiState.selectedDateMillis)
                          val isToday = DateTimeUtils.isSameDay(dayMillis, todayMillis)
                          val dayTasks =
                            uiState.allFilteredTasks.filter {
                              it.task.dueAt != null && DateTimeUtils.isSameDay(it.task.dueAt, dayMillis)
                            }
                          val dotColors =
                            dayTasks.take(3).map { taskItem ->
                              when {
                                isSelected -> MaterialTheme.colorScheme.onPrimary
                                taskItem.task.status != TaskStatus.COMPLETED &&
                                  DateTimeUtils.isOverdue(taskItem.task.dueAt, taskItem.task.isAllDay, todayMillis) ->
                                  semantic.overdue
                                taskItem.category != null -> Color(taskItem.category.colorHex)
                                else -> MaterialTheme.colorScheme.primary
                              }
                            }

                          Column(
                            modifier =
                              Modifier.weight(1f)
                                .heightIn(min = 46.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                  when {
                                    isSelected -> MaterialTheme.colorScheme.primary
                                    isToday -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f)
                                    else -> Color.Transparent
                                  }
                                )
                                .let { base ->
                                  if (isToday && !isSelected) {
                                    base.border(1.2.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.55f), RoundedCornerShape(12.dp))
                                  } else base
                                }
                                .clickable { viewModel.selectDay(dayMillis) }
                                .padding(vertical = 6.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                          ) {
                            Text(
                              text = dayNum.toString(),
                              style = MaterialTheme.typography.labelLarge.copy(
                                fontFamily = JetBrainsMonoFamily,
                                fontFeatureSettings = "tnum",
                              ),
                              fontWeight = if (isSelected || isToday) FontWeight.Bold else FontWeight.Medium,
                              color =
                                when {
                                  isSelected -> MaterialTheme.colorScheme.onPrimary
                                  isToday -> MaterialTheme.colorScheme.primary
                                  else -> MaterialTheme.colorScheme.onSurface
                                },
                              maxLines = 1,
                            )
                            Spacer(modifier = Modifier.height(3.dp))
                            if (dotColors.isNotEmpty()) {
                              Row(
                                horizontalArrangement = Arrangement.spacedBy(2.dp),
                                verticalAlignment = Alignment.CenterVertically,
                              ) {
                                dotColors.forEach { dotColor ->
                                  Box(
                                    modifier =
                                      Modifier.size(5.dp)
                                        .clip(CircleShape)
                                        .background(dotColor)
                                  )
                                }
                              }
                            } else {
                              Spacer(modifier = Modifier.height(5.dp))
                            }
                          }
                        }
                      }
                    }
                  }
                }
              }
            }
          }

          CalendarViewScope.WEEK -> {
            item(key = "week_strip") {
              val dowFmt = remember { SimpleDateFormat("EEE", Locale.getDefault()) }
              val dayFmt = remember { SimpleDateFormat("d", Locale.getDefault()) }
              Surface(
                shape = RoundedCornerShape(22.dp),
                color = semantic.cardElevated,
                border = BorderStroke(1.2.dp, MaterialTheme.colorScheme.outlineVariant),
                tonalElevation = 1.dp,
              ) {
                Row(
                  modifier = Modifier.fillMaxWidth().padding(10.dp),
                  horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                  uiState.weekDays.forEach { dayMillis ->
                    val isSelected = DateTimeUtils.isSameDay(dayMillis, uiState.selectedDateMillis)
                    val isToday = DateTimeUtils.isSameDay(dayMillis, todayMillis)
                    val count =
                      uiState.allFilteredTasks.count {
                        it.task.dueAt != null && DateTimeUtils.isSameDay(it.task.dueAt, dayMillis)
                      }
                    Column(
                      modifier =
                        Modifier.weight(1f)
                          .clip(RoundedCornerShape(14.dp))
                          .background(
                            when {
                              isSelected -> MaterialTheme.colorScheme.primary
                              isToday -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                              else -> MaterialTheme.colorScheme.surfaceContainerLow
                            }
                          )
                          .clickable { viewModel.selectDay(dayMillis) }
                          .padding(vertical = 10.dp),
                      horizontalAlignment = Alignment.CenterHorizontally,
                      verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                      Text(
                        text = dowFmt.format(Date(dayMillis)),
                        style = MaterialTheme.typography.labelSmall,
                        color =
                          if (isSelected) MaterialTheme.colorScheme.onPrimary
                          else MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                      )
                      Text(
                        text = dayFmt.format(Date(dayMillis)),
                        style = MaterialTheme.typography.titleSmall.copy(
                          fontFamily = JetBrainsMonoFamily,
                          fontFeatureSettings = "tnum",
                        ),
                        fontWeight = FontWeight.Bold,
                        color =
                          if (isSelected) MaterialTheme.colorScheme.onPrimary
                          else MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                      )
                      Text(
                        text = if (count > 0) "$count" else "·",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color =
                          if (isSelected) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.9f)
                          else if (count > 0) MaterialTheme.colorScheme.secondary
                          else MaterialTheme.colorScheme.onSurfaceVariant,
                      )
                    }
                  }
                }
              }
            }
          }

          CalendarViewScope.DAY -> {
            item(key = "day_nav_bar") {
              Row(
                modifier =
                  Modifier.fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(semantic.cardElevated)
                    .border(1.2.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(18.dp))
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
              ) {
                IconButton(onClick = { viewModel.selectDay(DateTimeUtils.addDays(uiState.selectedDateMillis, -1)) }) {
                  Icon(Icons.AutoMirrored.Rounded.KeyboardArrowLeft, contentDescription = "Previous day")
                }
                Text(
                  text = DateTimeUtils.formatFullDate(uiState.selectedDateMillis),
                  style = MaterialTheme.typography.titleMedium,
                  fontWeight = FontWeight.Bold,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
                )
                IconButton(onClick = { viewModel.selectDay(DateTimeUtils.addDays(uiState.selectedDateMillis, 1)) }) {
                  Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = "Next day")
                }
              }
            }
          }
        }

        // Day Agenda Section
        val timedTasks = uiState.tasksForSelectedDay.filter { !it.task.isAllDay }
        val allDayTasks = uiState.tasksForSelectedDay.filter { it.task.isAllDay }

        item(key = "agenda_header") {
          Row(
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Text(
              text = "Agenda · ${DateTimeUtils.formatShortDate(uiState.selectedDateMillis)}",
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Bold,
            )
            Surface(
              shape = RoundedCornerShape(9.dp),
              color = MaterialTheme.colorScheme.primaryContainer,
            ) {
              Text(
                text = "${uiState.tasksForSelectedDay.size} scheduled",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = 9.dp, vertical = 3.dp),
              )
            }
          }
        }

        if (uiState.tasksForSelectedDay.isEmpty()) {
          item(key = "day_empty") {
            EmptyStateView(
              icon = Icons.Rounded.Event,
              title = "No tasks scheduled",
              message = "Nothing due on ${DateTimeUtils.formatShortDate(uiState.selectedDateMillis)}. Add a task or reschedule an unscheduled item.",
              actionLabel = "Schedule Task",
              onAction = { onOpenQuickAddForDate(DateTimeUtils.endOfDay(uiState.selectedDateMillis)) },
              testTag = "calendar_day_empty",
            )
          }
        } else {
          if (timedTasks.isNotEmpty()) {
            item(key = "timed_label") {
              Text(
                text = "Timed",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
              )
            }
            items(timedTasks, key = { "timed_${it.task.id}" }) { item ->
              TaskRowCard(
                item = item,
                onClick = { onOpenTaskDetail(item.task.id) },
                onToggleComplete = { viewModel.toggleTaskCompletion(item.task.id) },
                onCategoryClick = onOpenCategoryDetail,
                onStartFocus = { onOpenFocus(item.task.id) },
                onMoveCategory = { reschedulingTask = item },
                onDelete = { viewModel.deleteTask(item.task.id) },
              )
            }
          }

          if (allDayTasks.isNotEmpty()) {
            item(key = "all_day_label") {
              Text(
                text = "All-day",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = semantic.marigold,
              )
            }
            items(allDayTasks, key = { "allday_${it.task.id}" }) { item ->
              TaskRowCard(
                item = item,
                onClick = { onOpenTaskDetail(item.task.id) },
                onToggleComplete = { viewModel.toggleTaskCompletion(item.task.id) },
                onCategoryClick = onOpenCategoryDetail,
                onStartFocus = { onOpenFocus(item.task.id) },
                onDelete = { viewModel.deleteTask(item.task.id) },
              )
            }
          }
        }

        if (uiState.unscheduledTasks.isNotEmpty()) {
          item(key = "unscheduled_header") {
            Text(
              text = "Unscheduled (${uiState.unscheduledTasks.size})",
              style = MaterialTheme.typography.titleSmall,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              modifier = Modifier.padding(top = 8.dp),
            )
          }
          items(uiState.unscheduledTasks.take(5), key = { "unsched_${it.task.id}" }) { item ->
            Surface(
              modifier =
                Modifier.fillMaxWidth()
                  .clip(RoundedCornerShape(16.dp))
                  .clickable { onOpenTaskDetail(item.task.id) },
              shape = RoundedCornerShape(16.dp),
              color = semantic.cardElevated,
              border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            ) {
              Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
              ) {
                Text(
                  text = item.task.title,
                  style = MaterialTheme.typography.bodyMedium,
                  fontWeight = FontWeight.Medium,
                  maxLines = 2,
                  overflow = TextOverflow.Ellipsis,
                  modifier = Modifier.weight(1f),
                )
                TextButton(onClick = { reschedulingTask = item }) {
                  Icon(Icons.Rounded.CalendarMonth, contentDescription = null, modifier = Modifier.size(16.dp))
                  Spacer(modifier = Modifier.width(6.dp))
                  Text("Schedule", maxLines = 1)
                }
              }
            }
          }
        }
      }
    }
  }

  reschedulingTask?.let { task ->
    val pickerState = rememberDatePickerState(initialSelectedDateMillis = uiState.selectedDateMillis)
    DatePickerDialog(
      onDismissRequest = { reschedulingTask = null },
      confirmButton = {
        TextButton(
          onClick = {
            pickerState.selectedDateMillis?.let { millis ->
              viewModel.rescheduleTask(task.task.id, millis)
            }
            reschedulingTask = null
          }
        ) {
          Text("Reschedule")
        }
      },
      dismissButton = { TextButton(onClick = { reschedulingTask = null }) { Text("Cancel") } },
    ) {
      DatePicker(state = pickerState)
    }
  }
}
