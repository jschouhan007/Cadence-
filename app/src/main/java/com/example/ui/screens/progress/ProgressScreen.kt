package com.example.ui.screens.progress

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoGraph
import androidx.compose.material.icons.rounded.CheckCircleOutline
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.PendingActions
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.domain.model.DailyCompletionPoint
import com.example.domain.time.DateTimeUtils
import com.example.ui.components.CadenceTopBar
import com.example.ui.components.EmptyStateView
import com.example.ui.components.categoryIconForKey
import com.example.ui.theme.CadenceThemeTokens
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.Spacing

@Composable
fun ProgressScreen(
  viewModel: ProgressViewModel,
  onOpenSearch: () -> Unit,
  onOpenFocus: (String?) -> Unit,
  onOpenReminders: () -> Unit,
  onOpenSettings: () -> Unit,
) {
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()
  val semantic = CadenceThemeTokens.colors

  Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
    CadenceTopBar(
      title = "Progress",
      subtitle = "${uiState.completedTasks} of ${uiState.totalTasks} tasks completed",
      onSearchClick = onOpenSearch,
      onFocusClick = { onOpenFocus(null) },
      onRemindersClick = onOpenReminders,
      onSettingsClick = onOpenSettings,
    )

    if (uiState.isLoading) {
      Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
      }
    } else {
      Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        LazyColumn(
          modifier = Modifier.widthIn(max = Spacing.maxContentWidth).fillMaxSize().testTag("progress_list"),
          contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 104.dp),
          verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
          item(key = "kpi_grid") {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
              Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MetricTile(
                  label = "Completed",
                  value = "${uiState.completedTasks} (${uiState.completionPercent}%)",
                  icon = Icons.Rounded.CheckCircleOutline,
                  tint = semantic.success,
                  containerBg = semantic.successContainer.copy(alpha = 0.5f),
                  modifier = Modifier.weight(1f),
                )
                MetricTile(
                  label = "Pending",
                  value = uiState.pendingTasks.toString(),
                  icon = Icons.Rounded.PendingActions,
                  tint = MaterialTheme.colorScheme.primary,
                  containerBg = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                  modifier = Modifier.weight(1f),
                )
              }
              Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MetricTile(
                  label = "Overdue",
                  value = uiState.overdueTasks.toString(),
                  icon = Icons.Rounded.WarningAmber,
                  tint = if (uiState.overdueTasks > 0) semantic.overdue else MaterialTheme.colorScheme.onSurfaceVariant,
                  containerBg =
                    if (uiState.overdueTasks > 0) semantic.overdueContainer.copy(alpha = 0.55f)
                    else semantic.cardElevated,
                  modifier = Modifier.weight(1f),
                )
                MetricTile(
                  label = "7-Day Focus",
                  value = "${uiState.totalFocusMinutesThisWeek} min",
                  icon = Icons.Rounded.Timer,
                  tint = semantic.marigold,
                  containerBg = semantic.marigoldContainer.copy(alpha = 0.55f),
                  modifier = Modifier.weight(1f),
                )
              }
            }
          }

          if (uiState.showStreaks && uiState.activeDayStreak > 0) {
            item(key = "streak_banner") {
              Surface(
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.secondaryContainer,
                border = BorderStroke(1.2.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.35f)),
              ) {
                Row(
                  modifier = Modifier.fillMaxWidth().padding(14.dp),
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                  Box(
                    modifier =
                      Modifier.size(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.secondary),
                    contentAlignment = Alignment.Center,
                  ) {
                    Icon(
                      Icons.Rounded.LocalFireDepartment,
                      contentDescription = null,
                      tint = MaterialTheme.colorScheme.onSecondary,
                    )
                  }
                  Column {
                    Text(
                      text = "${uiState.activeDayStreak}-day momentum",
                      style = MaterialTheme.typography.titleSmall,
                      fontWeight = FontWeight.Bold,
                    )
                    Text(
                      text = "Steady progress counts. You can toggle streak display anytime in Settings.",
                      style = MaterialTheme.typography.bodySmall,
                      color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                  }
                }
              }
            }
          }

          item(key = "weekly_chart") {
            Surface(
              shape = RoundedCornerShape(22.dp),
              color = semantic.cardElevated,
              border = BorderStroke(1.2.dp, MaterialTheme.colorScheme.outlineVariant),
              tonalElevation = 1.dp,
            ) {
              Column(modifier = Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically,
                ) {
                  Text(
                    text = "Completed in Last 7 Days",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                  )
                  val weeklyTotal = uiState.lastSevenDays.sumOf { it.completedCount }
                  Surface(
                    shape = RoundedCornerShape(9.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                  ) {
                    Text(
                      text = "$weeklyTotal total",
                      style = MaterialTheme.typography.labelSmall.copy(
                        fontFamily = JetBrainsMonoFamily,
                        fontFeatureSettings = "tnum",
                      ),
                      fontWeight = FontWeight.Bold,
                      color = MaterialTheme.colorScheme.primary,
                      modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                    )
                  }
                }
                WeeklyCompletionBarChart(points = uiState.lastSevenDays)
              }
            }
          }

          item(key = "category_breakdown_hdr") {
            Text(
              text = "By Category",
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Bold,
              modifier = Modifier.padding(top = 4.dp),
            )
          }

          if (uiState.categoryStats.isEmpty()) {
            item(key = "progress_cat_empty") {
              EmptyStateView(
                icon = Icons.Rounded.AutoGraph,
                title = "No category insights yet",
                message = "Create categories and complete tasks to see your breakdown across life areas.",
              )
            }
          } else {
            items(uiState.categoryStats, key = { "prog_cat_${it.category.id}" }) { stat ->
              val accent = Color(stat.category.colorHex)
              Surface(
                shape = RoundedCornerShape(18.dp),
                color = semantic.cardElevated,
                border = BorderStroke(1.2.dp, accent.copy(alpha = 0.25f)),
              ) {
                Column(modifier = Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                  ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                      Box(
                        modifier =
                          Modifier.size(30.dp)
                            .clip(RoundedCornerShape(9.dp))
                            .background(accent.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center,
                      ) {
                        Icon(
                          imageVector = categoryIconForKey(stat.category.iconKey),
                          contentDescription = null,
                          tint = accent,
                          modifier = Modifier.size(16.dp),
                        )
                      }
                      Text(
                        stat.category.name,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                      )
                    }
                    Text(
                      text = "${stat.completedTasks}/${stat.totalTasks} (${stat.completionPercent}%)",
                      style = MaterialTheme.typography.labelMedium.copy(
                        fontFamily = JetBrainsMonoFamily,
                        fontFeatureSettings = "tnum",
                      ),
                      fontWeight = FontWeight.SemiBold,
                      color = accent,
                    )
                  }
                  LinearProgressIndicator(
                    progress = { stat.completionRatio },
                    modifier = Modifier.fillMaxWidth().height(8.dp),
                    color = accent,
                    trackColor = accent.copy(alpha = 0.15f),
                    strokeCap = StrokeCap.Round,
                  )
                }
              }
            }
          }

          if (uiState.recentFocusSessions.isNotEmpty()) {
            item(key = "recent_focus_hdr") {
              Text(
                text = "Recent Focus Sessions",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 4.dp),
              )
            }
            items(uiState.recentFocusSessions, key = { "focus_log_${it.id}" }) { session ->
              Surface(
                shape = RoundedCornerShape(16.dp),
                color = semantic.cardElevated,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
              ) {
                Row(
                  modifier = Modifier.fillMaxWidth().padding(14.dp),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically,
                ) {
                  Column(modifier = Modifier.weight(1f)) {
                    Text(
                      text = session.taskTitleSnapshot.ifBlank { session.presetName },
                      style = MaterialTheme.typography.bodyMedium,
                      fontWeight = FontWeight.SemiBold,
                      maxLines = 1,
                      overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                      text = "${session.presetName} · ${session.plannedMinutes} min · ${session.status}",
                      style = MaterialTheme.typography.labelSmall,
                      color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                  }
                  Text(
                    text = DateTimeUtils.formatShortDate(session.startedAt),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold,
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

@Composable
private fun MetricTile(
  label: String,
  value: String,
  icon: ImageVector,
  tint: Color,
  containerBg: Color,
  modifier: Modifier = Modifier,
) {
  Surface(
    modifier = modifier,
    shape = RoundedCornerShape(18.dp),
    color = containerBg,
    border = BorderStroke(1.2.dp, tint.copy(alpha = 0.28f)),
  ) {
    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
      Box(
        modifier =
          Modifier.size(36.dp)
            .clip(RoundedCornerShape(11.dp))
            .background(tint.copy(alpha = 0.16f)),
        contentAlignment = Alignment.Center,
      ) {
        Icon(imageVector = icon, contentDescription = null, tint = tint, modifier = Modifier.size(19.dp))
      }
      Text(
        text = value,
        style = MaterialTheme.typography.titleLarge.copy(
          fontFamily = JetBrainsMonoFamily,
          fontFeatureSettings = "tnum",
        ),
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurface,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
      Text(
        text = label,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
  }
}

@Composable
private fun WeeklyCompletionBarChart(points: List<DailyCompletionPoint>) {
  val primary = MaterialTheme.colorScheme.primary
  val emerald = CadenceThemeTokens.colors.success
  val track = MaterialTheme.colorScheme.surfaceContainerLow
  val divider = CadenceThemeTokens.colors.subtleDivider
  val maxCount = (points.maxOfOrNull { it.completedCount } ?: 1).coerceAtLeast(1)

  Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
    Canvas(modifier = Modifier.fillMaxWidth().height(132.dp)) {
      if (points.isEmpty()) return@Canvas
      val barSpace = size.width / points.size
      val barWidth = (barSpace * 0.5f).coerceAtMost(38.dp.toPx())
      val dash = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)

      drawLine(
        color = divider,
        start = Offset(0f, size.height * 0.5f),
        end = Offset(size.width, size.height * 0.5f),
        strokeWidth = 1.dp.toPx(),
        pathEffect = dash,
      )

      points.forEachIndexed { index, pt ->
        val left = index * barSpace + (barSpace - barWidth) / 2f
        val radius = CornerRadius(10.dp.toPx(), 10.dp.toPx())
        drawRoundRect(
          color = track,
          topLeft = Offset(left, 0f),
          size = Size(barWidth, size.height),
          cornerRadius = radius,
        )
        if (pt.completedCount > 0) {
          val barHeight = ((pt.completedCount.toFloat() / maxCount.toFloat()) * size.height).coerceAtLeast(12.dp.toPx())
          drawRoundRect(
            brush =
              Brush.verticalGradient(
                colors = listOf(emerald, primary),
                startY = size.height - barHeight,
                endY = size.height,
              ),
            topLeft = Offset(left, size.height - barHeight),
            size = Size(barWidth, barHeight),
            cornerRadius = radius,
          )
        }
      }
    }
    Row(modifier = Modifier.fillMaxWidth()) {
      points.forEachIndexed { index, pt ->
        val isToday = index == points.lastIndex
        Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
          Text(
            text = pt.completedCount.toString(),
            style = MaterialTheme.typography.labelSmall.copy(
              fontFamily = JetBrainsMonoFamily,
              fontFeatureSettings = "tnum",
            ),
            fontWeight = if (pt.completedCount > 0) FontWeight.Bold else FontWeight.Medium,
            color = if (pt.completedCount > 0) primary else MaterialTheme.colorScheme.onSurfaceVariant,
          )
          Text(
            text = pt.shortDayLabel,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (isToday) FontWeight.Bold else FontWeight.Medium,
            color = if (isToday) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      }
    }
  }
}
