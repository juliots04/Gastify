package com.example.gastify.ui.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll

import androidx.compose.material3.*

import androidx.compose.runtime.*

import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

import com.example.gastify.data.local.QuickActionStore

import com.example.gastify.data.model.Account
import com.example.gastify.data.model.Category
import com.example.gastify.data.model.MovementType
import com.example.gastify.data.model.QuickAction
import com.example.gastify.data.model.Workspace

import com.example.gastify.ui.theme.*

import java.math.BigDecimal
import java.text.NumberFormat
import java.util.Locale
import java.util.UUID

private const val MAX_QUICK_AMOUNT =
    999_999_999_999L

private fun quickMoney(cents: Long): String {

    val formatter = NumberFormat.getNumberInstance(
        Locale.forLanguageTag("es-PE")
    )

    formatter.minimumFractionDigits = 2
    formatter.maximumFractionDigits = 2

    return "S/ ${
        formatter.format(
            BigDecimal.valueOf(cents, 2)
        )
    }"
}

private fun parseQuickMoney(
    value: String
): Long? {

    val text = value.trim()

    if (
        !Regex("""^\d{1,10}([.,]\d{1,2})?$""")
            .matches(text)
    ) {
        return null
    }

    return try {

        BigDecimal(
            text.replace(',', '.')
        ).movePointRight(2)
            .longValueExact()
            .takeIf {
                it in 1L..MAX_QUICK_AMOUNT
            }

    } catch (_: Exception) {

        null
    }
}

