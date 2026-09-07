package com.countup.app

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import java.time.ZonedDateTime

/**
 * Receiver waking up once per day at 00:00:01 local time to silently
 * refresh all placed home-screen widgets and advance day counts without battery drain.
 */
class MidnightAlarmReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_MIDNIGHT_ROLLOVER = "com.countup.app.ACTION_MIDNIGHT_ROLLOVER"
        const val REQUEST_CODE_MIDNIGHT = 24001

        /**
         * Calculates the epoch millisecond timestamp for the nearest upcoming 00:00:01 local time.
         */
        fun calculateNextMidnightMillis(now: ZonedDateTime): Long {
            val todayTarget = now.toLocalDate().atTime(0, 0, 1).atZone(now.zone)
            return if (now.isBefore(todayTarget)) {
                todayTarget.toInstant().toEpochMilli()
            } else {
                now.toLocalDate().plusDays(1).atTime(0, 0, 1).atZone(now.zone).toInstant().toEpochMilli()
            }
        }

        /**
         * Schedules the daily midnight rollover alarm via [AlarmManager].
         * Idempotent and battery-friendly.
         */
        @SuppressLint("MissingPermission")
        fun scheduleMidnightAlarm(context: Context) {
            try {
                val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
                val intent = Intent(context, MidnightAlarmReceiver::class.java).apply {
                    action = ACTION_MIDNIGHT_ROLLOVER
                }
                val pendingIntent = PendingIntent.getBroadcast(
                    context,
                    REQUEST_CODE_MIDNIGHT,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                )

                val targetMillis = calculateNextMidnightMillis(ZonedDateTime.now())

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    if (alarmManager.canScheduleExactAlarms()) {
                        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC, targetMillis, pendingIntent)
                    } else {
                        alarmManager.setAndAllowWhileIdle(AlarmManager.RTC, targetMillis, pendingIntent)
                    }
                } else {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC, targetMillis, pendingIntent)
                }
            } catch (_: Throwable) {
                // Defensive fallback: must never crash app
            }
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        if (action != ACTION_MIDNIGHT_ROLLOVER &&
            action != Intent.ACTION_TIME_CHANGED &&
            action != Intent.ACTION_TIMEZONE_CHANGED
        ) {
            return
        }

        val appContext = context.applicationContext
        launchAsync {
            // Push update pass across all active widget varieties
            pushWidgetUpdate(appContext)
            pushAllHeroWidgetsUpdate(appContext)
            pushAllZenHorizonWidgetsUpdate(appContext)
            pushAllSolarRhythmWidgetsUpdate(appContext)
            pushAllZenPebbleWidgetsUpdate(appContext)

            // Re-arm next midnight rollover
            scheduleMidnightAlarm(appContext)
        }
    }
}
