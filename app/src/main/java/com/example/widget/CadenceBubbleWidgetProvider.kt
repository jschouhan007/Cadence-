package com.example.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.RemoteViews
import com.example.CadenceApp
import com.example.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

class CadenceBubbleWidgetProvider : AppWidgetProvider() {

  override fun onUpdate(
    context: Context,
    appWidgetManager: AppWidgetManager,
    appWidgetIds: IntArray,
  ) {
    updateAsync(context, appWidgetManager, appWidgetIds)
  }

  override fun onAppWidgetOptionsChanged(
    context: Context,
    appWidgetManager: AppWidgetManager,
    appWidgetId: Int,
    newOptions: Bundle,
  ) {
    super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions)
    updateAsync(context, appWidgetManager, intArrayOf(appWidgetId))
  }

  private fun updateAsync(
    context: Context,
    appWidgetManager: AppWidgetManager,
    appWidgetIds: IntArray,
  ) {
    val pendingResult: PendingResult? = goAsync()
    CoroutineScope(Dispatchers.IO).launch {
      try {
        updateWidgetsNow(context, appWidgetManager, appWidgetIds)
      } finally {
        pendingResult?.finish()
      }
    }
  }

  companion object {
    private val updateMutex = Mutex()

    fun notifyDataChanged(context: Context) {
      val appContext = context.applicationContext
      val manager = AppWidgetManager.getInstance(appContext) ?: return
      val component = ComponentName(appContext, CadenceBubbleWidgetProvider::class.java)
      val ids = manager.getAppWidgetIds(component)
      if (ids == null || ids.isEmpty()) return
      CoroutineScope(Dispatchers.IO).launch {
        updateWidgetsNow(appContext, manager, ids)
      }
    }

    internal suspend fun updateWidgetsNow(
      context: Context,
      appWidgetManager: AppWidgetManager,
      appWidgetIds: IntArray,
    ) = updateMutex.withLock {
      val app = context.applicationContext as? CadenceApp ?: return@withLock
      val pendingTasks = app.container.database.taskDao().getPendingTasksWithDetails()
      val pendingCount = pendingTasks.size

      for (appWidgetId in appWidgetIds) {
        val options = appWidgetManager.getAppWidgetOptions(appWidgetId)
        val minWidth = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 56)
        val minHeight = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 56)
        val useExpandedLayout = minWidth >= 180 && minHeight >= 130

        val openSnippetIntent =
          Intent(context, TaskSnippetBubbleActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
          }
        val openSnippetPendingIntent =
          PendingIntent.getActivity(
            context,
            appWidgetId,
            openSnippetIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
          )

        val views =
          if (useExpandedLayout) {
            RemoteViews(context.packageName, R.layout.widget_bubble_expanded).apply {
              setTextViewText(R.id.widget_header_count, pendingCount.toString())
              setOnClickPendingIntent(R.id.widget_header, openSnippetPendingIntent)
              setOnClickPendingIntent(R.id.widget_empty_view, openSnippetPendingIntent)

              val serviceIntent =
                Intent(context, CadenceWidgetService::class.java).apply {
                  putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                  data = Uri.parse(toUri(Intent.URI_INTENT_SCHEME))
                }
              setRemoteAdapter(R.id.widget_task_list, serviceIntent)
              setEmptyView(R.id.widget_task_list, R.id.widget_empty_view)

              val itemTemplateIntent =
                Intent(context, TaskSnippetBubbleActivity::class.java).apply {
                  flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                }
              val itemPendingIntent =
                PendingIntent.getActivity(
                  context,
                  appWidgetId + 50_000,
                  itemTemplateIntent,
                  PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE,
                )
              setPendingIntentTemplate(R.id.widget_task_list, itemPendingIntent)
            }
          } else {
            RemoteViews(context.packageName, R.layout.widget_bubble_compact).apply {
              val badgeText = if (pendingCount > 99) "99+" else pendingCount.toString()
              setTextViewText(R.id.widget_bubble_badge, badgeText)
              setViewVisibility(
                R.id.widget_bubble_badge,
                if (pendingCount > 0) View.VISIBLE else View.GONE,
              )
              setOnClickPendingIntent(R.id.widget_bubble_root, openSnippetPendingIntent)
            }
          }

        withContext(Dispatchers.Main.immediate) {
          appWidgetManager.updateAppWidget(appWidgetId, views)
          if (useExpandedLayout) {
            appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetId, R.id.widget_task_list)
          }
        }
      }
    }
  }
}
