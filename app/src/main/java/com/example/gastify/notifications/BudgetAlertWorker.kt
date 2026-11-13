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

import androidx.work.BackoffPolicy
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

import com.example.gastify.data.local.BudgetStore
import com.example.gastify.data.local.NotificationPreferencesStore

import com.example.gastify.data.model.Budget
import com.example.gastify.data.model.BudgetPeriod

import com.google.android.gms.tasks.Task
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.Source

import kotlinx.coroutines.suspendCancellableCoroutine

import java.math.BigDecimal
import java.util.Calendar
import java.util.Date
import java.util.concurrent.TimeUnit

import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class BudgetAlertWorker(
    appContext: Context,
    workerParameters: WorkerParameters
) : CoroutineWorker(
    appContext,
    workerParameters
) {

    companion object {

        private const val CHANNEL_ID =
            "gastify_budget_alerts"

        private const val CHANNEL_NAME =
            "Alertas de presupuestos"

        private const val PERIODIC_WORK_NAME =
            "gastify_budget_periodic_check"

        private const val IMMEDIATE_WORK_NAME =
            "gastify_budget_immediate_check"

        private const val PREFERENCES_NAME =
            "gastify_budget_alert_status"

        private val alertLock = Any()

        // ======================================
        // COMPROBAR SI ESTÁ ACTIVADO
        // ======================================

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
            ).areBudgetsEnabled()
        }

        // ======================================
        // PROGRAMAR
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
                PeriodicWorkRequestBuilder<BudgetAlertWorker>(
                    30,
                    TimeUnit.MINUTES
                )
                    .setConstraints(constraints)
                    .build()

            WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork(
                    PERIODIC_WORK_NAME,
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

            val constraints = Constraints.Builder()
                .setRequiredNetworkType(
                    NetworkType.CONNECTED
                )
                .build()

            val request =
                OneTimeWorkRequestBuilder<BudgetAlertWorker>()
                    .setConstraints(constraints)
                    .setBackoffCriteria(
                        BackoffPolicy.EXPONENTIAL,
                        30,
                        TimeUnit.SECONDS
                    )
                    .build()

            WorkManager.getInstance(context)
                .enqueueUniqueWork(
                    IMMEDIATE_WORK_NAME,
                    ExistingWorkPolicy.KEEP,
                    request
                )
        }

        // ======================================
        // CANCELAR
        // ======================================

        fun cancelScheduled(
            context: Context
        ) {

            val manager = WorkManager.getInstance(
                context
            )

            manager.cancelUniqueWork(
                PERIODIC_WORK_NAME
            )

            manager.cancelUniqueWork(
                IMMEDIATE_WORK_NAME
            )
        }

        // ======================================
        // FORMATO DE MONEDA
        // ======================================

        private fun money(
            cents: Long
        ): String {

            return "S/ " + BigDecimal.valueOf(
                cents,
                2
            ).toPlainString()
        }

        // ======================================
        // RANGO DE FECHAS
        // ======================================

        private fun periodRange(
            period: BudgetPeriod
        ): Pair<Long, Long> {

            val start = Calendar.getInstance().apply {

                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }

            when (period) {

                BudgetPeriod.DAILY -> Unit

                BudgetPeriod.WEEKLY -> {

                    val daysSinceMonday = (
                        start.get(Calendar.DAY_OF_WEEK) + 5
                    ) % 7

                    start.add(
                        Calendar.DAY_OF_MONTH,
                        -daysSinceMonday
                    )
                }

                BudgetPeriod.MONTHLY -> {

                    start.set(
                        Calendar.DAY_OF_MONTH,
                        1
                    )
                }
            }

            val end = (
                start.clone() as Calendar
            ).apply {

                when (period) {

                    BudgetPeriod.DAILY -> {
                        add(Calendar.DAY_OF_MONTH, 1)
                    }

                    BudgetPeriod.WEEKLY -> {
                        add(Calendar.DAY_OF_MONTH, 7)
                    }

                    BudgetPeriod.MONTHLY -> {
                        add(Calendar.MONTH, 1)
                    }
                }
            }

            return start.timeInMillis to
                end.timeInMillis
        }
    }

    private val database =
        FirebaseFirestore.getInstance()

    // ==========================================
    // EJECUTAR COMPROBACIÓN
    // ==========================================

    override suspend fun doWork(): Result {

        val userId = FirebaseAuth.getInstance()
            .currentUser
            ?.uid
            ?: return Result.success()

        if (!alertsEnabled(userId)) {
            return Result.success()
        }

        val budgets = BudgetStore(
            applicationContext
        ).getAll().filter {

            it.userId == userId &&
                it.limitCents > 0L
        }

        if (budgets.isEmpty()) {
            return Result.success()
        }

        return try {

            val grouped = budgets.groupBy {
                it.workspaceId
            }

            grouped.forEach {
                    (workspaceId, workspaceBudgets) ->

                if (alertsEnabled(userId)) {

                    checkWorkspace(
                        userId,
                        workspaceId,
                        workspaceBudgets
                    )
                }
            }

            Result.success()

        } catch (exception: FirebaseFirestoreException) {

            when (exception.code) {

                FirebaseFirestoreException.Code.PERMISSION_DENIED,

                FirebaseFirestoreException.Code.INVALID_ARGUMENT -> {

                    Result.failure()
                }

                else -> {

                    Result.retry()
                }
            }

        } catch (_: Exception) {

            Result.retry()
        }
    }

    // ==========================================
    // CONSULTAR ESPACIO
    // ==========================================

    private suspend fun checkWorkspace(
        userId: String,
        workspaceId: String,
        budgets: List<Budget>
    ) {

        val workspace = database
            .collection("users")
            .document(userId)
            .collection("workspaces")
            .document(workspaceId)

        val categoryNames = try {

            workspace.collection("categories")
                .get(Source.SERVER)
                .awaitResult()
                .documents
                .associate { document ->

                    document.id to (
                        document.getString("name")
                            ?: "Categoría"
                    )
                }

        } catch (_: Exception) {

            emptyMap()
        }

        val grouped = budgets.groupBy {
            BudgetPeriod.valueOf(it.period)
        }

        grouped.forEach {
                (period, periodBudgets) ->

            if (!alertsEnabled(userId)) {
                return
            }

            val range = periodRange(
                period
            )

            val startTimestamp = Timestamp(
                Date(range.first)
            )

            val endTimestamp = Timestamp(
                Date(range.second)
            )

            val snapshot = workspace
                .collection("movements")
                .whereGreaterThanOrEqualTo(
                    "createdAt",
                    startTimestamp
                )
                .whereLessThan(
                    "createdAt",
                    endTimestamp
                )
                .orderBy(
                    "createdAt",
                    Query.Direction.DESCENDING
                )
                .get(Source.SERVER)
                .awaitResult()

            val expenses = snapshot.documents
                .mapNotNull { document ->

                    val type = document.getString(
                        "type"
                    )

                    val categoryId = document.getString(
                        "categoryId"
                    )

                    val amount = document.getLong(
                        "amountCents"
                    )

                    if (
                        type != "EXPENSE" ||
                        categoryId == null ||
                        amount == null ||
                        amount <= 0L
                    ) {

                        null

                    } else {

                        categoryId to amount
                    }
                }

            periodBudgets.forEach { budget ->

                var spentCents = 0L

                expenses.forEach {
                        (categoryId, amount) ->

                    if (
                        budget.categoryId.isEmpty() ||
                        budget.categoryId == categoryId
                    ) {

                        spentCents = if (
                            Long.MAX_VALUE -
                                spentCents < amount
                        ) {

                            Long.MAX_VALUE

                        } else {

                            spentCents + amount
                        }
                    }
                }

                val categoryName = if (
                    budget.categoryId.isEmpty()
                ) {

                    "Presupuesto general"

                } else {

                    categoryNames[budget.categoryId]
                        ?: "Presupuesto de categoría"
                }

                checkThreshold(
                    userId = userId,
                    budget = budget,
                    categoryName = categoryName,
                    spentCents = spentCents,
                    periodStart = range.first
                )
            }
        }
    }

    // ==========================================
    // COMPROBAR PORCENTAJE
    // ==========================================

    private fun checkThreshold(
        userId: String,
        budget: Budget,
        categoryName: String,
        spentCents: Long,
        periodStart: Long
    ) {

        if (
            budget.limitCents <= 0L ||
            spentCents <= 0L
        ) {
            return
        }

        if (!alertsEnabled(userId)) {
            return
        }

        val percentage = (
            spentCents.toDouble() /
                budget.limitCents.toDouble()
        ) * 100.0

        val threshold = when {

            percentage >= 100.0 -> 100

            percentage >= 90.0 -> 90

            percentage >= 80.0 -> 80

            else -> 0
        }

        if (threshold == 0) {
            return
        }

        if (!canNotify()) {
            return
        }

        val alertKey = listOf(
            userId,
            budget.workspaceId,
            budget.id,
            budget.period,
            periodStart,
            budget.limitCents
        ).joinToString("_")

        val preferences =
            applicationContext.getSharedPreferences(
                PREFERENCES_NAME,
                Context.MODE_PRIVATE
            )

        synchronized(alertLock) {

            if (!alertsEnabled(userId)) {
                return
            }

            val previousThreshold =
                preferences.getInt(
                    alertKey,
                    0
                )

            if (threshold <= previousThreshold) {
                return
            }

            val sent = showNotification(
                budget = budget,
                categoryName = categoryName,
                spentCents = spentCents,
                threshold = threshold
            )

            if (sent) {

                preferences.edit()
                    .putInt(
                        alertKey,
                        threshold
                    )
                    .commit()
            }
        }
    }

    // ==========================================
    // ESTADO DE CONFIGURACIÓN
    // ==========================================

    private fun alertsEnabled(
        userId: String
    ): Boolean {

        if (
            FirebaseAuth.getInstance()
                .currentUser
                ?.uid != userId
        ) {
            return false
        }

        return NotificationPreferencesStore(
            applicationContext,
            userId
        ).areBudgetsEnabled()
    }

    // ==========================================
    // PERMISO DE NOTIFICACIONES
    // ==========================================

    private fun canNotify(): Boolean {

        if (
            Build.VERSION.SDK_INT >=
            Build.VERSION_CODES.TIRAMISU
        ) {

            val permission =
                ContextCompat.checkSelfPermission(
                    applicationContext,
                    Manifest.permission.POST_NOTIFICATIONS
                )

            if (
                permission !=
                PackageManager.PERMISSION_GRANTED
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

    private fun showNotification(
        budget: Budget,
        categoryName: String,
        spentCents: Long,
        threshold: Int
    ): Boolean {

        createChannel()

        val title = when (threshold) {

            80 -> "Presupuesto al 80%"

            90 -> "Presupuesto al 90%"

            else -> "Límite de presupuesto alcanzado"
        }

        val remaining = (
            budget.limitCents - spentCents
        ).coerceAtLeast(0L)

        val message = if (threshold >= 100) {

            "$categoryName: gastaste ${
                money(spentCents)
            } de ${
                money(budget.limitCents)
            }. Has alcanzado o superado tu límite."

        } else {

            "$categoryName: llevas ${
                money(spentCents)
            } de ${
                money(budget.limitCents)
            }. Te quedan ${
                money(remaining)
            }."
        }

        val notificationId = (
            "${budget.userId}_" +
                "${budget.workspaceId}_" +
                budget.id
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
            .setCategory(
                NotificationCompat.CATEGORY_REMINDER
            )
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
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
    // CANAL DE NOTIFICACIONES
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
                "Alertas de gastos y límites de presupuestos."

            enableVibration(true)
        }

        val manager =
            applicationContext.getSystemService(
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
