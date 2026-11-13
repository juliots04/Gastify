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

import com.google.firebase.auth.FirebaseAuth

import java.math.BigDecimal
import java.text.NumberFormat
import java.util.Locale

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

        if (
            intent.action !=
            ReminderScheduler.ACTION_REMINDER
        ) {
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

        // Programar el siguiente aviso.

        ReminderScheduler.schedule(
            context,
            reminder
        )

        val currentUserId = FirebaseAuth
            .getInstance()
            .currentUser
            ?.uid

        if (currentUserId != reminder.userId) {
            return
        }

        if (
            Build.VERSION.SDK_INT >=
            Build.VERSION_CODES.TIRAMISU
        ) {

            if (
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                return
            }
        }

        createNotificationChannel(context)

        showNotification(
            context,
            reminder,
            triggerTime
        )
    }

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
                "Avisos para confirmar pagos de transporte."

            enableVibration(true)
        }

        val manager = context.getSystemService(
            NotificationManager::class.java
        )

        manager.createNotificationChannel(
            channel
        )
    }

    private fun showNotification(
        context: Context,
        reminder: PassageReminder,
        triggerTime: Long
    ) {

        val amount = formatMoney(
            reminder.amountCents
        )

        val notificationId = (
            reminder.id + triggerTime
        ).hashCode()

        // ======================================
        // ABRIR APLICACIÓN
        // ======================================

        val openAppIntent = Intent(
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
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or
                PendingIntent.FLAG_IMMUTABLE
        )

        // ======================================
        // BOTÓN: SÍ, PAGUÉ
        // ======================================

        val paidIntent = createActionIntent(
            context,
            reminder,
            triggerTime,
            ReminderActionReceiver.ACTION_PAID
        )

        // ======================================
        // BOTÓN: NO
        // ======================================

        val notPaidIntent = createActionIntent(
            context,
            reminder,
            triggerTime,
            ReminderActionReceiver.ACTION_NOT_PAID
        )

        // ======================================
        // CREAR NOTIFICACIÓN
        // ======================================

        val notification = NotificationCompat.Builder(
            context,
            CHANNEL_ID
        )

            .setSmallIcon(
                R.drawable.ic_notification_bus
            )

            .setContentTitle(
                "¿Ya pagaste tu pasaje?"
            )

            .setContentText(
                "${reminder.title} • $amount"
            )

            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText(
                        "${reminder.title}\nImporte: $amount"
                    )
            )

            .setPriority(
                NotificationCompat.PRIORITY_HIGH
            )

            .setCategory(
                NotificationCompat.CATEGORY_REMINDER
            )

            .setContentIntent(
                openPendingIntent
            )

            .addAction(
                R.drawable.ic_notification_bus,
                "Sí, pagué",
                paidIntent
            )

            .addAction(
                R.drawable.ic_notification_bus,
                "No",
                notPaidIntent
            )

            .setAutoCancel(true)

            .build()

        try {

            NotificationManagerCompat.from(context)
                .notify(
                    notificationId,
                    notification
                )

        } catch (_: SecurityException) {
            // Permiso no disponible.
        }
    }

    // ==========================================
    // INTENT PARA CADA BOTÓN
    // ==========================================

    private fun createActionIntent(
        context: Context,
        reminder: PassageReminder,
        triggerTime: Long,
        action: String
    ): PendingIntent {

        val intent = Intent(
            context,
            ReminderActionReceiver::class.java
        ).apply {

            this.action = action

            // URI única para cada botón y aviso.

            data = Uri.parse(
                "gastify://response/$action/${reminder.id}/$triggerTime"
            )

            putExtra(
                ReminderActionReceiver.EXTRA_USER_ID,
                reminder.userId
            )

            putExtra(
                ReminderActionReceiver.EXTRA_WORKSPACE_ID,
                reminder.workspaceId
            )

            putExtra(
                ReminderActionReceiver.EXTRA_ACCOUNT_ID,
                reminder.accountId
            )

            putExtra(
                ReminderActionReceiver.EXTRA_CATEGORY_ID,
                reminder.categoryId
            )

            putExtra(
                ReminderActionReceiver.EXTRA_REMINDER_ID,
                reminder.id
            )

            putExtra(
                ReminderActionReceiver.EXTRA_TRIGGER_TIME,
                triggerTime
            )

            putExtra(
                ReminderActionReceiver.EXTRA_AMOUNT_CENTS,
                reminder.amountCents
            )

            putExtra(
                ReminderActionReceiver.EXTRA_TITLE,
                reminder.title
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

    // ==========================================
    // FORMATEAR DINERO
    // ==========================================

    private fun formatMoney(
        cents: Long
    ): String {

        val formatter = NumberFormat.getNumberInstance(
            Locale.forLanguageTag("es-PE")
        )

        formatter.minimumFractionDigits = 2
        formatter.maximumFractionDigits = 2

        return "S/ ${
            formatter.format(
                BigDecimal.valueOf(cents, 2)
            )
        }"
    }
}
