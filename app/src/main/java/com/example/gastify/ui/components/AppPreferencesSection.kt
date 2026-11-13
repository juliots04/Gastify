package com.example.gastify.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.CheckCircleOutline

import androidx.compose.material3.*

import androidx.compose.runtime.*

import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

import com.example.gastify.data.local.AppPreferences
import com.example.gastify.data.local.AppPreferencesStore
import com.example.gastify.data.local.AppStartSection

import com.example.gastify.data.model.Workspace

import com.example.gastify.ui.theme.*

private fun sectionTitle(
    section: AppStartSection
): String {

    return when (section) {

        AppStartSection.HOME ->
            "Inicio"

        AppStartSection.MOVEMENTS ->
            "Movimientos"

        AppStartSection.WORKSPACES ->
            "Espacios"

        AppStartSection.STATISTICS ->
            "Estadísticas"

        AppStartSection.SETTINGS ->
            "Ajustes"
    }
}

@Composable
fun AppPreferencesSection(
    workspaces: List<Workspace>,
    preferences: AppPreferences,
    store: AppPreferencesStore,
    onPreferencesChanged: (AppPreferences) -> Unit
) {

    var startMenuExpanded by remember {
        mutableStateOf(false)
    }

    var workspaceMenuExpanded by remember {
        mutableStateOf(false)
    }

    var errorMessage by remember {
        mutableStateOf<String?>(null)
    }

    // ==========================================
    // ACTUALIZAR PREFERENCIAS
    // ==========================================

    fun updatePreferences(
        saved: Boolean
    ) {

        if (saved) {

            errorMessage = null

            onPreferencesChanged(
                store.read()
            )

        } else {

            errorMessage =
                "No se pudieron guardar las preferencias."
        }
    }

    // ==========================================
    // CAMBIAR PANTALLA INICIAL
    // ==========================================

    fun changeStartSection(
        section: AppStartSection
    ) {

        updatePreferences(
            store.setStartSection(section)
        )
    }

    // ==========================================
    // CAMBIAR ESPACIO PREDETERMINADO
    // ==========================================

    fun changeDefaultWorkspace(
        workspaceId: String?
    ) {

        updatePreferences(
            store.setDefaultWorkspace(
                workspaceId
            )
        )
    }

    // ==========================================
    // CAMBIAR RECORDATORIO DE ESPACIO
    // ==========================================

    fun changeRememberLastWorkspace(
        enabled: Boolean
    ) {

        updatePreferences(
            store.setRememberLastWorkspace(
                enabled
            )
        )
    }

    // ==========================================
    // OBTENER ESPACIOS
    // ==========================================

    val defaultWorkspace = workspaces
        .firstOrNull {

            it.id == preferences.defaultWorkspaceId
        }

    val lastWorkspace = workspaces
        .firstOrNull {

            it.id == preferences.lastWorkspaceId
        }

    val availableIds = workspaces
        .map {
            it.id
        }
        .toSet()

    val preferredWorkspaceId =
        preferences.preferredWorkspaceId(
            availableIds
        )

    val preferredWorkspace = workspaces
        .firstOrNull {

            it.id == preferredWorkspaceId
        }

    // ==========================================
    // INTERFAZ
    // ==========================================

    Column(
        modifier = Modifier.fillMaxWidth()
    ) {

        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {

            Icon(
                imageVector = Icons.Outlined.Home,

                contentDescription = null,

                tint = PrimaryGreen,

                modifier = Modifier.size(26.dp)
            )

            Spacer(
                Modifier.width(10.dp)
            )

            Text(
                text = "Preferencias",

                fontSize = 20.sp,

                fontWeight = FontWeight.Bold,

                color = TextPrimary
            )
        }

        Spacer(
            Modifier.height(8.dp)
        )

        Text(
            text = "Personaliza cómo se abre GASTIFY y qué espacio utilizar.",

            fontSize = 13.sp,

            lineHeight = 19.sp,

            color = TextSecondary
        )

        Spacer(
            Modifier.height(20.dp)
        )

        // ======================================
        // PANTALLA DE INICIO
        // ======================================

        PreferenceCard(
            title = "Pantalla de inicio",

            description =
                "Elige qué sección se mostrará al abrir GASTIFY.",

            icon = Icons.Outlined.Home
        ) {

            Box(
                modifier = Modifier.fillMaxWidth()
            ) {

                OutlinedButton(
                    onClick = {

                        startMenuExpanded = true
                    },

                    modifier = Modifier.fillMaxWidth(),

                    shape = RoundedCornerShape(12.dp)
                ) {

                    Text(
                        text = sectionTitle(
                            preferences.startSection
                        ),

                        modifier = Modifier.weight(1f),

                        color = TextPrimary
                    )

                    Icon(
                        imageVector = GastifyIcons.expand,

                        contentDescription = null,

                        tint = PrimaryGreen
                    )
                }

                DropdownMenu(
                    expanded = startMenuExpanded,

                    onDismissRequest = {

                        startMenuExpanded = false
                    },

                    containerColor = SurfaceDark
                ) {

                    AppStartSection.entries.forEach {
                            section ->

                        DropdownMenuItem(
                            text = {

                                Row(
                                    verticalAlignment =
                                        Alignment.CenterVertically
                                ) {

                                    Text(
                                        text = sectionTitle(
                                            section
                                        ),

                                        modifier = Modifier.weight(
                                            1f
                                        ),

                                        color = TextPrimary
                                    )

                                    if (
                                        preferences.startSection ==
                                        section
                                    ) {

                                        Icon(
                                            imageVector =
                                                Icons.Outlined.CheckCircleOutline,

                                            contentDescription = null,

                                            tint = PrimaryGreen
                                        )
                                    }
                                }
                            },

                            onClick = {

                                changeStartSection(
                                    section
                                )

                                startMenuExpanded = false
                            }
                        )
                    }
                }
            }

            Spacer(
                Modifier.height(10.dp)
            )

            Text(
                text = "Se aplicará la próxima vez que abras la aplicación.",

                color = TextSecondary,

                fontSize = 12.sp,

                lineHeight = 18.sp
            )
        }

        Spacer(
            Modifier.height(14.dp)
        )

        // ======================================
        // ESPACIO PREDETERMINADO
        // ======================================

        PreferenceCard(
            title = "Espacio predeterminado",

            description =
                "Selecciona el espacio que GASTIFY utilizará inicialmente.",

            icon = Icons.Outlined.Folder
        ) {

            Box(
                modifier = Modifier.fillMaxWidth()
            ) {

                OutlinedButton(
                    onClick = {

                        workspaceMenuExpanded = true
                    },

                    modifier = Modifier.fillMaxWidth(),

                    shape = RoundedCornerShape(12.dp)
                ) {

                    Text(
                        text = defaultWorkspace?.name
                            ?: "Sin espacio fijo",

                        modifier = Modifier.weight(1f),

                        color = TextPrimary,

                        maxLines = 1,

                        overflow = TextOverflow.Ellipsis
                    )

                    Icon(
                        imageVector = GastifyIcons.expand,

                        contentDescription = null,

                        tint = PrimaryGreen
                    )
                }

                DropdownMenu(
                    expanded = workspaceMenuExpanded,

                    onDismissRequest = {

                        workspaceMenuExpanded = false
                    },

                    containerColor = SurfaceDark
                ) {

                    DropdownMenuItem(
                        text = {

                            Text(
                                text = "Sin espacio fijo",

                                color = TextPrimary
                            )
                        },

                        onClick = {

                            changeDefaultWorkspace(
                                null
                            )

                            workspaceMenuExpanded = false
                        }
                    )

                    workspaces.forEach { workspace ->

                        DropdownMenuItem(
                            text = {

                                Row(
                                    verticalAlignment =
                                        Alignment.CenterVertically
                                ) {

                                    Text(
                                        text = workspace.name,

                                        modifier = Modifier.weight(
                                            1f
                                        ),

                                        color = TextPrimary,

                                        maxLines = 1,

                                        overflow =
                                            TextOverflow.Ellipsis
                                    )

                                    if (
                                        preferences.defaultWorkspaceId ==
                                        workspace.id
                                    ) {

                                        Icon(
                                            imageVector =
                                                Icons.Outlined.CheckCircleOutline,

                                            contentDescription = null,

                                            tint = PrimaryGreen
                                        )
                                    }
                                }
                            },

                            onClick = {

                                changeDefaultWorkspace(
                                    workspace.id
                                )

                                workspaceMenuExpanded = false
                            }
                        )
                    }
                }
            }

            Spacer(
                Modifier.height(10.dp)
            )

            Text(
                text = if (workspaces.isEmpty()) {

                    "Primero debes crear un espacio."

                } else {

                    "Si no seleccionas ninguno, GASTIFY utilizará el primer espacio disponible."
                },

                color = TextSecondary,

                fontSize = 12.sp,

                lineHeight = 18.sp
            )
        }

        Spacer(
            Modifier.height(14.dp)
        )

        // ======================================
        // RECORDAR ÚLTIMO ESPACIO
        // ======================================

        PreferenceCard(
            title = "Recordar último espacio",

            description =
                "Al volver a abrir GASTIFY, utilizará el último espacio que seleccionaste.",

            icon = Icons.Outlined.History
        ) {

            Row(
                modifier = Modifier.fillMaxWidth(),

                verticalAlignment = Alignment.CenterVertically
            ) {

                Column(
                    modifier = Modifier.weight(1f)
                ) {

                    Text(
                        text = if (
                            preferences.rememberLastWorkspace
                        ) {

                            "Activado"

                        } else {

                            "Desactivado"
                        },

                        color = TextPrimary,

                        fontWeight = FontWeight.SemiBold,

                        fontSize = 14.sp
                    )

                    Spacer(
                        Modifier.height(5.dp)
                    )

                    Text(
                        text = "Último espacio: ${
                            lastWorkspace?.name ?: "Ninguno"
                        }",

                        color = TextSecondary,

                        fontSize = 12.sp,

                        maxLines = 2
                    )
                }

                Switch(
                    checked =
                        preferences.rememberLastWorkspace,

                    onCheckedChange = {

                        changeRememberLastWorkspace(it)
                    },

                    colors = SwitchDefaults.colors(
                        checkedThumbColor = AmoledBlack,
                        checkedTrackColor = PrimaryGreen,
                        uncheckedThumbColor = TextSecondary,
                        uncheckedTrackColor = CardDark
                    )
                )
            }
        }

        Spacer(
            Modifier.height(14.dp)
        )

        // ======================================
        // ESPACIO QUE SE UTILIZARÁ
        // ======================================

        Card(
            modifier = Modifier.fillMaxWidth(),

            shape = RoundedCornerShape(18.dp),

            colors = CardDefaults.cardColors(
                containerColor = CardDark
            )
        ) {

            Row(
                modifier = Modifier.padding(18.dp),

                verticalAlignment = Alignment.CenterVertically
            ) {

                Icon(
                    imageVector =
                        Icons.Outlined.CheckCircleOutline,

                    contentDescription = null,

                    tint = PrimaryGreen
                )

                Spacer(
                    Modifier.width(12.dp)
                )

                Column {

                    Text(
                        text = "Espacio de inicio",

                        color = TextSecondary,

                        fontSize = 12.sp
                    )

                    Spacer(
                        Modifier.height(6.dp)
                    )

                    Text(
                        text = preferredWorkspace?.name
                            ?: workspaces.firstOrNull()?.name
                            ?: "Sin espacios disponibles",

                        color = TextPrimary,

                        fontSize = 15.sp,

                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        if (errorMessage != null) {

            Spacer(
                Modifier.height(12.dp)
            )

            Text(
                text = errorMessage.orEmpty(),

                color = ExpenseRed,

                fontSize = 12.sp
            )
        }

        Spacer(
            Modifier.height(16.dp)
        )

        Text(
            text = "Todas las preferencias se guardan automáticamente. No necesitas confirmar los cambios.",

            color = TextSecondary,

            fontSize = 12.sp,

            lineHeight = 19.sp
        )
    }
}

// ==========================================
// TARJETA REUTILIZABLE
// ==========================================

@Composable
private fun PreferenceCard(
    title: String,
    description: String,
    icon: ImageVector,
    content: @Composable ColumnScope.() -> Unit
) {

    Card(
        modifier = Modifier.fillMaxWidth(),

        shape = RoundedCornerShape(20.dp),

        colors = CardDefaults.cardColors(
            containerColor = SurfaceDark
        )
    ) {

        Column(
            modifier = Modifier.padding(20.dp)
        ) {

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {

                Icon(
                    imageVector = icon,

                    contentDescription = null,

                    tint = PrimaryGreen,

                    modifier = Modifier.size(23.dp)
                )

                Spacer(
                    Modifier.width(10.dp)
                )

                Text(
                    text = title,

                    color = TextPrimary,

                    fontSize = 16.sp,

                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(
                Modifier.height(10.dp)
            )

            Text(
                text = description,

                color = TextSecondary,

                fontSize = 12.sp,

                lineHeight = 18.sp
            )

            Spacer(
                Modifier.height(16.dp)
            )

            content()
        }
    }
}
