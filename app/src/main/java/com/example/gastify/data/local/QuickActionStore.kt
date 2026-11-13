package com.example.gastify.data.local

import android.content.Context

import com.example.gastify.data.model.MovementType
import com.example.gastify.data.model.QuickAction

import org.json.JSONArray
import org.json.JSONObject

class QuickActionStore(
    context: Context
) {

    private val preferences = context.applicationContext
        .getSharedPreferences(
            "gastify_quick_actions",
            Context.MODE_PRIVATE
        )

    companion object {
        private const val KEY_ACTIONS = "actions"

        private const val MAX_AMOUNT_CENTS =
            999_999_999_999L
    }

    fun getAll(): List<QuickAction> {

        val json = preferences.getString(
            KEY_ACTIONS,
            "[]"
        ) ?: "[]"

        return try {

            val array = JSONArray(json)

            buildList {

                for (index in 0 until array.length()) {

                    val item = array.optJSONObject(index)
                        ?: continue

                    val action = QuickAction(
                        id = item.optString("id"),
                        userId = item.optString("userId"),
                        workspaceId = item.optString("workspaceId"),
                        name = item.optString("name"),
                        accountId = item.optString("accountId"),
                        categoryId = item.optString("categoryId"),
                        type = item.optString("type"),
                        amountCents = item.optLong(
                            "amountCents",
                            0L
                        ),
                        note = item.optString("note")
                    )

                    if (
                        action.id.isNotBlank() &&
                        action.userId.isNotBlank() &&
                        action.workspaceId.isNotBlank() &&
                        action.name.isNotBlank() &&
                        action.accountId.isNotBlank() &&
                        action.categoryId.isNotBlank() &&
                        action.type in listOf(
                            MovementType.INCOME.name,
                            MovementType.EXPENSE.name
                        ) &&
                        action.amountCents in
                            1L..MAX_AMOUNT_CENTS
                    ) {

                        add(action)
                    }
                }
            }

        } catch (_: Exception) {

            emptyList()
        }
    }

    fun getByWorkspace(
        userId: String,
        workspaceId: String
    ): List<QuickAction> {

        return getAll().filter {
            it.userId == userId &&
                it.workspaceId == workspaceId
        }
    }

    fun save(action: QuickAction) {

        require(action.id.isNotBlank()) {
            "El identificador es obligatorio."
        }

        require(
            action.userId.isNotBlank() &&
                action.workspaceId.isNotBlank()
        ) {
            "El espacio no es válido."
        }

        require(
            action.name.isNotBlank() &&
                action.name.length <= 60
        ) {
            "El nombre debe tener entre 1 y 60 caracteres."
        }

        require(
            action.accountId.isNotBlank() &&
                action.categoryId.isNotBlank()
        ) {
            "Selecciona una cuenta y una categoría."
        }

        require(
            action.type == MovementType.INCOME.name ||
                action.type == MovementType.EXPENSE.name
        ) {
            "El tipo de movimiento no es válido."
        }

        require(
            action.amountCents in 1L..MAX_AMOUNT_CENTS
        ) {
            "El importe no es válido."
        }

        require(action.note.length <= 200) {
            "La descripción es demasiado larga."
        }

        val existing = getAll()
            .firstOrNull { it.id == action.id }

        require(
            existing == null ||
                (
                    existing.userId == action.userId &&
                    existing.workspaceId == action.workspaceId
                )
        ) {
            "No puedes modificar un acceso de otro espacio."
        }

        val updated = getAll()
            .filterNot { it.id == action.id }
            .toMutableList()

        updated.add(action)

        saveAll(updated)
    }

    fun delete(
        userId: String,
        workspaceId: String,
        actionId: String
    ) {

        val updated = getAll().filterNot {
            it.id == actionId &&
                it.userId == userId &&
                it.workspaceId == workspaceId
        }

        saveAll(updated)
    }

    private fun saveAll(
        actions: List<QuickAction>
    ) {

        val array = JSONArray()

        actions.forEach { action ->

            val item = JSONObject().apply {

                put("id", action.id)

                put("userId", action.userId)

                put("workspaceId", action.workspaceId)

                put("name", action.name)

                put("accountId", action.accountId)

                put("categoryId", action.categoryId)

                put("type", action.type)

                put(
                    "amountCents",
                    action.amountCents
                )

                put("note", action.note)
            }

            array.put(item)
        }

        val saved = preferences.edit()
            .putString(
                KEY_ACTIONS,
                array.toString()
            )
            .commit()

        check(saved) {
            "No se pudieron guardar los accesos rápidos."
        }
    }
}
