package com.example.gastify.data.local

import android.content.Context

enum class AppStartSection {
    HOME,
    MOVEMENTS,
    WORKSPACES,
    STATISTICS,
    SETTINGS
}

data class AppPreferences(
    val startSection: AppStartSection = AppStartSection.HOME,
    val defaultWorkspaceId: String? = null,
    val rememberLastWorkspace: Boolean = true,
    val lastWorkspaceId: String? = null
) {

    fun preferredWorkspaceId(
        availableIds: Set<String>
    ): String? {

        if (rememberLastWorkspace) {

            val last = lastWorkspaceId

            if (
                last != null &&
                last in availableIds
            ) {
                return last
            }
        }

        val defaultId = defaultWorkspaceId

        if (
            defaultId != null &&
            defaultId in availableIds
        ) {
            return defaultId
        }

        return null
    }
}

class AppPreferencesStore(
    context: Context,
    private val userId: String
) {

    private val preferences = context
        .applicationContext
        .getSharedPreferences(
            "gastify_app_preferences",
            Context.MODE_PRIVATE
        )

    init {

        require(userId.isNotBlank()) {
            "Usuario no válido."
        }
    }

    private fun key(
        name: String
    ): String {

        return "${userId}_$name"
    }

    // ==========================================
    // LEER CONFIGURACIÓN
    // ==========================================

    fun read(): AppPreferences {

        val savedSection = preferences.getString(
            key("start_section"),
            AppStartSection.HOME.name
        )

        val section = AppStartSection.entries
            .firstOrNull {

                it.name == savedSection

            } ?: AppStartSection.HOME

        return AppPreferences(

            startSection = section,

            defaultWorkspaceId = preferences
                .getString(
                    key("default_workspace"),
                    null
                )
                ?.takeIf {
                    it.isNotBlank()
                },

            rememberLastWorkspace = preferences
                .getBoolean(
                    key("remember_last_workspace"),
                    true
                ),

            lastWorkspaceId = preferences
                .getString(
                    key("last_workspace"),
                    null
                )
                ?.takeIf {
                    it.isNotBlank()
                }
        )
    }

    // ==========================================
    // PANTALLA DE INICIO
    // ==========================================

    fun setStartSection(
        section: AppStartSection
    ): Boolean {

        return preferences.edit()
            .putString(
                key("start_section"),
                section.name
            )
            .commit()
    }

    // ==========================================
    // ESPACIO PREDETERMINADO
    // ==========================================

    fun setDefaultWorkspace(
        workspaceId: String?
    ): Boolean {

        require(
            workspaceId == null ||
                (
                    workspaceId.isNotBlank() &&
                    '/' !in workspaceId
                )
        ) {
            "Espacio no válido."
        }

        val editor = preferences.edit()

        if (workspaceId == null) {

            editor.remove(
                key("default_workspace")
            )

        } else {

            editor.putString(
                key("default_workspace"),
                workspaceId
            )
        }

        return editor.commit()
    }

    // ==========================================
    // RECORDAR ÚLTIMO ESPACIO
    // ==========================================

    fun setRememberLastWorkspace(
        enabled: Boolean
    ): Boolean {

        val editor = preferences.edit()
            .putBoolean(
                key("remember_last_workspace"),
                enabled
            )

        if (!enabled) {

            editor.remove(
                key("last_workspace")
            )
        }

        return editor.commit()
    }

    // ==========================================
    // REGISTRAR ESPACIO UTILIZADO
    // ==========================================

    fun recordWorkspaceOpened(
        workspaceId: String
    ): Boolean {

        require(
            workspaceId.isNotBlank() &&
                '/' !in workspaceId
        ) {
            "Espacio no válido."
        }

        if (!read().rememberLastWorkspace) {
            return true
        }

        return preferences.edit()
            .putString(
                key("last_workspace"),
                workspaceId
            )
            .commit()
    }
}
