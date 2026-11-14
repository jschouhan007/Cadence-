package com.example.ui.screens.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.entity.TaskStatus
import com.example.data.local.entity.TaskWithDetails
import com.example.data.preferences.UserPreferencesRepository
import com.example.data.repository.TaskRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SearchUiState(
  val query: String = "",
  val includeCompleted: Boolean = true,
  val results: List<TaskWithDetails> = emptyList(),
  val recentSearches: List<String> = emptyList(),
)

@OptIn(ExperimentalCoroutinesApi::class)
class SearchViewModel(
  private val taskRepository: TaskRepository,
  private val preferencesRepository: UserPreferencesRepository,
) : ViewModel() {

  private val queryFlow = MutableStateFlow("")
  private val includeCompletedFlow = MutableStateFlow(true)

  private val searchResultsFlow =
    queryFlow.flatMapLatest { q ->
      if (q.isBlank()) flowOf(emptyList()) else taskRepository.searchTasks(q)
    }

  val uiState: StateFlow<SearchUiState> =
    combine(
        queryFlow,
        includeCompletedFlow,
        searchResultsFlow,
        preferencesRepository.preferences,
      ) { query, includeCompleted, results, prefs ->
        val filtered =
          if (includeCompleted) results
          else results.filter { it.task.status != TaskStatus.COMPLETED }
        SearchUiState(
          query = query,
          includeCompleted = includeCompleted,
          results = filtered,
          recentSearches = prefs.recentSearches,
        )
      }
      .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SearchUiState(),
      )

  fun setQuery(query: String) {
    queryFlow.value = query
  }

  fun commitSearch() {
    val q = queryFlow.value.trim()
    if (q.isNotBlank()) {
      viewModelScope.launch { preferencesRepository.addRecentSearch(q) }
    }
  }

  fun setIncludeCompleted(include: Boolean) {
    includeCompletedFlow.value = include
  }

  fun clearRecentSearches() {
    viewModelScope.launch { preferencesRepository.clearRecentSearches() }
  }

  fun toggleTaskCompletion(taskId: String) {
    viewModelScope.launch { taskRepository.toggleTaskCompletion(taskId) }
  }
}
