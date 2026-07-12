package com.example.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.RemoteViews
import com.example.R
import com.example.repository.DuetRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class Water1x1WidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (appWidgetId in appWidgetIds) {
            val views = RemoteViews(context.packageName, R.layout.widget_water_1x1)

            // Setup click PendingIntent to add 100ml
            val intent = Intent(context, Water1x1WidgetProvider::class.java).apply {
                action = ACTION_ADD_WATER
                putExtra(EXTRA_AMOUNT_ML, 100)
            }
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                1001,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_1x1_container, pendingIntent)

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == ACTION_ADD_WATER) {
            val amount = intent.getIntExtra(EXTRA_AMOUNT_ML, 100)
            Log.d("Water1x1Widget", "Received click to add $amount ml water")
            
            val goAsync = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val repo = DuetRepository(context.applicationContext)
                    repo.updateWaterIntake(amount)
                    Log.d("Water1x1Widget", "Successfully logged $amount ml water via background widget receiver")
                    
                    // Force refresh both water widgets
                    refreshAllWaterWidgets(context)
                } catch (e: Exception) {
                    Log.e("Water1x1Widget", "Error updating water intake: ${e.message}", e)
                } finally {
                    goAsync.finish()
                }
            }
        }
    }

    companion object {
        const val ACTION_ADD_WATER = "com.example.action.ADD_WATER"
        const val EXTRA_AMOUNT_ML = "extra_amount_ml"

        fun refreshAllWaterWidgets(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            
            // 1x1 Widget update
            val comp1x1 = ComponentName(context, Water1x1WidgetProvider::class.java)
            val ids1x1 = manager.getAppWidgetIds(comp1x1)
            val intent1x1 = Intent(context, Water1x1WidgetProvider::class.java).apply {
                action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids1x1)
            }
            context.sendBroadcast(intent1x1)

            // 2x2 Widget update
            val comp2x2 = ComponentName(context, Water2x2WidgetProvider::class.java)
            val ids2x2 = manager.getAppWidgetIds(comp2x2)
            val intent2x2 = Intent(context, Water2x2WidgetProvider::class.java).apply {
                action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids2x2)
            }
            context.sendBroadcast(intent2x2)
        }
    }
}
