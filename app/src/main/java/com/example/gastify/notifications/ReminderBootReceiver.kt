package com.example.gastify.notifications

import android.app.AlarmManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class ReminderBootReceiver : BroadcastReceiver() {

    override fun onReceive(
        context: Context,
        intent: Intent
    ) {

        when (intent.action) {

            Intent.ACTION_BOOT_COMPLETED,

            Intent.ACTION_TIME_CHANGED,

            Intent.ACTION_TIMEZONE_CHANGED,

            Intent.ACTION_MY_PACKAGE_REPLACED -> {

                ReminderScheduler.scheduleAll(context)
            }

            AlarmManager
                .ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED -> {

                if (
                    ReminderScheduler.canScheduleExact(
                        context
                    )
                ) {

                    ReminderScheduler.scheduleAll(
                        context
                    )
                }
            }
        }
    }
}
