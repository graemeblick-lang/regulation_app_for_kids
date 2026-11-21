package com.calmcorner.app

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import java.util.Calendar

/** Preset daily check-in times a grown-up can switch on. */
enum class ReminderSlot(val label: String, val hour: Int, val minute: Int) {
    Morning("Before school (8:00)", 8, 0),
    AfterSchool("After school (15:30)", 15, 30),
    Bedtime("Bedtime (19:30)", 19, 30),
}

/** Gentle local reminders. No internet, no account: the phone's own alarm clock. */
object Reminders {
    private const val CHANNEL = "checkin"

    fun reschedule(context: Context, enabled: Set<String>) {
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        ReminderSlot.entries.forEach { slot ->
            val pi = pending(context, slot)
            am.cancel(pi)
            if (slot.name in enabled) {
                val next = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, slot.hour)
                    set(Calendar.MINUTE, slot.minute)
                    set(Calendar.SECOND, 0)
                    if (timeInMillis <= System.currentTimeMillis()) add(Calendar.DAY_OF_YEAR, 1)
                }
                // Inexact is fine (and kind to the battery): it may arrive a few minutes late.
                am.setInexactRepeating(AlarmManager.RTC_WAKEUP, next.timeInMillis, AlarmManager.INTERVAL_DAY, pi)
            }
        }
    }

    private fun pending(context: Context, slot: ReminderSlot): PendingIntent =
        PendingIntent.getBroadcast(
            context, slot.ordinal,
            Intent(context, ReminderReceiver::class.java).putExtra("slot", slot.name),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

    fun show(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = context.getSystemService(NotificationManager::class.java)
            nm.createNotificationChannel(NotificationChannel(CHANNEL, "Check-in reminders", NotificationManager.IMPORTANCE_DEFAULT))
        }
        val open = PendingIntent.getActivity(
            context, 100, Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val n = NotificationCompat.Builder(context, CHANNEL)
            .setSmallIcon(R.drawable.ic_stat_calm)
            .setContentTitle("Time for a check-in? 💛")
            .setContentText("How do you feel? Pick a colour.")
            .setContentIntent(open)
            .setAutoCancel(true)
            .build()
        try {
            NotificationManagerCompat.from(context).notify(1, n)
        } catch (e: SecurityException) {
            // Notifications not allowed: nothing to do.
        }
    }
}

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) = Reminders.show(context)
}

/** Alarms are cleared when the phone restarts, so set them again. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        val prefs = context.getSharedPreferences("calm_corner", Context.MODE_PRIVATE)
        Reminders.reschedule(context, prefs.getStringSet("reminders", emptySet())!!.toSet())
    }
}
