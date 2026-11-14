package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.ui.theme.CadenceThemeTokens
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.MotionDuration

@Composable
fun ProgressRing(
  progress: Float,
  centerLabel: String,
  modifier: Modifier = Modifier,
  size: Dp = 92.dp,
  strokeWidth: Dp = 10.dp,
  trackColor: Color = MaterialTheme.colorScheme.surfaceContainerHigh,
  indicatorColor: Color = MaterialTheme.colorScheme.primary,
  secondaryIndicatorColor: Color = MaterialTheme.colorScheme.tertiary,
  subLabel: String? = null,
) {
  val animatedProgress by
    animateFloatAsState(
      targetValue = progress.coerceIn(0f, 1f),
      animationSpec = tween(durationMillis = MotionDuration.Emphasized),
      label = "progressRing",
    )

  Box(modifier = modifier.size(size), contentAlignment = Alignment.Center) {
    Canvas(modifier = Modifier.size(size)) {
      val strokePx = strokeWidth.toPx()
      drawArc(
        color = trackColor,
        startAngle = -90f,
        sweepAngle = 360f,
        useCenter = false,
        style = Stroke(width = strokePx, cap = StrokeCap.Round),
      )
      if (animatedProgress > 0f) {
        drawArc(
          brush =
            Brush.sweepGradient(
              colors = listOf(indicatorColor, secondaryIndicatorColor, indicatorColor)
            ),
          startAngle = -90f,
          sweepAngle = 360f * animatedProgress,
          useCenter = false,
          style = Stroke(width = strokePx, cap = StrokeCap.Round),
        )
      }
    }
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
      Text(
        text = centerLabel,
        style = MaterialTheme.typography.titleLarge.copy(
          fontFamily = JetBrainsMonoFamily,
          fontFeatureSettings = "tnum",
        ),
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurface,
      )
      if (subLabel != null) {
        Text(
          text = subLabel,
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
    }
  }
}

@Composable
fun EmptyStateView(
  icon: ImageVector,
  title: String,
  message: String,
  modifier: Modifier = Modifier,
  actionLabel: String? = null,
  onAction: (() -> Unit)? = null,
  testTag: String = "empty_state_view",
) {
  val semantic = CadenceThemeTokens.colors
  Surface(
    modifier = modifier.fillMaxWidth().testTag(testTag),
    shape = RoundedCornerShape(24.dp),
    color = semantic.cardElevated,
    border = BorderStroke(1.2.dp, MaterialTheme.colorScheme.outlineVariant),
    tonalElevation = 1.dp,
  ) {
    Column(
      modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 26.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center,
    ) {
      Box(contentAlignment = Alignment.BottomEnd) {
        Image(
          painter = painterResource(id = R.drawable.img_empty_horizon),
          contentDescription = null,
          contentScale = ContentScale.Crop,
          modifier =
            Modifier.size(116.dp)
              .clip(RoundedCornerShape(24.dp)),
        )
        Box(
          modifier =
            Modifier.size(34.dp)
              .clip(CircleShape)
              .background(MaterialTheme.colorScheme.primary),
          contentAlignment = Alignment.Center,
        ) {
          Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier.size(18.dp),
          )
        }
      }
      Spacer(modifier = Modifier.height(16.dp))
      Text(
        text = title,
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurface,
        textAlign = TextAlign.Center,
      )
      Spacer(modifier = Modifier.height(6.dp))
      Text(
        text = message,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
      )
      if (actionLabel != null && onAction != null) {
        Spacer(modifier = Modifier.height(18.dp))
        Button(
          onClick = onAction,
          shape = RoundedCornerShape(14.dp),
          contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
          modifier = Modifier.testTag("${testTag}_action"),
        ) {
          Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
          Spacer(modifier = Modifier.width(6.dp))
          Text(actionLabel, style = MaterialTheme.typography.labelLarge)
        }
      }
    }
  }
}
