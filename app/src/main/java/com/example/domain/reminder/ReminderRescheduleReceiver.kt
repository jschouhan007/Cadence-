package com.example.domain.reminder

import android.app.AlarmManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.CadenceApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class ReminderRescheduleReceiver : BroadcastReceiver() {
  override fun onReceive(context: Context, intent: Intent) {
    val action = intent.action ?: return
    if (
      action != Intent.ACTION_BOOT_COMPLETED &&
        action != Intent.ACTION_TIME_CHANGED &&
        action != Intent.ACTION_TIMEZONE_CHANGED &&
        action != Intent.ACTION_MY_PACKAGE_REPLACED &&
        action != AlarmManager.ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED
    ) {
      return
    }

    val container = (context.applicationContext as? CadenceApp)?.container ?: return
    val wakeLock = ReminderWakeLock.acquire(context, "Cadence:ReminderRescheduleWakeLock", 20_000L)
    val pendingResult = goAsync()
    CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
      try {
        container.reminderScheduler.reconcileMissedOnSystemEvent()
      } finally {
        ReminderWakeLock.release(wakeLock)
        pendingResult?.finish()
      }
    }
  }
}
