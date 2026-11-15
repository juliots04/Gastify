package com.example.gastify.data.model

import com.google.firebase.Timestamp

enum class WorkspaceType(val displayName: String) {
    PERSONAL("Personal"),
    BUSINESS("Negocio")
}

data class Workspace(
    val id: String = "",
    val name: String = "",
    val type: String = WorkspaceType.PERSONAL.name,
    val createdAt: Timestamp? = null
) {
    val isBusiness: Boolean
        get() = type.equals("BUSINESS", ignoreCase = true) || type.equals("NEGOCIO", ignoreCase = true)

    val typeLabel: String
        get() = if (isBusiness) "Negocio" else "Personal"
}
