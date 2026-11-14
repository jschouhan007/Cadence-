package com.example.ui.screens.taskdetail

import android.Manifest
import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Alarm
import androidx.compose.material.icons.rounded.AlarmAdd
import androidx.compose.material.icons.rounded.CalendarToday
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Inbox
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Tag
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.entity.RecurrenceFrequency
import com.example.data.local.entity.ReminderEntity
import com.example.data.local.entity.TaskPriority
import com.example.data.local.entity.TaskStatus
import com.example.domain.model.PendingReminderDraft
import com.example.domain.time.DateTimeUtils
import com.example.ui.components.AlertTimePickerDialog
import com.example.ui.components.categoryIconForKey
import com.example.ui.theme.CadenceThemeTokens
import com.example.ui.theme.Spacing
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun TaskDetailScreen(
  viewModel: TaskDetailViewModel,
  onNavigateBack: () -> Unit,
  onStartFocus: (String) -> Unit,
  onOpenDuplicatedTask: (String) -> Unit,
) {
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()
  val draft = uiState.draft
  val semantic = CadenceThemeTokens.colors
  val context = LocalContext.current
  val lifecycleOwner = LocalLifecycleOwner.current

  var newSubtaskTitle by remember { mutableStateOf("") }
  var newTagName by remember { mutableStateOf("") }
  var showDueDatePicker by remember { mutableStateOf(false) }
  var showStartDatePicker by remember { mutableStateOf(false) }
  var showDueTimePicker by remember { mutableStateOf(false) }
  var showReminderMenuDialog by remember { mutableStateOf(false) }
  var showCustomAlertPicker by remember { mutableStateOf(false) }
  var editingSavedReminder by remember { mutableStateOf<ReminderEntity?>(null) }
  var editingPendingReminder by remember { mutableStateOf<PendingReminderDraft?>(null) }
  var showDeleteConfirm by remember { mutableStateOf(false) }
  var showHistory by remember { mutableStateOf(false) }
  var showActionsMenu by remember { mutableStateOf(false) }

  var hasNotificationPermission by remember {
    mutableStateOf(
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
          PackageManager.PERMISSION_GRANTED
      } else true
    )
  }
  var canScheduleExactAlarms by remember {
    mutableStateOf(
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        (context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager)?.canScheduleExactAlarms() == true
      } else true
    )
  }

  DisposableEffect(lifecycleOwner) {
    val observer = LifecycleEventObserver { _, event ->
      if (event == Lifecycle.Event.ON_RESUME) {
        hasNotificationPermission =
          if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
              PackageManager.PERMISSION_GRANTED
          } else true
        canScheduleExactAlarms =
          if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            (context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager)?.canScheduleExactAlarms() == true
          } else true
      }
    }
    lifecycleOwner.lifecycle.addObserver(observer)
    onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
  }

  val permissionLauncher =
    rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
      hasNotificationPermission = granted
    }

  val requestNotifPermissionIfNeeded: () -> Unit = {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !hasNotificationPermission) {
      permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
    }
  }

  Scaffold(
    containerColor = MaterialTheme.colorScheme.background,
    topBar = {
      TopAppBar(
        colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
        navigationIcon = {
          IconButton(onClick = onNavigateBack, modifier = Modifier.testTag("task_detail_back")) {
            Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Navigate back")
          }
        },
        title = {
          Text(
            text = if (uiState.isNewTask) "New Task" else "Task Details",
            maxLines = 1,
            fontWeight = FontWeight.Bold,
          )
        },
        actions = {
          Button(
            onClick = { viewModel.saveTask { onNavigateBack() } },
            enabled = draft.title.isNotBlank() && !uiState.isSaving,
            shape = RoundedCornerShape(14.dp),
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
            modifier = Modifier.padding(end = if (uiState.isNewTask) 12.dp else 4.dp).testTag("task_detail_save"),
          ) {
            Icon(Icons.Rounded.Check, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Save", maxLines = 1, fontWeight = FontWeight.Bold)
          }
          if (!uiState.isNewTask) {
            Box {
              IconButton(
                onClick = { showActionsMenu = true },
                modifier = Modifier.testTag("task_detail_more_menu"),
              ) {
                Icon(Icons.Rounded.MoreVert, contentDescription = "More task actions")
              }
              DropdownMenu(
                expanded = showActionsMenu,
                onDismissRequest = { showActionsMenu = false },
              ) {
                DropdownMenuItem(
                  text = { Text("Focus on task") },
                  leadingIcon = { Icon(Icons.Rounded.Timer, contentDescription = null) },
                  onClick = {
                    showActionsMenu = false
                    uiState.taskId?.let(onStartFocus)
                  },
                  modifier = Modifier.testTag("task_detail_focus"),
                )
                DropdownMenuItem(
                  text = { Text("Duplicate task") },
                  leadingIcon = { Icon(Icons.Rounded.ContentCopy, contentDescription = null) },
                  onClick = {
                    showActionsMenu = false
                    viewModel.duplicateTask(onOpenDuplicatedTask)
                  },
                  modifier = Modifier.testTag("task_detail_duplicate"),
                )
                DropdownMenuItem(
                  text = { Text("Delete task", color = MaterialTheme.colorScheme.error) },
                  leadingIcon = {
                    Icon(
                      Icons.Rounded.DeleteOutline,
                      contentDescription = null,
                      tint = MaterialTheme.colorScheme.error,
                    )
                  },
                  onClick = {
                    showActionsMenu = false
                    showDeleteConfirm = true
                  },
                  modifier = Modifier.testTag("task_detail_delete"),
                )
              }
            }
          }
        },
      )
    },
  ) { innerPadding ->
    if (uiState.isLoading) {
      Box(modifier = Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
      }
    } else {
      Box(
        modifier = Modifier.fillMaxSize().padding(innerPadding).imePadding(),
        contentAlignment = Alignment.TopCenter,
      ) {
        Column(
          modifier =
            Modifier.widthIn(max = Spacing.maxContentWidth)
              .fillMaxSize()
              .verticalScroll(rememberScrollState())
              .padding(horizontal = 16.dp, vertical = 8.dp),
          verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
          // Title & Description
          OutlinedTextField(
            value = draft.title,
            onValueChange = viewModel::updateTitle,
            label = { Text("Task title") },
            placeholder = { Text("e.g., Submit compiler lab report") },
            singleLine = false,
            maxLines = 3,
            modifier = Modifier.fillMaxWidth().testTag("task_detail_title_input"),
          )

          OutlinedTextField(
            value = draft.description,
            onValueChange = viewModel::updateDescription,
            label = { Text("Description (optional)") },
            minLines = 2,
            maxLines = 5,
            modifier = Modifier.fillMaxWidth().testTag("task_detail_desc_input"),
          )

          // Status & Priority Card
          Surface(
            shape = RoundedCornerShape(18.dp),
            color = semantic.cardElevated,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
          ) {
            Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
              Text("Status", style = MaterialTheme.typography.labelLarge)
              Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
              ) {
                TaskStatus.entries.forEach { st ->
                  FilterChip(
                    selected = draft.status == st,
                    onClick = { viewModel.updateStatus(st) },
                    label = { Text(st.label) },
                  )
                }
              }

              HorizontalDivider(color = semantic.subtleDivider)

              Text("Priority", style = MaterialTheme.typography.labelLarge)
              Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
              ) {
                TaskPriority.entries.forEach { prio ->
                  FilterChip(
                    selected = draft.priority == prio,
                    onClick = { viewModel.updatePriority(prio) },
                    label = { Text(prio.label) },
                  )
                }
              }

              HorizontalDivider(color = semantic.subtleDivider)

              Text("Category", style = MaterialTheme.typography.labelLarge)
              Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
              ) {
                FilterChip(
                  selected = draft.categoryId == null,
                  onClick = { viewModel.updateCategory(null) },
                  label = { Text("Inbox") },
                  leadingIcon = { Icon(Icons.Rounded.Inbox, contentDescription = null, modifier = Modifier.size(16.dp)) },
                )
                uiState.categories.forEach { cat ->
                  FilterChip(
                    selected = draft.categoryId == cat.id,
                    onClick = { viewModel.updateCategory(cat.id) },
                    label = { Text(cat.name) },
                    leadingIcon = {
                      Icon(
                        imageVector = categoryIconForKey(cat.iconKey),
                        contentDescription = null,
                        tint = Color(cat.colorHex),
                        modifier = Modifier.size(16.dp),
                      )
                    },
                  )
                }
              }
            }
          }

          // Schedule, Time, Duration, Recurrence
          Surface(
            shape = RoundedCornerShape(18.dp),
            color = semantic.cardElevated,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
          ) {
            Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
              Text("Schedule & Effort", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)

              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
              ) {
                OutlinedButton(
                  onClick = { showDueDatePicker = true },
                  modifier = Modifier.weight(1f).testTag("task_detail_due_date_button"),
                ) {
                  Icon(Icons.Rounded.CalendarToday, contentDescription = null, modifier = Modifier.size(16.dp))
                  Text(
                    text =
                      draft.dueAt?.let { " Due: ${DateTimeUtils.formatShortDate(it)}" } ?: " Set due date",
                    maxLines = 1,
                  )
                }
                if (draft.dueAt != null) {
                  OutlinedButton(onClick = { showDueTimePicker = true }) {
                    Icon(Icons.Rounded.Schedule, contentDescription = null, modifier = Modifier.size(16.dp))
                    Text(
                      text = if (draft.isAllDay) " All day" else " ${DateTimeUtils.formatTime(draft.dueAt)}",
                      maxLines = 1,
                    )
                  }
                  IconButton(onClick = { viewModel.updateDueAt(null, true) }) {
                    Icon(Icons.Rounded.Close, contentDescription = "Clear due date")
                  }
                }
              }

              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
              ) {
                OutlinedButton(
                  onClick = { showStartDatePicker = true },
                  modifier = Modifier.weight(1f),
                ) {
                  Icon(Icons.Rounded.CalendarToday, contentDescription = null, modifier = Modifier.size(16.dp))
                  Text(
                    text =
                      draft.startAt?.let { " Start: ${DateTimeUtils.formatShortDate(it)}" }
                        ?: " Planned start (optional)",
                    maxLines = 1,
                  )
                }
                if (draft.startAt != null) {
                  IconButton(onClick = { viewModel.updateStartAt(null) }) {
                    Icon(Icons.Rounded.Close, contentDescription = "Clear start date")
                  }
                }
              }

              Text("Estimated duration", style = MaterialTheme.typography.labelLarge)
              Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
              ) {
                listOf(null to "None", 15 to "15m", 30 to "30m", 45 to "45m", 60 to "1h", 90 to "90m").forEach {
                  (mins, label) ->
                  FilterChip(
                    selected = draft.estimatedMinutes == mins,
                    onClick = { viewModel.updateEstimatedMinutes(mins) },
                    label = { Text(label) },
                  )
                }
              }

              if (uiState.actualMinutes > 0) {
                Text(
                  text = "Tracked focus time: ${uiState.actualMinutes} min",
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.primary,
                )
              }

              HorizontalDivider(color = semantic.subtleDivider)

              Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(Icons.Rounded.Repeat, contentDescription = null, modifier = Modifier.size(18.dp))
                Text("Recurrence", style = MaterialTheme.typography.labelLarge)
              }
              Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
              ) {
                RecurrenceFrequency.entries.forEach { freq ->
                  FilterChip(
                    selected = draft.recurrenceFrequency == freq,
                    onClick = { viewModel.updateRecurrence(freq) },
                    label = { Text(freq.label) },
                  )
                }
              }
            }
          }

          // Subtasks
          Surface(
            shape = RoundedCornerShape(18.dp),
            color = semantic.cardElevated,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
          ) {
            Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
              Text(
                text = "Subtasks (${uiState.subtasks.count { it.isCompleted }}/${uiState.subtasks.size})",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
              )

              uiState.subtasks.forEach { sub ->
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  verticalAlignment = Alignment.CenterVertically,
                ) {
                  Checkbox(
                    checked = sub.isCompleted,
                    onCheckedChange = { checked -> viewModel.toggleSubtask(sub.id, checked) },
                  )
                  Text(
                    text = sub.title,
                    style = MaterialTheme.typography.bodyMedium,
                    textDecoration = if (sub.isCompleted) TextDecoration.LineThrough else TextDecoration.None,
                    modifier = Modifier.weight(1f),
                  )
                  IconButton(onClick = { viewModel.deleteSubtask(sub.id) }) {
                    Icon(Icons.Rounded.Close, contentDescription = "Remove subtask", modifier = Modifier.size(18.dp))
                  }
                }
              }

              Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
              ) {
                OutlinedTextField(
                  value = newSubtaskTitle,
                  onValueChange = { newSubtaskTitle = it },
                  placeholder = { Text("Add a subtask...") },
                  singleLine = true,
                  modifier = Modifier.weight(1f).testTag("task_detail_subtask_input"),
                )
                Button(
                  onClick = {
                    if (newSubtaskTitle.isNotBlank()) {
                      viewModel.addSubtask(newSubtaskTitle)
                      newSubtaskTitle = ""
                    }
                  },
                  enabled = newSubtaskTitle.isNotBlank() && draft.title.isNotBlank(),
                  modifier = Modifier.testTag("task_detail_add_subtask_button"),
                ) {
                  Icon(Icons.Rounded.Add, contentDescription = "Add subtask")
                }
              }
            }
          }

          // Alerts & Reminders
          Surface(
            shape = RoundedCornerShape(18.dp),
            color = semantic.cardElevated,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
          ) {
            Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
              ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                  Icon(Icons.Rounded.Alarm, contentDescription = null, modifier = Modifier.size(18.dp))
                  Text("Alerts & Reminders", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                }
                TextButton(
                  onClick = { showReminderMenuDialog = true },
                  modifier = Modifier.testTag("task_detail_add_reminder"),
                ) {
                  Icon(Icons.Rounded.AlarmAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                  Text(" Set alert")
                }
              }

              val totalReminders = uiState.reminders.size + uiState.pendingReminders.size
              if (totalReminders > 0 && !hasNotificationPermission && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                Surface(
                  shape = RoundedCornerShape(12.dp),
                  color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.65f),
                ) {
                  Row(
                    modifier = Modifier.fillMaxWidth().padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                  ) {
                    Icon(
                      Icons.Rounded.NotificationsActive,
                      contentDescription = null,
                      tint = MaterialTheme.colorScheme.secondary,
                      modifier = Modifier.size(18.dp),
                    )
                    Text(
                      text = "Allow notifications so alerts can fire on time.",
                      style = MaterialTheme.typography.bodySmall,
                      modifier = Modifier.weight(1f),
                    )
                    TextButton(onClick = { permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS) }) {
                      Text("Allow")
                    }
                  }
                }
              }

              if (totalReminders > 0 && !canScheduleExactAlarms && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                Surface(
                  shape = RoundedCornerShape(12.dp),
                  color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.6f),
                ) {
                  Row(
                    modifier = Modifier.fillMaxWidth().padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                  ) {
                    Icon(
                      Icons.Rounded.Alarm,
                      contentDescription = null,
                      tint = MaterialTheme.colorScheme.tertiary,
                      modifier = Modifier.size(18.dp),
                    )
                    Text(
                      text = "Enable Alarms & reminders access for exact alert timing.",
                      style = MaterialTheme.typography.bodySmall,
                      modifier = Modifier.weight(1f),
                    )
                    TextButton(
                      onClick = {
                        val intent =
                          Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                            data = Uri.fromParts("package", context.packageName, null)
                          }
                        runCatching { context.startActivity(intent) }
                      }
                    ) {
                      Text("Enable")
                    }
                  }
                }
              }

              if (uiState.reminders.isEmpty() && uiState.pendingReminders.isEmpty()) {
                Text(
                  text = "No alert time set yet. Choose an exact date & time to receive a notification for this task.",
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                OutlinedButton(
                  onClick = { showCustomAlertPicker = true },
                  modifier = Modifier.fillMaxWidth().testTag("task_detail_pick_exact_alert_inline"),
                ) {
                  Icon(Icons.Rounded.AlarmAdd, contentDescription = null, modifier = Modifier.size(18.dp))
                  Text(" Pick specific alert time", modifier = Modifier.padding(start = 6.dp))
                }
              } else {
                uiState.pendingReminders.forEach { pending ->
                  ReminderScheduleRow(
                    title = reminderLabel(pending.type, pending.offsetMinutes, pending.triggerAt),
                    subtitle = "${DateTimeUtils.formatDateTimeShort(pending.triggerAt)} · Tap to change time",
                    isEnabled = pending.isEnabled,
                    onClick = { editingPendingReminder = pending },
                    onToggle = { enabled ->
                      viewModel.togglePendingReminder(pending.tempId, enabled)
                      if (enabled) requestNotifPermissionIfNeeded()
                    },
                    onDelete = { viewModel.deletePendingReminder(pending.tempId) },
                  )
                }

                uiState.reminders.forEach { rem ->
                  ReminderScheduleRow(
                    title = reminderLabel(rem.type, rem.offsetMinutes, rem.triggerAt),
                    subtitle = "${DateTimeUtils.formatDateTimeShort(rem.triggerAt)} · Tap to change time",
                    isEnabled = rem.isEnabled,
                    onClick = { editingSavedReminder = rem },
                    onToggle = { enabled ->
                      viewModel.toggleReminder(rem, enabled)
                      if (enabled) requestNotifPermissionIfNeeded()
                    },
                    onDelete = { viewModel.deleteReminder(rem) },
                  )
                }
              }
            }
          }

          // Tags & Notes
          Surface(
            shape = RoundedCornerShape(18.dp),
            color = semantic.cardElevated,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
          ) {
            Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
              Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(Icons.Rounded.Tag, contentDescription = null, modifier = Modifier.size(18.dp))
                Text("Tags & Notes", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
              }

              if (uiState.tags.isNotEmpty()) {
                FlowRow(
                  horizontalArrangement = Arrangement.spacedBy(8.dp),
                  verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                  uiState.tags.forEach { tag ->
                    InputChip(
                      selected = true,
                      onClick = { viewModel.removeTag(tag.id) },
                      label = { Text("#${tag.name}") },
                      trailingIcon = { Icon(Icons.Rounded.Close, contentDescription = "Remove tag", modifier = Modifier.size(14.dp)) },
                    )
                  }
                }
              }

              Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
              ) {
                OutlinedTextField(
                  value = newTagName,
                  onValueChange = { newTagName = it },
                  placeholder = { Text("Add a tag (e.g., exam, deep-work)") },
                  singleLine = true,
                  modifier = Modifier.weight(1f),
                )
                Button(
                  onClick = {
                    if (newTagName.isNotBlank()) {
                      viewModel.addTag(newTagName)
                      newTagName = ""
                    }
                  },
                  enabled = newTagName.isNotBlank() && draft.title.isNotBlank(),
                ) {
                  Text("Tag")
                }
              }

              OutlinedTextField(
                value = draft.notes,
                onValueChange = viewModel::updateNotes,
                label = { Text("Notes & links") },
                minLines = 3,
                maxLines = 8,
                modifier = Modifier.fillMaxWidth(),
              )
            }
          }

          // Collapsible History
          if (uiState.history.isNotEmpty()) {
            Surface(
              shape = RoundedCornerShape(18.dp),
              color = MaterialTheme.colorScheme.surfaceContainerLow,
              border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            ) {
              Column(modifier = Modifier.fillMaxWidth().padding(14.dp)) {
                Row(
                  modifier =
                    Modifier.fillMaxWidth()
                      .clip(RoundedCornerShape(10.dp))
                      .clickable { showHistory = !showHistory }
                      .padding(4.dp),
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                  Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Rounded.History, contentDescription = null, modifier = Modifier.size(18.dp))
                    Text(
                      text = "Activity history (${uiState.history.size})",
                      style = MaterialTheme.typography.titleSmall,
                    )
                  }
                  Icon(
                    imageVector = if (showHistory) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                    contentDescription = null,
                  )
                }
                AnimatedVisibility(visible = showHistory) {
                  Column(
                    modifier = Modifier.padding(top = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                  ) {
                    uiState.history.forEach { entry ->
                      Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                      ) {
                        Text(entry.summary, style = MaterialTheme.typography.bodySmall)
                        Text(
                          text =
                            "${DateTimeUtils.formatShortDate(entry.timestamp)} ${DateTimeUtils.formatTime(entry.timestamp)}",
                          style = MaterialTheme.typography.labelSmall,
                          color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                      }
                    }
                  }
                }
              }
            }
          }

          Spacer(modifier = Modifier.height(32.dp))
        }
      }
    }
  }

  if (showDueDatePicker) {
    val state =
      rememberDatePickerState(
        initialSelectedDateMillis = DateTimeUtils.localDateToUtcMidnight(draft.dueAt ?: System.currentTimeMillis())
      )
    DatePickerDialog(
      onDismissRequest = { showDueDatePicker = false },
      confirmButton = {
        TextButton(
          onClick = {
            state.selectedDateMillis?.let { millis ->
              val localDay = DateTimeUtils.utcMidnightToLocalStartOfDay(millis)
              val currentDue = draft.dueAt
              if (!draft.isAllDay && currentDue != null) {
                val cal = Calendar.getInstance().apply { timeInMillis = currentDue }
                val updated =
                  DateTimeUtils.combineDateAndTime(
                    localDay,
                    cal.get(Calendar.HOUR_OF_DAY),
                    cal.get(Calendar.MINUTE),
                  )
                viewModel.updateDueAt(updated, isAllDay = false)
              } else {
                viewModel.updateDueAt(DateTimeUtils.endOfDay(localDay), isAllDay = true)
              }
            }
            showDueDatePicker = false
          }
        ) {
          Text("Select")
        }
      },
      dismissButton = { TextButton(onClick = { showDueDatePicker = false }) { Text("Cancel") } },
    ) {
      DatePicker(state = state)
    }
  }

  if (showStartDatePicker) {
    val state =
      rememberDatePickerState(
        initialSelectedDateMillis = DateTimeUtils.localDateToUtcMidnight(draft.startAt ?: System.currentTimeMillis())
      )
    DatePickerDialog(
      onDismissRequest = { showStartDatePicker = false },
      confirmButton = {
        TextButton(
          onClick = {
            state.selectedDateMillis?.let { millis ->
              val localDay = DateTimeUtils.utcMidnightToLocalStartOfDay(millis)
              viewModel.updateStartAt(localDay)
            }
            showStartDatePicker = false
          }
        ) {
          Text("Select")
        }
      },
      dismissButton = { TextButton(onClick = { showStartDatePicker = false }) { Text("Cancel") } },
    ) {
      DatePicker(state = state)
    }
  }

  if (showDueTimePicker) {
    val cal = Calendar.getInstance().apply { timeInMillis = draft.dueAt ?: System.currentTimeMillis() }
    val timeState =
      rememberTimePickerState(
        initialHour = if (draft.isAllDay) 18 else cal.get(Calendar.HOUR_OF_DAY),
        initialMinute = if (draft.isAllDay) 0 else cal.get(Calendar.MINUTE),
      )
    AlertDialog(
      onDismissRequest = { showDueTimePicker = false },
      title = { Text("Select due time") },
      text = { TimePicker(state = timeState) },
      confirmButton = {
        TextButton(
          onClick = {
            val baseDate = draft.dueAt ?: System.currentTimeMillis()
            val combined = DateTimeUtils.combineDateAndTime(baseDate, timeState.hour, timeState.minute)
            viewModel.updateDueAt(combined, isAllDay = false)
            showDueTimePicker = false
          }
        ) {
          Text("Apply")
        }
      },
      dismissButton = {
        TextButton(
          onClick = {
            draft.dueAt?.let { viewModel.updateDueAt(DateTimeUtils.endOfDay(it), isAllDay = true) }
            showDueTimePicker = false
          }
        ) {
          Text("All day")
        }
      },
    )
  }

  if (showReminderMenuDialog) {
    val presets =
      listOf(
        0 to "At due time",
        10 to "10 minutes before",
        30 to "30 minutes before",
        60 to "1 hour before",
        1440 to "1 day before",
      )
    AlertDialog(
      onDismissRequest = { showReminderMenuDialog = false },
      title = { Text("Schedule Task Alert") },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Surface(
            modifier =
              Modifier.fillMaxWidth()
                .testTag("task_detail_pick_custom_alert_time")
                .clip(RoundedCornerShape(12.dp))
                .clickable {
                  showReminderMenuDialog = false
                  showCustomAlertPicker = true
                },
            color = MaterialTheme.colorScheme.primaryContainer,
          ) {
            Row(
              modifier = Modifier.padding(14.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
              Icon(
                Icons.Rounded.AlarmAdd,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
              )
              Column {
                Text(
                  text = "Choose specific date & time",
                  style = MaterialTheme.typography.titleSmall,
                  fontWeight = FontWeight.Bold,
                  color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
                Text(
                  text = "Set an exact time for the alert notification",
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f),
                )
              }
            }
          }

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            OutlinedButton(
              onClick = {
                viewModel.addCustomReminder(System.currentTimeMillis() + 15 * 60_000L)
                showReminderMenuDialog = false
                requestNotifPermissionIfNeeded()
              },
              modifier = Modifier.weight(1f),
            ) {
              Text("In 15m")
            }
            OutlinedButton(
              onClick = {
                viewModel.addCustomReminder(System.currentTimeMillis() + 60 * 60_000L)
                showReminderMenuDialog = false
                requestNotifPermissionIfNeeded()
              },
              modifier = Modifier.weight(1f),
            ) {
              Text("In 1h")
            }
          }

          if (draft.dueAt != null) {
            HorizontalDivider(modifier = Modifier.padding(vertical = 2.dp))
            Text(
              text = "Relative to due time",
              style = MaterialTheme.typography.labelMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            presets.forEach { (offset, label) ->
              Surface(
                modifier =
                  Modifier.fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .clickable {
                      viewModel.addReminderOffset(offset)
                      showReminderMenuDialog = false
                      requestNotifPermissionIfNeeded()
                    },
                color = MaterialTheme.colorScheme.surfaceContainerLow,
              ) {
                Text(
                  text = label,
                  style = MaterialTheme.typography.bodyMedium,
                  modifier = Modifier.padding(12.dp),
                )
              }
            }
          }
        }
      },
      confirmButton = { TextButton(onClick = { showReminderMenuDialog = false }) { Text("Close") } },
    )
  }

  if (showCustomAlertPicker) {
    val defaultInitial =
      if (draft.dueAt != null && !draft.isAllDay && draft.dueAt > System.currentTimeMillis()) {
        draft.dueAt
      } else {
        DateTimeUtils.defaultAlertTimeMillis()
      }
    AlertTimePickerDialog(
      initialTimestampMillis = defaultInitial,
      onDismiss = { showCustomAlertPicker = false },
      onConfirm = { triggerAt ->
        viewModel.addCustomReminder(triggerAt)
        showCustomAlertPicker = false
        requestNotifPermissionIfNeeded()
      },
    )
  }

  editingSavedReminder?.let { rem ->
    AlertTimePickerDialog(
      title = "Update alert time",
      initialTimestampMillis = rem.triggerAt.coerceAtLeast(System.currentTimeMillis() + 60_000L),
      onDismiss = { editingSavedReminder = null },
      onConfirm = { newTriggerAt ->
        viewModel.updateReminderTime(rem, newTriggerAt)
        editingSavedReminder = null
        requestNotifPermissionIfNeeded()
      },
    )
  }

  editingPendingReminder?.let { pending ->
    AlertTimePickerDialog(
      title = "Update alert time",
      initialTimestampMillis = pending.triggerAt.coerceAtLeast(System.currentTimeMillis() + 60_000L),
      onDismiss = { editingPendingReminder = null },
      onConfirm = { newTriggerAt ->
        viewModel.updatePendingReminderTime(pending.tempId, newTriggerAt)
        editingPendingReminder = null
        requestNotifPermissionIfNeeded()
      },
    )
  }

  if (showDeleteConfirm) {
    AlertDialog(
      onDismissRequest = { showDeleteConfirm = false },
      title = { Text("Delete task?") },
      text = { Text("This will permanently remove \"${draft.title}\" and any associated subtasks or reminders.") },
      confirmButton = {
        Button(
          onClick = {
            showDeleteConfirm = false
            viewModel.deleteTask(onNavigateBack)
          },
          colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
        ) {
          Text("Delete")
        }
      },
      dismissButton = { TextButton(onClick = { showDeleteConfirm = false }) { Text("Cancel") } },
    )
  }
}

