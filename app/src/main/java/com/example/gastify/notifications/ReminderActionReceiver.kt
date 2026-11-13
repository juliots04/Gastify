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

import com.google.firebase.auth.FirebaseAuth

import java.util.concurrent.TimeUnit

class ReminderActionReceiver : BroadcastReceiver() {

    companion object {

        const val ACTION_PAID =
            "com.example.gastify.REMINDER_PAID"

        const val ACTION_NOT_PAID =
            "com.example.gastify.REMINDER_NOT_PAID"

        const val EXTRA_USER_ID = "userId"
        const val EXTRA_WORKSPACE_ID = "workspaceId"
        const val EXTRA_ACCOUNT_ID = "accountId"
        const val EXTRA_CATEGORY_ID = "categoryId"
        const val EXTRA_REMINDER_ID = "reminderId"
        const val EXTRA_TRIGGER_TIME = "triggerTime"
        const val EXTRA_AMOUNT_CENTS = "amountCents"
        const val EXTRA_TITLE = "title"
    }

    override fun onReceive(
        context: Context,
        intent: Intent
    ) {

        val action = intent.action ?: return

        if (
            action != ACTION_PAID &&
            action != ACTION_NOT_PAID
        ) {
            return
        }

        val userId = intent.getStringExtra(
            EXTRA_USER_ID
        ) ?: return

        val workspaceId = intent.getStringExtra(
            EXTRA_WORKSPACE_ID
        ) ?: return

        val accountId = intent.getStringExtra(
            EXTRA_ACCOUNT_ID
        ) ?: return

        val categoryId = intent.getStringExtra(
            EXTRA_CATEGORY_ID
        ) ?: return

        val reminderId = intent.getStringExtra(
            EXTRA_REMINDER_ID
        ) ?: return

        val triggerTime = intent.getLongExtra(
            EXTRA_TRIGGER_TIME,
            0L
        )

        val amountCents = intent.getLongExtra(
            EXTRA_AMOUNT_CENTS,
            0L
        )

        val title = intent.getStringExtra(
            EXTRA_TITLE
        ) ?: "Pasaje"

        if (
            triggerTime <= 0L ||
            amountCents <= 0L
        ) {
            return
        }

        val notificationId = (
            reminderId + triggerTime
        ).hashCode()

        val currentUserId = FirebaseAuth
            .getInstance()
            .currentUser
            ?.uid

        if (currentUserId != userId) {
            return
        }

        // Identificador único de este aviso.

        val eventId = "${reminderId}_${triggerTime}"

        // Evitar que ambos botones procesen el mismo aviso.

        val preferences = context.getSharedPreferences(
            "gastify_reminder_responses",
            Context.MODE_PRIVATE
        )

        val responseKey = "response_$eventId"

        if (preferences.contains(responseKey)) {
            return
        }

        val saved = preferences.edit()
            .putString(
                responseKey,
                action
            )
            .commit()

        if (!saved) {
            return
        }

        val notificationManager = context.getSystemService(
            Context.NOTIFICATION_SERVICE
        ) as NotificationManager

        notificationManager.cancel(
            notificationId
        )

        // ======================================
        // NO PAGUÉ
        // ======================================

        if (action == ACTION_NOT_PAID) {

            // No se modifica ningún saldo.
            return
        }

        // ======================================
        // SÍ, PAGUÉ
        // ======================================

        val data = Data.Builder()

            .putString(
                ReminderPaymentWorker.KEY_USER_ID,
                userId
            )

            .putString(
                ReminderPaymentWorker.KEY_WORKSPACE_ID,
                workspaceId
            )

            .putString(
                ReminderPaymentWorker.KEY_ACCOUNT_ID,
                accountId
            )

            .putString(
                ReminderPaymentWorker.KEY_CATEGORY_ID,
                categoryId
            )

            .putString(
                ReminderPaymentWorker.KEY_REMINDER_ID,
                reminderId
            )

            .putLong(
                ReminderPaymentWorker.KEY_TRIGGER_TIME,
                triggerTime
            )

            .putLong(
                ReminderPaymentWorker.KEY_AMOUNT_CENTS,
                amountCents
            )

            .putString(
                ReminderPaymentWorker.KEY_TITLE,
                title
            )

            .build()

        val constraints = Constraints.Builder()

            .setRequiredNetworkType(
                NetworkType.CONNECTED
            )

            .build()

        val request = OneTimeWorkRequestBuilder<ReminderPaymentWorker>()

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

            ReminderPaymentFeedback.show(
                context,
                notificationId,
                "Confirmación recibida",
                "Registrando tu pasaje. Si no hay internet, quedará pendiente."
            )

        } catch (_: Exception) {

            preferences.edit()
                .remove(responseKey)
                .commit()

            ReminderPaymentFeedback.show(
                context,
                notificationId,
                "No se pudo procesar",
                "Abre GASTIFY e intenta registrar el pasaje manualmente."
            )
        }
    }
}
