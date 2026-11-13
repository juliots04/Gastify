package com.example.gastify.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.NotificationsActive

import androidx.compose.material3.*

import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable

import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

import com.example.gastify.data.model.Workspace

import com.example.gastify.ui.components.GastifyIcons
import com.example.gastify.ui.theme.*

@Composable
fun WorkspacesScreen(
    modifier: Modifier = Modifier,
    workspaces: List<Workspace>,
    isSaving: Boolean = false,
    onAddWorkspace: (String) -> Unit,
    onDeleteWorkspace: (String) -> Unit,
    onOpenCategories: (Workspace) -> Unit,
    onOpenAccounts: (Workspace) -> Unit,
    onOpenReminders: (Workspace) -> Unit
) {

    var showCreateDialog by rememberSaveable {
        mutableStateOf(false)
    }

    var workspaceName by rememberSaveable {
        mutableStateOf("")
    }

    var workspaceToDeleteId by rememberSaveable {
        mutableStateOf<String?>(null)
    }

    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(20.dp)
    ) {

        Spacer(Modifier.height(16.dp))

        Text(
            text = "Mis espacios",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )

        Spacer(Modifier.height(8.dp))

        Text(
            text = "Organiza tus finanzas en diferentes espacios.",
            fontSize = 14.sp,
            color = TextSecondary
        )

        Spacer(Modifier.height(28.dp))

        // ======================================
        // CREAR ESPACIO
        // ======================================

        Button(
            onClick = {

                workspaceName = ""
                showCreateDialog = true
            },

            enabled = !isSaving,

            modifier = Modifier.fillMaxWidth(),

            shape = RoundedCornerShape(16.dp),

            colors = ButtonDefaults.buttonColors(
                containerColor = PrimaryGreen,
                contentColor = AmoledBlack
            ),

            contentPadding = PaddingValues(16.dp)
        ) {

            Icon(
                imageVector = GastifyIcons.add,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )

            Spacer(Modifier.width(8.dp))

            Text(
                text = "Crear nuevo espacio",
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(Modifier.height(30.dp))

        Text(
            text = "Espacios registrados (${workspaces.size})",
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold,
            color = TextPrimary
        )

        Spacer(Modifier.height(16.dp))

        // ======================================
        // LISTA DE ESPACIOS
        // ======================================

        if (workspaces.isEmpty()) {

            Card(
                modifier = Modifier.fillMaxWidth(),

                shape = RoundedCornerShape(20.dp),

                colors = CardDefaults.cardColors(
                    containerColor = SurfaceDark
                )
            ) {

                Column(
                    modifier = Modifier.padding(24.dp)
                ) {

                    Text(
                        text = "Todavía no tienes espacios",
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )

                    Spacer(Modifier.height(10.dp))

                    Text(
                        text = "Crea tu primer espacio para comenzar a organizar tus finanzas.",
                        fontSize = 13.sp,
                        lineHeight = 20.sp,
                        color = TextSecondary
                    )
                }
            }

        } else {

            workspaces.forEach { workspace ->

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),

                    shape = RoundedCornerShape(20.dp),

                    colors = CardDefaults.cardColors(
                        containerColor = SurfaceDark
                    )
                ) {

                    Column(
                        modifier = Modifier.padding(20.dp)
                    ) {

                        Text(
                            text = workspace.name,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary
                        )

                        Spacer(Modifier.height(6.dp))

                        Text(
                            text = "Espacio financiero",
                            fontSize = 12.sp,
                            color = TextSecondary
                        )

                        Spacer(Modifier.height(20.dp))

                        // ==========================
                        // CATEGORÍAS
                        // ==========================

                        Button(
                            onClick = {
                                onOpenCategories(workspace)
                            },

                            modifier = Modifier.fillMaxWidth(),

                            shape = RoundedCornerShape(12.dp),

                            colors = ButtonDefaults.buttonColors(
                                containerColor = CardDark,
                                contentColor = PrimaryGreen
                            )
                        ) {

                            Icon(
                                imageVector = GastifyIcons.categories,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )

                            Spacer(Modifier.width(8.dp))

                            Text(
                                text = "Administrar categorías",
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.weight(1f)
                            )

                            Icon(
                                imageVector = GastifyIcons.next,
                                contentDescription = null
                            )
                        }

                        Spacer(Modifier.height(10.dp))

                        // ==========================
                        // CUENTAS
                        // ==========================

                        Button(
                            onClick = {
                                onOpenAccounts(workspace)
                            },

                            modifier = Modifier.fillMaxWidth(),

                            shape = RoundedCornerShape(12.dp),

                            colors = ButtonDefaults.buttonColors(
                                containerColor = CardDark,
                                contentColor = TextPrimary
                            )
                        ) {

                            Icon(
                                imageVector = GastifyIcons.accounts,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )

                            Spacer(Modifier.width(8.dp))

                            Text(
                                text = "Administrar cuentas",
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.weight(1f)
                            )

                            Icon(
                                imageVector = GastifyIcons.next,
                                contentDescription = null
                            )
                        }

                        Spacer(Modifier.height(10.dp))

                        // ==========================
                        // RECORDATORIOS
                        // ==========================

                        Button(
                            onClick = {
                                onOpenReminders(workspace)
                            },

                            modifier = Modifier.fillMaxWidth(),

                            shape = RoundedCornerShape(12.dp),

                            colors = ButtonDefaults.buttonColors(
                                containerColor = CardDark,
                                contentColor = PrimaryGreen
                            )
                        ) {

                            Icon(
                                imageVector = Icons.Outlined.NotificationsActive,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )

                            Spacer(Modifier.width(8.dp))

                            Text(
                                text = "Recordatorios",
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.weight(1f)
                            )

                            Icon(
                                imageVector = GastifyIcons.next,
                                contentDescription = null
                            )
                        }

                        Spacer(Modifier.height(10.dp))

                        // ==========================
                        // ELIMINAR
                        // ==========================

                        TextButton(
                            onClick = {
                                workspaceToDeleteId = workspace.id
                            },

                            enabled = !isSaving
                        ) {

                            Icon(
                                imageVector = GastifyIcons.delete,
                                contentDescription = null,
                                tint = ExpenseRed,
                                modifier = Modifier.size(19.dp)
                            )

                            Spacer(Modifier.width(6.dp))

                            Text(
                                text = "Eliminar espacio",
                                color = ExpenseRed
                            )
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(24.dp))
    }

    // ==========================================
    // CREAR ESPACIO
    // ==========================================

    if (showCreateDialog) {

        val cleanName = workspaceName.trim()

        val alreadyExists = workspaces.any {
            it.name.equals(
                cleanName,
                ignoreCase = true
            )
        }

        AlertDialog(
            onDismissRequest = {

                if (!isSaving) {
                    showCreateDialog = false
                }
            },

            containerColor = SurfaceDark,

            title = {

                Text(
                    text = "Nuevo espacio",
                    color = TextPrimary
                )
            },

            text = {

                Column {

                    Text(
                        text = "Introduce un nombre para tu espacio.",
                        color = TextSecondary
                    )

                    Spacer(Modifier.height(16.dp))

                    OutlinedTextField(
                        value = workspaceName,

                        onValueChange = {
                            workspaceName = it.take(60)
                        },

                        label = {
                            Text("Nombre")
                        },

                        placeholder = {
                            Text("Ejemplo: Mis pasajes")
                        },

                        singleLine = true,

                        isError = alreadyExists,

                        modifier = Modifier.fillMaxWidth()
                    )

                    if (alreadyExists) {

                        Spacer(Modifier.height(8.dp))

                        Text(
                            text = "Ya existe un espacio con ese nombre.",
                            color = ExpenseRed,
                            fontSize = 12.sp
                        )
                    }
                }
            },

            confirmButton = {

                TextButton(
                    enabled = cleanName.isNotEmpty() &&
                        !alreadyExists &&
                        !isSaving,

                    onClick = {

                        onAddWorkspace(cleanName)

                        workspaceName = ""

                        showCreateDialog = false
                    }
                ) {

                    Text(
                        text = "Crear",
                        color = PrimaryGreen
                    )
                }
            },

            dismissButton = {

                TextButton(
                    onClick = {
                        showCreateDialog = false
                    },

                    enabled = !isSaving
                ) {

                    Text(
                        text = "Cancelar",
                        color = TextSecondary
                    )
                }
            }
        )
    }

    // ==========================================
    // ELIMINAR ESPACIO
    // ==========================================

    val workspaceToDelete = workspaces.firstOrNull {
        it.id == workspaceToDeleteId
    }

    if (workspaceToDelete != null) {

        AlertDialog(
            onDismissRequest = {
                workspaceToDeleteId = null
            },

            containerColor = SurfaceDark,

            title = {

                Text(
                    text = "Eliminar espacio",
                    color = TextPrimary
                )
            },

            text = {

                Text(
                    text = "¿Seguro que deseas eliminar \"${workspaceToDelete.name}\"?\n\nPor ahora, elimina únicamente espacios vacíos.",
                    color = TextSecondary
                )
            },

            confirmButton = {

                TextButton(
                    enabled = !isSaving,

                    onClick = {

                        onDeleteWorkspace(
                            workspaceToDelete.name
                        )

                        workspaceToDeleteId = null
                    }
                ) {

                    Text(
                        text = "Eliminar",
                        color = ExpenseRed
                    )
                }
            },

            dismissButton = {

                TextButton(
                    onClick = {
                        workspaceToDeleteId = null
                    }
                ) {

                    Text(
                        text = "Cancelar",
                        color = TextSecondary
                    )
                }
            }
        )
    }
}
