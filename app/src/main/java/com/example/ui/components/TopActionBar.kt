package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.NotificationsNone
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.ui.theme.CadenceThemeTokens

@Composable
fun CadenceTopBar(
  title: String,
  subtitle: String? = null,
  reminderBadgeCount: Int = 0,
  onSearchClick: () -> Unit,
  onFocusClick: () -> Unit,
  onRemindersClick: () -> Unit,
  onSettingsClick: () -> Unit,
  extraActions: @Composable RowScope.() -> Unit = {},
) {
  val semantic = CadenceThemeTokens.colors

  Row(
    modifier =
      Modifier.fillMaxWidth()
        .background(MaterialTheme.colorScheme.background)
        .statusBarsPadding()
        .padding(start = 16.dp, end = 12.dp, top = 12.dp, bottom = 8.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(10.dp),
  ) {
    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
      Text(
        text = title,
        style = MaterialTheme.typography.headlineMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onBackground,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
      if (subtitle != null) {
        Text(
          text = subtitle,
          style = MaterialTheme.typography.labelMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
      }
    }

    Surface(
      shape = RoundedCornerShape(24.dp),
      color = semantic.cardElevated,
      border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
      tonalElevation = 1.dp,
    ) {
      Row(
        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(0.dp),
      ) {
        extraActions()
        TopDockActionIcon(
          icon = Icons.Rounded.Search,
          contentDescription = "Search tasks",
          tint = MaterialTheme.colorScheme.onSurface,
          onClick = onSearchClick,
          testTag = "topbar_search_button",
        )
        TopDockActionIcon(
          icon = Icons.Rounded.Timer,
          contentDescription = "Focus mode",
          tint = MaterialTheme.colorScheme.primary,
          onClick = onFocusClick,
          testTag = "topbar_focus_button",
        )
        Box(
          modifier =
            Modifier.minimumInteractiveComponentSize()
              .size(40.dp)
              .clip(CircleShape)
              .clickable(role = Role.Button, onClick = onRemindersClick)
              .testTag("topbar_reminders_button"),
          contentAlignment = Alignment.Center,
        ) {
          if (reminderBadgeCount > 0) {
            BadgedBox(
              badge = {
                Badge(
                  containerColor = MaterialTheme.colorScheme.secondary,
                  contentColor = Color.White,
                ) {
                  Text(reminderBadgeCount.coerceAtMost(99).toString())
                }
              }
            ) {
              Icon(
                imageVector = Icons.Rounded.NotificationsNone,
                contentDescription = "Reminder centre",
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(20.dp),
              )
            }
          } else {
            Icon(
              imageVector = Icons.Rounded.NotificationsNone,
              contentDescription = "Reminder centre",
              tint = MaterialTheme.colorScheme.onSurface,
              modifier = Modifier.size(20.dp),
            )
          }
        }
        TopDockActionIcon(
          icon = Icons.Rounded.Settings,
          contentDescription = "Settings",
          tint = MaterialTheme.colorScheme.onSurfaceVariant,
          onClick = onSettingsClick,
          testTag = "topbar_settings_button",
        )
      }
    }
  }
}

@Composable
fun TopDockActionIcon(
  icon: ImageVector,
  contentDescription: String,
  onClick: () -> Unit,
  tint: Color = MaterialTheme.colorScheme.onSurface,
  testTag: String? = null,
) {
  Box(
    modifier =
      Modifier.minimumInteractiveComponentSize()
        .size(40.dp)
        .clip(CircleShape)
        .let { if (testTag != null) it.testTag(testTag) else it }
        .clickable(role = Role.Button, onClick = onClick),
    contentAlignment = Alignment.Center,
  ) {
    Icon(
      imageVector = icon,
      contentDescription = contentDescription,
      tint = tint,
      modifier = Modifier.size(20.dp),
    )
  }
}