private fun reminderLabel(type: String, offsetMinutes: Int, triggerAt: Long): String {
  if (type == ReminderEntity.TYPE_CUSTOM_TIME || offsetMinutes < 0) {
    return "Alert at ${DateTimeUtils.formatTime(triggerAt)}"
  }
  return when (offsetMinutes) {
    0 -> "At scheduled time"
    10 -> "10 minutes before"
    30 -> "30 minutes before"
    60 -> "1 hour before"
    1440 -> "1 day before"
    else -> "$offsetMinutes min before"
  }
}

@Composable
private fun ReminderScheduleRow(
  title: String,
  subtitle: String,
  isEnabled: Boolean,
  onClick: () -> Unit,
  onToggle: (Boolean) -> Unit,
  onDelete: () -> Unit,
) {
  Row(
    modifier =
      Modifier.fillMaxWidth()
        .clip(RoundedCornerShape(12.dp))
        .clickable(onClick = onClick)
        .padding(vertical = 4.dp, horizontal = 4.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(8.dp),
  ) {
    Column(modifier = Modifier.weight(1f)) {
      Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
          text = title,
          style = MaterialTheme.typography.bodyMedium,
          fontWeight = FontWeight.Medium,
        )
        Icon(
          imageVector = Icons.Rounded.Edit,
          contentDescription = "Edit alert time",
          modifier = Modifier.size(14.dp),
          tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
      Text(
        text = subtitle,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
    Switch(
      checked = isEnabled,
      onCheckedChange = onToggle,
    )
    IconButton(onClick = onDelete) {
      Icon(Icons.Rounded.Close, contentDescription = "Delete reminder", modifier = Modifier.size(18.dp))
    }
  }
}