@Composable
fun QuickActionsSection(
    userId: String,
    workspace: Workspace,
    accounts: List<Account>,
    categories: List<Category>,
    isSaving: Boolean,
    onRegister: (QuickAction) -> Unit
) {

    val context = LocalContext.current

    val store = remember(context) {
        QuickActionStore(context)
    }

    var actions by remember(
        userId,
        workspace.id
    ) {

        mutableStateOf(
            store.getByWorkspace(
                userId,
                workspace.id
            )
        )
    }

    var showEditor by remember {
        mutableStateOf(false)
    }

    var editingAction by remember {
        mutableStateOf<QuickAction?>(null)
    }

    var confirmingAction by remember {
        mutableStateOf<QuickAction?>(null)
    }

    var deletingAction by remember {
        mutableStateOf<QuickAction?>(null)
    }

    var name by remember {
        mutableStateOf("")
    }

    var type by remember {
        mutableStateOf(MovementType.EXPENSE.name)
    }

    var accountId by remember {
        mutableStateOf("")
    }

    var categoryId by remember {
        mutableStateOf("")
    }

    var amountText by remember {
        mutableStateOf("2.80")
    }

    var note by remember {
        mutableStateOf("")
    }

    var localMessage by remember {
        mutableStateOf<String?>(null)
    }

    val filteredCategories = categories.filter {
        it.type == type
    }

    fun refresh() {

        actions = store.getByWorkspace(
            userId,
            workspace.id
        )
    }

    fun openEditor(action: QuickAction?) {

        editingAction = action

        name = action?.name ?: ""

        type = action?.type
            ?: MovementType.EXPENSE.name

        accountId = action?.accountId
            ?: accounts.firstOrNull()?.id.orEmpty()

        categoryId = action?.categoryId
            ?: categories.firstOrNull {
                it.type == type
            }?.id.orEmpty()

        amountText = action?.let {
            BigDecimal.valueOf(
                it.amountCents,
                2
            ).toPlainString()
        } ?: "2.80"

        note = action?.note ?: ""

        localMessage = null

        showEditor = true
    }

    // ======================================
    // ENCABEZADO
    // ======================================

    Column(
        modifier = Modifier.fillMaxWidth()
    ) {

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {

            Text(
                text = "Accesos rápidos",
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary
            )

            TextButton(
                onClick = {
                    openEditor(null)
                },
                enabled = !isSaving &&
                    accounts.isNotEmpty() &&
                    categories.isNotEmpty()
            ) {

                Icon(
                    imageVector = GastifyIcons.add,
                    contentDescription = null,
                    tint = PrimaryGreen,
                    modifier = Modifier.size(20.dp)
                )

                Spacer(Modifier.width(4.dp))

                Text(
                    text = "Crear",
                    color = PrimaryGreen
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        Text(
            text = "Registra movimientos frecuentes sin llenar formularios.",
            fontSize = 12.sp,
            lineHeight = 18.sp,
            color = TextSecondary
        )

        if (localMessage != null) {

            Spacer(Modifier.height(10.dp))

            Text(
                text = localMessage.orEmpty(),
                fontSize = 12.sp,
                color = ExpenseRed
            )
        }

        Spacer(Modifier.height(14.dp))

        if (actions.isEmpty()) {

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(
                    containerColor = SurfaceDark
                )
            ) {

                Column(
                    modifier = Modifier.padding(20.dp)
                ) {

                    Text(
                        text = "Sin accesos rápidos",
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )

                    Spacer(Modifier.height(8.dp))

                    Text(
                        text = "Crea uno para registrar tus pasajes, compras o ingresos frecuentes.",
                        fontSize = 13.sp,
                        lineHeight = 20.sp,
                        color = TextSecondary
                    )

                    if (
                        accounts.isEmpty() ||
                        categories.isEmpty()
                    ) {

                        Spacer(Modifier.height(10.dp))

                        Text(
                            text = "Necesitas una cuenta y una categoría para comenzar.",
                            fontSize = 12.sp,
                            color = PrimaryGreen
                        )
                    }
                }
            }

        } else {

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(
                        rememberScrollState()
                    ),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {

                actions.forEach { action ->

                    val account = accounts.firstOrNull {
                        it.id == action.accountId
                    }

                    val category = categories.firstOrNull {
                        it.id == action.categoryId &&
                            it.type == action.type
                    }

                    val validReferences =
                        account != null &&
                            category != null

                    Card(
                        modifier = Modifier.width(172.dp),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = SurfaceDark
                        )
                    ) {

                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {

                            Surface(
                                color = CardDark,
                                shape = RoundedCornerShape(12.dp)
                            ) {

                                Box(
                                    modifier = Modifier.size(44.dp),
                                    contentAlignment = Alignment.Center
                                ) {

                                    Icon(
                                        imageVector =
                                            GastifyIcons.category(
                                                category?.icon ?: ""
                                            ),
                                        contentDescription = null,
                                        tint = PrimaryGreen,
                                        modifier = Modifier.size(25.dp)
                                    )
                                }
                            }

                            Spacer(Modifier.height(12.dp))

                            Text(
                                text = action.name,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TextPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )

                            Spacer(Modifier.height(6.dp))

                            Text(
                                text = quickMoney(
                                    action.amountCents
                                ),
                                fontSize = 19.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (
                                    action.type ==
                                    MovementType.INCOME.name
                                ) {
                                    IncomeGreen
                                } else {
                                    ExpenseRed
                                }
                            )

                            Spacer(Modifier.height(8.dp))

                            Text(
                                text = account?.name
                                    ?: "Cuenta no disponible",
                                fontSize = 11.sp,
                                color = TextSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )

                            Spacer(Modifier.height(14.dp))

                            Button(
                                onClick = {
                                    confirmingAction = action
                                },
                                enabled = !isSaving &&
                                    validReferences,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = PrimaryGreen,
                                    contentColor = AmoledBlack
                                )
                            ) {

                                Text(
                                    text = "Registrar",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement =
                                    Arrangement.SpaceBetween
                            ) {

                                IconButton(
                                    onClick = {
                                        openEditor(action)
                                    },
                                    enabled = !isSaving
                                ) {

                                    Icon(
                                        imageVector = GastifyIcons.edit,
                                        contentDescription = "Editar",
                                        tint = TextSecondary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }

                                IconButton(
                                    onClick = {
                                        deletingAction = action
                                    },
                                    enabled = !isSaving
                                ) {

                                    Icon(
                                        imageVector = GastifyIcons.delete,
                                        contentDescription = "Eliminar",
                                        tint = ExpenseRed,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // ======================================
    // CREAR / EDITAR ACCESO
    // ======================================

    if (showEditor) {

        val amount = parseQuickMoney(amountText)

        val selectedAccount = accounts.firstOrNull {
            it.id == accountId
        }

        val selectedCategory = filteredCategories.firstOrNull {
            it.id == categoryId
        }

        val canSave =
            name.trim().isNotEmpty() &&
                amount != null &&
                selectedAccount != null &&
                selectedCategory != null &&
                !isSaving

        AlertDialog(
            onDismissRequest = {
                showEditor = false
            },
            containerColor = SurfaceDark,
            title = {

                Text(
                    text = if (editingAction == null) {
                        "Nuevo acceso rápido"
                    } else {
                        "Editar acceso rápido"
                    },
                    color = TextPrimary
                )
            },
            text = {

                Column(
                    modifier = Modifier
                        .heightIn(max = 460.dp)
                        .verticalScroll(
                            rememberScrollState()
                        )
                ) {

                    OutlinedTextField(
                        value = name,
                        onValueChange = {
                            name = it.take(60)
                        },
                        label = {
                            Text("Nombre del acceso")
                        },
                        placeholder = {
                            Text("Ejemplo: Pasaje de ida")
                        },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(Modifier.height(18.dp))

                    Text(
                        text = "Tipo de movimiento",
                        fontWeight = FontWeight.Medium
                    )

                    Spacer(Modifier.height(8.dp))

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {

                        FilterChip(
                            selected =
                                type == MovementType.EXPENSE.name,
                            onClick = {

                                type = MovementType.EXPENSE.name

                                categoryId = categories.firstOrNull {
                                    it.type == type
                                }?.id.orEmpty()
                            },
                            label = {
                                Text("Gasto")
                            },
                            leadingIcon = {

                                Icon(
                                    imageVector = GastifyIcons.expense,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        )

                        FilterChip(
                            selected =
                                type == MovementType.INCOME.name,
                            onClick = {

                                type = MovementType.INCOME.name

                                categoryId = categories.firstOrNull {
                                    it.type == type
                                }?.id.orEmpty()
                            },
                            label = {
                                Text("Ingreso")
                            },
                            leadingIcon = {

                                Icon(
                                    imageVector = GastifyIcons.income,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        )
                    }

                    Spacer(Modifier.height(18.dp))

                    Text(
                        text = "Cuenta",
                        fontWeight = FontWeight.Medium
                    )

                    Spacer(Modifier.height(8.dp))

                    QuickActionDropdown(
                        selectedText = selectedAccount?.name
                            ?: "Seleccionar cuenta",
                        options = accounts.map {
                            it.id to it.name
                        },
                        onSelect = {
                            accountId = it
                        }
                    )

                    Spacer(Modifier.height(18.dp))

                    Text(
                        text = "Categoría",
                        fontWeight = FontWeight.Medium
                    )

                    Spacer(Modifier.height(8.dp))

                    QuickActionDropdown(
                        selectedText = selectedCategory?.name
                            ?: "Seleccionar categoría",
                        options = filteredCategories.map {
                            it.id to it.name
                        },
                        onSelect = {
                            categoryId = it
                        }
                    )

                    Spacer(Modifier.height(18.dp))

                    OutlinedTextField(
                        value = amountText,
                        onValueChange = {
                            amountText = it.take(16)
                        },
                        label = {
                            Text("Importe (S/)")
                        },
                        placeholder = {
                            Text("2.80")
                        },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Decimal
                        ),
                        singleLine = true,
                        isError =
                            amountText.isNotBlank() &&
                                amount == null,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(Modifier.height(16.dp))

                    OutlinedTextField(
                        value = note,
                        onValueChange = {
                            note = it.take(200)
                        },
                        label = {
                            Text("Descripción (opcional)")
                        },
                        placeholder = {
                            Text("Ejemplo: Corredor Morado")
                        },
                        maxLines = 3,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(Modifier.height(14.dp))

                    Text(
                        text = "Esta configuración se reutilizará cada vez que registres el movimiento.",
                        color = TextSecondary,
                        fontSize = 12.sp,
                        lineHeight = 18.sp
                    )
                }
            },
            confirmButton = {

                TextButton(
                    enabled = canSave,
                    onClick = {

                        if (
                            amount != null &&
                            selectedAccount != null &&
                            selectedCategory != null
                        ) {

                            try {

                                store.save(
                                    QuickAction(
                                        id = editingAction?.id
                                            ?: UUID.randomUUID()
                                                .toString(),
                                        userId = userId,
                                        workspaceId = workspace.id,
                                        name = name.trim(),
                                        accountId = selectedAccount.id,
                                        categoryId = selectedCategory.id,
                                        type = type,
                                        amountCents = amount,
                                        note = note.trim()
                                    )
                                )

                                refresh()

                                showEditor = false

                                localMessage = null

                            } catch (exception: Exception) {

                                localMessage =
                                    exception.localizedMessage
                                        ?: "No se pudo guardar."
                            }
                        }
                    }
                ) {

                    Text(
                        text = "Guardar",
                        color = PrimaryGreen
                    )
                }
            },
            dismissButton = {

                TextButton(
                    onClick = {
                        showEditor = false
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

    // ======================================
    // CONFIRMAR MOVIMIENTO
    // ======================================

    confirmingAction?.let { action ->

        val account = accounts.firstOrNull {
            it.id == action.accountId
        }

        val category = categories.firstOrNull {
            it.id == action.categoryId &&
                it.type == action.type
        }

        val nextBalance = account?.let {

            try {

                if (
                    action.type == MovementType.INCOME.name
                ) {

                    Math.addExact(
                        it.balanceCents,
                        action.amountCents
                    )

                } else {

                    Math.subtractExact(
                        it.balanceCents,
                        action.amountCents
                    )
                }

            } catch (_: ArithmeticException) {

                null
            }
        }

        val validBalance =
            nextBalance != null &&
                nextBalance in 0L..MAX_QUICK_AMOUNT

        AlertDialog(
            onDismissRequest = {
                if (!isSaving) {
                    confirmingAction = null
                }
            },
            containerColor = SurfaceDark,
            title = {

                Text(
                    text = "Confirmar movimiento",
                    color = TextPrimary
                )
            },
            text = {

                Column {

                    Text(
                        text = action.name,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )

                    Spacer(Modifier.height(12.dp))

                    Text(
                        text = quickMoney(
                            action.amountCents
                        ),
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (
                            action.type ==
                            MovementType.INCOME.name
                        ) {
                            IncomeGreen
                        } else {
                            ExpenseRed
                        }
                    )

                    Spacer(Modifier.height(14.dp))

                    Text(
                        text = "Cuenta: ${
                            account?.name
                                ?: "No disponible"
                        }",
                        color = TextSecondary
                    )

                    Spacer(Modifier.height(6.dp))

                    Text(
                        text = "Categoría: ${
                            category?.name
                                ?: "No disponible"
                        }",
                        color = TextSecondary
                    )

                    Spacer(Modifier.height(14.dp))

                    if (account != null) {

                        Text(
                            text = "Saldo actual: ${
                                quickMoney(account.balanceCents)
                            }",
                            color = TextPrimary
                        )

                        Spacer(Modifier.height(8.dp))

                        Text(
                            text = "Nuevo saldo: ${
                                nextBalance?.let {
                                    quickMoney(it)
                                } ?: "No disponible"
                            }",
                            color = if (validBalance) {
                                PrimaryGreen
                            } else {
                                ExpenseRed
                            },
                            fontWeight = FontWeight.SemiBold
                        )

                        if (!validBalance) {

                            Spacer(Modifier.height(10.dp))

                            Text(
                                text = "Saldo insuficiente o importe fuera del límite permitido.",
                                color = ExpenseRed,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            },
            confirmButton = {

                TextButton(
                    enabled =
                        validBalance &&
                            category != null &&
                            !isSaving,
                    onClick = {

                        if (
                            account != null &&
                            category != null &&
                            validBalance
                        ) {

                            confirmingAction = null

                            onRegister(action)
                        }
                    }
                ) {

                    Text(
                        text = "Confirmar",
                        color = PrimaryGreen
                    )
                }
            },
            dismissButton = {

                TextButton(
                    onClick = {
                        confirmingAction = null
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

    // ======================================
    // ELIMINAR ACCESO
    // ======================================

    deletingAction?.let { action ->

        AlertDialog(
            onDismissRequest = {
                deletingAction = null
            },
            containerColor = SurfaceDark,
            title = {

                Text(
                    text = "Eliminar acceso rápido",
                    color = TextPrimary
                )
            },
            text = {

                Text(
                    text = "¿Eliminar \"${action.name}\"?\n\nNo se eliminarán los movimientos que ya hayas registrado.",
                    color = TextSecondary
                )
            },
            confirmButton = {

                TextButton(
                    onClick = {

                        try {

                            store.delete(
                                userId = userId,
                                workspaceId = workspace.id,
                                actionId = action.id
                            )

                            refresh()

                            deletingAction = null

                            localMessage = null

                        } catch (exception: Exception) {

                            localMessage =
                                exception.localizedMessage
                                    ?: "No se pudo eliminar."
                        }
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
                        deletingAction = null
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

@Composable
private fun QuickActionDropdown(
    selectedText: String,
    options: List<Pair<String, String>>,
    onSelect: (String) -> Unit
) {

    var expanded by remember {
        mutableStateOf(false)
    }

    Box(
        modifier = Modifier.fillMaxWidth()
    ) {

        OutlinedButton(
            onClick = {
                expanded = true
            },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        ) {

            Text(
                text = selectedText,
                modifier = Modifier.weight(1f),
                color = TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(Modifier.width(8.dp))

            Icon(
                imageVector = GastifyIcons.expand,
                contentDescription = "Desplegar",
                tint = PrimaryGreen
            )
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = {
                expanded = false
            },
            containerColor = SurfaceDark
        ) {

            options.forEach { option ->

                DropdownMenuItem(
                    text = {

                        Text(
                            text = option.second,
                            color = TextPrimary
                        )
                    },
                    onClick = {

                        onSelect(option.first)

                        expanded = false
                    }
                )
            }

            if (options.isEmpty()) {

                DropdownMenuItem(
                    text = {
                        Text(
                            "Sin opciones disponibles",
                            color = TextSecondary
                        )
                    },
                    onClick = {
                        expanded = false
                    }
                )
            }
        }
    }
}
