package com.example.ui.screens.focus

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.domain.time.DateTimeUtils
import com.example.ui.theme.CadenceThemeTokens
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.Spacing
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FocusScreen(
  viewModel: FocusViewModel,
  onNavigateBack: () -> Unit,
) {
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()
  val active = uiState.activeSession
  val semantic = CadenceThemeTokens.colors
  var taskPickerExpanded by remember { mutableStateOf(false) }

  val mins = uiState.remainingSeconds / 60
  val secs = uiState.remainingSeconds % 60
  val timeFormatted = String.format(Locale.getDefault(), "%02d:%02d", mins, secs)

  Scaffold(
    containerColor = MaterialTheme.colorScheme.background,
    topBar = {
      TopAppBar(
        colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
        navigationIcon = {
          IconButton(onClick = onNavigateBack, modifier = Modifier.testTag("focus_back")) {
            Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Navigate back")
          }
        },
        title = { Text("Focus Mode") },
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
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
      ) {
        // Timer Ring
        item(key = "timer_ring") {
          val trackColor = MaterialTheme.colorScheme.surfaceContainerHigh
          val primaryRing =
            if (active?.status == "PAUSED") MaterialTheme.colorScheme.secondary
            else MaterialTheme.colorScheme.primary
          val secondaryRing =
            if (active?.status == "PAUSED") semantic.marigold
            else semantic.success

          Surface(
            shape = RoundedCornerShape(28.dp),
            color = semantic.cardElevated,
            border = BorderStroke(1.2.dp, MaterialTheme.colorScheme.outlineVariant),
            tonalElevation = 1.dp,
            modifier = Modifier.fillMaxWidth(),
          ) {
            Box(
              modifier = Modifier.fillMaxWidth().padding(vertical = 26.dp),
              contentAlignment = Alignment.Center,
            ) {
              Box(modifier = Modifier.size(230.dp), contentAlignment = Alignment.Center) {
                Canvas(modifier = Modifier.size(230.dp)) {
                  val stroke = 16.dp.toPx()
                  drawArc(
                    color = trackColor,
                    startAngle = -90f,
                    sweepAngle = 360f,
                    useCenter = false,
                    style = Stroke(width = stroke, cap = StrokeCap.Round),
                  )
                  val sweep =
                    if (active == null) 360f
                    else (1f - uiState.progressFraction).coerceIn(0f, 1f) * 360f
                  drawArc(
                    brush = Brush.sweepGradient(colors = listOf(primaryRing, secondaryRing, primaryRing)),
                    startAngle = -90f,
                    sweepAngle = sweep,
                    useCenter = false,
                    style = Stroke(width = stroke, cap = StrokeCap.Round),
                  )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                  Text(
                    text = timeFormatted,
                    style = MaterialTheme.typography.displayLarge.copy(
                      fontFamily = JetBrainsMonoFamily,
                      fontFeatureSettings = "tnum",
                    ),
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.testTag("focus_timer_readout"),
                  )
                  Text(
                    text =
                      when {
                        active == null -> "${uiState.plannedMinutes} min ready"
                        active.status == "PAUSED" -> "Paused · ${active.presetName}"
                        else -> "Focusing · ${active.presetName}"
                      },
                    style = MaterialTheme.typography.labelLarge,
                    color = primaryRing,
                  )
                }
              }
            }
          }
        }

        // Controls
        item(key = "timer_controls") {
          if (active == null) {
            Button(
              onClick = viewModel::startSession,
              modifier = Modifier.fillMaxWidth().height(54.dp).testTag("focus_start_button"),
              shape = RoundedCornerShape(16.dp),
            ) {
              Icon(Icons.Rounded.PlayArrow, contentDescription = null)
              Spacer(modifier = Modifier.width(6.dp))
              Text("Start Focus Session", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
          } else {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
              Button(
                onClick = {
                  if (active.status == "PAUSED") viewModel.resumeSession() else viewModel.pauseSession()
                },
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.weight(1f).height(50.dp).testTag("focus_pause_resume_button"),
              ) {
                Icon(
                  imageVector = if (active.status == "PAUSED") Icons.Rounded.PlayArrow else Icons.Rounded.Pause,
                  contentDescription = null,
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(if (active.status == "PAUSED") "Resume" else "Pause", maxLines = 1)
              }
              OutlinedButton(
                onClick = viewModel::completeSession,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.height(50.dp).testTag("focus_complete_button"),
              ) {
                Icon(Icons.Rounded.Check, contentDescription = null)
                Spacer(modifier = Modifier.width(6.dp))
                Text("Done", maxLines = 1)
              }
              OutlinedButton(
                onClick = viewModel::stopSession,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.height(50.dp).testTag("focus_stop_button"),
              ) {
                Icon(Icons.Rounded.Stop, contentDescription = "Stop session")
              }
            }
          }
        }

        // Presets & Task Link (editable when idle)
        if (active == null) {
          item(key = "presets_card") {
            Surface(
              shape = RoundedCornerShape(18.dp),
              color = semantic.cardElevated,
              border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            ) {
              Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Session Preset", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Row(
                  modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                  horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                  FocusPresets.forEach { preset ->
                    FilterChip(
                      selected = !uiState.isCustom && uiState.selectedPreset == preset,
                      onClick = { viewModel.selectPreset(preset) },
                      label = { Text("${preset.name} · ${preset.workMinutes}m") },
                    )
                  }
                }

                Row(
                  modifier = Modifier.fillMaxWidth(),
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                  Text("Custom duration", style = MaterialTheme.typography.bodyMedium)
                  Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { viewModel.setCustomMinutes(uiState.plannedMinutes - 5) }) {
                      Icon(Icons.Rounded.Remove, contentDescription = "Decrease minutes")
                    }
                    Text("${uiState.plannedMinutes} min", style = MaterialTheme.typography.titleSmall)
                    IconButton(onClick = { viewModel.setCustomMinutes(uiState.plannedMinutes + 5) }) {
                      Icon(Icons.Rounded.Add, contentDescription = "Increase minutes")
                    }
                  }
                }

                Box {
                  OutlinedButton(
                    onClick = { taskPickerExpanded = true },
                    modifier = Modifier.fillMaxWidth(),
                  ) {
                    Text(
                      text =
                        uiState.selectedTask?.task?.title?.let { "Linked task: $it" }
                          ?: "Link a task (optional)",
                      maxLines = 1,
                      overflow = TextOverflow.Ellipsis,
                    )
                  }
                  DropdownMenu(
                    expanded = taskPickerExpanded,
                    onDismissRequest = { taskPickerExpanded = false },
                  ) {
                    DropdownMenuItem(
                      text = { Text("No linked task") },
                      onClick = {
                        viewModel.selectTask(null)
                        taskPickerExpanded = false
                      },
                    )
                    uiState.activeTasks.forEach { task ->
                      DropdownMenuItem(
                        text = { Text(task.task.title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                        onClick = {
                          viewModel.selectTask(task.task.id)
                          taskPickerExpanded = false
                        },
                      )
                    }
                  }
                }
              }
            }
          }
        } else {
          item(key = "active_task_banner") {
            Surface(
              shape = RoundedCornerShape(16.dp),
              color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
            ) {
              Column(modifier = Modifier.fillMaxWidth().padding(14.dp)) {
                Text("Current Focus", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(4.dp))
                Text(active.taskTitleSnapshot, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
              }
            }
          }
        }

        if (uiState.recentSessions.isNotEmpty()) {
          item(key = "focus_history_hdr") {
            Text(
              text = "Recent Sessions",
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Bold,
              modifier = Modifier.fillMaxWidth(),
            )
          }
          items(uiState.recentSessions, key = { it.id }) { session ->
            Surface(
              shape = RoundedCornerShape(14.dp),
              color = MaterialTheme.colorScheme.surfaceContainerLow,
            ) {
              Row(
                modifier = Modifier.fillMaxWidth().padding(14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
              ) {
                Column(modifier = Modifier.weight(1f)) {
                  Text(session.taskTitleSnapshot.ifBlank { session.presetName }, style = MaterialTheme.typography.bodyMedium)
                  Text(
                    "${session.presetName} · ${session.plannedMinutes} min · ${session.status}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                  )
                }
                Text(
                  text = DateTimeUtils.formatShortDate(session.startedAt),
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
}
