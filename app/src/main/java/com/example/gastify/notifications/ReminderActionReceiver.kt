package com.example.gastify.notifications

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager

import com.example.gastify.data.local.ReminderStore

import java.util.concurrent.TimeUnit

class ReminderActionReceiver : BroadcastReceiver() {

    companion object {

        const val ACTION_PAID =
            "com.example.gastify.REMINDER_PAID"

        const val ACTION_LATER =
            "com.example.gastify.REMINDER_LATER"

        const val EXTRA_USER_ID = "userId"

        const val EXTRA_REMINDER_ID = "reminderId"

        const val EXTRA_TRIGGER_TIME = "triggerTime"
    }

    override fun onReceive(
        context: Context,
        intent: Intent
    ) {

        val action = intent.action ?: return

        if (
            action != ACTION_PAID &&
            action != ACTION_LATER
        ) {
            return
        }

        val userId = intent.getStringExtra(
            EXTRA_USER_ID
        ) ?: return

        val reminderId = intent.getStringExtra(
            EXTRA_REMINDER_ID
        ) ?: return

        val triggerTime = intent.getLongExtra(
            EXTRA_TRIGGER_TIME,
            0L
        )

        if (triggerTime <= 0L) {
            return
        }

        if (
            userId !=
            ReminderScheduler.activeUserId(context)
        ) {
            return
        }

        val reminder = ReminderStore(context)
            .getById(reminderId)
            ?: return

        if (
            !reminder.enabled ||
            reminder.userId != userId
        ) {
            return
        }

        val eventId =
            "${reminderId}_${triggerTime}"

        val notificationId = (
            reminderId + triggerTime
        ).hashCode()

        val preferences = context.getSharedPreferences(
            "gastify_reminder_responses",
            Context.MODE_PRIVATE
        )

        val responseKey = "response_$eventId"

        if (preferences.contains(responseKey)) {
            return
        }

        // ======================================
        // MÁS TARDE
        // ======================================

        if (action == ACTION_LATER) {

            try {

                ReminderScheduler.snooze(
                    context,
                    reminderId,
                    triggerTime
                )

                dismissNotification(
                    context,
                    notificationId
                )

            } catch (exception: Exception) {

                ReminderPaymentFeedback.show(
                    context,
                    notificationId,
                    "No se pudo aplazar",
                    exception.localizedMessage
                        ?: "Abre GASTIFY para revisar el recordatorio."
                )
            }

            return
        }

        // ======================================
        // SÍ, PAGUÉ
        // ======================================

        val saved = preferences.edit()
            .putString(
                responseKey,
                ACTION_PAID
            )
            .commit()

        if (!saved) {
            return
        }

        // Registrar el movimiento con los datos
        // actuales del recordatorio.

        val data = Data.Builder()
            .putString(
                ReminderPaymentWorker.KEY_USER_ID,
                reminder.userId
            )
            .putString(
                ReminderPaymentWorker.KEY_WORKSPACE_ID,
                reminder.workspaceId
            )
            .putString(
                ReminderPaymentWorker.KEY_ACCOUNT_ID,
                reminder.accountId
            )
            .putString(
                ReminderPaymentWorker.KEY_CATEGORY_ID,
                reminder.categoryId
            )
            .putString(
                ReminderPaymentWorker.KEY_REMINDER_ID,
                reminder.id
            )
            .putLong(
                ReminderPaymentWorker.KEY_TRIGGER_TIME,
                triggerTime
            )
            .putLong(
                ReminderPaymentWorker.KEY_AMOUNT_CENTS,
                reminder.amountCents
            )
            .putString(
                ReminderPaymentWorker.KEY_TITLE,
                reminder.title
            )
            .build()

        val constraints = Constraints.Builder()
            .setRequiredNetworkType(
                NetworkType.CONNECTED
            )
            .build()

        val request =
            OneTimeWorkRequestBuilder<ReminderPaymentWorker>()
                .setInputData(data)
                .setConstraints(constraints)
                .setBackoffCriteria(
                    BackoffPolicy.EXPONENTIAL,
                    30,
                    TimeUnit.SECONDS
                )
                .build()

        try {

            WorkManager.getInstance(context)
                .enqueueUniqueWork(
                    "gastify_payment_$eventId",
                    ExistingWorkPolicy.KEEP,
                    request
                )

            dismissNotification(
                context,
                notificationId
            )

            ReminderPaymentFeedback.show(
                context,
                notificationId,
                "Confirmación recibida",
                "Registrando el pago de tu pasaje."
            )

        } catch (_: Exception) {

            preferences.edit()
                .remove(responseKey)
                .commit()

            ReminderPaymentFeedback.show(
                context,
                notificationId,
                "No se pudo procesar",
                "Abre GASTIFY y registra el pago manualmente."
            )
        }
    }

    private fun dismissNotification(
        context: Context,
        notificationId: Int
    ) {

        val manager = context.getSystemService(
            Context.NOTIFICATION_SERVICE
        ) as NotificationManager

        manager.cancel(notificationId)
    }
}
