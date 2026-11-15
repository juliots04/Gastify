package com.example.gastify.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.PointOfSale
import androidx.compose.material.icons.outlined.Storefront

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
import com.example.gastify.data.model.WorkspaceType
import com.example.gastify.ui.components.GastifyIcons
import com.example.gastify.ui.theme.*

@Composable
fun WorkspacesScreen(
    modifier: Modifier = Modifier,
    workspaces: List<Workspace>,
    isSaving: Boolean = false,
    onAddWorkspace: (name: String, type: String) -> Unit,
    onUpdateWorkspace: (id: String, name: String, type: String) -> Unit = { _, _, _ -> },
    onDeleteWorkspace: (String) -> Unit,
    onOpenCategories: (Workspace) -> Unit = {},
    onOpenAccounts: (Workspace) -> Unit = {},
    onOpenReminders: (Workspace) -> Unit = {},
    onOpenBudgets: (Workspace) -> Unit = {},
    onOpenInventory: (Workspace) -> Unit = {},
    onOpenCommerce: (Workspace) -> Unit = {},
    onOpenProfitability: (Workspace) -> Unit = {}
) {

    var showCreateDialog by rememberSaveable {
        mutableStateOf(false)
    }

    var workspaceName by rememberSaveable {
        mutableStateOf("")
    }

    var workspaceType by rememberSaveable {
        mutableStateOf(WorkspaceType.PERSONAL.name)
    }

    var workspaceToEdit by remember {
        mutableStateOf<Workspace?>(null)
    }

    var editName by rememberSaveable {
        mutableStateOf("")
    }

    var editType by rememberSaveable {
        mutableStateOf(WorkspaceType.PERSONAL.name)
    }

    var workspaceToDeleteId by rememberSaveable {
        mutableStateOf<String?>(null)
    }

    LazyColumn(modifier = modifier, contentPadding = PaddingValues(20.dp)) {
        item {
            Spacer(Modifier.height(16.dp))

            Text(
                text = "Espacios de trabajo",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )

            Spacer(Modifier.height(8.dp))

            Text(
                text = "Gestiona tus espacios Personal y Negocio. Configura su tipo y módulos.",
                fontSize = 14.sp,
                color = TextSecondary
            )

            Spacer(Modifier.height(24.dp))

            Button(
                onClick = {
                    workspaceName = ""
                    workspaceType = WorkspaceType.PERSONAL.name
                    showCreateDialog = true
                },
                enabled = !isSaving,
                modifier = Modifier.wrapContentWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = PrimaryGreen,
                    contentColor = AmoledBlack
                ),
                contentPadding = PaddingValues(horizontal = 18.dp, vertical = 12.dp)
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

            Spacer(Modifier.height(28.dp))

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
                    colors = CardDefaults.cardColors(containerColor = SurfaceDark)
                ) {
                    Column(modifier = Modifier.padding(24.dp)) {
                        Text(
                            text = "Todavía no tienes espacios",
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary
                        )

                        Spacer(Modifier.height(10.dp))

                        Text(
                            text = "Crea tu primer espacio para comenzar a controlar tus finanzas.",
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
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 14.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceDark)
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(
                                        if (workspace.isBusiness) Color(0xFF64B5F6).copy(alpha = 0.16f)
                                        else PrimaryGreen.copy(alpha = 0.16f)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (workspace.isBusiness) Icons.Outlined.Storefront else Icons.Outlined.Person,
                                    contentDescription = null,
                                    tint = if (workspace.isBusiness) Color(0xFF64B5F6) else PrimaryGreen,
                                    modifier = Modifier.size(22.dp)
                                )
                            }

                            Spacer(Modifier.width(12.dp))

                            Column(Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = workspace.name,
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = TextPrimary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )

                                    Spacer(Modifier.width(8.dp))

                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = if (workspace.isBusiness) Color(0xFF64B5F6).copy(alpha = 0.18f) else PrimaryGreen.copy(alpha = 0.18f)
                                    ) {
                                        Text(
                                            text = workspace.typeLabel,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = if (workspace.isBusiness) Color(0xFF90CAF9) else PrimaryGreen,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }

                                Spacer(Modifier.height(4.dp))

                                Text(
                                    text = if (workspace.isBusiness) "Negocio • Todos los módulos habilitados" else "Personal • Cuentas, categorías, recordatorios y presupuestos",
                                    fontSize = 12.sp,
                                    color = TextSecondary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            Box {
                                IconButton(onClick = { menuExpanded = true }, enabled = !isSaving) {
                                    Icon(Icons.Outlined.MoreVert, contentDescription = "Opciones del espacio", tint = TextSecondary)
                                }
                                DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }, containerColor = CardDark) {
                                    DropdownMenuItem(
                                        text = { Text("Editar espacio", color = TextPrimary) },
                                        leadingIcon = { Icon(GastifyIcons.edit, contentDescription = null, tint = PrimaryGreen) },
                                        onClick = {
                                            menuExpanded = false
                                            workspaceToEdit = workspace
                                            editName = workspace.name
                                            editType = workspace.type
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Eliminar espacio", color = ExpenseRed) },
                                        leadingIcon = { Icon(GastifyIcons.delete, contentDescription = null, tint = ExpenseRed) },
                                        onClick = {
                                            menuExpanded = false
                                            workspaceToDeleteId = workspace.id
                                        }
                                    )
                                }
                            }
                        }

                        Spacer(Modifier.height(16.dp))

                        val actions = buildList {
                            add(Triple("Categorías", GastifyIcons.categories, { onOpenCategories(workspace) }))
                            add(Triple("Cuentas", GastifyIcons.accounts, { onOpenAccounts(workspace) }))
                            add(Triple("Recordatorios", Icons.Outlined.NotificationsActive, { onOpenReminders(workspace) }))
                            add(Triple("Presupuestos", GastifyIcons.statistics, { onOpenBudgets(workspace) }))
                            if (workspace.isBusiness) {
                                add(Triple("Inventario", Icons.Outlined.Inventory2, { onOpenInventory(workspace) }))
                                add(Triple("Compras y ventas", Icons.Outlined.PointOfSale, { onOpenCommerce(workspace) }))
                                add(Triple("Rentabilidad", GastifyIcons.statistics, { onOpenProfitability(workspace) }))
                            }
                        }

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
            it.name.equals(cleanName, ignoreCase = true)
        }

        AlertDialog(
            onDismissRequest = {
                if (!isSaving) {
                    showCreateDialog = false
                }
            },
            containerColor = SurfaceDark,
            title = {
                Text("Nuevo espacio", color = TextPrimary)
            },
            text = {
                Column {
                    Text(
                        text = "Introduce el nombre y elige el tipo de espacio.",
                        color = TextSecondary,
                        fontSize = 13.sp
                    )

                    Spacer(Modifier.height(16.dp))

                    OutlinedTextField(
                        value = workspaceName,
                        onValueChange = { workspaceName = it.take(60) },
                        label = { Text("Nombre") },
                        placeholder = { Text("Ej: Finanzas Personales o Mi Tienda") },
                        singleLine = true,
                        isError = alreadyExists,
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (alreadyExists) {
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = "Ya existe un espacio con ese nombre.",
                            color = ExpenseRed,
                            fontSize = 12.sp
                        )
                    }

                    Spacer(Modifier.height(18.dp))

                    Text(
                        text = "Tipo de espacio",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )

                    Spacer(Modifier.height(10.dp))

                    // Selector tipo Personal
                    WorkspaceTypeOption(
                        title = "Personal",
                        description = "Categorías, cuentas, recordatorios y presupuestos.",
                        icon = Icons.Outlined.Person,
                        isSelected = workspaceType == WorkspaceType.PERSONAL.name,
                        onClick = { workspaceType = WorkspaceType.PERSONAL.name }
                    )

                    Spacer(Modifier.height(8.dp))

                    // Selector tipo Negocio
                    WorkspaceTypeOption(
                        title = "Negocio",
                        description = "Todos los módulos + Inventario, compras/ventas y rentabilidad.",
                        icon = Icons.Outlined.Storefront,
                        isSelected = workspaceType == WorkspaceType.BUSINESS.name,
                        onClick = { workspaceType = WorkspaceType.BUSINESS.name }
                    )
                }
            },
            confirmButton = {
                Button(
                    enabled = cleanName.isNotEmpty() && !alreadyExists && !isSaving,
                    onClick = {
                        onAddWorkspace(cleanName, workspaceType)
                        workspaceName = ""
                        workspaceType = WorkspaceType.PERSONAL.name
                        showCreateDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PrimaryGreen,
                        contentColor = AmoledBlack
                    )
                ) {
                    Text("Crear", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }

    // ==========================================
    // EDITAR ESPACIO
    // ==========================================

    val editing = workspaceToEdit
    if (editing != null) {
        val cleanName = editName.trim()
        val alreadyExists = workspaces.any {
            it.id != editing.id && it.name.equals(cleanName, ignoreCase = true)
        }

        AlertDialog(
            onDismissRequest = {
                if (!isSaving) {
                    workspaceToEdit = null
                }
            },
            containerColor = SurfaceDark,
            title = {
                Text("Editar espacio", color = TextPrimary)
            },
            text = {
                Column {
                    Text(
                        text = "Modifica el nombre o cambia el tipo de este espacio.",
                        color = TextSecondary,
                        fontSize = 13.sp
                    )

                    Spacer(Modifier.height(16.dp))

                    OutlinedTextField(
                        value = editName,
                        onValueChange = { editName = it.take(60) },
                        label = { Text("Nombre") },
                        singleLine = true,
                        isError = alreadyExists,
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (alreadyExists) {
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = "Ya existe otro espacio con ese nombre.",
                            color = ExpenseRed,
                            fontSize = 12.sp
                        )
                    }

                    Spacer(Modifier.height(18.dp))

                    Text(
                        text = "Tipo de espacio",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )

                    Spacer(Modifier.height(10.dp))

                    WorkspaceTypeOption(
                        title = "Personal",
                        description = "Categorías, cuentas, recordatorios y presupuestos.",
                        icon = Icons.Outlined.Person,
                        isSelected = editType.equals(WorkspaceType.PERSONAL.name, ignoreCase = true),
                        onClick = { editType = WorkspaceType.PERSONAL.name }
                    )

                    Spacer(Modifier.height(8.dp))

                    WorkspaceTypeOption(
                        title = "Negocio",
                        description = "Todos los módulos + Inventario, compras/ventas y rentabilidad.",
                        icon = Icons.Outlined.Storefront,
                        isSelected = editType.equals(WorkspaceType.BUSINESS.name, ignoreCase = true) || editType.equals("NEGOCIO", ignoreCase = true),
                        onClick = { editType = WorkspaceType.BUSINESS.name }
                    )
                }
            },
            confirmButton = {
                Button(
                    enabled = cleanName.isNotEmpty() && !alreadyExists && !isSaving,
                    onClick = {
                        onUpdateWorkspace(editing.id, cleanName, editType)
                        workspaceToEdit = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PrimaryGreen,
                        contentColor = AmoledBlack
                    )
                ) {
                    Text("Guardar", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { workspaceToEdit = null }) {
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
            onDismissRequest = { workspaceToDeleteId = null },
            containerColor = SurfaceDark,
            title = {
                Text("Eliminar espacio", color = TextPrimary)
            },
            text = {
                Text(
                    text = "¿Seguro que deseas eliminar \"${workspaceToDelete.name}\"?\n\nLos datos asociados a este espacio se desvincularán.",
                    color = TextSecondary
                )
            },
            confirmButton = {
                TextButton(
                    enabled = !isSaving,
                    onClick = {
                        onDeleteWorkspace(workspaceToDelete.name)
                        workspaceToDeleteId = null
                    }
                ) {
                    Text("Eliminar", color = ExpenseRed, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { workspaceToDeleteId = null }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

@Composable
private fun WorkspaceTypeOption(
    title: String,
    description: String,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) PrimaryGreen.copy(alpha = 0.12f) else Color.White.copy(alpha = 0.04f),
        border = BorderStroke(
            1.dp,
            if (isSelected) PrimaryGreen else BorderDark
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isSelected) PrimaryGreen else TextSecondary,
                modifier = Modifier.size(24.dp)
            )

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    color = if (isSelected) PrimaryGreen else TextPrimary
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = description,
                    fontSize = 11.sp,
                    color = TextSecondary,
                    lineHeight = 15.sp
                )
            }

            RadioButton(
                selected = isSelected,
                onClick = onClick,
                colors = RadioButtonDefaults.colors(
                    selectedColor = PrimaryGreen,
                    unselectedColor = TextSecondary
                )
            )
        }
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
