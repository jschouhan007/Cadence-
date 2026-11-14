package com.example.ui.screens.focus

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.entity.FocusSessionEntity
import com.example.data.local.entity.TaskStatus
import com.example.data.local.entity.TaskWithDetails
import com.example.data.repository.FocusRepository
import com.example.data.repository.TaskRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class FocusPreset(val name: String, val workMinutes: Int, val breakMinutes: Int)

val FocusPresets =
  listOf(
    FocusPreset("Pomodoro", 25, 5),
    FocusPreset("Deep Work", 50, 10),
    FocusPreset("Long Session", 90, 15),
    FocusPreset("Quick Sprint", 15, 3),
  )

private data class FocusSelectionState(
  val selectedPreset: FocusPreset = FocusPresets.first(),
  val customMinutes: Int = 25,
  val isCustom: Boolean = false,
  val selectedTaskId: String? = null,
)

data class FocusUiState(
  val selectedPreset: FocusPreset = FocusPresets.first(),
  val plannedMinutes: Int = 25,
  val isCustom: Boolean = false,
  val activeTasks: List<TaskWithDetails> = emptyList(),
  val selectedTask: TaskWithDetails? = null,
  val activeSession: FocusSessionEntity? = null,
  val remainingSeconds: Int = 25 * 60,
  val progressFraction: Float = 0f,
  val recentSessions: List<FocusSessionEntity> = emptyList(),
)

class FocusViewModel(
  initialTaskId: String?,
  private val focusRepository: FocusRepository,
  private val taskRepository: TaskRepository,
) : ViewModel() {

  private val selection = MutableStateFlow(FocusSelectionState(selectedTaskId = initialTaskId))

  private val ticker =
    flow {
      while (true) {
        emit(System.currentTimeMillis())
        delay(500L)
      }
    }

  val uiState: StateFlow<FocusUiState> =
    combine(
        focusRepository.observeActiveSession(),
        focusRepository.observeAllSessions(),
        taskRepository.observeAllActiveTasks(),
        selection,
        ticker,
      ) { activeSession, allSessions, tasks, sel, now ->
        val activeTasks =
          tasks.filter {
            it.task.status != TaskStatus.COMPLETED && it.task.status != TaskStatus.CANCELLED
          }
        val selectedTask = activeTasks.find { it.task.id == sel.selectedTaskId }
        val plannedMinutes = if (sel.isCustom) sel.customMinutes else sel.selectedPreset.workMinutes

        val (remainingSecs, progress) =
          if (activeSession == null) {
            (plannedMinutes * 60) to 0f
          } else {
            val totalMillis = activeSession.plannedMinutes * 60_000L
            val pausedExtra =
              if (activeSession.status == "PAUSED" && activeSession.pausedAt != null) {
                (now - activeSession.pausedAt).coerceAtLeast(0L)
              } else 0L
            val elapsed =
              (now - activeSession.startedAt - activeSession.pausedAccumulatedMillis - pausedExtra)
                .coerceAtLeast(0L)
            val left = (totalMillis - elapsed).coerceAtLeast(0L)
            val prog =
              if (totalMillis <= 0L) 1f else (elapsed.toFloat() / totalMillis.toFloat()).coerceIn(0f, 1f)
            (left / 1000L).toInt() to prog
          }

        FocusUiState(
          selectedPreset = sel.selectedPreset,
          plannedMinutes = activeSession?.plannedMinutes ?: plannedMinutes,
          isCustom = sel.isCustom,
          activeTasks = activeTasks,
          selectedTask = selectedTask,
          activeSession = activeSession,
          remainingSeconds = remainingSecs,
          progressFraction = progress,
          recentSessions = allSessions.filter { it.status == "COMPLETED" || it.status == "STOPPED" }.take(6),
        )
      }
      .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = FocusUiState(),
      )

  fun selectPreset(preset: FocusPreset) {
    selection.update { it.copy(selectedPreset = preset, isCustom = false) }
  }

  fun setCustomMinutes(minutes: Int) {
    selection.update { it.copy(customMinutes = minutes.coerceIn(5, 180), isCustom = true) }
  }

  fun selectTask(taskId: String?) {
    selection.update { it.copy(selectedTaskId = taskId) }
  }

  fun startSession() {
    val state = uiState.value
    if (state.activeSession != null) return
    val task = state.selectedTask
    val presetName = if (state.isCustom) "Custom" else state.selectedPreset.name
    viewModelScope.launch {
      focusRepository.startSession(
        taskId = task?.task?.id,
        taskTitle = task?.task?.title ?: "Unlinked Focus",
        presetName = presetName,
        plannedMinutes = state.plannedMinutes,
      )
    }
  }

  fun pauseSession() {
    val id = uiState.value.activeSession?.id ?: return
    viewModelScope.launch { focusRepository.pauseSession(id) }
  }

  fun resumeSession() {
    val id = uiState.value.activeSession?.id ?: return
    viewModelScope.launch { focusRepository.resumeSession(id) }
  }

  fun completeSession() {
    val id = uiState.value.activeSession?.id ?: return
    viewModelScope.launch { focusRepository.finishSession(id, completedNaturally = true) }
  }

  fun stopSession() {
    val id = uiState.value.activeSession?.id ?: return
    viewModelScope.launch { focusRepository.finishSession(id, completedNaturally = false) }
  }
}
