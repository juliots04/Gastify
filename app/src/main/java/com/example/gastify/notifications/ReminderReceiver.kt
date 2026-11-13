package com.example.gastify.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build

import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat

import com.example.gastify.MainActivity
import com.example.gastify.R
import com.example.gastify.data.local.ReminderStore
import com.example.gastify.data.model.PassageReminder

import java.math.BigDecimal

class ReminderReceiver : BroadcastReceiver() {

    companion object {

        const val CHANNEL_ID =
            "gastify_passage_reminders"

        private const val CHANNEL_NAME =
            "Recordatorios de pasajes"
    }

    override fun onReceive(
        context: Context,
        intent: Intent
    ) {

        val isRegular =
            intent.action ==
                ReminderScheduler.ACTION_REMINDER

        val isSnooze =
            intent.action ==
                ReminderScheduler.ACTION_SNOOZE

        if (!isRegular && !isSnooze) {
            return
        }

        val reminderId = intent.getStringExtra(
            ReminderScheduler.EXTRA_REMINDER_ID
        ) ?: return

        val triggerTime = intent.getLongExtra(
            ReminderScheduler.EXTRA_TRIGGER_TIME,
            0L
        )

        if (triggerTime <= 0L) {
            return
        }

        val reminder = ReminderStore(context)
            .getById(reminderId)
            ?: return

        if (!reminder.enabled) {
            return
        }

        // Validar la sesión guardada localmente.
        // No dependemos de cargar FirebaseAuth
        // para mostrar el aviso en segundo plano.

        if (
            reminder.userId !=
            ReminderScheduler.activeUserId(context)
        ) {
            return
        }

        // Programar el siguiente aviso normal.
        // Un aviso aplazado NO genera otra repetición.

        if (isRegular) {

            ReminderScheduler.schedule(
                context,
                reminder
            )
        }

        // Evitar volver a mostrar un evento
        // que ya fue confirmado.

        val eventId =
            "${reminder.id}_${triggerTime}"

        val responses = context.getSharedPreferences(
            "gastify_reminder_responses",
            Context.MODE_PRIVATE
        )

        if (
            responses.contains(
                "response_$eventId"
            )
        ) {
            return
        }

        if (
            Build.VERSION.SDK_INT >=
            Build.VERSION_CODES.TIRAMISU
        ) {

            val permission = ContextCompat
                .checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                )

            if (
                permission !=
                PackageManager.PERMISSION_GRANTED
            ) {
                return
            }
        }

        if (
            !NotificationManagerCompat
                .from(context)
                .areNotificationsEnabled()
        ) {
            return
        }

        createNotificationChannel(context)

        showNotification(
            context,
            reminder,
            triggerTime,
            isSnooze
        )
    }

    // ==========================================
    // CANAL DE NOTIFICACIONES
    // ==========================================

    private fun createNotificationChannel(
        context: Context
    ) {

        if (
            Build.VERSION.SDK_INT <
            Build.VERSION_CODES.O
        ) {
            return
        }

        val channel = NotificationChannel(
            CHANNEL_ID,
            CHANNEL_NAME,
            NotificationManager.IMPORTANCE_HIGH
        ).apply {

            description =
                "Confirmación de pagos de transporte."

            enableVibration(true)
        }

        val manager = context.getSystemService(
            NotificationManager::class.java
        )

        manager.createNotificationChannel(channel)
    }

    // ==========================================
    // MOSTRAR NOTIFICACIÓN
    // ==========================================

    private fun showNotification(
        context: Context,
        reminder: PassageReminder,
        triggerTime: Long,
        isSnooze: Boolean
    ) {

        val notificationId = (
            reminder.id + triggerTime
        ).hashCode()

        val amount = BigDecimal.valueOf(
            reminder.amountCents,
            2
        ).toPlainString()

        val openIntent = Intent(
            context,
            MainActivity::class.java
        ).apply {

            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                Intent.FLAG_ACTIVITY_CLEAR_TOP or
                Intent.FLAG_ACTIVITY_SINGLE_TOP
        }

        val openPendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or
                PendingIntent.FLAG_IMMUTABLE
        )

        val paidIntent = createActionIntent(
            context,
            reminder,
            triggerTime,
            ReminderActionReceiver.ACTION_PAID
        )

        val laterIntent = createActionIntent(
            context,
            reminder,
            triggerTime,
            ReminderActionReceiver.ACTION_LATER
        )

        val notification = NotificationCompat.Builder(
            context,
            CHANNEL_ID
        )
            .setSmallIcon(
                R.drawable.ic_notification_bus
            )
            .setContentTitle(
                if (isSnooze) {
                    "Recordatorio de tu pasaje"
                } else {
                    "¿Ya pagaste tu pasaje?"
                }
            )
            .setContentText(
                "${reminder.title} • S/ $amount"
            )
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText(
                        "${reminder.title}\n" +
                            "Importe: S/ $amount\n" +
                            "Confirma el pago o aplaza el aviso."
                    )
            )
            .setPriority(
                NotificationCompat.PRIORITY_HIGH
            )
            .setCategory(
                NotificationCompat.CATEGORY_REMINDER
            )
            .setContentIntent(openPendingIntent)
            .addAction(
                R.drawable.ic_notification_bus,
                "Sí, pagué",
                paidIntent
            )
            .addAction(
                R.drawable.ic_notification_bus,
                "Más tarde (10 min)",
                laterIntent
            )
            .setAutoCancel(true)
            .build()

        try {

            NotificationManagerCompat
                .from(context)
                .notify(
                    notificationId,
                    notification
                )

        } catch (_: SecurityException) {

            // Permiso de notificación revocado.
        }
    }

    // ==========================================
    // BOTONES DE NOTIFICACIÓN
    // ==========================================

    private fun createActionIntent(
        context: Context,
        reminder: PassageReminder,
        triggerTime: Long,
        actionName: String
    ): PendingIntent {

        val intent = Intent(
            context,
            ReminderActionReceiver::class.java
        ).apply {

            action = actionName

            data = Uri.parse(
                "gastify://response/" +
                    "$actionName/" +
                    "${reminder.id}/" +
                    triggerTime
            )

            putExtra(
                ReminderActionReceiver.EXTRA_USER_ID,
                reminder.userId
            )

            putExtra(
                ReminderActionReceiver.EXTRA_REMINDER_ID,
                reminder.id
            )

            putExtra(
                ReminderActionReceiver.EXTRA_TRIGGER_TIME,
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
