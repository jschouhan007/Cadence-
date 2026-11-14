package com.example.domain.sync

import kotlinx.serialization.Serializable

enum class SyncState {
  OFFLINE_LOCAL_ONLY,
  READY,
  SYNCING,
  ERROR,
}

interface CloudSyncGateway {
  fun currentSyncState(): SyncState

  suspend fun syncNow(): Result<Unit>
}

class LocalOnlySyncGateway : CloudSyncGateway {
  override fun currentSyncState(): SyncState = SyncState.OFFLINE_LOCAL_ONLY

  override suspend fun syncNow(): Result<Unit> = Result.success(Unit)
}

@Serializable
data class CadenceBackupPayload(
  val schemaVersion: Int = 1,
  val exportedAtMillis: Long,
  val categoryCount: Int,
  val taskCount: Int,
  val focusSessionCount: Int,
)
