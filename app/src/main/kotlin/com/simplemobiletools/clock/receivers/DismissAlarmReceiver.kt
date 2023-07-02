package com.simplemobiletools.clock.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.simplemobiletools.clock.extensions.*
import com.simplemobiletools.clock.helpers.ALARM_ID
import com.simplemobiletools.clock.helpers.NOTIFICATION_ID
import com.simplemobiletools.clock.models.Alarm
import com.simplemobiletools.commons.helpers.ensureBackgroundThread
import java.util.*

class DismissAlarmReceiver: BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val alarmId = intent.getIntExtra(ALARM_ID, -1)
        val notificationId = intent.getIntExtra(NOTIFICATION_ID, -1)
        if (alarmId == -1) return

        context.hideNotification(notificationId)

        ensureBackgroundThread {
            context.dbHelper.getAlarmWithId(alarmId)?.let { alarm ->
                context.cancelAlarmClock(alarm)
                scheduleNextAlarm(alarm, context)
                if (alarm.days < 0) {
                    context.dbHelper.updateAlarmEnabledState(alarm.id, false)
                    context.updateWidgets()
                }
            }
        }

    }

    private fun scheduleNextAlarm(alarm: Alarm, context: Context) {
        val oldBitmask = alarm.days
        alarm.days = removeTodayFromBitmask(oldBitmask)
        context.scheduleNextAlarm(alarm, true)
        alarm.days = oldBitmask
    }

    private fun removeTodayFromBitmask(bitmask: Int): Int {
        val calendar = Calendar.getInstance()
        var dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK) - Calendar.MONDAY  // This will give values from 0 (Monday) to 6 (Sunday)

        if (dayOfWeek < 0) {  // Adjust for Calendar.MONDAY being 2
            dayOfWeek += 7
        }

        val todayBitmask = 1 shl dayOfWeek  // This will shift the number 1 to the left by dayOfWeek places, creating a bitmask for today
        return bitmask and todayBitmask.inv()  // This will return a new bitmask without today included
    }
}
