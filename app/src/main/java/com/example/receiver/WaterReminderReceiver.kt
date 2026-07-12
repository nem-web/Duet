package com.example.receiver

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import java.time.LocalDate
import java.util.Calendar

class WaterReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        Log.d("WaterReminderReceiver", "Alarm received! Posting notification.")
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "water_reminders"
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Water Reminders",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Reminders to drink water"
            }
            notificationManager.createNotificationChannel(channel)
        }
        
        val mainIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            mainIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val prefs = context.getSharedPreferences("duet_prefs", Context.MODE_PRIVATE)
        val stateDate = prefs.getString("water_state_date", "")
        val todayStr = LocalDate.now().toString()
        val currentIntake = if (stateDate == todayStr) prefs.getInt("water_intake_ml", 0) else 0
        val targetGoal = prefs.getInt("water_target_ml", 2000)

        val remainingMl = (targetGoal - currentIntake).coerceAtLeast(0)
        
        // Dynamic notification text reflecting the actual target progress
        val notificationText = if (remainingMl <= 0) {
            "You have achieved your hydration goal of $targetGoal ml today! Outstanding work! 🎉"
        } else {
            "You have $remainingMl ml remaining to reach your goal of $targetGoal ml. Take a sip now! 💧"
        }

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Duet Hydration Alert! 💧")
            .setContentText(notificationText)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()
            
        notificationManager.notify(1001, notification)

        // Automatically reschedule next smart/dynamic water reminder
        try {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val nextIntent = Intent(context, WaterReminderReceiver::class.java)
            val nextPendingIntent = PendingIntent.getBroadcast(
                context,
                2002,
                nextIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val calendar = Calendar.getInstance()
            val currentHour = calendar.get(Calendar.HOUR_OF_DAY)
            val currentMinute = calendar.get(Calendar.MINUTE)

            val startHour = prefs.getInt("water_start_hour", 8)
            val endHour = prefs.getInt("water_end_hour", 22)

            val isSleepTime = currentHour >= endHour || currentHour < startHour

            val triggerAtMillis: Long
            val debugMsg: String

            if (isSleepTime) {
                // Sleep window: Reschedule for the startHour of awake tomorrow (or today if early morning)
                val nextAlarmCalendar = Calendar.getInstance().apply {
                    if (get(Calendar.HOUR_OF_DAY) >= endHour) {
                        add(Calendar.DAY_OF_YEAR, 1)
                    }
                    set(Calendar.HOUR_OF_DAY, startHour)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                triggerAtMillis = nextAlarmCalendar.timeInMillis
                debugMsg = "Sleep time detected. Rescheduled next reminder for wake time tomorrow: ${nextAlarmCalendar.time}"
            } else {
                if (remainingMl <= 0) {
                    // Target completed! Remind again in a generic 4 hours if still during awake hours
                    val intervalMs = 4 * 60 * 60 * 1000L
                    triggerAtMillis = System.currentTimeMillis() + intervalMs
                    debugMsg = "Goal already met today! Next checklist scheduled in 4 hours."
                } else {
                    // Compute smart interval based on target progress and hours left until sleep
                    val portionsNeeded = remainingMl / 250.0 // assuming a 250ml cup size
                    val hoursRemaining = (endHour - (currentHour + currentMinute / 60.0)).coerceAtLeast(1.0)
                    
                    val intervalHours = if (portionsNeeded > 0) {
                        hoursRemaining / portionsNeeded
                    } else {
                        2.0
                    }
                    
                    // Limit reminder interval between 30 minutes and 4 hours to avoid spamming
                    val finalIntervalHours = intervalHours.coerceIn(0.5, 4.0)
                    val intervalMs = (finalIntervalHours * 60 * 60 * 1000L).toLong()
                    triggerAtMillis = System.currentTimeMillis() + intervalMs
                    debugMsg = "Smart interval calculated: $finalIntervalHours hours ($remainingMl ml remaining in $hoursRemaining awake hours)."
                }
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerAtMillis, nextPendingIntent)
                } else {
                    alarmManager.set(AlarmManager.RTC_WAKEUP, triggerAtMillis, nextPendingIntent)
                }
            } else {
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerAtMillis, nextPendingIntent)
            }
            Log.d("WaterReminderReceiver", "Rescheduled alarm successfully. $debugMsg")
        } catch (e: Exception) {
            Log.e("WaterReminderReceiver", "Failed to reschedule water reminder: ${e.message}")
        }
    }
}
