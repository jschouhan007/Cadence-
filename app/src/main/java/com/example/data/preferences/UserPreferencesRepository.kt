package com.example.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.ui.theme.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "cadence_prefs")

data class UserPreferences(
  val hasCompletedOnboarding: Boolean = false,
  val themeMode: ThemeMode = ThemeMode.SYSTEM,
  val useDynamicColor: Boolean = false,
  val dailyTaskGoal: Int = 5,
  val defaultFocusMinutes: Int = 25,
  val defaultBreakMinutes: Int = 5,
  val dailyBriefingEnabled: Boolean = true,
  val quietHoursEnabled: Boolean = true,
  val quietHoursStartMinute: Int = 22 * 60,
  val quietHoursEndMinute: Int = 7 * 60,
  val showStreaksInProgress: Boolean = true,
  val recentSearches: List<String> = emptyList(),
)

class UserPreferencesRepository(private val context: Context) {
  private object Keys {
    val ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
    val THEME_MODE = stringPreferencesKey("theme_mode")
    val DYNAMIC_COLOR = booleanPreferencesKey("dynamic_color")
    val DAILY_TASK_GOAL = intPreferencesKey("daily_task_goal")
    val DEFAULT_FOCUS_MINUTES = intPreferencesKey("default_focus_minutes")
    val DEFAULT_BREAK_MINUTES = intPreferencesKey("default_break_minutes")
    val DAILY_BRIEFING_ENABLED = booleanPreferencesKey("daily_briefing_enabled")
    val QUIET_HOURS_ENABLED = booleanPreferencesKey("quiet_hours_enabled")
    val QUIET_START_MINUTE = intPreferencesKey("quiet_start_minute")
    val QUIET_END_MINUTE = intPreferencesKey("quiet_end_minute")
    val SHOW_STREAKS = booleanPreferencesKey("show_streaks")
    val RECENT_SEARCHES = stringPreferencesKey("recent_searches")
  }

  val preferences: Flow<UserPreferences> =
    context.dataStore.data.map { prefs ->
      val themeName = prefs[Keys.THEME_MODE] ?: ThemeMode.SYSTEM.name
      val recentRaw = prefs[Keys.RECENT_SEARCHES] ?: ""
      val recentList =
        if (recentRaw.isBlank()) emptyList()
        else recentRaw.split("||").filter { it.isNotBlank() }

      UserPreferences(
        hasCompletedOnboarding = prefs[Keys.ONBOARDING_COMPLETED] ?: false,
        themeMode = ThemeMode.entries.find { it.name == themeName } ?: ThemeMode.SYSTEM,
        useDynamicColor = prefs[Keys.DYNAMIC_COLOR] ?: false,
        dailyTaskGoal = (prefs[Keys.DAILY_TASK_GOAL] ?: 5).coerceIn(1, 30),
        defaultFocusMinutes = (prefs[Keys.DEFAULT_FOCUS_MINUTES] ?: 25).coerceIn(5, 180),
        defaultBreakMinutes = (prefs[Keys.DEFAULT_BREAK_MINUTES] ?: 5).coerceIn(1, 60),
        dailyBriefingEnabled = prefs[Keys.DAILY_BRIEFING_ENABLED] ?: true,
        quietHoursEnabled = prefs[Keys.QUIET_HOURS_ENABLED] ?: true,
        quietHoursStartMinute = prefs[Keys.QUIET_START_MINUTE] ?: (22 * 60),
        quietHoursEndMinute = prefs[Keys.QUIET_END_MINUTE] ?: (7 * 60),
        showStreaksInProgress = prefs[Keys.SHOW_STREAKS] ?: true,
        recentSearches = recentList,
      )
    }

  suspend fun setOnboardingCompleted(completed: Boolean) {
    context.dataStore.edit { it[Keys.ONBOARDING_COMPLETED] = completed }
  }

  suspend fun setThemeMode(mode: ThemeMode) {
    context.dataStore.edit { it[Keys.THEME_MODE] = mode.name }
  }

  suspend fun setUseDynamicColor(enabled: Boolean) {
    context.dataStore.edit { it[Keys.DYNAMIC_COLOR] = enabled }
  }

  suspend fun setDailyTaskGoal(goal: Int) {
    context.dataStore.edit { it[Keys.DAILY_TASK_GOAL] = goal.coerceIn(1, 30) }
  }

  suspend fun setDefaultFocusMinutes(minutes: Int, breakMinutes: Int) {
    context.dataStore.edit {
      it[Keys.DEFAULT_FOCUS_MINUTES] = minutes.coerceIn(5, 180)
      it[Keys.DEFAULT_BREAK_MINUTES] = breakMinutes.coerceIn(1, 60)
    }
  }

  suspend fun setDailyBriefingEnabled(enabled: Boolean) {
    context.dataStore.edit { it[Keys.DAILY_BRIEFING_ENABLED] = enabled }
  }

  suspend fun setQuietHoursEnabled(enabled: Boolean) {
    context.dataStore.edit { it[Keys.QUIET_HOURS_ENABLED] = enabled }
  }

  suspend fun setShowStreaksInProgress(show: Boolean) {
    context.dataStore.edit { it[Keys.SHOW_STREAKS] = show }
  }

  suspend fun addRecentSearch(query: String) {
    val trimmed = query.trim()
    if (trimmed.isBlank()) return
    context.dataStore.edit { prefs ->
      val current =
        (prefs[Keys.RECENT_SEARCHES] ?: "")
          .split("||")
          .filter { it.isNotBlank() && !it.equals(trimmed, ignoreCase = true) }
      val updated = (listOf(trimmed) + current).take(8)
      prefs[Keys.RECENT_SEARCHES] = updated.joinToString("||")
    }
  }

  suspend fun clearRecentSearches() {
    context.dataStore.edit { it.remove(Keys.RECENT_SEARCHES) }
  }
}
