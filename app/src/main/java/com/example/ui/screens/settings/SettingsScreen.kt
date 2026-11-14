package com.example.ui.screens.settings

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Intent
import android.os.Build
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Archive
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.RestoreFromTrash
import androidx.compose.material.icons.rounded.TrackChanges
import androidx.compose.material.icons.rounded.Widgets
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
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
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.components.StudioSegmentedTabs
import com.example.ui.components.StudioTabOption
import com.example.ui.theme.CadenceThemeTokens
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.Spacing
import com.example.ui.theme.ThemeMode
import com.example.widget.CadenceBubbleWidgetProvider
import com.example.widget.TaskSnippetBubbleActivity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
  viewModel: SettingsViewModel,
  onNavigateBack: () -> Unit,
) {
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()
  val prefs = uiState.preferences
  val semantic = CadenceThemeTokens.colors
  val context = LocalContext.current

  Scaffold(
    containerColor = MaterialTheme.colorScheme.background,
    topBar = {
      TopAppBar(
        colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
        navigationIcon = {
          IconButton(onClick = onNavigateBack, modifier = Modifier.testTag("settings_back")) {
            Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Navigate back")
          }
        },
        title = {
          Text(
            text = "Settings",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
          )
        },
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
        verticalArrangement = Arrangement.spacedBy(14.dp),
      ) {
        // Appearance
        item(key = "appearance_card") {
          Surface(
            shape = RoundedCornerShape(22.dp),
            color = semantic.cardElevated,
            border = BorderStroke(1.2.dp, MaterialTheme.colorScheme.outlineVariant),
          ) {
            Column(
              modifier = Modifier.fillMaxWidth().padding(18.dp),
              verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
              SettingsSectionHeader(
                icon = Icons.Rounded.Palette,
                title = "Appearance",
                accentColor = MaterialTheme.colorScheme.primary,
                containerColor = MaterialTheme.colorScheme.primaryContainer,
              )

              StudioSegmentedTabs(
                options =
                  ThemeMode.entries.map { mode ->
                    StudioTabOption(
                      value = mode,
                      label = mode.label,
                      testTag = "settings_theme_${mode.name.lowercase()}",
                    )
                  },
                selectedValue = prefs.themeMode,
                onSelect = viewModel::setThemeMode,
              )

              if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                HorizontalDivider(color = semantic.subtleDivider)
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                  Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                    Text(
                      "Material You dynamic color",
                      style = MaterialTheme.typography.bodyLarge,
                      fontWeight = FontWeight.Medium,
                    )
                    Text(
                      "Adapt palette to your Android wallpaper",
                      style = MaterialTheme.typography.bodySmall,
                      color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                  }
                  Switch(
                    checked = prefs.useDynamicColor,
                    onCheckedChange = viewModel::setDynamicColor,
                  )
                }
              }
            }
          }
        }

        // Productivity & Progress
        item(key = "productivity_card") {
          Surface(
            shape = RoundedCornerShape(22.dp),
            color = semantic.cardElevated,
            border = BorderStroke(1.2.dp, MaterialTheme.colorScheme.outlineVariant),
          ) {
            Column(
              modifier = Modifier.fillMaxWidth().padding(18.dp),
              verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
              SettingsSectionHeader(
                icon = Icons.Rounded.TrackChanges,
                title = "Productivity & Progress",
                accentColor = MaterialTheme.colorScheme.tertiary,
                containerColor = MaterialTheme.colorScheme.tertiaryContainer,
              )

              Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
              ) {
                Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                  Text(
                    "Daily task goal",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                  )
                  Text(
                    "Target tasks completed per day",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                  )
                }
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                  FilledTonalIconButton(
                    onClick = { viewModel.setDailyGoal(prefs.dailyTaskGoal - 1) },
                    modifier = Modifier.size(38.dp),
                    colors =
                      IconButtonDefaults.filledTonalIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                      ),
                  ) {
                    Icon(Icons.Rounded.Remove, contentDescription = "Decrease daily goal", modifier = Modifier.size(18.dp))
                  }
                  Text(
                    text = "${prefs.dailyTaskGoal}",
                    style = MaterialTheme.typography.titleMedium,
                    fontFamily = JetBrainsMonoFamily,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 6.dp),
                  )
                  FilledTonalIconButton(
                    onClick = { viewModel.setDailyGoal(prefs.dailyTaskGoal + 1) },
                    modifier = Modifier.size(38.dp),
                    colors =
                      IconButtonDefaults.filledTonalIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                      ),
                  ) {
                    Icon(Icons.Rounded.Add, contentDescription = "Increase daily goal", modifier = Modifier.size(18.dp))
                  }
                }
              }

              HorizontalDivider(color = semantic.subtleDivider)

              Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
              ) {
                Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                  Text(
                    "Show streak indicators",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                  )
                  Text(
                    "Optional gentle momentum tracking in Progress",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                  )
                }
                Switch(
                  checked = prefs.showStreaksInProgress,
                  onCheckedChange = viewModel::setShowStreaks,
                )
              }
            }
          }
        }

        // Reminders
        item(key = "reminders_card") {
          Surface(
            shape = RoundedCornerShape(22.dp),
            color = semantic.cardElevated,
            border = BorderStroke(1.2.dp, MaterialTheme.colorScheme.outlineVariant),
          ) {
            Column(
              modifier = Modifier.fillMaxWidth().padding(18.dp),
              verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
              SettingsSectionHeader(
                icon = Icons.Rounded.NotificationsActive,
                title = "Reminders",
                accentColor = MaterialTheme.colorScheme.secondary,
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
              )

              Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
              ) {
                Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                  Text(
                    "Scheduled task notifications",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                  )
                  Text(
                    "Deliver local alarms for upcoming deadlines",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                  )
                }
                Switch(
                  checked = prefs.dailyBriefingEnabled,
                  onCheckedChange = viewModel::setDailyBriefing,
                )
              }

              HorizontalDivider(color = semantic.subtleDivider)

              Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
              ) {
                Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                  Text(
                    "Quiet hours (10 PM – 7 AM)",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                  )
                  Text(
                    "Respect rest hours for non-urgent alerts",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                  )
                }
                Switch(
                  checked = prefs.quietHoursEnabled,
                  onCheckedChange = viewModel::setQuietHours,
                )
              }
            }
          }
        }

        // Archived Categories
        if (uiState.archivedCategories.isNotEmpty()) {
          item(key = "archived_categories_card") {
            Surface(
              shape = RoundedCornerShape(22.dp),
              color = semantic.cardElevated,
              border = BorderStroke(1.2.dp, MaterialTheme.colorScheme.outlineVariant),
            ) {
              Column(
                modifier = Modifier.fillMaxWidth().padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
              ) {
                SettingsSectionHeader(
                  icon = Icons.Rounded.Archive,
                  title = "Archived Categories",
                  accentColor = semantic.marigold,
                  containerColor = semantic.marigoldContainer,
                )
                uiState.archivedCategories.forEach { cat ->
                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                  ) {
                    Text(
                      cat.name,
                      style = MaterialTheme.typography.bodyLarge,
                      fontWeight = FontWeight.Medium,
                      modifier = Modifier.weight(1f),
                      maxLines = 1,
                      overflow = TextOverflow.Ellipsis,
                    )
                    IconButton(onClick = { viewModel.unarchiveCategory(cat.id) }) {
                      Icon(Icons.Rounded.RestoreFromTrash, contentDescription = "Restore category")
                    }
                    IconButton(onClick = { viewModel.deleteArchivedCategory(cat.id) }) {
                      Icon(
                        Icons.Rounded.DeleteOutline,
                        contentDescription = "Delete archived category",
                        tint = MaterialTheme.colorScheme.error,
                      )
                    }
                  }
                }
              }
            }
          }
        }

        // Home Screen Floating Bubble Widget Showcase
        item(key = "home_widget_card") {
          Surface(
            shape = RoundedCornerShape(22.dp),
            color = Color.Transparent,
            border = BorderStroke(1.2.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.28f)),
          ) {
            Column(
              modifier =
                Modifier.fillMaxWidth()
                  .background(Brush.linearGradient(listOf(semantic.heroStart, semantic.heroEnd)))
                  .padding(18.dp),
              verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
              SettingsSectionHeader(
                icon = Icons.Rounded.Widgets,
                title = "Floating Bubble Widget",
                accentColor = MaterialTheme.colorScheme.primary,
                containerColor = MaterialTheme.colorScheme.surface,
              )
              Text(
                "Add a home screen bubble that opens a scrollable snippet of your tasks to do.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
              ) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                  Button(
                    onClick = {
                      val appWidgetManager = AppWidgetManager.getInstance(context)
                      val provider = ComponentName(context, CadenceBubbleWidgetProvider::class.java)
                      if (appWidgetManager.isRequestPinAppWidgetSupported) {
                        appWidgetManager.requestPinAppWidget(provider, null, null)
                      } else {
                        Toast.makeText(
                          context,
                          "Long-press your home screen and select Widgets to add Task Bubble.",
                          Toast.LENGTH_LONG,
                        ).show()
                      }
                    },
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.weight(1f).testTag("settings_pin_bubble_widget"),
                  ) {
                    Text("Pin Bubble", maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis)
                  }
                }
                OutlinedButton(
                  onClick = {
                    context.startActivity(Intent(context, TaskSnippetBubbleActivity::class.java))
                  },
                  shape = RoundedCornerShape(14.dp),
                  modifier = Modifier.weight(1f).testTag("settings_preview_floating_snippet"),
                ) {
                  Text("Preview Snippet", maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis)
                }
              }
            }
          }
        }

        // Local-First Storage Card
        item(key = "storage_sync_card") {
          Surface(
            shape = RoundedCornerShape(22.dp),
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            border = BorderStroke(1.2.dp, MaterialTheme.colorScheme.outlineVariant),
          ) {
            Column(
              modifier = Modifier.fillMaxWidth().padding(18.dp),
              verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
              SettingsSectionHeader(
                icon = Icons.Rounded.CloudOff,
                title = "Local-First Storage",
                accentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                containerColor = MaterialTheme.colorScheme.surface,
              )
              Text(
                "All tasks, categories, and focus history persist offline in Room SQLite.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
              OutlinedButton(
                onClick = viewModel::generateBackupSnapshot,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth().testTag("settings_backup_snapshot"),
              ) {
                Text("Inspect Backup Snapshot")
              }
            }
          }
        }
      }
    }
  }

  uiState.backupPreviewJson?.let { snapshot ->
    AlertDialog(
      onDismissRequest = viewModel::dismissBackupPreview,
      title = { Text("Workspace Snapshot (v1)") },
      text = {
        Text(
          text = snapshot,
          style = MaterialTheme.typography.bodySmall,
          fontFamily = FontFamily.Monospace,
        )
      },
      confirmButton = { TextButton(onClick = viewModel::dismissBackupPreview) { Text("Close") } },
    )
  }
}

@Composable
private fun SettingsSectionHeader(
  icon: ImageVector,
  title: String,
  accentColor: Color,
  containerColor: Color,
) {
  Row(
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(10.dp),
  ) {
    Box(
      modifier =
        Modifier.size(34.dp)
          .clip(RoundedCornerShape(10.dp))
          .background(containerColor),
      contentAlignment = Alignment.Center,
    ) {
      Icon(
        imageVector = icon,
        contentDescription = null,
        tint = accentColor,
        modifier = Modifier.size(18.dp),
      )
    }
    Text(
      text = title,
      style = MaterialTheme.typography.titleMedium,
      fontWeight = FontWeight.Bold,
    )
  }
}
