package com.example.receiver

import android.app.*
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.example.MainActivity
import java.util.Calendar

class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val title = intent.getStringExtra("title") ?: "Scheduled Task"
        val taskId = intent.getIntExtra("taskId", 0)

        Log.d("AlarmReceiver", "Alarm onReceive triggered for: $title with taskId: $taskId")
        showNotification(context, title, taskId)
    }

    companion object {
        private const val CHANNEL_ID = "task_channel"

        fun showNotification(context: Context, title: String, taskId: Int) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    "Task Reminders",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Notifications for reaching your task scheduled times"
                    enableLights(true)
                    enableVibration(true)
                }
                manager.createNotificationChannel(channel)
            }

            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            val pendingIntent = PendingIntent.getActivity(
                context,
                taskId,
                intent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )

            val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                Notification.Builder(context, CHANNEL_ID)
            } else {
                @Suppress("DEPRECATION")
                Notification.Builder(context)
            }

            val notification = builder
                .setContentTitle("To-Do Reminder! ⏰")
                .setContentText(title)
                .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .build()

            manager.notify(taskId, notification)
        }

        fun setAlarm(context: Context, timeInMillis: Long, title: String, taskId: Int) {
            // Check if alarm time is in the past; if so, push into the future (or ignore) to avoid immediate firing
            val currentMillis = System.currentTimeMillis()
            val finalTimeInMillis = if (timeInMillis <= currentMillis) {
                // If it was scheduled for today, and has passed, default it to trigger 1 minute from now
                currentMillis + 60_000
            } else {
                timeInMillis
            }

            Log.d("AlarmReceiver", "Scheduling system alarm for: $title at $finalTimeInMillis [ID:$taskId]")
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            
            val intent = Intent(context, AlarmReceiver::class.java).apply {
                putExtra("title", title)
                putExtra("taskId", taskId)
            }

            val pendingIntent = PendingIntent.getBroadcast(
                context,
                taskId,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    if (!alarmManager.canScheduleExactAlarms()) {
                        alarmManager.set(AlarmManager.RTC_WAKEUP, finalTimeInMillis, pendingIntent)
                    } else {
                        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, finalTimeInMillis, pendingIntent)
                    }
                } else {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, finalTimeInMillis, pendingIntent)
                }
            } catch (e: SecurityException) {
                Log.w("AlarmReceiver", "SecurityException during schedule exact alarm, falling back to set()", e)
                alarmManager.set(AlarmManager.RTC_WAKEUP, finalTimeInMillis, pendingIntent)
            } catch (e: Exception) {
                Log.e("AlarmReceiver", "Error setting exact alarm", e)
                alarmManager.set(AlarmManager.RTC_WAKEUP, finalTimeInMillis, pendingIntent)
            }
        }

        fun cancelAlarm(context: Context, taskId: Int) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val intent = Intent(context, AlarmReceiver::class.java)
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                taskId,
                intent,
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
            )
            if (pendingIntent != null) {
                alarmManager.cancel(pendingIntent)
                pendingIntent.cancel()
                Log.d("AlarmReceiver", "Successfully cancelled alarm for taskId: $taskId")
            }
        }
    }
}
