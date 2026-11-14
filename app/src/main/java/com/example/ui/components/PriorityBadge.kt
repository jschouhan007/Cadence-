package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.local.entity.TaskPriority
import com.example.ui.theme.CadenceThemeTokens

@Composable
fun PrioritySignalMeter(
  priority: TaskPriority,
  tint: Color,
  modifier: Modifier = Modifier,
) {
  val filledBars =
    when (priority) {
      TaskPriority.URGENT, TaskPriority.HIGH -> 3
      TaskPriority.MEDIUM -> 2
      TaskPriority.LOW -> 1
    }
  val heights = listOf(5.dp, 8.dp, 11.dp)

  Row(
    modifier = modifier.semantics { contentDescription = "${priority.label} priority" },
    verticalAlignment = Alignment.Bottom,
    horizontalArrangement = Arrangement.spacedBy(1.5.dp),
  ) {
    heights.forEachIndexed { index, barHeight ->
      val isFilled = index < filledBars
      Box(
        modifier =
          Modifier.width(2.5.dp)
            .height(barHeight)
            .clip(RoundedCornerShape(2.dp))
            .background(if (isFilled) tint else tint.copy(alpha = 0.24f))
      )
    }
  }
}

@Composable
fun PriorityBadge(
  priority: TaskPriority,
  modifier: Modifier = Modifier,
  compact: Boolean = false,
) {
  val semantic = CadenceThemeTokens.colors
  val (fg, bg, borderAlpha) =
    when (priority) {
      TaskPriority.URGENT -> Triple(semantic.urgent, semantic.urgentContainer, 0.36f)
      TaskPriority.HIGH -> Triple(semantic.high, semantic.highContainer, 0.32f)
      TaskPriority.MEDIUM -> Triple(semantic.medium, semantic.mediumContainer, 0.26f)
      TaskPriority.LOW -> Triple(semantic.low, semantic.lowContainer, 0.20f)
    }

  Surface(
    modifier = modifier,
    shape = RoundedCornerShape(9.dp),
    color = bg,
    border = BorderStroke(1.dp, fg.copy(alpha = borderAlpha)),
  ) {
    Row(
      modifier = Modifier.padding(horizontal = if (compact) 7.dp else 9.dp, vertical = 3.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
      PrioritySignalMeter(priority = priority, tint = fg)
      Text(
        text = priority.label,
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.Bold,
        color = fg,
        maxLines = 1,
        softWrap = false,
      )
    }
  }
}

@Composable
fun CategoryPill(
  name: String,
  iconKey: String,
  colorHex: Long,
  modifier: Modifier = Modifier,
  onClick: (() -> Unit)? = null,
) {
  val accent = Color(colorHex)
  val bg = accent.copy(alpha = 0.13f)
  val shape = RoundedCornerShape(9.dp)
  val baseModifier =
    modifier
      .clip(shape)
      .background(bg)
      .let { if (onClick != null) it.clickable(onClick = onClick) else it }
      .padding(horizontal = 8.dp, vertical = 3.dp)

  Row(
    modifier = baseModifier,
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(4.dp),
  ) {
    Icon(
      imageVector = categoryIconForKey(iconKey),
      contentDescription = null,
      tint = accent,
      modifier = Modifier.size(13.dp),
    )
    Text(
      text = name,
      style = MaterialTheme.typography.labelSmall,
      fontWeight = FontWeight.SemiBold,
      color = accent,
      maxLines = 1,
      softWrap = false,
      overflow = TextOverflow.Ellipsis,
    )
  }
}

@Composable
fun StatusPill(
  label: String,
  icon: ImageVector,
  containerColor: Color,
  contentColor: Color,
  modifier: Modifier = Modifier,
) {
  Row(
    modifier =
      modifier
        .clip(RoundedCornerShape(9.dp))
        .background(containerColor)
        .padding(horizontal = 8.dp, vertical = 3.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(4.dp),
  ) {
    Icon(imageVector = icon, contentDescription = null, tint = contentColor, modifier = Modifier.size(13.dp))
    Text(
      text = label,
      style = MaterialTheme.typography.labelSmall,
      fontWeight = FontWeight.SemiBold,
      color = contentColor,
      maxLines = 1,
      softWrap = false,
      overflow = TextOverflow.Ellipsis,
    )
  }
}
