package com.example.ui.screens.tasks

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.scrollBy
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Sort
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Archive
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.DragIndicator
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.FilterList
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.Inbox
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.SearchOff
import androidx.compose.material.icons.rounded.SwapVert
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.entity.CategoryEntity
import com.example.data.local.entity.SortDirection
import com.example.data.local.entity.TaskFilterSection
import com.example.data.local.entity.TaskPriority
import com.example.data.local.entity.TaskSortOption
import com.example.data.local.entity.TaskWithDetails
import com.example.domain.model.CategoryWithStats
import com.example.ui.components.CadenceTopBar
import com.example.ui.components.CategoryEditorDialog
import com.example.ui.components.EmptyStateView
import com.example.ui.components.MoveCategoryDialog
import com.example.ui.components.PrioritySignalMeter
import com.example.ui.components.SafeDeleteCategoryDialog
import com.example.ui.components.StudioSegmentedTabs
import com.example.ui.components.StudioTabOption
import com.example.ui.components.TaskRowCard
import com.example.ui.components.categoryIconForKey
import com.example.ui.theme.CadenceThemeTokens
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.Spacing
import kotlinx.coroutines.launch

@Composable
fun TasksScreen(
  viewModel: TasksViewModel,
  snackbarHostState: SnackbarHostState,
  onOpenQuickAdd: () -> Unit,
  onOpenTaskDetail: (String) -> Unit,
  onOpenCategoryDetail: (String) -> Unit,
  onOpenSearch: () -> Unit,
  onOpenFocus: (String?) -> Unit,
  onOpenReminders: () -> Unit,
  onOpenSettings: () -> Unit,
) {
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()
  val scope = rememberCoroutineScope()
  val focusManager = LocalFocusManager.current
  val haptic = LocalHapticFeedback.current
  val semantic = CadenceThemeTokens.colors

  var showCategoryEditor by remember { mutableStateOf(false) }
  var editingCategory by remember { mutableStateOf<CategoryEntity?>(null) }
  var deletingCategoryStats by remember { mutableStateOf<CategoryWithStats?>(null) }
  var movingTask by remember { mutableStateOf<TaskWithDetails?>(null) }
  var showSortMenu by remember { mutableStateOf(false) }
  var showPriorityMenu by remember { mutableStateOf(false) }

  Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
    CadenceTopBar(
      title = "Workspace",
      subtitle = "${uiState.inboxCount} in Inbox · ${uiState.categoriesWithStats.size} categories",
      reminderBadgeCount = uiState.reminderBadgeCount,
      onSearchClick = onOpenSearch,
      onFocusClick = { onOpenFocus(null) },
      onRemindersClick = onOpenReminders,
      onSettingsClick = onOpenSettings,
    )

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
      Column(modifier = Modifier.widthIn(max = Spacing.maxContentWidth).fillMaxSize()) {
        // Tab Switcher: Tasks vs Categories
        StudioSegmentedTabs(
          options =
            TasksTabMode.entries.map { mode ->
              StudioTabOption(
                value = mode,
                label = mode.label,
                testTag = "tasks_tab_${mode.name.lowercase()}",
              )
            },
          selectedValue = uiState.tabMode,
          onSelect = { viewModel.setTabMode(it) },
          modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
        )

        if (uiState.isLoading) {
          Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
          }
        } else if (uiState.tabMode == TasksTabMode.TASKS) {
          val isSearching = uiState.searchQuery.isNotBlank()

          // Real-time local Room search bar
          OutlinedTextField(
            value = uiState.searchQuery,
            onValueChange = viewModel::setSearchQuery,
            placeholder = {
              Text(
                text = "Filter tasks by title or description...",
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
              )
            },
            leadingIcon = {
              Icon(
                imageVector = Icons.Rounded.Search,
                contentDescription = "Search tasks",
                tint =
                  if (isSearching) MaterialTheme.colorScheme.primary
                  else MaterialTheme.colorScheme.onSurfaceVariant,
              )
            },
            trailingIcon = {
              if (uiState.searchQuery.isNotEmpty()) {
                IconButton(
                  onClick = viewModel::clearSearchQuery,
                  modifier = Modifier.testTag("tasks_search_clear"),
                ) {
                  Icon(
                    imageVector = Icons.Rounded.Close,
                    contentDescription = "Clear search",
                  )
                }
              }
            },
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            colors =
              OutlinedTextFieldDefaults.colors(
                focusedContainerColor = semantic.cardElevated,
                unfocusedContainerColor = semantic.cardElevated,
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
              ),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
            modifier =
              Modifier.fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp)
                .testTag("tasks_search_input"),
          )

          AnimatedVisibility(visible = isSearching) {
            Row(
              modifier =
                Modifier.fillMaxWidth()
                  .padding(horizontal = 18.dp, vertical = 2.dp)
                  .testTag("tasks_search_result_banner"),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.SpaceBetween,
            ) {
              Text(
                text =
                  if (uiState.tasks.size == 1) "1 match in ${uiState.section.label}"
                  else "${uiState.tasks.size} matches in ${uiState.section.label}",
                style = MaterialTheme.typography.labelMedium,
                fontFamily = JetBrainsMonoFamily,
                color = MaterialTheme.colorScheme.primary,
              )
              if (uiState.section != TaskFilterSection.ALL && uiState.allSectionMatchCount > uiState.tasks.size) {
                TextButton(
                  onClick = { viewModel.setSection(TaskFilterSection.ALL) },
                  contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                  modifier = Modifier.testTag("tasks_search_switch_all"),
                ) {
                  Text(
                    text = "See all (${uiState.allSectionMatchCount})",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                  )
                }
              }
            }
          }

          // Section Filter Chips
          Row(
            modifier =
              Modifier.fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            TaskFilterSection.entries.forEach { sec ->
              val label =
                if (sec == TaskFilterSection.INBOX && uiState.inboxCount > 0) "Inbox (${uiState.inboxCount})"
                else sec.label
              FilterChip(
                selected = uiState.section == sec,
                onClick = { viewModel.setSection(sec) },
                label = { Text(label) },
                modifier = Modifier.testTag("section_chip_${sec.name.lowercase()}"),
              )
            }
          }

          // Priority, Sort, Asc/Desc Toggle, Reorder Action Row
          Row(
            modifier =
              Modifier.fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Box {
              val selectedPrioColor =
                when (uiState.selectedPriority) {
                  TaskPriority.URGENT -> semantic.urgent
                  TaskPriority.HIGH -> semantic.high
                  TaskPriority.MEDIUM -> semantic.medium
                  TaskPriority.LOW -> semantic.low
                  null -> MaterialTheme.colorScheme.onSurfaceVariant
                }
              AssistChip(
                onClick = { showPriorityMenu = true },
                label = { Text(uiState.selectedPriority?.label ?: "All priorities") },
                leadingIcon = {
                  val selectedPrio = uiState.selectedPriority
                  if (selectedPrio != null) {
                    PrioritySignalMeter(priority = selectedPrio, tint = selectedPrioColor)
                  } else {
                    Icon(Icons.Rounded.FilterList, contentDescription = null, modifier = Modifier.size(16.dp))
                  }
                },
                modifier = Modifier.testTag("tasks_priority_filter_chip"),
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
                  val prioColor =
                    when (prio) {
                      TaskPriority.URGENT -> semantic.urgent
                      TaskPriority.HIGH -> semantic.high
                      TaskPriority.MEDIUM -> semantic.medium
                      TaskPriority.LOW -> semantic.low
                    }
                  DropdownMenuItem(
                    text = { Text(prio.label, color = prioColor, fontWeight = FontWeight.SemiBold) },
                    leadingIcon = { PrioritySignalMeter(priority = prio, tint = prioColor) },
                    onClick = {
                      viewModel.setPriorityFilter(prio)
                      showPriorityMenu = false
                    },
                  )
                }
              }
            }

            Box {
              AssistChip(
                onClick = { showSortMenu = true },
                label = { Text(uiState.sortOption.label) },
                leadingIcon = {
                  Icon(Icons.AutoMirrored.Rounded.Sort, contentDescription = null, modifier = Modifier.size(16.dp))
                },
                modifier = Modifier.testTag("tasks_sort_menu_chip"),
              )
              DropdownMenu(expanded = showSortMenu, onDismissRequest = { showSortMenu = false }) {
                TaskSortOption.entries.forEach { opt ->
                  val isSelected = uiState.sortOption == opt
                  DropdownMenuItem(
                    text = {
                      Text(
                        text = opt.label,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                      )
                    },
                    trailingIcon =
                      if (isSelected) {
                        {
                          Icon(
                            imageVector = Icons.Rounded.Check,
                            contentDescription = "Selected",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp),
                          )
                        }
                      } else null,
                    onClick = {
                      viewModel.setSortOption(opt)
                      showSortMenu = false
                    },
                    modifier = Modifier.testTag("tasks_sort_option_${opt.name.lowercase()}"),
                  )
                }
                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                DropdownMenuItem(
                  text = {
                    Text(
                      text =
                        if (uiState.sortDirection == SortDirection.ASCENDING) "Order: Ascending (Tap to flip)"
                        else "Order: Descending (Tap to flip)",
                      fontWeight = FontWeight.Medium,
                    )
                  },
                  leadingIcon = {
                    Icon(
                      imageVector =
                        if (uiState.sortDirection == SortDirection.ASCENDING) Icons.Rounded.ArrowUpward
                        else Icons.Rounded.ArrowDownward,
                      contentDescription = null,
                      tint = MaterialTheme.colorScheme.primary,
                      modifier = Modifier.size(16.dp),
                    )
                  },
                  onClick = {
                    viewModel.toggleSortDirection()
                    showSortMenu = false
                  },
                )
              }
            }

            FilterChip(
              selected = uiState.sortOption != TaskSortOption.MANUAL,
              onClick = viewModel::toggleSortDirection,
              label = { Text(uiState.sortDirection.shortLabel) },
              leadingIcon = {
                Icon(
                  imageVector =
                    if (uiState.sortDirection == SortDirection.ASCENDING) Icons.Rounded.ArrowUpward
                    else Icons.Rounded.ArrowDownward,
                  contentDescription = "Toggle sort direction, currently ${uiState.sortDirection.label}",
                  modifier = Modifier.size(16.dp),
                )
              },
              modifier = Modifier.testTag("tasks_sort_direction_toggle"),
            )

            FilterChip(
              selected = uiState.isReorderMode,
              onClick = viewModel::toggleReorderMode,
              enabled = !isSearching,
              label = { Text(if (uiState.isReorderMode) "Done" else "Reorder") },
              leadingIcon = {
                Icon(Icons.Rounded.SwapVert, contentDescription = null, modifier = Modifier.size(16.dp))
              },
              modifier = Modifier.testTag("tasks_reorder_toggle"),
            )
          }

          if (uiState.tasks.isEmpty()) {
            if (isSearching) {
              val canShowAllMatches =
                uiState.section != TaskFilterSection.ALL && uiState.allSectionMatchCount > 0
              EmptyStateView(
                icon = Icons.Rounded.SearchOff,
                title = "No matching tasks",
                message =
                  "No tasks in ${uiState.section.label} match \"${uiState.searchQuery.trim()}\" in title or description.",
                actionLabel =
                  if (canShowAllMatches) "View ${uiState.allSectionMatchCount} in All"
                  else "Clear search",
                onAction = {
                  if (canShowAllMatches) {
                    viewModel.setSection(TaskFilterSection.ALL)
                  } else {
                    viewModel.clearSearchQuery()
                  }
                },
                testTag = "tasks_search_empty_state",
              )
            } else {
              EmptyStateView(
                icon = Icons.Rounded.Inbox,
                title = "No tasks in ${uiState.section.label}",
                message = "Capture a task quickly or adjust your active priority filters.",
                actionLabel = "Add Task",
                onAction = onOpenQuickAdd,
                testTag = "tasks_empty_state",
              )
            }
          } else {
            val listState = rememberLazyListState()
            var localTasks by remember { mutableStateOf(uiState.tasks) }
            var draggedTaskId by remember { mutableStateOf<String?>(null) }
            var draggingOffsetY by remember { mutableFloatStateOf(0f) }

            LaunchedEffect(uiState.tasks, draggedTaskId) {
              if (draggedTaskId == null) {
                localTasks = uiState.tasks
              }
            }

            val dragEnabled = !isSearching && localTasks.size > 1
            val currentUiTasks by rememberUpdatedState(uiState.tasks)
            val currentLocalTasks by rememberUpdatedState(localTasks)
            val currentDraggedId by rememberUpdatedState(draggedTaskId)
            val currentOnCommitReorder by rememberUpdatedState(viewModel::commitDragReorder)

            if (dragEnabled) {
              Row(
                modifier =
                  Modifier.fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
              ) {
                Icon(
                  imageVector = Icons.Rounded.DragIndicator,
                  contentDescription = null,
                  tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                  modifier = Modifier.size(14.dp),
                )
                Text(
                  text =
                    if (uiState.isReorderMode) "Press & drag any task or use arrows to reorder"
                    else "Press & hold any task to drag-reorder priority",
                  style = MaterialTheme.typography.labelSmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
                )
              }
            }

            LazyColumn(
              state = listState,
              modifier = Modifier.fillMaxSize().testTag("tasks_list"),
              contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 104.dp),
              verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
              itemsIndexed(localTasks, key = { _, item -> item.task.id }) { index, item ->
                val isBeingDragged = draggedTaskId == item.task.id
                val itemModifier =
                  Modifier
                    .then(if (isBeingDragged) Modifier else Modifier.animateItem())
                    .zIndex(if (isBeingDragged) 2f else 0f)
                    .graphicsLayer {
                      translationY = if (isBeingDragged) draggingOffsetY else 0f
                      scaleX = if (isBeingDragged) 1.02f else 1f
                      scaleY = if (isBeingDragged) 1.02f else 1f
                    }
                    .pointerInput(item.task.id, dragEnabled) {
                      if (!dragEnabled) return@pointerInput
                      detectDragGesturesAfterLongPress(
                        onDragStart = {
                          draggedTaskId = item.task.id
                          draggingOffsetY = 0f
                          haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        },
                        onDragEnd = {
                          if (currentDraggedId == item.task.id) {
                            val committedOrder = currentLocalTasks.map { it.task.id }
                            draggedTaskId = null
                            draggingOffsetY = 0f
                            currentOnCommitReorder(committedOrder)
                          }
                        },
                        onDragCancel = {
                          draggedTaskId = null
                          draggingOffsetY = 0f
                          localTasks = currentUiTasks
                        },
                        onDrag = { change, dragAmount ->
                          change.consume()
                          if (currentDraggedId != item.task.id) return@detectDragGesturesAfterLongPress
                          draggingOffsetY += dragAmount.y

                          val visibleItems = listState.layoutInfo.visibleItemsInfo
                          val draggedInfo =
                            visibleItems.firstOrNull { it.key == item.task.id }
                              ?: return@detectDragGesturesAfterLongPress
                          val activeIndex = currentLocalTasks.indexOfFirst { it.task.id == item.task.id }
                          if (activeIndex == -1 || draggedInfo.index != activeIndex) {
                            return@detectDragGesturesAfterLongPress
                          }

                          val draggedTop = draggedInfo.offset + draggingOffsetY
                          val draggedBottom = draggedTop + draggedInfo.size
                          val draggedCenter = draggedTop + draggedInfo.size / 2f

                          if (draggingOffsetY > 0f && activeIndex < currentLocalTasks.lastIndex) {
                            val nextItem = currentLocalTasks[activeIndex + 1]
                            val nextInfo = visibleItems.firstOrNull { it.key == nextItem.task.id }
                            if (nextInfo != null && draggedCenter > nextInfo.offset + nextInfo.size * 0.45f) {
                              val mutable = currentLocalTasks.toMutableList()
                              val moved = mutable.removeAt(activeIndex)
                              mutable.add(activeIndex + 1, moved)
                              localTasks = mutable
                              draggingOffsetY -= (nextInfo.offset - draggedInfo.offset)
                              haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            }
                          } else if (draggingOffsetY < 0f && activeIndex > 0) {
                            val prevItem = currentLocalTasks[activeIndex - 1]
                            val prevInfo = visibleItems.firstOrNull { it.key == prevItem.task.id }
                            if (prevInfo != null && draggedCenter < prevInfo.offset + prevInfo.size * 0.55f) {
                              val mutable = currentLocalTasks.toMutableList()
                              val moved = mutable.removeAt(activeIndex)
                              mutable.add(activeIndex - 1, moved)
                              localTasks = mutable
                              draggingOffsetY += (draggedInfo.offset - prevInfo.offset)
                              haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            }
                          }

                          val viewportStart = listState.layoutInfo.viewportStartOffset
                          val viewportEnd = listState.layoutInfo.viewportEndOffset
                          if (draggedTop < viewportStart + 48) {
                            scope.launch { listState.scrollBy(-14f) }
                          } else if (draggedBottom > viewportEnd - 48) {
                            scope.launch { listState.scrollBy(14f) }
                          }
                        },
                      )
                    }

                TaskRowCard(
                  item = item,
                  modifier = itemModifier,
                  isDragging = isBeingDragged,
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
                  onMoveCategory = { movingTask = item },
                  onDuplicate = { viewModel.duplicateTask(item.task.id) },
                  onDelete = { viewModel.deleteTask(item.task.id) },
                  showReorderControls = uiState.isReorderMode,
                  canMoveUp = index > 0,
                  canMoveDown = index < localTasks.lastIndex,
                  onMoveUp = { viewModel.moveTask(item.task.id, moveUp = true) },
                  onMoveDown = { viewModel.moveTask(item.task.id, moveUp = false) },
                )
              }
            }
          }
        } else {
          // Categories Workspace List
          Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Text(
              text = "Your Categories",
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Bold,
            )
            Button(
              onClick = {
                editingCategory = null
                showCategoryEditor = true
              },
              shape = RoundedCornerShape(14.dp),
              contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
              modifier = Modifier.testTag("add_category_button"),
            ) {
              Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
              Spacer(modifier = Modifier.width(6.dp))
              Text("New Category", maxLines = 1)
            }
          }

          if (uiState.categoriesWithStats.isEmpty()) {
            EmptyStateView(
              icon = Icons.Rounded.FolderOpen,
              title = "No categories yet",
              message = "Create categories for College, Projects, Personal errands, or Fitness to keep work structured.",
              actionLabel = "Create Category",
              onAction = {
                editingCategory = null
                showCategoryEditor = true
              },
              testTag = "categories_empty_state",
            )
          } else {
            LazyColumn(
              modifier = Modifier.fillMaxSize(),
              contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 104.dp),
              verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
              itemsIndexed(uiState.categoriesWithStats, key = { _, stat -> stat.category.id }) {
                index,
                stat ->
                CategoryCard(
                  stats = stat,
                  canMoveUp = index > 0,
                  canMoveDown = index < uiState.categoriesWithStats.lastIndex,
                  onClick = { onOpenCategoryDetail(stat.category.id) },
                  onEdit = {
                    editingCategory = stat.category
                    showCategoryEditor = true
                  },
                  onTogglePin = { viewModel.togglePinCategory(stat.category.id) },
                  onMoveUp = { viewModel.moveCategory(stat.category.id, moveUp = true) },
                  onMoveDown = { viewModel.moveCategory(stat.category.id, moveUp = false) },
                  onArchive = { viewModel.archiveCategory(stat.category.id) },
                  onDelete = { deletingCategoryStats = stat },
                )
              }
            }
          }
        }
      }
    }
  }

  if (showCategoryEditor) {
    CategoryEditorDialog(
      initialCategory = editingCategory,
      onDismiss = { showCategoryEditor = false },
      onSave = { name, desc, iconKey, colorHex ->
        val existing = editingCategory
        if (existing == null) {
          viewModel.createCategory(name, desc, iconKey, colorHex)
        } else {
          viewModel.updateCategory(existing.id, name, desc, iconKey, colorHex)
        }
      },
    )
  }

  deletingCategoryStats?.let { stats ->
    SafeDeleteCategoryDialog(
      category = stats.category,
      associatedTaskCount = stats.totalTasks,
      otherCategories = uiState.activeCategories.filter { it.id != stats.category.id },
      onDismiss = { deletingCategoryStats = null },
      onConfirmDelete = { strategy, targetId ->
        viewModel.deleteCategorySafe(stats.category.id, strategy, targetId)
      },
    )
  }

  movingTask?.let { task ->
    MoveCategoryDialog(
      categories = uiState.activeCategories,
      currentCategoryId = task.task.categoryId,
      onDismiss = { movingTask = null },
      onSelectCategory = { targetId -> viewModel.moveTaskToCategory(task.task.id, targetId) },
    )
  }
}

