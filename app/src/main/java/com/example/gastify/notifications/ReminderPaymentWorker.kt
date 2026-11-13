package com.example.gastify.notifications

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build

import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat

import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

import com.example.gastify.MainActivity
import com.example.gastify.R

import com.example.gastify.data.repository.ReminderPaymentRepository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestoreException

class ReminderPaymentWorker(
    appContext: Context,
    workerParameters: WorkerParameters
) : CoroutineWorker(
    appContext,
    workerParameters
) {

    companion object {

        const val KEY_USER_ID = "userId"
        const val KEY_WORKSPACE_ID = "workspaceId"
        const val KEY_ACCOUNT_ID = "accountId"
        const val KEY_CATEGORY_ID = "categoryId"
        const val KEY_REMINDER_ID = "reminderId"
        const val KEY_TRIGGER_TIME = "triggerTime"
        const val KEY_AMOUNT_CENTS = "amountCents"
        const val KEY_TITLE = "title"
    }

    override suspend fun doWork(): Result {

        val userId = inputData.getString(
            KEY_USER_ID
        ) ?: return Result.failure()

        val workspaceId = inputData.getString(
            KEY_WORKSPACE_ID
        ) ?: return Result.failure()

        val accountId = inputData.getString(
            KEY_ACCOUNT_ID
        ) ?: return Result.failure()

        val categoryId = inputData.getString(
            KEY_CATEGORY_ID
        ) ?: return Result.failure()

        val reminderId = inputData.getString(
            KEY_REMINDER_ID
        ) ?: return Result.failure()

        val triggerTime = inputData.getLong(
            KEY_TRIGGER_TIME,
            0L
        )

        val amountCents = inputData.getLong(
            KEY_AMOUNT_CENTS,
            0L
        )

        val title = inputData.getString(
            KEY_TITLE
        ) ?: "Pasaje"

        val notificationId = (
            reminderId + triggerTime
        ).hashCode()

        if (
            triggerTime <= 0L ||
            amountCents <= 0L
        ) {
            return Result.failure()
        }

        // Evitar registrar pagos demasiado antiguos.

        val age = System.currentTimeMillis() - triggerTime

        if (
            age > 48L * 60L * 60L * 1000L
        ) {

            ReminderPaymentFeedback.show(
                applicationContext,
                notificationId,
                "Confirmación vencida",
                "Registra el pasaje manualmente en GASTIFY."
            )

            return Result.failure()
        }

        // Nunca utilizar la sesión de otro usuario.

        val currentUserId = FirebaseAuth
            .getInstance()
            .currentUser
            ?.uid

        if (currentUserId != userId) {

            // Mantener pendiente mientras no esté
            // iniciada la sesión correspondiente.

            return Result.retry()
        }

        return try {

            ReminderPaymentRepository(
                userId = userId,
                workspaceId = workspaceId
            ).confirmPayment(

                reminderId = reminderId,

                triggerTime = triggerTime,

                accountId = accountId,

                categoryId = categoryId,

                amountCents = amountCents,

                title = title
            )

            ReminderPaymentFeedback.show(
                applicationContext,
                notificationId,
                "Pasaje registrado",
                "El gasto se descontó de tu cuenta."
            )

            Result.success()

        } catch (exception: IllegalStateException) {

            ReminderPaymentFeedback.show(
                applicationContext,
                notificationId,
                "No se registró el pasaje",
                exception.message
                    ?: "Revisa la cuenta y el saldo."
            )

            Result.failure()

        } catch (exception: FirebaseFirestoreException) {

            when (exception.code) {

                FirebaseFirestoreException.Code.PERMISSION_DENIED,

                FirebaseFirestoreException.Code.INVALID_ARGUMENT,

                FirebaseFirestoreException.Code.FAILED_PRECONDITION -> {

                    ReminderPaymentFeedback.show(
                        applicationContext,
                        notificationId,
                        "No se registró el pasaje",
                        "Revisa tu cuenta y la configuración de Firebase."
                    )

                    Result.failure()
                }

                else -> {

                    // Fallos de red u otros errores
                    // recuperables: reintentar.

                    Result.retry()
                }
            }

        } catch (_: Exception) {

            Result.retry()
        }
    }
}

// ==========================================
// NOTIFICACIONES DEL RESULTADO
// ==========================================

object ReminderPaymentFeedback {

    fun show(
        context: Context,
        notificationId: Int,
        title: String,
        message: String
    ) {

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
                permission != PackageManager.PERMISSION_GRANTED
            ) {
                return
            }
        }

        val manager = NotificationManagerCompat.from(
            context
        )

        if (!manager.areNotificationsEnabled()) {
            return
        }

        val openAppIntent = Intent(
            context,
            MainActivity::class.java
        )

        val pendingIntent = PendingIntent.getActivity(

            context,

            notificationId,

            openAppIntent,

            PendingIntent.FLAG_UPDATE_CURRENT or
                PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(
            context,
            ReminderReceiver.CHANNEL_ID
        )

            .setSmallIcon(
                R.drawable.ic_notification_bus
            )

            .setContentTitle(title)

            .setContentText(message)

            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText(message)
            )

            .setPriority(
                NotificationCompat.PRIORITY_DEFAULT
            )

            .setContentIntent(pendingIntent)

            .setAutoCancel(true)

            .build()

        try {

            manager.notify(
                notificationId,
                notification
            )

        } catch (_: SecurityException) {
            // Permiso revocado.
        }
    }
}
