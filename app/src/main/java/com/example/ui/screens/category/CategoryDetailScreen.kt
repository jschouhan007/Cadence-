package com.example.ui.screens.category

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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.SwapVert
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.entity.CategoryViewMode
import com.example.data.local.entity.TaskStatus
import com.example.data.local.entity.TaskWithDetails
import com.example.domain.time.DateTimeUtils
import com.example.ui.components.CategoryEditorDialog
import com.example.ui.components.EmptyStateView
import com.example.ui.components.MoveCategoryDialog
import com.example.ui.components.PriorityBadge
import com.example.ui.components.SafeDeleteCategoryDialog
import com.example.ui.components.StudioSegmentedTabs
import com.example.ui.components.StudioTabOption
import com.example.ui.components.TaskRowCard
import com.example.ui.components.categoryIconForKey
import com.example.ui.theme.CadenceThemeTokens
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.Spacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryDetailScreen(
  viewModel: CategoryDetailViewModel,
  onNavigateBack: () -> Unit,
  onOpenQuickAddForCategory: (String) -> Unit,
  onOpenTaskDetail: (String) -> Unit,
  onStartFocus: (String) -> Unit,
) {
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()
  val category = uiState.category

  var showEditDialog by remember { mutableStateOf(false) }
  var showDeleteDialog by remember { mutableStateOf(false) }
  var movingTask by remember { mutableStateOf<TaskWithDetails?>(null) }

  Scaffold(
    containerColor = MaterialTheme.colorScheme.background,
    topBar = {
      TopAppBar(
        colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
        navigationIcon = {
          IconButton(onClick = onNavigateBack, modifier = Modifier.testTag("category_detail_back")) {
            Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Navigate back")
          }
        },
        title = {
          Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            if (category != null) {
              Box(
                modifier =
                  Modifier.size(32.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(category.colorHex).copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center,
              ) {
                Icon(
                  imageVector = categoryIconForKey(category.iconKey),
                  contentDescription = null,
                  tint = Color(category.colorHex),
                  modifier = Modifier.size(18.dp),
                )
              }
              Text(
                text = category.name,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
              )
            }
          }
        },
        actions = {
          if (category != null) {
            IconButton(onClick = { showEditDialog = true }, modifier = Modifier.testTag("category_detail_edit")) {
              Icon(Icons.Rounded.Edit, contentDescription = "Edit category")
            }
            IconButton(onClick = { showDeleteDialog = true }, modifier = Modifier.testTag("category_detail_delete")) {
              Icon(Icons.Rounded.DeleteOutline, contentDescription = "Delete category")
            }
          }
        },
      )
    },
    floatingActionButton = {
      if (category != null) {
        FloatingActionButton(
          onClick = { onOpenQuickAddForCategory(category.id) },
          containerColor = MaterialTheme.colorScheme.primary,
          contentColor = MaterialTheme.colorScheme.onPrimary,
          modifier = Modifier.testTag("category_detail_add_fab"),
        ) {
          Icon(Icons.Rounded.Add, contentDescription = "Add task to category")
        }
      }
    },
  ) { innerPadding ->
    if (uiState.isLoading) {
      Box(modifier = Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
      }
    } else if (category == null) {
      EmptyStateView(
        icon = Icons.Rounded.FolderOpen,
        title = "Category not found",
        message = "This category may have been deleted.",
        actionLabel = "Go Back",
        onAction = onNavigateBack,
        modifier = Modifier.padding(innerPadding),
      )
    } else {
      val accent = Color(category.colorHex)
      Box(
        modifier = Modifier.fillMaxSize().padding(innerPadding),
        contentAlignment = Alignment.TopCenter,
      ) {
        Column(modifier = Modifier.widthIn(max = Spacing.maxContentWidth).fillMaxSize()) {
          // Summary header + progress
          Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            if (category.description.isNotBlank()) {
              Text(
                text = category.description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
            Row(
              modifier = Modifier.fillMaxWidth(),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
              LinearProgressIndicator(
                progress = {
                  if (uiState.tasks.isEmpty()) 0f
                  else uiState.completedCount.toFloat() / uiState.tasks.size.toFloat()
                },
                modifier = Modifier.weight(1f).height(6.dp),
                color = accent,
                trackColor = accent.copy(alpha = 0.14f),
                strokeCap = StrokeCap.Round,
              )
              Text(
                text = "${uiState.completionPercent}% (${uiState.completedCount}/${uiState.tasks.size})",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }

            Row(
              modifier = Modifier.fillMaxWidth(),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
              StudioSegmentedTabs(
                options =
                  CategoryViewMode.entries.map { mode ->
                    StudioTabOption(
                      value = mode,
                      label = mode.label,
                      testTag = "category_view_${mode.name.lowercase()}",
                    )
                  },
                selectedValue = uiState.viewMode,
                onSelect = { viewModel.setViewMode(it) },
                modifier = Modifier.weight(1f),
              )
              if (uiState.viewMode == CategoryViewMode.LIST) {
                FilterChip(
                  selected = uiState.isReorderMode,
                  onClick = viewModel::toggleReorderMode,
                  label = { Text(if (uiState.isReorderMode) "Done" else "Order") },
                  leadingIcon = {
                    Icon(Icons.Rounded.SwapVert, contentDescription = null, modifier = Modifier.size(16.dp))
                  },
                )
              }
            }
          }

          if (uiState.tasks.isEmpty()) {
            EmptyStateView(
              icon = categoryIconForKey(category.iconKey),
              title = "No tasks in ${category.name}",
              message = "Tap + to capture a task straight into this workspace.",
              actionLabel = "Add Task",
              onAction = { onOpenQuickAddForCategory(category.id) },
            )
          } else {
            when (uiState.viewMode) {
              CategoryViewMode.LIST -> {
                LazyColumn(
                  modifier = Modifier.fillMaxSize(),
                  contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
                  verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                  itemsIndexed(uiState.tasks, key = { _, it -> it.task.id }) { index, item ->
                    TaskRowCard(
                      item = item,
                      onClick = { onOpenTaskDetail(item.task.id) },
                      onToggleComplete = { viewModel.toggleTaskCompletion(item.task.id) },
                      onStartFocus = { onStartFocus(item.task.id) },
                      onMoveCategory = { movingTask = item },
                      onDuplicate = { viewModel.duplicateTask(item.task.id) },
                      onDelete = { viewModel.deleteTask(item.task.id) },
                      showReorderControls = uiState.isReorderMode,
                      canMoveUp = index > 0,
                      canMoveDown = index < uiState.tasks.lastIndex,
                      onMoveUp = { viewModel.moveTask(item.task.id, moveUp = true) },
                      onMoveDown = { viewModel.moveTask(item.task.id, moveUp = false) },
                    )
                  }
                }
              }

              CategoryViewMode.BOARD -> {
                val columns =
                  listOf(
                    TaskStatus.TODO to "To do",
                    TaskStatus.IN_PROGRESS to "In progress",
                    TaskStatus.COMPLETED to "Completed",
                  )
                Row(
                  modifier =
                    Modifier.fillMaxSize()
                      .horizontalScroll(rememberScrollState())
                      .padding(horizontal = 16.dp, vertical = 10.dp),
                  horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                  columns.forEach { (status, title) ->
                    val colTasks = uiState.tasks.filter { it.task.status == status }
                    KanbanColumn(
                      title = title,
                      status = status,
                      tasks = colTasks,
                      onTaskClick = onOpenTaskDetail,
                      onMoveStatus = { taskId, nextStatus -> viewModel.updateTaskStatus(taskId, nextStatus) },
                    )
                  }
                }
              }

              CategoryViewMode.CALENDAR -> {
                val grouped =
                  uiState.tasks
                    .sortedBy { it.task.dueAt ?: Long.MAX_VALUE }
                    .groupBy { item ->
                      item.task.dueAt?.let { DateTimeUtils.formatShortDate(it) } ?: "No due date"
                    }
                LazyColumn(
                  modifier = Modifier.fillMaxSize(),
                  contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
                  verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                  grouped.forEach { (dateLabel, dayTasks) ->
                    item(key = "hdr_$dateLabel") {
                      Text(
                        text = dateLabel,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 6.dp),
                      )
                    }
                    items(dayTasks, key = { "cal_${it.task.id}" }) { item ->
                      TaskRowCard(
                        item = item,
                        onClick = { onOpenTaskDetail(item.task.id) },
                        onToggleComplete = { viewModel.toggleTaskCompletion(item.task.id) },
                        onStartFocus = { onStartFocus(item.task.id) },
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
  }

  if (showEditDialog && category != null) {
    CategoryEditorDialog(
      initialCategory = category,
      onDismiss = { showEditDialog = false },
      onSave = { name, desc, iconKey, colorHex ->
        viewModel.updateCategory(name, desc, iconKey, colorHex)
      },
    )
  }

  if (showDeleteDialog && category != null) {
    SafeDeleteCategoryDialog(
      category = category,
      associatedTaskCount = uiState.tasks.size,
      otherCategories = uiState.otherCategories,
      onDismiss = { showDeleteDialog = false },
      onConfirmDelete = { strategy, targetId ->
        viewModel.deleteCategorySafe(strategy, targetId, onDeleted = onNavigateBack)
      },
    )
  }

  movingTask?.let { task ->
    MoveCategoryDialog(
      categories = uiState.otherCategories + listOfNotNull(category),
      currentCategoryId = task.task.categoryId,
      onDismiss = { movingTask = null },
      onSelectCategory = { targetId -> viewModel.moveTaskToCategory(task.task.id, targetId) },
    )
  }
}

@Composable
private fun KanbanColumn(
  title: String,
  status: TaskStatus,
  tasks: List<TaskWithDetails>,
  onTaskClick: (String) -> Unit,
  onMoveStatus: (String, TaskStatus) -> Unit,
) {
  val semantic = CadenceThemeTokens.colors
  val statusTint =
    when (status) {
      TaskStatus.TODO -> MaterialTheme.colorScheme.primary
      TaskStatus.IN_PROGRESS -> semantic.marigold
      TaskStatus.COMPLETED, TaskStatus.CANCELLED -> semantic.success
    }
  Surface(
    modifier = Modifier.width(284.dp).fillMaxSize(),
    shape = RoundedCornerShape(20.dp),
    color = MaterialTheme.colorScheme.surfaceContainerLow,
    border = BorderStroke(1.2.dp, statusTint.copy(alpha = 0.28f)),
  ) {
    Column(modifier = Modifier.fillMaxSize().padding(12.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Box(
          modifier = Modifier.size(10.dp).clip(CircleShape).background(statusTint)
        )
        Text(
          text = title,
          style = MaterialTheme.typography.titleSmall,
          fontWeight = FontWeight.Bold,
          modifier = Modifier.weight(1f),
        )
        Surface(
          shape = RoundedCornerShape(8.dp),
          color = statusTint.copy(alpha = 0.14f),
        ) {
          Text(
            text = tasks.size.toString(),
            style = MaterialTheme.typography.labelSmall.copy(
              fontFamily = JetBrainsMonoFamily,
              fontFeatureSettings = "tnum",
            ),
            fontWeight = FontWeight.Bold,
            color = statusTint,
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
          )
        }
      }

      if (tasks.isEmpty()) {
        Box(
          modifier = Modifier.fillMaxWidth().weight(1f),
          contentAlignment = Alignment.Center,
        ) {
          Text(
            text = "No tasks in $title",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      } else {
        Column(
          modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()),
          verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          tasks.forEach { item ->
            Surface(
              modifier =
                Modifier.fillMaxWidth()
                  .clip(RoundedCornerShape(14.dp))
                  .clickable { onTaskClick(item.task.id) },
              shape = RoundedCornerShape(14.dp),
              color = semantic.cardElevated,
              border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            ) {
              Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                  text = item.task.title,
                  style = MaterialTheme.typography.titleSmall,
                  maxLines = 2,
                  overflow = TextOverflow.Ellipsis,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                  PriorityBadge(priority = item.task.priority, compact = true)
                  DateTimeUtils.formatDueLabel(item.task.dueAt, item.task.isAllDay)?.let { due ->
                    Text(
                      text = due,
                      style = MaterialTheme.typography.labelSmall,
                      color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                  }
                }
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.End,
                ) {
                  when (status) {
                    TaskStatus.TODO -> {
                      TextButton(onClick = { onMoveStatus(item.task.id, TaskStatus.IN_PROGRESS) }) {
                        Text("Start")
                      }
                      TextButton(onClick = { onMoveStatus(item.task.id, TaskStatus.COMPLETED) }) {
                        Text("Done")
                      }
                    }
                    TaskStatus.IN_PROGRESS -> {
                      TextButton(onClick = { onMoveStatus(item.task.id, TaskStatus.TODO) }) {
                        Text("To do")
                      }
                      TextButton(onClick = { onMoveStatus(item.task.id, TaskStatus.COMPLETED) }) {
                        Text("Complete")
                      }
                    }
                    TaskStatus.COMPLETED, TaskStatus.CANCELLED -> {
                      TextButton(onClick = { onMoveStatus(item.task.id, TaskStatus.TODO) }) {
                        Text("Reopen")
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
  }
}
