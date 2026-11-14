package com.example.ui.screens.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.preferences.UserPreferencesRepository
import com.example.data.repository.CategoryRepository
import com.example.domain.model.StarterCategoryTemplates
import com.example.ui.theme.ThemeMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class OnboardingUiState(
  val selectedTheme: ThemeMode = ThemeMode.SYSTEM,
  val selectedCategoryKeys: Set<String> = StarterCategoryTemplates.take(4).map { it.key }.toSet(),
  val remindersEnabled: Boolean = true,
  val isCompleting: Boolean = false,
)

class OnboardingViewModel(
  private val categoryRepository: CategoryRepository,
  private val preferencesRepository: UserPreferencesRepository,
) : ViewModel() {

  private val _uiState = MutableStateFlow(OnboardingUiState())
  val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

  fun selectTheme(mode: ThemeMode) {
    _uiState.update { it.copy(selectedTheme = mode) }
    viewModelScope.launch { preferencesRepository.setThemeMode(mode) }
  }

  fun toggleStarterCategory(key: String) {
    _uiState.update { state ->
      val next =
        if (key in state.selectedCategoryKeys) state.selectedCategoryKeys - key
        else state.selectedCategoryKeys + key
      state.copy(selectedCategoryKeys = next)
    }
  }

  fun setRemindersEnabled(enabled: Boolean) {
    _uiState.update { it.copy(remindersEnabled = enabled) }
    viewModelScope.launch { preferencesRepository.setDailyBriefingEnabled(enabled) }
  }

  fun completeOnboarding(onFinished: () -> Unit) {
    if (_uiState.value.isCompleting) return
    _uiState.update { it.copy(isCompleting = true) }
    viewModelScope.launch {
      val state = _uiState.value
      categoryRepository.seedStarterCategoriesIfEmpty(state.selectedCategoryKeys.toList())
      preferencesRepository.setThemeMode(state.selectedTheme)
      preferencesRepository.setDailyBriefingEnabled(state.remindersEnabled)
      preferencesRepository.setOnboardingCompleted(true)
      onFinished()
    }
  }
}
