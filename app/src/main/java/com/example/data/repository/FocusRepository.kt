package com.example.data.repository

import androidx.room.withTransaction
import com.example.data.local.CadenceDatabase
import com.example.data.local.dao.FocusSessionDao
import com.example.data.local.dao.TaskDao
import com.example.data.local.entity.FocusSessionEntity
import java.util.UUID
import kotlinx.coroutines.flow.Flow

class FocusRepository(
  private val db: CadenceDatabase,
  private val focusSessionDao: FocusSessionDao,
  private val taskDao: TaskDao,
) {
  fun observeAllSessions(): Flow<List<FocusSessionEntity>> = focusSessionDao.observeAllSessions()

  fun observeActiveSession(): Flow<FocusSessionEntity?> = focusSessionDao.observeActiveSession()

  suspend fun startSession(
    taskId: String?,
    taskTitle: String,
    presetName: String,
    plannedMinutes: Int,
  ): String {
    val now = System.currentTimeMillis()
    val id = UUID.randomUUID().toString()
    val session =
      FocusSessionEntity(
        id = id,
        taskId = taskId,
        taskTitleSnapshot = taskTitle,
        presetName = presetName,
        plannedMinutes = plannedMinutes,
        startedAt = now,
        status = "RUNNING",
      )
    focusSessionDao.upsertSession(session)
    return id
  }

  suspend fun pauseSession(sessionId: String) {
    val session = focusSessionDao.getSessionById(sessionId) ?: return
    if (session.status != "RUNNING") return
    focusSessionDao.upsertSession(
      session.copy(pausedAt = System.currentTimeMillis(), status = "PAUSED")
    )
  }

  suspend fun resumeSession(sessionId: String) {
    val session = focusSessionDao.getSessionById(sessionId) ?: return
    if (session.status != "PAUSED") return
    val pausedAt = session.pausedAt ?: System.currentTimeMillis()
    val addedPause = (System.currentTimeMillis() - pausedAt).coerceAtLeast(0L)
    focusSessionDao.upsertSession(
      session.copy(
        pausedAt = null,
        pausedAccumulatedMillis = session.pausedAccumulatedMillis + addedPause,
        status = "RUNNING",
      )
    )
  }

  suspend fun finishSession(sessionId: String, completedNaturally: Boolean) {
    val session = focusSessionDao.getSessionById(sessionId) ?: return
    val now = System.currentTimeMillis()
    val extraPause =
      if (session.status == "PAUSED" && session.pausedAt != null) {
        (now - session.pausedAt).coerceAtLeast(0L)
      } else 0L
    val elapsedMillis =
      (now - session.startedAt - session.pausedAccumulatedMillis - extraPause).coerceAtLeast(0L)
    val elapsedMinutes = (elapsedMillis / 60_000L).toInt().coerceAtLeast(if (completedNaturally) session.plannedMinutes else 0)

    db.withTransaction {
      focusSessionDao.upsertSession(
        session.copy(
          endedAt = now,
          pausedAt = null,
          pausedAccumulatedMillis = session.pausedAccumulatedMillis + extraPause,
          status = if (completedNaturally) "COMPLETED" else "STOPPED",
        )
      )
      if (session.taskId != null && elapsedMinutes > 0) {
        val task = taskDao.getTaskById(session.taskId)
        if (task != null) {
          taskDao.updateTask(
            task.copy(actualMinutes = task.actualMinutes + elapsedMinutes, updatedAt = now)
          )
        }
      }
    }
  }
}
