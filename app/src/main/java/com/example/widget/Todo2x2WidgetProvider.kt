package com.example.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.view.View
import android.widget.RemoteViews
import com.example.MainActivity
import com.example.R

class Todo2x2WidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        val prefs = context.getSharedPreferences("duet_prefs", Context.MODE_PRIVATE)
        val jsonStr = prefs.getString("cached_todos_json", null)
        val isEmpty = jsonStr.isNullOrEmpty() || jsonStr == "[]"

        for (appWidgetId in appWidgetIds) {
            val views = RemoteViews(context.packageName, R.layout.widget_todo_2x2)

            if (isEmpty) {
                views.setViewVisibility(R.id.lv_todos, View.GONE)
                views.setViewVisibility(R.id.tv_empty_todos, View.VISIBLE)
            } else {
                views.setViewVisibility(R.id.lv_todos, View.VISIBLE)
                views.setViewVisibility(R.id.tv_empty_todos, View.GONE)

                // Set up the RemoteViewsService adapter
                val serviceIntent = Intent(context, TodoWidgetService::class.java).apply {
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                    data = Uri.parse(toUri(Intent.URI_INTENT_SCHEME))
                }
                views.setRemoteAdapter(R.id.lv_todos, serviceIntent)
            }

            // Clicking any item in the ListView or the header launches MainActivity
            val mainIntent = Intent(context, MainActivity::class.java)
            val mainPendingIntent = PendingIntent.getActivity(
                context,
                2002,
                mainIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setPendingIntentTemplate(R.id.lv_todos, mainPendingIntent)

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        // Refresh todo data on widget updates or custom broadcasts
        val manager = AppWidgetManager.getInstance(context)
        val comp = ComponentName(context, Todo2x2WidgetProvider::class.java)
        val ids = manager.getAppWidgetIds(comp)
        manager.notifyAppWidgetViewDataChanged(ids, R.id.lv_todos)
    }

    companion object {
        fun triggerRefresh(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val comp = ComponentName(context, Todo2x2WidgetProvider::class.java)
            val ids = manager.getAppWidgetIds(comp)
            
            // Re-update layout states (e.g. Empty list vs list view visibility)
            val intent = Intent(context, Todo2x2WidgetProvider::class.java).apply {
                action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
            }
            context.sendBroadcast(intent)
            
            // Notify list view adapter
            manager.notifyAppWidgetViewDataChanged(ids, R.id.lv_todos)
        }
    }
}
