package com.example.gastify.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.PointOfSale

import androidx.compose.material3.*

import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable

import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
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
    onOpenReminders: (Workspace) -> Unit,
    onOpenBudgets: (Workspace) -> Unit,
    onOpenInventory: (Workspace) -> Unit,
    onOpenCommerce: (Workspace) -> Unit,
    onOpenProfitability: (Workspace) -> Unit
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
            .verticalScroll(
                rememberScrollState()
            )
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
                contentDescription = null
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
                        text = "Crea tu primer espacio para comenzar.",
                        color = TextSecondary,
                        fontSize = 13.sp
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

                        WorkspaceActionButton(
                            title = "Administrar categorías",
                            icon = GastifyIcons.categories,
                            onClick = {
                                onOpenCategories(workspace)
                            }
                        )

                        Spacer(Modifier.height(10.dp))

                        WorkspaceActionButton(
                            title = "Administrar cuentas",
                            icon = GastifyIcons.accounts,
                            onClick = {
                                onOpenAccounts(workspace)
                            }
                        )

                        Spacer(Modifier.height(10.dp))

                        WorkspaceActionButton(
                            title = "Recordatorios",
                            icon = Icons.Outlined.NotificationsActive,
                            onClick = {
                                onOpenReminders(workspace)
                            }
                        )

                        Spacer(Modifier.height(10.dp))

                        WorkspaceActionButton(
                            title = "Presupuestos",
                            icon = GastifyIcons.statistics,
                            onClick = {
                                onOpenBudgets(workspace)
                            }
                        )

                        Spacer(Modifier.height(10.dp))

                        WorkspaceActionButton(
                            title = "Inventario",
                            icon = Icons.Outlined.Inventory2,
                            onClick = {
                                onOpenInventory(workspace)
                            }
                        )

                        Spacer(Modifier.height(10.dp))

                        WorkspaceActionButton(
                            title = "Compras y ventas",
                            icon = Icons.Outlined.PointOfSale,
                            onClick = {
                                onOpenCommerce(workspace)
                            }
                        )

                        Spacer(Modifier.height(10.dp))

                        WorkspaceActionButton(
                            title = "Rentabilidad",
                            icon = GastifyIcons.statistics,
                            onClick = {
                                onOpenProfitability(workspace)
                            }
                        )

                        Spacer(Modifier.height(10.dp))

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
                Text("Nuevo espacio")
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
                            Text("Ejemplo: Mi negocio")
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
                    }
                ) {

                    Text("Cancelar")
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
                Text("Eliminar espacio")
            },

            text = {

                Text(
                    text = "¿Seguro que deseas eliminar \"${workspaceToDelete.name}\"?\n\nElimina únicamente espacios vacíos. Los datos asociados no se eliminan automáticamente.",
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

                    Text("Cancelar")
                }
            }
        )
    }
}

// ==========================================
// BOTÓN DE ACCIÓN
// ==========================================

@Composable
private fun WorkspaceActionButton(
    title: String,
    icon: ImageVector,
    onClick: () -> Unit
) {

    Button(
        onClick = onClick,

        modifier = Modifier.fillMaxWidth(),

        shape = RoundedCornerShape(12.dp),

        colors = ButtonDefaults.buttonColors(
            containerColor = CardDark,
            contentColor = PrimaryGreen
        )
    ) {

        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(20.dp)
        )

        Spacer(Modifier.width(8.dp))

        Text(
            text = title,
            modifier = Modifier.weight(1f),
            fontWeight = FontWeight.Medium
        )

        Icon(
            imageVector = GastifyIcons.next,
            contentDescription = null
        )
    }
}
