package com.example.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.example.R

class Water2x2WidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        val prefs = context.getSharedPreferences("duet_prefs", Context.MODE_PRIVATE)
        val todayStr = java.time.LocalDate.now().toString()
        val stateDate = prefs.getString("water_state_date", "")
        
        val currentIntake = if (stateDate == todayStr) prefs.getInt("water_intake_ml", 0) else 0
        val targetGoal = prefs.getInt("water_target_ml", 2000)

        for (appWidgetId in appWidgetIds) {
            val views = RemoteViews(context.packageName, R.layout.widget_water_2x2)

            // Set progress text
            views.setTextViewText(R.id.tv_water_progress, "$currentIntake / $targetGoal ml")

            // Setup clicks for each button
            setupButton(context, views, R.id.btn_add_50, 50, appWidgetId)
            setupButton(context, views, R.id.btn_add_100, 100, appWidgetId)
            setupButton(context, views, R.id.btn_add_250, 250, appWidgetId)
            setupButton(context, views, R.id.btn_add_500, 500, appWidgetId)

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }

    private fun setupButton(context: Context, views: RemoteViews, viewId: Int, amount: Int, widgetId: Int) {
        val intent = Intent(context, Water1x1WidgetProvider::class.java).apply {
            action = Water1x1WidgetProvider.ACTION_ADD_WATER
            putExtra(Water1x1WidgetProvider.EXTRA_AMOUNT_ML, amount)
        }
        
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            viewId + widgetId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(viewId, pendingIntent)
    }
}
