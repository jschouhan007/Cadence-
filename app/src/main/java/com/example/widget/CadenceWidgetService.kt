package com.example.widget

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Binder
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import com.example.CadenceApp
import com.example.R
import com.example.data.local.entity.TaskPriority
import com.example.data.local.entity.TaskWithDetails
import com.example.domain.reminder.LocalReminderScheduler
import com.example.domain.time.DateTimeUtils
import kotlinx.coroutines.runBlocking

class CadenceWidgetService : RemoteViewsService() {
  override fun onGetViewFactory(intent: Intent): RemoteViewsFactory {
    return CadenceWidgetViewsFactory(applicationContext)
  }
}

private class CadenceWidgetViewsFactory(
  private val context: Context,
) : RemoteViewsService.RemoteViewsFactory {

  private var items: List<TaskWithDetails> = emptyList()

  override fun onCreate() {}

  override fun onDataSetChanged() {
    val identityToken = Binder.clearCallingIdentity()
    try {
      val app = context.applicationContext as? CadenceApp ?: return
      items = runBlocking { app.container.database.taskDao().getPendingTasksWithDetails() }
    } finally {
      Binder.restoreCallingIdentity(identityToken)
    }
  }

  override fun onDestroy() {
    items = emptyList()
  }

  override fun getCount(): Int = items.size

  override fun getViewAt(position: Int): RemoteViews {
    val views = RemoteViews(context.packageName, R.layout.widget_task_item)
    val item = items.getOrNull(position) ?: return views
    val task = item.task

    views.setTextViewText(R.id.widget_task_title, task.title)

    val dueLabel = DateTimeUtils.formatDueLabel(task.dueAt, task.isAllDay)
    val activeAlert = item.reminders.filter { it.isEnabled && !it.isDismissed }.minByOrNull { it.triggerAt }
    val parts = buildList {
      item.category?.name?.let { add(it) }
      dueLabel?.let { add(it) }
      activeAlert?.let { add("Alert ${DateTimeUtils.formatTime(it.triggerAt)}") }
    }
    views.setTextViewText(
      R.id.widget_task_meta,
      if (parts.isEmpty()) "Inbox · ${task.priority.label}" else parts.joinToString(" · "),
    )

    val dotColor =
      when (task.priority) {
        TaskPriority.URGENT -> Color.parseColor("#E03E36")
        TaskPriority.HIGH -> Color.parseColor("#FF5D47")
        TaskPriority.MEDIUM -> Color.parseColor("#2357E8")
        TaskPriority.LOW -> Color.parseColor("#10A37F")
      }
    views.setTextColor(R.id.widget_task_priority_badge, dotColor)

    val fillInIntent =
      Intent().apply {
        putExtra(LocalReminderScheduler.EXTRA_TASK_ID, task.id)
      }
    views.setOnClickFillInIntent(R.id.widget_task_row_root, fillInIntent)
    return views
  }

  override fun getLoadingView(): RemoteViews? = null

  override fun getViewTypeCount(): Int = 1

  override fun getItemId(position: Int): Long {
    return items.getOrNull(position)?.task?.id?.hashCode()?.toLong() ?: position.toLong()
  }

  override fun hasStableIds(): Boolean = true
}
