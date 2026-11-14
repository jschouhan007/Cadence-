package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.ui.theme.CadenceThemeTokens
import com.example.ui.theme.MotionDuration

data class StudioTabOption<T>(
  val value: T,
  val label: String,
  val testTag: String? = null,
)

@Composable
fun <T> StudioSegmentedTabs(
  options: List<StudioTabOption<T>>,
  selectedValue: T,
  onSelect: (T) -> Unit,
  modifier: Modifier = Modifier,
) {
  val semantic = CadenceThemeTokens.colors
  Surface(
    modifier = modifier.fillMaxWidth(),
    shape = RoundedCornerShape(16.dp),
    color = MaterialTheme.colorScheme.surfaceContainerLow,
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
  ) {
    Row(
      modifier = Modifier.fillMaxWidth().padding(4.dp),
      horizontalArrangement = Arrangement.spacedBy(4.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      options.forEach { option ->
        val selected = option.value == selectedValue
        val bgColor by
          animateColorAsState(
            targetValue = if (selected) MaterialTheme.colorScheme.primary else Color.Transparent,
            animationSpec = tween(MotionDuration.Fast),
            label = "studioTabBg",
          )
        val fgColor by
          animateColorAsState(
            targetValue =
              if (selected) MaterialTheme.colorScheme.onPrimary
              else MaterialTheme.colorScheme.onSurfaceVariant,
            animationSpec = tween(MotionDuration.Fast),
            label = "studioTabFg",
          )

        Box(
          modifier =
            Modifier.weight(1f)
              .heightIn(min = 42.dp)
              .clip(RoundedCornerShape(12.dp))
              .background(bgColor)
              .let { base ->
                if (option.testTag != null) base.testTag(option.testTag) else base
              }
              .clickable(role = Role.Tab) { onSelect(option.value) }
              .padding(horizontal = 8.dp, vertical = 10.dp),
          contentAlignment = Alignment.Center,
        ) {
          Text(
            text = option.label,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color = fgColor,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis,
          )
        }
      }
    }
  }
}
