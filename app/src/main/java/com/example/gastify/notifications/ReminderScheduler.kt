package com.example.gastify.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build

import com.example.gastify.data.local.ReminderStore
import com.example.gastify.data.model.PassageReminder

import java.util.Calendar

object ReminderScheduler {

    const val ACTION_REMINDER =
        "com.example.gastify.ACTION_REMINDER"

    const val ACTION_SNOOZE =
        "com.example.gastify.ACTION_REMINDER_SNOOZE"

    const val EXTRA_REMINDER_ID = "reminder_id"

    const val EXTRA_TRIGGER_TIME = "trigger_time"

    const val SNOOZE_MINUTES = 10

    private const val SESSION_PREFERENCES =
        "gastify_reminder_session"

    private const val ACTIVE_USER_KEY =
        "active_user_id"

    private fun manager(
        context: Context
    ): AlarmManager {

        return context.getSystemService(
            Context.ALARM_SERVICE
        ) as AlarmManager
    }

    // ==========================================
    // USUARIO ACTIVO
    // ==========================================

    fun activeUserId(
        context: Context
    ): String? {

        return context.getSharedPreferences(
            SESSION_PREFERENCES,
            Context.MODE_PRIVATE
        ).getString(
            ACTIVE_USER_KEY,
            null
        )
    }

    fun activateUser(
        context: Context,
        userId: String
    ) {

        val previousUser = activeUserId(context)

        if (
            previousUser != null &&
            previousUser != userId
        ) {

            ReminderStore(context)
                .getAll()
                .filter {
                    it.userId == previousUser
                }
                .forEach {
                    cancel(context, it.id)
                }
        }

        context.getSharedPreferences(
            SESSION_PREFERENCES,
            Context.MODE_PRIVATE
        ).edit()
            .putString(ACTIVE_USER_KEY, userId)
            .commit()
    }

    fun deactivateUser(
        context: Context
    ) {

        val userId = activeUserId(context)

        if (userId != null) {

            ReminderStore(context)
                .getAll()
                .filter {
                    it.userId == userId
                }
                .forEach {
                    cancel(context, it.id)
                }
        }

        context.getSharedPreferences(
            SESSION_PREFERENCES,
            Context.MODE_PRIVATE
        ).edit()
            .remove(ACTIVE_USER_KEY)
            .commit()
    }

    // ==========================================
    // COMPROBAR ALARMAS EXACTAS
    // ==========================================

    fun canScheduleExact(
        context: Context
    ): Boolean {

        return if (
            Build.VERSION.SDK_INT >=
            Build.VERSION_CODES.S
        ) {

            manager(context).canScheduleExactAlarms()

        } else {

            true
        }
    }

    // ==========================================
    // PROGRAMAR ALARMA
    // ==========================================

    private fun scheduleAlarm(
        context: Context,
        triggerTime: Long,
        pendingIntent: PendingIntent
    ) {

        val alarmManager = manager(context)

        if (canScheduleExact(context)) {

            try {

                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerTime,
                    pendingIntent
                )

                return

            } catch (_: SecurityException) {

                // El permiso pudo haber sido revocado.
            }
        }

        // Alternativa aproximada.

        alarmManager.setAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            triggerTime,
            pendingIntent
        )
    }

    // ==========================================
    // PROGRAMAR RECORDATORIO NORMAL
    // ==========================================

    fun schedule(
        context: Context,
        reminder: PassageReminder
    ) {

        cancel(context, reminder.id)

        if (!reminder.enabled) {
            return
        }

        if (
            reminder.userId != activeUserId(context)
        ) {
            return
        }

        val nextTime = calculateNextTime(reminder)
            ?: return

        val intent = Intent(
            context,
            ReminderReceiver::class.java
        ).apply {

            action = ACTION_REMINDER

            data = Uri.parse(
                "gastify://reminder/regular/${reminder.id}"
            )

            putExtra(
                EXTRA_REMINDER_ID,
                reminder.id
            )

            putExtra(
                EXTRA_TRIGGER_TIME,
                nextTime
            )
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or
                PendingIntent.FLAG_IMMUTABLE
        )

        scheduleAlarm(
            context,
            nextTime,
            pendingIntent
        )
    }

    // ==========================================
    // APLAZAR 10 MINUTOS
    // ==========================================

    fun snooze(
        context: Context,
        reminderId: String,
        originalTriggerTime: Long
    ) {

        require(originalTriggerTime > 0L) {
            "Fecha del recordatorio inválida."
        }

        val reminder = ReminderStore(context)
            .getById(reminderId)
            ?: throw IllegalStateException(
                "El recordatorio ya no existe."
            )

        require(reminder.enabled) {
            "El recordatorio está desactivado."
        }

        require(
            reminder.userId == activeUserId(context)
        ) {
            "El usuario no corresponde."
        }

        val nextTime = System.currentTimeMillis() +
            SNOOZE_MINUTES * 60_000L

        val intent = Intent(
            context,
            ReminderReceiver::class.java
        ).apply {

            action = ACTION_SNOOZE

            data = Uri.parse(
                "gastify://reminder/snooze/" +
                    "$reminderId/$originalTriggerTime"
            )

            putExtra(
                EXTRA_REMINDER_ID,
                reminderId
            )

            // Conservamos el evento original para
            // impedir descuentos duplicados.
            putExtra(
                EXTRA_TRIGGER_TIME,
                originalTriggerTime
            )
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or
                PendingIntent.FLAG_IMMUTABLE
        )

        scheduleAlarm(
            context,
            nextTime,
            pendingIntent
        )
    }

    // ==========================================
    // CANCELAR RECORDATORIO NORMAL
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
                "gastify://reminder/regular/$reminderId"
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

            manager(context).cancel(pendingIntent)

            pendingIntent.cancel()
        }
    }

    // ==========================================
    // REPROGRAMAR TODOS
    // ==========================================

    fun scheduleAll(
        context: Context
    ) {

        val userId = activeUserId(context)
            ?: return

        ReminderStore(context)
            .getAll()
            .filter {
                it.userId == userId
            }
            .forEach { reminder ->

                schedule(context, reminder)
            }
    }

    // ==========================================
    // CALCULAR PRÓXIMO HORARIO
    // ==========================================

    fun calculateNextTime(
        reminder: PassageReminder
    ): Long? {

        if (
            reminder.daysOfWeek.isEmpty() ||
            reminder.hour !in 0..23 ||
            reminder.minute !in 0..59
        ) {
            return null
        }

        val now = System.currentTimeMillis()

        for (offset in 0..7) {

            val candidate = Calendar.getInstance().apply {

                add(
                    Calendar.DAY_OF_YEAR,
                    offset
                )

                set(
                    Calendar.HOUR_OF_DAY,
                    reminder.hour
                )

                set(
                    Calendar.MINUTE,
                    reminder.minute
                )

                set(Calendar.SECOND, 0)

                set(Calendar.MILLISECOND, 0)
            }

            // Android: domingo = 1.
            // GASTIFY: lunes = 1.

            val day = (
                candidate.get(Calendar.DAY_OF_WEEK) + 5
            ) % 7 + 1

            if (
                day in reminder.daysOfWeek &&
                candidate.timeInMillis > now
            ) {

                return candidate.timeInMillis
            }
        }

        return null
    }
}
