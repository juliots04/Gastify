package com.example.gastify.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build

import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat

import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters

import com.example.gastify.MainActivity
import com.example.gastify.R

import com.example.gastify.data.local.NotificationPreferencesStore

import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Source

import kotlinx.coroutines.suspendCancellableCoroutine

import java.util.concurrent.TimeUnit

import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class StockAlertWorker(
    appContext: Context,
    workerParameters: WorkerParameters
) : CoroutineWorker(
    appContext,
    workerParameters
) {

    companion object {

        private const val CHANNEL_ID =
            "gastify_stock_alerts"

        private const val CHANNEL_NAME =
            "Alertas de inventario"

        private const val PERIODIC_WORK =
            "gastify_stock_periodic_check"

        private const val IMMEDIATE_WORK =
            "gastify_stock_immediate_check"

        private const val STATUS_PREFERENCES =
            "gastify_stock_alert_status"

        private val statusLock = Any()

        private fun isEnabled(
            context: Context
        ): Boolean {

            val userId = FirebaseAuth.getInstance()
                .currentUser
                ?.uid
                ?: return false

            return NotificationPreferencesStore(
                context,
                userId
            ).areStockAlertsEnabled()
        }

        // ======================================
        // PROGRAMAR COMPROBACIÓN
        // ======================================

        fun ensureScheduled(
            context: Context
        ) {

            if (!isEnabled(context)) {
                return
            }

            val constraints = Constraints.Builder()
                .setRequiredNetworkType(
                    NetworkType.CONNECTED
                )
                .build()

            val request =
                PeriodicWorkRequestBuilder<StockAlertWorker>(
                    30,
                    TimeUnit.MINUTES
                )
                    .setConstraints(constraints)
                    .build()

            WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork(
                    PERIODIC_WORK,
                    ExistingPeriodicWorkPolicy.KEEP,
                    request
                )
        }

        // ======================================
        // COMPROBAR AHORA
        // ======================================

        fun checkNow(
            context: Context
        ) {

            if (!isEnabled(context)) {
                return
            }

            val request =
                OneTimeWorkRequestBuilder<StockAlertWorker>()
                    .setConstraints(
                        Constraints.Builder()
                            .setRequiredNetworkType(
                                NetworkType.CONNECTED
                            )
                            .build()
                    )
                    .build()

            WorkManager.getInstance(context)
                .enqueueUniqueWork(
                    IMMEDIATE_WORK,
                    ExistingWorkPolicy.KEEP,
                    request
                )
        }

        // ======================================
        // CANCELAR COMPROBACIONES
        // ======================================

        fun cancelScheduled(
            context: Context
        ) {

            val manager = WorkManager.getInstance(
                context
            )

            manager.cancelUniqueWork(
                PERIODIC_WORK
            )

            manager.cancelUniqueWork(
                IMMEDIATE_WORK
            )
        }
    }

    private val database =
        FirebaseFirestore.getInstance()

    // ==========================================
    // EJECUTAR
    // ==========================================

    override suspend fun doWork(): Result {

        val userId = FirebaseAuth.getInstance()
            .currentUser
            ?.uid
            ?: return Result.success()

        if (!alertsEnabled(userId)) {
            return Result.success()
        }

        return try {

            val workspaces = database
                .collection("users")
                .document(userId)
                .collection("workspaces")
                .get(Source.SERVER)
                .awaitResult()

            for (workspace in workspaces.documents) {

                if (!alertsEnabled(userId)) {
                    break
                }

                checkWorkspace(
                    userId = userId,
                    workspaceId = workspace.id,
                    workspaceName = workspace.getString(
                        "name"
                    ) ?: "Mi espacio"
                )
            }

            Result.success()

        } catch (_: Exception) {

            Result.retry()
        }
    }

    // ==========================================
    // COMPROBAR ESPACIO
    // ==========================================

    private suspend fun checkWorkspace(
        userId: String,
        workspaceId: String,
        workspaceName: String
    ) {

        val products = database
            .collection("users")
            .document(userId)
            .collection("workspaces")
            .document(workspaceId)
            .collection("products")
            .get(Source.SERVER)
            .awaitResult()

        for (document in products.documents) {

            if (!alertsEnabled(userId)) {
                return
            }

            val productName = document.getString(
                "name"
            ) ?: continue

            val quantity = document.getLong(
                "quantity"
            ) ?: continue

            val minimum = document.getLong(
                "minQuantity"
            ) ?: continue

            if (
                quantity !in 0L..1_000_000L ||
                minimum !in 0L..1_000_000L
            ) {
                continue
            }

            val status = when {

                quantity == 0L -> "OUT"

                minimum > 0L &&
                    quantity <= minimum -> "LOW"

                else -> "NORMAL"
            }

            checkProductStatus(
                userId = userId,
                workspaceId = workspaceId,
                workspaceName = workspaceName,
                productId = document.id,
                productName = productName,
                quantity = quantity,
                minimum = minimum,
                status = status
            )
        }
    }

    // ==========================================
    // EVITAR NOTIFICACIONES DUPLICADAS
    // ==========================================

    private fun checkProductStatus(
        userId: String,
        workspaceId: String,
        workspaceName: String,
        productId: String,
        productName: String,
        quantity: Long,
        minimum: Long,
        status: String
    ) {

        val preferences =
            applicationContext.getSharedPreferences(
                STATUS_PREFERENCES,
                Context.MODE_PRIVATE
            )

        val key =
            "${userId}_${workspaceId}_${productId}"

        synchronized(statusLock) {

            if (status == "NORMAL") {

                preferences.edit()
                    .remove(key)
                    .commit()

                return
            }

            val previousStatus = preferences.getString(
                key,
                ""
            )

            if (previousStatus == status) {
                return
            }

            if (!alertsEnabled(userId)) {
                return
            }

            if (!canNotify()) {
                return
            }

            val sent = showStockNotification(
                userId = userId,
                workspaceId = workspaceId,
                workspaceName = workspaceName,
                productId = productId,
                productName = productName,
                quantity = quantity,
                minimum = minimum,
                status = status
            )

            if (sent) {

                preferences.edit()
                    .putString(
                        key,
                        status
                    )
                    .commit()
            }
        }
    }

    // ==========================================
    // COMPROBAR CONFIGURACIÓN
    // ==========================================

    private fun alertsEnabled(
        userId: String
    ): Boolean {

        val currentUser = FirebaseAuth.getInstance()
            .currentUser
            ?.uid

        if (currentUser != userId) {
            return false
        }

        return NotificationPreferencesStore(
            applicationContext,
            userId
        ).areStockAlertsEnabled()
    }

    // ==========================================
    // COMPROBAR PERMISO
    // ==========================================

    private fun canNotify(): Boolean {

        if (
            Build.VERSION.SDK_INT >=
            Build.VERSION_CODES.TIRAMISU
        ) {

            if (
                ContextCompat.checkSelfPermission(
                    applicationContext,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                return false
            }
        }

        return NotificationManagerCompat
            .from(applicationContext)
            .areNotificationsEnabled()
    }

    // ==========================================
    // MOSTRAR NOTIFICACIÓN
    // ==========================================

    private fun showStockNotification(
        userId: String,
        workspaceId: String,
        workspaceName: String,
        productId: String,
        productName: String,
        quantity: Long,
        minimum: Long,
        status: String
    ): Boolean {

        createChannel()

        val title = if (status == "OUT") {

            "Producto agotado"

        } else {

            "Stock bajo"
        }

        val message = if (status == "OUT") {

            "$productName se agotó en $workspaceName."

        } else {

            "$productName tiene $quantity unidades. " +
                "Stock mínimo: $minimum."
        }

        val notificationId = (
            "${userId}_${workspaceId}_${productId}"
        ).hashCode()

        val intent = Intent(
            applicationContext,
            MainActivity::class.java
        ).apply {

            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                Intent.FLAG_ACTIVITY_CLEAR_TOP or
                Intent.FLAG_ACTIVITY_SINGLE_TOP
        }

        val pendingIntent = PendingIntent.getActivity(
            applicationContext,
            notificationId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or
                PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(
            applicationContext,
            CHANNEL_ID
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
                NotificationCompat.PRIORITY_HIGH
            )
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        return try {

            NotificationManagerCompat
                .from(applicationContext)
                .notify(
                    notificationId,
                    notification
                )

            true

        } catch (_: SecurityException) {

            false
        }
    }

    // ==========================================
    // CREAR CANAL
    // ==========================================

    private fun createChannel() {

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
                "Alertas de productos con stock bajo o agotado."
        }

        val manager = applicationContext
            .getSystemService(
                NotificationManager::class.java
            )

        manager.createNotificationChannel(
            channel
        )
    }

    // ==========================================
    // ESPERAR FIREBASE
    // ==========================================

    private suspend fun <T> Task<T>.awaitResult(): T {

        return suspendCancellableCoroutine {
                continuation ->

            addOnSuccessListener { result ->

                if (continuation.isActive) {

                    continuation.resume(result)
                }
            }

            addOnFailureListener { exception ->

                if (continuation.isActive) {

                    continuation.resumeWithException(
                        exception
                    )
                }
            }
        }
    }
}
