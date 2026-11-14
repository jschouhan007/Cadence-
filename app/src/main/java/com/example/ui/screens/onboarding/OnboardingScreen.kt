package com.example.ui.screens.onboarding

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.domain.model.StarterCategoryTemplates
import com.example.ui.components.categoryIconForKey
import com.example.ui.theme.CadenceThemeTokens
import com.example.ui.theme.Spacing
import com.example.ui.theme.ThemeMode

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun OnboardingScreen(
  viewModel: OnboardingViewModel,
  onFinished: () -> Unit,
) {
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()
  val semantic = CadenceThemeTokens.colors

  val notificationPermissionLauncher =
    rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { _ ->
      viewModel.completeOnboarding(onFinished)
    }

  Box(
    modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).safeDrawingPadding(),
    contentAlignment = Alignment.TopCenter,
  ) {
    Column(
      modifier =
        Modifier.widthIn(max = Spacing.maxContentWidth)
          .fillMaxSize()
          .verticalScroll(rememberScrollState())
          .padding(horizontal = Spacing.lg, vertical = Spacing.md),
      verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          Box(
            modifier =
              Modifier.size(40.dp)
                .clip(RoundedCornerShape(13.dp))
                .background(MaterialTheme.colorScheme.primary),
            contentAlignment = Alignment.Center,
          ) {
            Icon(
              imageVector = Icons.Rounded.AutoAwesome,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.onPrimary,
            )
          }
          Column {
            Text(
              text = "Cadence",
              style = MaterialTheme.typography.titleLarge,
              fontWeight = FontWeight.Bold,
            )
            Text(
              text = "Sunlit Studio Desk",
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.secondary,
            )
          }
        }
        TextButton(
          onClick = { viewModel.completeOnboarding(onFinished) },
          modifier = Modifier.testTag("onboarding_skip_button"),
        ) {
          Text("Skip", fontWeight = FontWeight.SemiBold)
        }
      }

      // Hero Illustration Card
      Surface(
        shape = RoundedCornerShape(24.dp),
        color = semantic.cardElevated,
        border = BorderStroke(1.2.dp, MaterialTheme.colorScheme.outlineVariant),
        tonalElevation = 1.dp,
      ) {
        Image(
          painter = painterResource(id = R.drawable.img_onboarding_studio),
          contentDescription = "Sunlit Studio productivity desk illustration",
          contentScale = ContentScale.Crop,
          modifier = Modifier.fillMaxWidth().height(168.dp),
        )
      }

      Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
          text = "Bright, tactile focus\nfor every day.",
          style = MaterialTheme.typography.displayMedium,
          color = MaterialTheme.colorScheme.onBackground,
        )
        Text(
          text =
            "Capture tasks in seconds, color-code your workspaces, and stay ahead with exact reminders — completely offline-first.",
          style = MaterialTheme.typography.bodyLarge,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }

      // Theme selector
      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
          text = "1. Choose your atmosphere",
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.Bold,
        )
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
          ThemeMode.entries.forEach { mode ->
            val selected = uiState.selectedTheme == mode
            val icon =
              when (mode) {
                ThemeMode.SYSTEM -> Icons.Rounded.PhoneAndroid
                ThemeMode.LIGHT -> Icons.Rounded.LightMode
                ThemeMode.DARK -> Icons.Rounded.DarkMode
              }
            val accentTint =
              when (mode) {
                ThemeMode.SYSTEM -> MaterialTheme.colorScheme.primary
                ThemeMode.LIGHT -> semantic.marigold
                ThemeMode.DARK -> MaterialTheme.colorScheme.tertiary
              }
            Surface(
              modifier =
                Modifier.weight(1f)
                  .clip(RoundedCornerShape(18.dp))
                  .clickable { viewModel.selectTheme(mode) }
                  .testTag("onboarding_theme_${mode.name.lowercase()}"),
              shape = RoundedCornerShape(18.dp),
              color =
                if (selected) MaterialTheme.colorScheme.primaryContainer
                else semantic.cardElevated,
              border =
                BorderStroke(
                  width = if (selected) 1.75.dp else 1.dp,
                  color =
                    if (selected) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.outlineVariant,
                ),
            ) {
              Column(
                modifier = Modifier.padding(vertical = 14.dp, horizontal = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp),
              ) {
                Box(
                  modifier =
                    Modifier.size(34.dp)
                      .clip(RoundedCornerShape(10.dp))
                      .background(accentTint.copy(alpha = 0.14f)),
                  contentAlignment = Alignment.Center,
                ) {
                  Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accentTint,
                    modifier = Modifier.size(18.dp),
                  )
                }
                Text(
                  text = mode.label,
                  style = MaterialTheme.typography.labelLarge,
                  fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                  color =
                    if (selected) MaterialTheme.colorScheme.onPrimaryContainer
                    else MaterialTheme.colorScheme.onSurface,
                  maxLines = 1,
                )
              }
            }
          }
        }
      }

      // Starter categories
      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
          text = "2. Pick starter workspaces",
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.Bold,
        )
        Text(
          text = "Each workspace gets its own vibrant color accent — edit or add more anytime.",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        FlowRow(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(10.dp),
          verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          StarterCategoryTemplates.forEach { tpl ->
            val selected = tpl.key in uiState.selectedCategoryKeys
            val accent = Color(tpl.colorHex)
            Surface(
              modifier =
                Modifier.clip(RoundedCornerShape(14.dp))
                  .clickable { viewModel.toggleStarterCategory(tpl.key) }
                  .testTag("onboarding_cat_${tpl.key}"),
              shape = RoundedCornerShape(14.dp),
              color =
                if (selected) accent.copy(alpha = 0.14f)
                else semantic.cardElevated,
              border =
                BorderStroke(
                  width = if (selected) 1.5.dp else 1.dp,
                  color = if (selected) accent else MaterialTheme.colorScheme.outlineVariant,
                ),
            ) {
              Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
              ) {
                Icon(
                  imageVector = categoryIconForKey(tpl.iconKey),
                  contentDescription = null,
                  tint = accent,
                  modifier = Modifier.size(18.dp),
                )
                Text(
                  text = tpl.name,
                  style = MaterialTheme.typography.labelLarge,
                  color = MaterialTheme.colorScheme.onSurface,
                )
                if (selected) {
                  Box(
                    modifier = Modifier.size(18.dp).clip(CircleShape).background(accent),
                    contentAlignment = Alignment.Center,
                  ) {
                    Icon(
                      imageVector = Icons.Rounded.Check,
                      contentDescription = "Selected",
                      tint = Color.White,
                      modifier = Modifier.size(12.dp),
                    )
                  }
                }
              }
            }
          }
        }
      }

      // Reminder preference
      Surface(
        shape = RoundedCornerShape(18.dp),
        color = semantic.cardElevated,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
      ) {
        Row(
          modifier = Modifier.fillMaxWidth().padding(16.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
          Box(
            modifier =
              Modifier.size(42.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.secondaryContainer),
            contentAlignment = Alignment.Center,
          ) {
            Icon(
              imageVector = Icons.Rounded.NotificationsActive,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.secondary,
            )
          }
          Column(modifier = Modifier.weight(1f)) {
            Text("Enable task reminders", style = MaterialTheme.typography.titleSmall)
            Text(
              "Receive timely alerts for scheduled deadlines and focus sessions.",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
          Switch(
            checked = uiState.remindersEnabled,
            onCheckedChange = viewModel::setRemindersEnabled,
            modifier = Modifier.testTag("onboarding_reminders_switch"),
          )
        }
      }

      Button(
        onClick = {
          if (uiState.remindersEnabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
          } else {
            viewModel.completeOnboarding(onFinished)
          }
        },
        enabled = !uiState.isCompleting,
        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 14.dp),
        modifier = Modifier.fillMaxWidth().height(54.dp).testTag("onboarding_get_started_button"),
        shape = RoundedCornerShape(16.dp),
      ) {
        Text("Start Organizing", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.width(8.dp))
        Icon(Icons.AutoMirrored.Rounded.ArrowForward, contentDescription = null, modifier = Modifier.size(18.dp))
      }
    }
  }
}
