package com.example.ui.screens.reminders

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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Alarm
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.NotificationsNone
import androidx.compose.material.icons.rounded.Snooze
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.domain.time.DateTimeUtils
import com.example.ui.components.EmptyStateView
import com.example.ui.components.TaskRowCard
import com.example.ui.theme.CadenceThemeTokens
import com.example.ui.theme.Spacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReminderCentreScreen(
  viewModel: ReminderCentreViewModel,
  onNavigateBack: () -> Unit,
  onOpenTaskDetail: (String) -> Unit,
) {
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()
  val context = LocalContext.current
  val lifecycleOwner = LocalLifecycleOwner.current
  val semantic = CadenceThemeTokens.colors

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

  Scaffold(
    containerColor = MaterialTheme.colorScheme.background,
    topBar = {
      TopAppBar(
        colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
        navigationIcon = {
          IconButton(onClick = onNavigateBack, modifier = Modifier.testTag("reminders_back")) {
            Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Navigate back")
          }
        },
        title = { Text("Reminder Centre") },
      )
    },
  ) { innerPadding ->
    Box(
      modifier = Modifier.fillMaxSize().padding(innerPadding),
      contentAlignment = Alignment.TopCenter,
    ) {
      LazyColumn(
        modifier = Modifier.widthIn(max = Spacing.maxContentWidth).fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        if (!hasNotificationPermission && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
          item(key = "notif_permission_banner") {
            Surface(
              shape = RoundedCornerShape(18.dp),
              color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f),
            ) {
              Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                  Icon(Icons.Rounded.NotificationsActive, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                  Text("Notifications are disabled", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                }
                Text(
                  "Grant notification permission so Cadence can deliver your scheduled task reminders.",
                  style = MaterialTheme.typography.bodySmall,
                )
                Button(onClick = { permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS) }) {
                  Text("Allow Notifications")
                }
              }
            }
          }
        }

        if (!canScheduleExactAlarms && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
          item(key = "exact_alarm_permission_banner") {
            Surface(
              shape = RoundedCornerShape(18.dp),
              color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.7f),
            ) {
              Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                  Icon(Icons.Rounded.Alarm, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary)
                  Text("Exact alarm timing disabled", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                }
                Text(
                  "Grant Alarms & reminders access so scheduled task alerts fire at the exact minute you choose.",
                  style = MaterialTheme.typography.bodySmall,
                )
                OutlinedButton(
                  onClick = {
                    val intent =
                      Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                        data = Uri.fromParts("package", context.packageName, null)
                      }
                    runCatching { context.startActivity(intent) }
                  }
                ) {
                  Text("Enable Exact Alerts")
                }
              }
            }
          }
        }

        if (
          uiState.activeReminders.isEmpty() &&
            uiState.overdueTasks.isEmpty() &&
            uiState.dismissedReminders.isEmpty()
        ) {
          item(key = "reminders_empty") {
            EmptyStateView(
              icon = Icons.Rounded.NotificationsNone,
              title = "All caught up",
              message = "Scheduled task reminders and overdue alerts will appear here.",
            )
          }
        }

        if (uiState.activeReminders.isNotEmpty()) {
          item(key = "upcoming_rem_hdr") {
            Text(
              text = "Scheduled Reminders (${uiState.activeReminders.size})",
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Bold,
            )
          }
          items(uiState.activeReminders, key = { "rem_${it.reminder.id}" }) { item ->
            Surface(
              modifier =
                Modifier.fillMaxWidth()
                  .clip(RoundedCornerShape(16.dp))
                  .clickable { onOpenTaskDetail(item.task.task.id) },
              shape = RoundedCornerShape(16.dp),
              color = semantic.cardElevated,
              border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            ) {
              Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                  Column(modifier = Modifier.weight(1f)) {
                    Text(item.task.task.title, style = MaterialTheme.typography.titleSmall)
                    Text(
                      text =
                        "${DateTimeUtils.formatShortDate(item.reminder.triggerAt)} · ${DateTimeUtils.formatTime(item.reminder.triggerAt)}",
                      style = MaterialTheme.typography.bodySmall,
                      color = MaterialTheme.colorScheme.primary,
                    )
                  }
                  Switch(
                    checked = item.reminder.isEnabled,
                    onCheckedChange = { enabled -> viewModel.toggleEnabled(item.reminder, enabled) },
                  )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                  OutlinedButton(
                    onClick = { viewModel.snooze(item.reminder, 15) },
                    shape = RoundedCornerShape(12.dp),
                  ) {
                    Icon(Icons.Rounded.Snooze, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Snooze 15m", maxLines = 1)
                  }
                  TextButton(onClick = { viewModel.dismiss(item.reminder) }) {
                    Text("Dismiss")
                  }
                }
              }
            }
          }
        }

        if (uiState.overdueTasks.isNotEmpty()) {
          item(key = "overdue_rem_hdr") {
            Text(
              text = "Overdue Attention (${uiState.overdueTasks.size})",
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Bold,
              color = semantic.overdue,
              modifier = Modifier.padding(top = 4.dp),
            )
          }
          items(uiState.overdueTasks, key = { "ov_${it.task.id}" }) { task ->
            TaskRowCard(
              item = task,
              onClick = { onOpenTaskDetail(task.task.id) },
              onToggleComplete = { viewModel.toggleTaskCompletion(task.task.id) },
            )
          }
        }
      }
    }
  }
}
