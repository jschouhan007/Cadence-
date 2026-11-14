package com.example.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.entity.CategoryEntity
import com.example.data.preferences.UserPreferences
import com.example.data.preferences.UserPreferencesRepository
import com.example.data.repository.CategoryRepository
import com.example.data.repository.FocusRepository
import com.example.data.repository.TaskRepository
import com.example.domain.model.CategoryDeleteStrategy
import com.example.domain.sync.CadenceBackupPayload
import com.example.domain.sync.CloudSyncGateway
import com.example.domain.sync.SyncState
import com.example.ui.theme.ThemeMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json

data class SettingsUiState(
  val preferences: UserPreferences = UserPreferences(),
  val archivedCategories: List<CategoryEntity> = emptyList(),
  val syncState: SyncState = SyncState.OFFLINE_LOCAL_ONLY,
  val backupPreviewJson: String? = null,
)

class SettingsViewModel(
  private val preferencesRepository: UserPreferencesRepository,
  private val categoryRepository: CategoryRepository,
  private val taskRepository: TaskRepository,
  private val focusRepository: FocusRepository,
  private val cloudSyncGateway: CloudSyncGateway,
) : ViewModel() {

  private val backupPreview = MutableStateFlow<String?>(null)
  private val json = Json { prettyPrint = true }

  val uiState: StateFlow<SettingsUiState> =
    combine(
        preferencesRepository.preferences,
        categoryRepository.observeAllCategories(),
        backupPreview,
      ) { prefs, allCategories, preview ->
        SettingsUiState(
          preferences = prefs,
          archivedCategories = allCategories.filter { it.isArchived },
          syncState = cloudSyncGateway.currentSyncState(),
          backupPreviewJson = preview,
        )
      }
      .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SettingsUiState(),
      )

  fun setThemeMode(mode: ThemeMode) {
    viewModelScope.launch { preferencesRepository.setThemeMode(mode) }
  }

  fun setDynamicColor(enabled: Boolean) {
    viewModelScope.launch { preferencesRepository.setUseDynamicColor(enabled) }
  }

  fun setDailyGoal(goal: Int) {
    viewModelScope.launch { preferencesRepository.setDailyTaskGoal(goal) }
  }

  fun setDailyBriefing(enabled: Boolean) {
    viewModelScope.launch { preferencesRepository.setDailyBriefingEnabled(enabled) }
  }

  fun setQuietHours(enabled: Boolean) {
    viewModelScope.launch { preferencesRepository.setQuietHoursEnabled(enabled) }
  }

  fun setShowStreaks(show: Boolean) {
    viewModelScope.launch { preferencesRepository.setShowStreaksInProgress(show) }
  }

  fun unarchiveCategory(categoryId: String) {
    viewModelScope.launch { categoryRepository.setArchived(categoryId, false) }
  }

  fun deleteArchivedCategory(categoryId: String) {
    viewModelScope.launch {
      categoryRepository.deleteCategorySafe(categoryId, CategoryDeleteStrategy.MOVE_TO_INBOX)
    }
  }

  fun generateBackupSnapshot() {
    viewModelScope.launch {
      val categories = categoryRepository.observeAllCategories().first()
      val tasks = taskRepository.observeAllTasks().first()
      val sessions = focusRepository.observeAllSessions().first()
      val payload =
        CadenceBackupPayload(
          schemaVersion = 1,
          exportedAtMillis = System.currentTimeMillis(),
          categoryCount = categories.size,
          taskCount = tasks.size,
          focusSessionCount = sessions.size,
        )
      backupPreview.value = json.encodeToString(CadenceBackupPayload.serializer(), payload)
    }
  }

  fun dismissBackupPreview() {
    backupPreview.value = null
  }
}