@Composable
private fun CategoryCard(
  stats: CategoryWithStats,
  canMoveUp: Boolean,
  canMoveDown: Boolean,
  onClick: () -> Unit,
  onEdit: () -> Unit,
  onTogglePin: () -> Unit,
  onMoveUp: () -> Unit,
  onMoveDown: () -> Unit,
  onArchive: () -> Unit,
  onDelete: () -> Unit,
) {
  val cat = stats.category
  val accent = Color(cat.colorHex)
  val semantic = CadenceThemeTokens.colors
  var menuExpanded by remember { mutableStateOf(false) }

  Surface(
    modifier =
      Modifier.fillMaxWidth()
        .testTag("category_card_${cat.id}")
        .clip(RoundedCornerShape(20.dp))
        .clickable(onClick = onClick),
    shape = RoundedCornerShape(20.dp),
    color = semantic.cardElevated,
    border = BorderStroke(1.2.dp, accent.copy(alpha = 0.28f)),
    tonalElevation = 1.dp,
  ) {
    Column(
      modifier =
        Modifier.fillMaxWidth()
          .background(
            Brush.horizontalGradient(
              colors = listOf(accent.copy(alpha = 0.08f), Color.Transparent)
            )
          )
          .padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(
          modifier =
            Modifier.size(46.dp)
              .clip(RoundedCornerShape(14.dp))
              .background(accent.copy(alpha = 0.16f)),
          contentAlignment = Alignment.Center,
        ) {
          Icon(
            imageVector = categoryIconForKey(cat.iconKey),
            contentDescription = null,
            tint = accent,
            modifier = Modifier.size(22.dp),
          )
        }
        Column(modifier = Modifier.weight(1f)) {
          Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
              text = cat.name,
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Bold,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
            if (cat.isPinned) {
              Icon(
                imageVector = Icons.Rounded.PushPin,
                contentDescription = "Pinned",
                tint = accent,
                modifier = Modifier.size(14.dp),
              )
            }
          }
          Text(
            text =
              if (cat.description.isNotBlank()) cat.description
              else "${stats.activeTasks} active · ${stats.completedTasks} completed",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
        }
        Box {
          IconButton(onClick = { menuExpanded = true }, modifier = Modifier.testTag("category_menu_${cat.id}")) {
            Icon(Icons.Rounded.MoreVert, contentDescription = "Category options")
          }
          DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
            DropdownMenuItem(
              text = { Text("Edit") },
              leadingIcon = { Icon(Icons.Rounded.Edit, contentDescription = null) },
              onClick = {
                menuExpanded = false
                onEdit()
              },
            )
            DropdownMenuItem(
              text = { Text(if (cat.isPinned) "Unpin" else "Pin to top") },
              leadingIcon = { Icon(Icons.Rounded.PushPin, contentDescription = null) },
              onClick = {
                menuExpanded = false
                onTogglePin()
              },
            )
            if (canMoveUp) {
              DropdownMenuItem(
                text = { Text("Move up") },
                leadingIcon = { Icon(Icons.Rounded.ArrowUpward, contentDescription = null) },
                onClick = {
                  menuExpanded = false
                  onMoveUp()
                },
              )
            }
            if (canMoveDown) {
              DropdownMenuItem(
                text = { Text("Move down") },
                leadingIcon = { Icon(Icons.Rounded.ArrowDownward, contentDescription = null) },
                onClick = {
                  menuExpanded = false
                  onMoveDown()
                },
              )
            }
            DropdownMenuItem(
              text = { Text("Archive") },
              leadingIcon = { Icon(Icons.Rounded.Archive, contentDescription = null) },
              onClick = {
                menuExpanded = false
                onArchive()
              },
            )
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

      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        LinearProgressIndicator(
          progress = { stats.completionRatio },
          modifier = Modifier.weight(1f).height(8.dp),
          color = accent,
          trackColor = accent.copy(alpha = 0.16f),
          strokeCap = StrokeCap.Round,
        )
        Text(
          text = "${stats.completionPercent}% (${stats.completedTasks}/${stats.totalTasks})",
          style = MaterialTheme.typography.labelSmall.copy(
            fontFamily = JetBrainsMonoFamily,
            fontFeatureSettings = "tnum",
          ),
          fontWeight = FontWeight.Bold,
          color = accent,
        )
      }
    }
  }
}
