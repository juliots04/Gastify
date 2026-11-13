package com.example.gastify.notifications

import android.app.AlarmManager
import android.app.PendingIntent

import android.content.Context
import android.content.Intent

import android.net.Uri

import com.example.gastify.data.local.ReminderStore
import com.example.gastify.data.model.PassageReminder

import java.util.Calendar

object ReminderScheduler {

    const val ACTION_REMINDER =
        "com.example.gastify.ACTION_REMINDER"

    const val EXTRA_REMINDER_ID =
        "reminder_id"

    const val EXTRA_TRIGGER_TIME =
        "trigger_time"

    // ==========================================
    // PROGRAMAR RECORDATORIO
    // ==========================================

    fun schedule(
        context: Context,
        reminder: PassageReminder
    ) {

        cancel(context, reminder.id)

        if (!reminder.enabled) {
            return
        }

        val nextTime = calculateNextTime(reminder)
            ?: return

        val alarmManager = context.getSystemService(
            Context.ALARM_SERVICE
        ) as AlarmManager

        val pendingIntent = createPendingIntent(
            context,
            reminder.id,
            nextTime
        )

        // Alarma aproximada compatible con Android 7+.
        // No requiere el permiso de alarmas exactas.

        alarmManager.setAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            nextTime,
            pendingIntent
        )
    }

    // ==========================================
    // CANCELAR RECORDATORIO
    // ==========================================

    fun cancel(
        context: Context,
        reminderId: String
    ) {

        val intent = Intent(
            context,
            ReminderReceiver::class.java
        ).apply {

            action = ACTION_REMINDER

            data = Uri.parse(
                "gastify://reminder/$reminderId"
            )
        }

        val pendingIntent = PendingIntent.getBroadcast(

            context,

            0,

            intent,

            PendingIntent.FLAG_NO_CREATE or
                PendingIntent.FLAG_IMMUTABLE
        )

        if (pendingIntent != null) {

            val alarmManager = context.getSystemService(
                Context.ALARM_SERVICE
            ) as AlarmManager

            alarmManager.cancel(pendingIntent)

            pendingIntent.cancel()
        }
    }

    // ==========================================
    // REPROGRAMAR TODOS
    // ==========================================

    fun scheduleAll(
        context: Context
    ) {

        val store = ReminderStore(context)

        store.getAll().forEach { reminder ->

            schedule(context, reminder)
        }
    }

    // ==========================================
    // CALCULAR PRÓXIMA FECHA
    // ==========================================

    fun calculateNextTime(
        reminder: PassageReminder
    ): Long? {

        if (reminder.daysOfWeek.isEmpty()) {
            return null
        }

        val now = Calendar.getInstance()

        for (offset in 0..7) {

            val candidate = Calendar.getInstance().apply {

                add(Calendar.DAY_OF_YEAR, offset)

                set(Calendar.HOUR_OF_DAY, reminder.hour)

                set(Calendar.MINUTE, reminder.minute)

                set(Calendar.SECOND, 0)

                set(Calendar.MILLISECOND, 0)
            }

            // Calendar: domingo = 1, lunes = 2.
            // Gastify: lunes = 1, domingo = 7.

            val dayOfWeek = (
                (candidate.get(Calendar.DAY_OF_WEEK) + 5) % 7
            ) + 1

            if (
                dayOfWeek in reminder.daysOfWeek &&
                candidate.timeInMillis > now.timeInMillis
            ) {

                return candidate.timeInMillis
            }
        }

        return null
    }

    // ==========================================
    // CREAR PENDING INTENT
    // ==========================================

    private fun createPendingIntent(
        context: Context,
        reminderId: String,
        triggerTime: Long
    ): PendingIntent {

        val intent = Intent(
            context,
            ReminderReceiver::class.java
        ).apply {

            action = ACTION_REMINDER

            data = Uri.parse(
                "gastify://reminder/$reminderId"
            )

            putExtra(
                EXTRA_REMINDER_ID,
                reminderId
            )

            putExtra(
                EXTRA_TRIGGER_TIME,
                triggerTime
            )
        }

        return PendingIntent.getBroadcast(

            context,

            0,

            intent,

            PendingIntent.FLAG_UPDATE_CURRENT or
                PendingIntent.FLAG_IMMUTABLE
        )
    }
}
