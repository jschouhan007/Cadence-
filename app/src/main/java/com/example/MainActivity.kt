package com.example

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.preferences.UserPreferences
import com.example.domain.reminder.LocalReminderScheduler
import com.example.ui.navigation.CadenceAppShell
import com.example.ui.theme.CadenceTheme

class MainActivity : ComponentActivity() {
  private val pendingNotificationTaskId = mutableStateOf<String?>(null)

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    pendingNotificationTaskId.value = intent?.getStringExtra(LocalReminderScheduler.EXTRA_TASK_ID)
    val container = (application as CadenceApp).container

    setContent {
      val prefs by
        container.preferencesRepository.preferences.collectAsStateWithLifecycle(
          initialValue = null
        )

      val activePrefs = prefs ?: UserPreferences()
      CadenceTheme(
        themeMode = activePrefs.themeMode,
        dynamicColor = activePrefs.useDynamicColor,
      ) {
        val loadedPrefs = prefs
        if (loadedPrefs == null) {
          Box(
            modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
            contentAlignment = Alignment.Center,
          ) {
            CircularProgressIndicator()
          }
        } else {
          val initialCompleted = androidx.compose.runtime.remember { loadedPrefs.hasCompletedOnboarding }
          val openTaskId by pendingNotificationTaskId
          CadenceAppShell(
            container = container,
            hasCompletedOnboarding = initialCompleted,
            pendingOpenTaskId = openTaskId,
            onClearPendingOpenTaskId = { pendingNotificationTaskId.value = null },
          )
        }
      }
    }
  }

  override fun onNewIntent(intent: Intent) {
    super.onNewIntent(intent)
    setIntent(intent)
    intent.getStringExtra(LocalReminderScheduler.EXTRA_TASK_ID)?.let { taskId ->
      pendingNotificationTaskId.value = taskId
    }
  }
}
