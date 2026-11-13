package com.example.gastify.data.local

import android.content.Context

import com.example.gastify.data.model.Budget
import com.example.gastify.data.model.BudgetPeriod

import org.json.JSONArray
import org.json.JSONObject

class BudgetStore(
    context: Context
) {

    private val preferences = context
        .applicationContext
        .getSharedPreferences(
            "gastify_budgets",
            Context.MODE_PRIVATE
        )

    companion object {

        private const val KEY_BUDGETS =
            "budgets"

        private const val MAX_LIMIT_CENTS =
            999_999_999_999L

        private const val MIGRATION_PREFIX =
            "cloud_migrated_"
    }

    // ==========================================
    // LEER PRESUPUESTOS LOCALES
    // ==========================================

    fun getAll(): List<Budget> {

        val json = preferences.getString(
            KEY_BUDGETS,
            "[]"
        ) ?: "[]"

        return try {

            val array = JSONArray(json)

            buildList {

                for (index in 0 until array.length()) {

                    val item = array.optJSONObject(index)
                        ?: continue

                    val budget = Budget(

                        id = item.optString("id"),

                        userId = item.optString("userId"),

                        workspaceId = item.optString(
                            "workspaceId"
                        ),

                        categoryId = item.optString(
                            "categoryId"
                        ),

                        period = item.optString("period"),

                        limitCents = item.optLong(
                            "limitCents",
                            0L
                        )
                    )

                    if (
                        budget.id.isNotBlank() &&
                        budget.userId.isNotBlank() &&
                        budget.workspaceId.isNotBlank() &&
                        BudgetPeriod.entries.any {
                            it.name == budget.period
                        } &&
                        budget.limitCents in
                            1L..MAX_LIMIT_CENTS
                    ) {

                        add(budget)
                    }
                }
            }

        } catch (_: Exception) {

            emptyList()
        }
    }

    // ==========================================
    // PRESUPUESTOS DE UN ESPACIO
    // ==========================================

    fun getByWorkspace(
        userId: String,
        workspaceId: String
    ): List<Budget> {

        return getAll().filter {

            it.userId == userId &&
                it.workspaceId == workspaceId
        }
    }

    // ==========================================
    // ACTUALIZAR CACHÉ DESDE FIREBASE
    // ==========================================

    fun replaceWorkspace(
        userId: String,
        workspaceId: String,
        cloudBudgets: List<Budget>
    ): Boolean {

        val previous = getAll()

        val others = previous.filterNot {

            it.userId == userId &&
                it.workspaceId == workspaceId
        }

        val validCloudBudgets = cloudBudgets.filter {

            it.userId == userId &&
                it.workspaceId == workspaceId &&
                it.limitCents in 1L..MAX_LIMIT_CENTS &&
                BudgetPeriod.entries.any { period ->
                    period.name == it.period
                }
        }

        val updated = others + validCloudBudgets

        // No escribir de nuevo si los datos
        // no han cambiado.

        if (
            previous.sortedBy { it.id } ==
            updated.sortedBy { it.id }
        ) {

            return false
        }

        saveAll(updated)

        return true
    }

    // ==========================================
    // COMPROBAR MIGRACIÓN
    // ==========================================

    fun isMigrated(
        userId: String,
        workspaceId: String
    ): Boolean {

        return preferences.getBoolean(
            migrationKey(userId, workspaceId),
            false
        )
    }

    // ==========================================
    // MARCAR MIGRACIÓN COMPLETADA
    // ==========================================

    fun markMigrated(
        userId: String,
        workspaceId: String
    ) {

        val saved = preferences.edit()
            .putBoolean(
                migrationKey(userId, workspaceId),
                true
            )
            .commit()

        check(saved) {
            "No se pudo completar la migración local."
        }
    }

    private fun migrationKey(
        userId: String,
        workspaceId: String
    ): String {

        return "$MIGRATION_PREFIX${userId}_$workspaceId"
    }

    // ==========================================
    // GUARDAR CACHÉ
    // ==========================================

    private fun saveAll(
        budgets: List<Budget>
    ) {

        val array = JSONArray()

        budgets.forEach { budget ->

            array.put(
                JSONObject().apply {

                    put("id", budget.id)

                    put("userId", budget.userId)

                    put(
                        "workspaceId",
                        budget.workspaceId
                    )

                    put(
                        "categoryId",
                        budget.categoryId
                    )

                    put("period", budget.period)

                    put(
                        "limitCents",
                        budget.limitCents
                    )
                }
            )
        }

        val saved = preferences.edit()
            .putString(
                KEY_BUDGETS,
                array.toString()
            )
            .commit()

        check(saved) {
            "No se pudo actualizar la caché."
        }
    }
}
