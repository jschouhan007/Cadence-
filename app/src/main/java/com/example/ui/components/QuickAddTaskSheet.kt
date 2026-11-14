package com.example.ui.components

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
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Alarm
import androidx.compose.material.icons.rounded.AlarmAdd
import androidx.compose.material.icons.rounded.CalendarToday
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Inbox
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.data.local.entity.CategoryEntity
import com.example.data.local.entity.TaskPriority
import com.example.domain.time.DateTimeUtils
import java.util.Calendar

private enum class QuickAlertPreset {
  NONE,
  IN_15M,
  IN_1H,
  TOMORROW_9AM,
  CUSTOM,
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickAddTaskSheet(
  categories: List<CategoryEntity>,
  initialCategoryId: String? = null,
  initialDueAt: Long? = null,
  onDismiss: () -> Unit,
  onSaveQuickTask: (
    title: String,
    categoryId: String?,
    priority: TaskPriority,
    dueAt: Long?,
    isAllDay: Boolean,
    reminderAt: Long?,
  ) -> Unit,
  onOpenFullEditor: (
    title: String,
    categoryId: String?,
    priority: TaskPriority,
    dueAt: Long?,
    isAllDay: Boolean,
    reminderAt: Long?,
  ) -> Unit,
) {
  val context = LocalContext.current
  val lifecycleOwner = LocalLifecycleOwner.current
  val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
  var title by remember { mutableStateOf("") }
  var selectedCategoryId by remember(initialCategoryId) { mutableStateOf(initialCategoryId) }
  var selectedPriority by remember { mutableStateOf(TaskPriority.MEDIUM) }
  var selectedDueAt by remember(initialDueAt) { mutableStateOf(initialDueAt) }
  var selectedIsAllDay by remember { mutableStateOf(true) }
  var selectedAlertAt by remember { mutableStateOf<Long?>(null) }
  var selectedAlertPreset by remember { mutableStateOf(QuickAlertPreset.NONE) }

  var showDatePicker by remember { mutableStateOf(false) }
  var showDueTimePicker by remember { mutableStateOf(false) }
  var showAlertTimePicker by remember { mutableStateOf(false) }

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

  val now = remember { System.currentTimeMillis() }
  val todayEnd = remember(now) { DateTimeUtils.endOfDay(now) }
  val tomorrowEnd = remember(now) { DateTimeUtils.endOfDay(DateTimeUtils.addDays(now, 1)) }
  val weekendEnd = remember(now) { DateTimeUtils.upcomingWeekendMillis(now) }
  val nextWeekEnd = remember(now) { DateTimeUtils.nextWeekMondayMillis(now) }
  val tomorrow9Am = remember(now) { DateTimeUtils.tomorrowAtTimeMillis(9, 0, now) }

  ModalBottomSheet(
    onDismissRequest = onDismiss,
    sheetState = sheetState,
    containerColor = MaterialTheme.colorScheme.surface,
  ) {
    Column(
      modifier =
        Modifier.fillMaxWidth()
          .verticalScroll(rememberScrollState())
          .padding(horizontal = 20.dp)
          .navigationBarsPadding()
          .imePadding()
          .padding(bottom = 20.dp),
      verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        Box(
          modifier =
            Modifier.size(34.dp)
              .clip(RoundedCornerShape(10.dp))
              .background(MaterialTheme.colorScheme.primaryContainer),
          contentAlignment = Alignment.Center,
        ) {
          Icon(
            imageVector = Icons.Rounded.AlarmAdd,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(18.dp),
          )
        }
        Text(
          text = "New Task",
          style = MaterialTheme.typography.headlineSmall,
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.onSurface,
        )
      }

      OutlinedTextField(
        value = title,
        onValueChange = { title = it },
        placeholder = { Text("What needs to get done?") },
        singleLine = true,
        modifier = Modifier.fillMaxWidth().testTag("quick_add_title_input"),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
        keyboardActions =
          KeyboardActions(
            onDone = {
              if (title.isNotBlank()) {
                onSaveQuickTask(
                  title.trim(),
                  selectedCategoryId,
                  selectedPriority,
                  selectedDueAt,
                  selectedIsAllDay,
                  selectedAlertAt,
                )
                onDismiss()
              }
            }
          ),
      )

      Text(
        text = "Schedule",
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )

      Row(
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        FilterChip(
          selected = selectedDueAt != null && DateTimeUtils.isSameDay(selectedDueAt!!, todayEnd),
          onClick = {
            selectedDueAt = todayEnd
            selectedIsAllDay = true
          },
          label = { Text("Today") },
          leadingIcon = { Icon(Icons.Rounded.CalendarToday, contentDescription = null, modifier = Modifier.size(16.dp)) },
          modifier = Modifier.testTag("quick_add_due_today"),
        )
        FilterChip(
          selected = selectedDueAt != null && DateTimeUtils.isSameDay(selectedDueAt!!, tomorrowEnd),
          onClick = {
            selectedDueAt = tomorrowEnd
            selectedIsAllDay = true
          },
          label = { Text("Tomorrow") },
          leadingIcon = { Icon(Icons.Rounded.Schedule, contentDescription = null, modifier = Modifier.size(16.dp)) },
        )
        FilterChip(
          selected = selectedDueAt != null && DateTimeUtils.isSameDay(selectedDueAt!!, weekendEnd),
          onClick = {
            selectedDueAt = weekendEnd
            selectedIsAllDay = true
          },
          label = { Text("Weekend") },
        )
        FilterChip(
          selected = selectedDueAt != null && DateTimeUtils.isSameDay(selectedDueAt!!, nextWeekEnd),
          onClick = {
            selectedDueAt = nextWeekEnd
            selectedIsAllDay = true
          },
          label = { Text("Next week") },
        )
        FilterChip(
          selected =
            selectedDueAt != null &&
              !DateTimeUtils.isSameDay(selectedDueAt!!, todayEnd) &&
              !DateTimeUtils.isSameDay(selectedDueAt!!, tomorrowEnd) &&
              !DateTimeUtils.isSameDay(selectedDueAt!!, weekendEnd) &&
              !DateTimeUtils.isSameDay(selectedDueAt!!, nextWeekEnd),
          onClick = { showDatePicker = true },
          label = {
            Text(
              if (selectedDueAt != null) DateTimeUtils.formatShortDate(selectedDueAt!!)
              else "Pick date"
            )
          },
        )
        if (selectedDueAt != null) {
          FilterChip(
            selected = !selectedIsAllDay,
            onClick = { showDueTimePicker = true },
            label = {
              Text(
                if (selectedIsAllDay) "Due time"
                else DateTimeUtils.formatTime(selectedDueAt!!)
              )
            },
            leadingIcon = { Icon(Icons.Rounded.Schedule, contentDescription = null, modifier = Modifier.size(16.dp)) },
            modifier = Modifier.testTag("quick_add_due_time_chip"),
          )
          FilterChip(
            selected = false,
            onClick = {
              selectedDueAt = null
              selectedIsAllDay = true
            },
            label = { Text("No date") },
            leadingIcon = { Icon(Icons.Rounded.Close, contentDescription = null, modifier = Modifier.size(16.dp)) },
          )
        }
      }

      Text(
        text = "Alert me",
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )

      Row(
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        FilterChip(
          selected = selectedAlertPreset == QuickAlertPreset.NONE,
          onClick = {
            selectedAlertPreset = QuickAlertPreset.NONE
            selectedAlertAt = null
          },
          label = { Text("No alert") },
          modifier = Modifier.testTag("quick_add_alert_none"),
        )
        FilterChip(
          selected = selectedAlertPreset == QuickAlertPreset.IN_15M,
          onClick = {
            selectedAlertPreset = QuickAlertPreset.IN_15M
            selectedAlertAt = System.currentTimeMillis() + 15 * 60_000L
            requestNotifPermissionIfNeeded()
          },
          label = { Text("In 15m") },
          leadingIcon = { Icon(Icons.Rounded.Alarm, contentDescription = null, modifier = Modifier.size(16.dp)) },
          modifier = Modifier.testTag("quick_add_alert_15m"),
        )
        FilterChip(
          selected = selectedAlertPreset == QuickAlertPreset.IN_1H,
          onClick = {
            selectedAlertPreset = QuickAlertPreset.IN_1H
            selectedAlertAt = System.currentTimeMillis() + 60 * 60_000L
            requestNotifPermissionIfNeeded()
          },
          label = { Text("In 1h") },
          leadingIcon = { Icon(Icons.Rounded.Alarm, contentDescription = null, modifier = Modifier.size(16.dp)) },
          modifier = Modifier.testTag("quick_add_alert_1h"),
        )
        FilterChip(
          selected = selectedAlertPreset == QuickAlertPreset.TOMORROW_9AM,
          onClick = {
            selectedAlertPreset = QuickAlertPreset.TOMORROW_9AM
            selectedAlertAt = tomorrow9Am
            requestNotifPermissionIfNeeded()
          },
          label = { Text("Tomorrow 9 AM") },
          modifier = Modifier.testTag("quick_add_alert_tomorrow"),
        )
        FilterChip(
          selected = selectedAlertPreset == QuickAlertPreset.CUSTOM,
          onClick = { showAlertTimePicker = true },
          label = {
            Text(
              if (selectedAlertPreset == QuickAlertPreset.CUSTOM && selectedAlertAt != null) {
                DateTimeUtils.formatDateTimeShort(selectedAlertAt!!)
              } else {
                "Pick alert time"
              }
            )
          },
          leadingIcon = { Icon(Icons.Rounded.AlarmAdd, contentDescription = null, modifier = Modifier.size(16.dp)) },
          modifier = Modifier.testTag("quick_add_alert_pick_time"),
        )
      }

      if (selectedAlertAt != null) {
        Text(
          text = "Will alert at ${DateTimeUtils.formatDateTimeShort(selectedAlertAt!!)}",
          style = MaterialTheme.typography.bodySmall,
          fontWeight = FontWeight.SemiBold,
          color = MaterialTheme.colorScheme.primary,
          modifier = Modifier.testTag("quick_add_alert_summary"),
        )
        if (!hasNotificationPermission && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
          Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.35f)),
          ) {
            Row(
              modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
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
                text = "Allow notifications so this alert triggers on your device.",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.weight(1f),
              )
              TextButton(onClick = { permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS) }) {
                Text("Allow", fontWeight = FontWeight.Bold)
              }
            }
          }
        }
        if (!canScheduleExactAlarms && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
          Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.7f),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.tertiary.copy(alpha = 0.35f)),
          ) {
            Row(
              modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
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
                text = "Enable Alarms & reminders for pinpoint alert timing.",
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
                Text("Enable", fontWeight = FontWeight.Bold)
              }
            }
          }
        }
      }

      Text(
        text = "Category",
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )

      Row(
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        FilterChip(
          selected = selectedCategoryId == null,
          onClick = { selectedCategoryId = null },
          label = { Text("Inbox") },
          leadingIcon = { Icon(Icons.Rounded.Inbox, contentDescription = null, modifier = Modifier.size(16.dp)) },
        )
        categories.forEach { cat ->
          FilterChip(
            selected = selectedCategoryId == cat.id,
            onClick = { selectedCategoryId = cat.id },
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

      Text(
        text = "Priority",
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )

      Row(
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        TaskPriority.entries.forEach { prio ->
          FilterChip(
            selected = selectedPriority == prio,
            onClick = { selectedPriority = prio },
            label = { Text(prio.label) },
          )
        }
      }

      Spacer(modifier = Modifier.height(2.dp))

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        OutlinedButton(
          onClick = {
            onOpenFullEditor(
              title.trim(),
              selectedCategoryId,
              selectedPriority,
              selectedDueAt,
              selectedIsAllDay,
              selectedAlertAt,
            )
          },
          shape = RoundedCornerShape(14.dp),
          contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
          modifier = Modifier.testTag("quick_add_more_options_button"),
        ) {
          Icon(Icons.Rounded.Tune, contentDescription = null, modifier = Modifier.size(18.dp))
          Spacer(modifier = Modifier.width(6.dp))
          Text("Full details", maxLines = 1)
        }
        Button(
          onClick = {
            if (title.isNotBlank()) {
              onSaveQuickTask(
                title.trim(),
                selectedCategoryId,
                selectedPriority,
                selectedDueAt,
                selectedIsAllDay,
                selectedAlertAt,
              )
              onDismiss()
            }
          },
          enabled = title.isNotBlank(),
          shape = RoundedCornerShape(14.dp),
          contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
          modifier = Modifier.weight(1f).testTag("quick_add_save_button"),
        ) {
          Text("Save Task", fontWeight = FontWeight.Bold)
        }
      }
    }
  }

  if (showDatePicker) {
    val datePickerState =
      rememberDatePickerState(
        initialSelectedDateMillis = DateTimeUtils.localDateToUtcMidnight(selectedDueAt ?: now)
      )
    DatePickerDialog(
      onDismissRequest = { showDatePicker = false },
      confirmButton = {
        TextButton(
          onClick = {
            datePickerState.selectedDateMillis?.let { millis ->
              val localDay = DateTimeUtils.utcMidnightToLocalStartOfDay(millis)
              selectedDueAt = DateTimeUtils.endOfDay(localDay)
              selectedIsAllDay = true
            }
            showDatePicker = false
          }
        ) {
          Text("Select")
        }
      },
      dismissButton = {
        TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
      },
    ) {
      DatePicker(state = datePickerState)
    }
  }

  if (showDueTimePicker) {
    val base = selectedDueAt ?: now
    val cal = Calendar.getInstance().apply { timeInMillis = base }
    val timeState =
      rememberTimePickerState(
        initialHour = if (selectedIsAllDay) 18 else cal.get(Calendar.HOUR_OF_DAY),
        initialMinute = if (selectedIsAllDay) 0 else cal.get(Calendar.MINUTE),
      )
    AlertDialog(
      onDismissRequest = { showDueTimePicker = false },
      title = { Text("Select due time") },
      text = { TimePicker(state = timeState) },
      confirmButton = {
        TextButton(
          onClick = {
            val combined = DateTimeUtils.combineDateAndTime(base, timeState.hour, timeState.minute)
            selectedDueAt = combined
            selectedIsAllDay = false
            if (selectedAlertPreset == QuickAlertPreset.NONE && combined > System.currentTimeMillis()) {
              selectedAlertAt = combined
              selectedAlertPreset = QuickAlertPreset.CUSTOM
              requestNotifPermissionIfNeeded()
            }
            showDueTimePicker = false
          }
        ) {
          Text("Apply")
        }
      },
      dismissButton = {
        TextButton(
          onClick = {
            selectedDueAt?.let { selectedDueAt = DateTimeUtils.endOfDay(it) }
            selectedIsAllDay = true
            showDueTimePicker = false
          }
        ) {
          Text("All day")
        }
      },
    )
  }

  if (showAlertTimePicker) {
    val defaultInitial =
      selectedAlertAt
        ?: if (selectedDueAt != null && !selectedIsAllDay && selectedDueAt!! > System.currentTimeMillis()) {
          selectedDueAt!!
        } else {
          DateTimeUtils.defaultAlertTimeMillis()
        }
    AlertTimePickerDialog(
      initialTimestampMillis = defaultInitial,
      onDismiss = { showAlertTimePicker = false },
      onConfirm = { alertMillis ->
        selectedAlertPreset = QuickAlertPreset.CUSTOM
        selectedAlertAt = alertMillis
        showAlertTimePicker = false
        requestNotifPermissionIfNeeded()
      },
    )
  }
}
