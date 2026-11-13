package com.example.gastify.data.local

import android.content.Context

import com.example.gastify.data.model.PassageReminder

import org.json.JSONArray
import org.json.JSONObject

class ReminderStore(
    context: Context
) {

    private val preferences = context
        .applicationContext
        .getSharedPreferences(
            "gastify_reminders",
            Context.MODE_PRIVATE
        )

    companion object {

        private const val KEY_REMINDERS = "reminders"
    }

    // ==========================================
    // OBTENER TODOS LOS RECORDATORIOS
    // ==========================================

    fun getAll(): List<PassageReminder> {

        val json = preferences.getString(
            KEY_REMINDERS,
            "[]"
        ) ?: "[]"

        return try {

            val array = JSONArray(json)

            val reminders = mutableListOf<PassageReminder>()

            for (index in 0 until array.length()) {

                val item = array.optJSONObject(index)
                    ?: continue

                val daysArray = item.optJSONArray("daysOfWeek")
                    ?: JSONArray()

                val days = mutableSetOf<Int>()

                for (dayIndex in 0 until daysArray.length()) {

                    val day = daysArray.optInt(dayIndex)

                    if (day in 1..7) {
                        days.add(day)
                    }
                }

                val reminder = PassageReminder(

                    id = item.optString("id"),

                    userId = item.optString("userId"),

                    workspaceId = item.optString("workspaceId"),

                    accountId = item.optString("accountId"),

                    categoryId = item.optString("categoryId"),

                    title = item.optString("title"),

                    amountCents = item.optLong(
                        "amountCents",
                        280L
                    ),

                    hour = item.optInt("hour", 7),

                    minute = item.optInt("minute", 30),

                    daysOfWeek = days,

                    enabled = item.optBoolean(
                        "enabled",
                        true
                    )
                )

                if (reminder.id.isNotBlank()) {

                    reminders.add(reminder)
                }
            }

            reminders

        } catch (_: Exception) {

            emptyList()
        }
    }

    // ==========================================
    // RECORDATORIOS DE UN ESPACIO
    // ==========================================

    fun getByWorkspace(
        userId: String,
        workspaceId: String
    ): List<PassageReminder> {

        return getAll().filter {

            it.userId == userId &&
            it.workspaceId == workspaceId
        }
    }

    // ==========================================
    // BUSCAR RECORDATORIO
    // ==========================================

    fun getById(
        reminderId: String
    ): PassageReminder? {

        return getAll().firstOrNull {
            it.id == reminderId
        }
    }

    // ==========================================
    // GUARDAR RECORDATORIO
    // ==========================================

    fun save(
        reminder: PassageReminder
    ) {

        require(reminder.id.isNotBlank()) {
            "El identificador es obligatorio."
        }

        require(reminder.userId.isNotBlank()) {
            "El usuario es obligatorio."
        }

        require(reminder.workspaceId.isNotBlank()) {
            "El espacio es obligatorio."
        }

        require(reminder.accountId.isNotBlank()) {
            "La cuenta es obligatoria."
        }

        require(reminder.categoryId.isNotBlank()) {
            "La categoría es obligatoria."
        }

        require(reminder.title.isNotBlank()) {
            "El nombre es obligatorio."
        }

        require(reminder.amountCents > 0L) {
            "El importe debe ser mayor que cero."
        }

        require(reminder.hour in 0..23) {
            "La hora no es válida."
        }

        require(reminder.minute in 0..59) {
            "Los minutos no son válidos."
        }

        require(
            reminder.daysOfWeek.isNotEmpty() &&
            reminder.daysOfWeek.all { it in 1..7 }
        ) {
            "Selecciona al menos un día válido."
        }

        val reminders = getAll()
            .filterNot {
                it.id == reminder.id
            }
            .toMutableList()

        reminders.add(reminder)

        saveAll(reminders)
    }

    // ==========================================
    // ELIMINAR RECORDATORIO
    // ==========================================

    fun delete(
        reminderId: String
    ) {

        val reminders = getAll().filterNot {
            it.id == reminderId
        }

        saveAll(reminders)
    }

    // ==========================================
    // GUARDAR LISTA COMPLETA
    // ==========================================

    private fun saveAll(
        reminders: List<PassageReminder>
    ) {

        val array = JSONArray()

        reminders.forEach { reminder ->

            val daysArray = JSONArray()

            reminder.daysOfWeek
                .sorted()
                .forEach { day ->

                    daysArray.put(day)
                }

            val item = JSONObject().apply {

                put("id", reminder.id)

                put("userId", reminder.userId)

                put("workspaceId", reminder.workspaceId)

                put("accountId", reminder.accountId)

                put("categoryId", reminder.categoryId)

                put("title", reminder.title)

                put("amountCents", reminder.amountCents)

                put("hour", reminder.hour)

                put("minute", reminder.minute)

                put("daysOfWeek", daysArray)

                put("enabled", reminder.enabled)
            }

            array.put(item)
        }

        val saved = preferences.edit()
            .putString(
                KEY_REMINDERS,
                array.toString()
            )
            .commit()

        check(saved) {
            "No se pudieron guardar los recordatorios."
        }
    }
}
