package com.example.gastify.data.local

import android.content.Context

data class NotificationPreferences(
    val remindersEnabled: Boolean = true,
    val budgetsEnabled: Boolean = true,
    val stockEnabled: Boolean = true
)

class NotificationPreferencesStore(
    context: Context,
    private val userId: String
) {

    private val preferences = context
        .applicationContext
        .getSharedPreferences(
            "gastify_notification_preferences",
            Context.MODE_PRIVATE
        )

    init {
        require(userId.isNotBlank()) {
            "El usuario no es válido."
        }
    }

    private fun key(name: String): String {
        return "${userId}_$name"
    }

    // ==========================================
    // LEER CONFIGURACIÓN
    // ==========================================

    fun read(): NotificationPreferences {

        return NotificationPreferences(

            remindersEnabled = preferences.getBoolean(
                key("reminders"),
                true
            ),

            budgetsEnabled = preferences.getBoolean(
                key("budgets"),
                true
            ),

            stockEnabled = preferences.getBoolean(
                key("stock"),
                true
            )
        )
    }

    // ==========================================
    // RECORDATORIOS
    // ==========================================

    fun setRemindersEnabled(
        enabled: Boolean
    ): Boolean {

        return preferences.edit()
            .putBoolean(
                key("reminders"),
                enabled
            )
            .commit()
    }

    // ==========================================
    // PRESUPUESTOS
    // ==========================================

    fun setBudgetsEnabled(
        enabled: Boolean
    ): Boolean {

        return preferences.edit()
            .putBoolean(
                key("budgets"),
                enabled
            )
            .commit()
    }

    // ==========================================
    // INVENTARIO
    // ==========================================

    fun setStockEnabled(
        enabled: Boolean
    ): Boolean {

        return preferences.edit()
            .putBoolean(
                key("stock"),
                enabled
            )
            .commit()
    }

    // ==========================================
    // CONSULTAS INDIVIDUALES
    // ==========================================

    fun areRemindersEnabled(): Boolean {
        return read().remindersEnabled
    }

    fun areBudgetsEnabled(): Boolean {
        return read().budgetsEnabled
    }

    fun areStockAlertsEnabled(): Boolean {
        return read().stockEnabled
    }
}
