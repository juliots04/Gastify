package com.example.gastify.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.PointOfSale

import androidx.compose.material3.*

import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable

import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
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

    LazyColumn(modifier = modifier, contentPadding = PaddingValues(20.dp)) {
        item {
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

            modifier = Modifier.wrapContentWidth(),

            shape = RoundedCornerShape(16.dp),

            colors = ButtonDefaults.buttonColors(
                containerColor = Color.White.copy(alpha = 0.09f),
                contentColor = Color.White
            ),

            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp)
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

        }

        if (workspaces.isEmpty()) {

            item {
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

            }
        } else {

            items(workspaces, key = { it.id }) { workspace ->

                var menuExpanded by remember(workspace.id) { mutableStateOf(false) }
                Card(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceDark)
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(workspace.name, fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Spacer(Modifier.height(4.dp))
                                Text("Espacio financiero", fontSize = 12.sp, color = TextSecondary)
                            }
                            Box {
                                IconButton(onClick = { menuExpanded = true }, enabled = !isSaving) {
                                    Icon(Icons.Outlined.MoreVert, contentDescription = "Opciones del espacio", tint = TextSecondary)
                                }
                                DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }, containerColor = CardDark) {
                                    DropdownMenuItem(
                                        text = { Text("Eliminar espacio", color = ExpenseRed) },
                                        leadingIcon = { Icon(GastifyIcons.delete, contentDescription = null, tint = ExpenseRed) },
                                        onClick = { menuExpanded = false; workspaceToDeleteId = workspace.id }
                                    )
                                }
                            }
                        }
                        Spacer(Modifier.height(16.dp))
                        val actions = listOf(
                            Triple("Categorías", GastifyIcons.categories, { onOpenCategories(workspace) }),
                            Triple("Cuentas", GastifyIcons.accounts, { onOpenAccounts(workspace) }),
                            Triple("Recordatorios", Icons.Outlined.NotificationsActive, { onOpenReminders(workspace) }),
                            Triple("Presupuestos", GastifyIcons.statistics, { onOpenBudgets(workspace) }),
                            Triple("Inventario", Icons.Outlined.Inventory2, { onOpenInventory(workspace) }),
                            Triple("Compras y ventas", Icons.Outlined.PointOfSale, { onOpenCommerce(workspace) }),
                            Triple("Rentabilidad", GastifyIcons.statistics, { onOpenProfitability(workspace) })
                        )
                        actions.chunked(2).forEachIndexed { index, row ->
                            if (index > 0) Spacer(Modifier.height(8.dp))
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                row.forEach { (title, icon, action) ->
                                    WorkspaceActionButton(title, icon, action, Modifier.weight(1f))
                                }
                                if (row.size == 1) Spacer(Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
        }

        item { Spacer(Modifier.height(24.dp)) }
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
private fun WorkspaceActionButton(title: String, icon: ImageVector, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.clip(RoundedCornerShape(14.dp))
            .background(Color.White.copy(alpha = 0.045f))
            .clickable(onClick = onClick).padding(horizontal = 12.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = PrimaryGreen, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(10.dp))
        Text(title, color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}