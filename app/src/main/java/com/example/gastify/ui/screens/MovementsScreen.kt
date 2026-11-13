package com.example.gastify.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel

import com.example.gastify.data.model.Account
import com.example.gastify.data.model.Category
import com.example.gastify.data.model.Movement
import com.example.gastify.data.model.MovementType
import com.example.gastify.data.model.Workspace
import com.example.gastify.ui.components.GastifyIcons
import com.example.gastify.ui.theme.*
import com.example.gastify.viewmodel.AccountViewModel
import com.example.gastify.viewmodel.CategoryViewModel
import com.example.gastify.viewmodel.MovementViewModel

import java.math.BigDecimal
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Locale

private fun formatMovementMoney(cents: Long): String {

    val formatter = NumberFormat.getNumberInstance(
        Locale.forLanguageTag("es-PE")
    )

    formatter.minimumFractionDigits = 2
    formatter.maximumFractionDigits = 2

    return "S/ ${
        formatter.format(BigDecimal.valueOf(cents, 2))
    }"
}

private fun parseMovementMoney(text: String): Long? {

    val clean = text.trim()

    if (!Regex("""^\d{1,10}([.,]\d{1,2})?$""").matches(clean)) {
        return null
    }

    return try {

        BigDecimal(
            clean.replace(',', '.')
        ).movePointRight(2).longValueExact()
            .takeIf {
                it in 1L..999_999_999_999L
            }

    } catch (_: Exception) {
        null
    }
}

private fun formatMovementDate(movement: Movement): String {

    val timestamp = movement.createdAt
        ?: return "Sincronizando fecha"

    val formatter = SimpleDateFormat(
        "dd MMM yyyy • HH:mm",
        Locale.forLanguageTag("es-PE")
    )

    return formatter.format(timestamp.toDate())
}

@Composable
fun MovementsScreen(
    userId: String,
    workspace: Workspace,
    onBack: () -> Unit
) {

    val movementVm: MovementViewModel = viewModel(
        key = "movements_${userId}_${workspace.id}",
        factory = MovementViewModel.factory(
            userId,
            workspace.id
        )
    )

    val accountVm: AccountViewModel = viewModel(
        key = "accounts_${userId}_${workspace.id}",
        factory = AccountViewModel.factory(
            userId,
            workspace.id
        )
    )

    val categoryVm: CategoryViewModel = viewModel(
        key = "categories_${userId}_${workspace.id}",
        factory = CategoryViewModel.factory(
            userId,
            workspace.id
        )
    )

    val movementState by movementVm.uiState.collectAsState()

    val accountState by accountVm.uiState.collectAsState()

    val categoryState by categoryVm.uiState.collectAsState()

    DisposableEffect(
        movementVm,
        accountVm,
        categoryVm
    ) {

        movementVm.startListening()
        accountVm.startListening()
        categoryVm.startListening()

        onDispose {
            movementVm.stopListening()
            accountVm.stopListening()
            categoryVm.stopListening()
        }
    }

    var showForm by rememberSaveable {
        mutableStateOf(false)
    }

    var type by rememberSaveable {
        mutableStateOf(MovementType.EXPENSE.name)
    }

    var accountId by rememberSaveable {
        mutableStateOf("")
    }

    var categoryId by rememberSaveable {
        mutableStateOf("")
    }

    var amountText by rememberSaveable {
        mutableStateOf("")
    }

    var noteText by rememberSaveable {
        mutableStateOf("")
    }

    val snackbar = remember {
        SnackbarHostState()
    }

    val filteredCategories = categoryState.categories.filter {
        it.type == type
    }

    LaunchedEffect(
        movementState.errorMessage,
        accountState.errorMessage,
        categoryState.errorMessage
    ) {

        val error = movementState.errorMessage
            ?: accountState.errorMessage
            ?: categoryState.errorMessage

        if (error != null) {

            snackbar.showSnackbar(error)

            movementVm.clearMessages()
            accountVm.clearError()
            categoryVm.clearError()
        }
    }

    LaunchedEffect(movementState.successMessage) {

        movementState.successMessage?.let {

            showForm = false
            movementVm.clearMessages()
            snackbar.showSnackbar(it)
        }
    }

    fun openForm() {

        type = MovementType.EXPENSE.name

        accountId = accountState.accounts
            .firstOrNull()?.id.orEmpty()

        categoryId = categoryState.categories
            .firstOrNull {
                it.type == type
            }?.id.orEmpty()

        amountText = ""
        noteText = ""

        showForm = true
    }

    val income = movementState.movements
        .filter {
            it.type == "INCOME"
        }
        .sumOf {
            it.amountCents
        }

    val expense = movementState.movements
        .filter {
            it.type == "EXPENSE"
        }
        .sumOf {
            it.amountCents
        }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AmoledBlack)
    ) {

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 20.dp,
                end = 20.dp,
                bottom = 32.dp
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            item {

                Spacer(Modifier.height(16.dp))

                TextButton(
                    onClick = onBack
                ) {

                    Icon(
                        imageVector = GastifyIcons.back,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )

                    Spacer(Modifier.width(6.dp))

                    Text("Volver")
                }

                Spacer(Modifier.height(12.dp))

                Text(
                    text = "Movimientos",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(Modifier.height(6.dp))

                Text(
                    text = workspace.name,
                    fontSize = 14.sp,
                    color = TextSecondary
                )

                Spacer(Modifier.height(24.dp))
            }

            item {

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {

                    MovementSummaryVector(
                        title = "Ingresos",
                        amount = formatMovementMoney(income),
                        color = IncomeGreen,
                        isIncome = true,
                        modifier = Modifier.weight(1f)
                    )

                    MovementSummaryVector(
                        title = "Gastos",
                        amount = formatMovementMoney(expense),
                        color = ExpenseRed,
                        isIncome = false,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            item {

                Spacer(Modifier.height(12.dp))

                Button(
                    onClick = {
                        openForm()
                    },
                    enabled = !movementState.isSaving &&
                        !accountState.isLoading &&
                        !categoryState.isLoading,
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
                        text = "Registrar movimiento",
                        fontWeight = FontWeight.Bold
                    )
                }

                if (accountState.accounts.isEmpty()) {

                    Spacer(Modifier.height(12.dp))

                    Text(
                        text = "Primero crea una cuenta para registrar movimientos.",
                        color = TextSecondary,
                        fontSize = 12.sp
                    )
                }

                if (categoryState.categories.isEmpty()) {

                    Spacer(Modifier.height(8.dp))

                    Text(
                        text = "Primero crea las categorías de ingresos o gastos.",
                        color = TextSecondary,
                        fontSize = 12.sp
                    )
                }

                Spacer(Modifier.height(28.dp))

                Text(
                    text = "Historial",
                    fontSize = 21.sp,
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(Modifier.height(6.dp))

                Text(
                    text = "Últimos 100 movimientos",
                    color = TextSecondary,
                    fontSize = 12.sp
                )

                Spacer(Modifier.height(12.dp))
            }

            if (movementState.isLoading) {

                item {

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {

                        CircularProgressIndicator(
                            color = PrimaryGreen
                        )
                    }
                }

            } else if (movementState.movements.isEmpty()) {

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
                                text = "Sin movimientos registrados",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.SemiBold
                            )

                            Spacer(Modifier.height(8.dp))

                            Text(
                                text = "Registra tu primer ingreso o gasto.",
                                color = TextSecondary,
                                fontSize = 13.sp
                            )
                        }
                    }
                }

            } else {

                items(
                    items = movementState.movements,
                    key = { it.id }
                ) { movement ->

                    val account = accountState.accounts
                        .firstOrNull {
                            it.id == movement.accountId
                        }

                    val category = categoryState.categories
                        .firstOrNull {
                            it.id == movement.categoryId
                        }

                    MovementCardVector(
                        movement = movement,
                        account = account,
                        category = category
                    )
                }
            }
        }

        SnackbarHost(
            hostState = snackbar,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }

    // FORMULARIO DE MOVIMIENTO

    if (showForm) {

        val amount = parseMovementMoney(amountText)

        val selectedAccount = accountState.accounts.firstOrNull {
            it.id == accountId
        }

        val selectedCategory = filteredCategories.firstOrNull {
            it.id == categoryId
        }

        val previewBalance = if (
            amount != null &&
            selectedAccount != null
        ) {

            try {

                if (type == "INCOME") {
                    Math.addExact(
                        selectedAccount.balanceCents,
                        amount
                    )
                } else {
                    Math.subtractExact(
                        selectedAccount.balanceCents,
                        amount
                    )
                }

            } catch (_: ArithmeticException) {
                null
            }

        } else {
            null
        }

        val validBalance = previewBalance != null &&
            previewBalance in 0L..999_999_999_999L

        val canSave = amount != null &&
            selectedAccount != null &&
            selectedCategory != null &&
            validBalance &&
            !movementState.isSaving

        AlertDialog(
            onDismissRequest = {
                if (!movementState.isSaving) {
                    showForm = false
                }
            },
            containerColor = SurfaceDark,
            title = {
                Text("Nuevo movimiento")
            },
            text = {

                Column(
                    modifier = Modifier
                        .heightIn(max = 460.dp)
                        .verticalScroll(rememberScrollState())
                ) {

                    Text(
                        text = "Tipo de movimiento",
                        fontWeight = FontWeight.Medium
                    )

                    Spacer(Modifier.height(8.dp))

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {

                        FilterChip(
                            selected = type == "EXPENSE",
                            onClick = {

                                type = "EXPENSE"

                                categoryId = categoryState.categories
                                    .firstOrNull {
                                        it.type == type
                                    }?.id.orEmpty()
                            },
                            leadingIcon = {

                                Icon(
                                    imageVector = GastifyIcons.expense,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                            },
                            label = {
                                Text("Gasto")
                            }
                        )

                        FilterChip(
                            selected = type == "INCOME",
                            onClick = {

                                type = "INCOME"

                                categoryId = categoryState.categories
                                    .firstOrNull {
                                        it.type == type
                                    }?.id.orEmpty()
                            },
                            leadingIcon = {

                                Icon(
                                    imageVector = GastifyIcons.income,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                            },
                            label = {
                                Text("Ingreso")
                            }
                        )
                    }

                    Spacer(Modifier.height(18.dp))

                    Text(
                        text = "Cuenta",
                        fontWeight = FontWeight.Medium
                    )

                    Spacer(Modifier.height(8.dp))

                    MovementDropdownVector(
                        selectedText = selectedAccount?.name
                            ?: "Seleccionar cuenta",
                        options = accountState.accounts.map {
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

                    MovementDropdownVector(
                        selectedText = selectedCategory?.name
                            ?: "Seleccionar categoría",
                        options = filteredCategories.map {
                            it.id to it.name
                        },
                        onSelect = {
                            categoryId = it
                        },
                        isCategory = true,
                        categoryLookup = filteredCategories
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
                            Text("Ejemplo: 2.80")
                        },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Decimal
                        ),
                        singleLine = true,
                        isError = amountText.isNotBlank() &&
                            amount == null,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(Modifier.height(14.dp))

                    OutlinedTextField(
                        value = noteText,
                        onValueChange = {
                            noteText = it.take(200)
                        },
                        label = {
                            Text("Descripción (opcional)")
                        },
                        placeholder = {
                            Text("Ejemplo: Pasaje de ida")
                        },
                        maxLines = 3,
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (
                        selectedAccount != null &&
                        amount != null
                    ) {

                        Spacer(Modifier.height(16.dp))

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = CardDark
                            ),
                            shape = RoundedCornerShape(14.dp)
                        ) {

                            Column(
                                modifier = Modifier.padding(16.dp)
                            ) {

                                Text(
                                    text = "Saldo actual",
                                    fontSize = 12.sp,
                                    color = TextSecondary
                                )

                                Spacer(Modifier.height(6.dp))

                                Text(
                                    text = formatMovementMoney(
                                        selectedAccount.balanceCents
                                    ),
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold
                                )

                                Spacer(Modifier.height(12.dp))

                                Text(
                                    text = "Saldo después del movimiento",
                                    fontSize = 12.sp,
                                    color = TextSecondary
                                )

                                Spacer(Modifier.height(6.dp))

                                Text(
                                    text = if (previewBalance == null) {
                                        "Fuera de rango"
                                    } else {
                                        formatMovementMoney(previewBalance)
                                    },
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (validBalance) {
                                        PrimaryGreen
                                    } else {
                                        ExpenseRed
                                    }
                                )

                                if (!validBalance) {

                                    Spacer(Modifier.height(8.dp))

                                    Text(
                                        text = "Saldo insuficiente o fuera del límite permitido.",
                                        color = ExpenseRed,
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {

                TextButton(
                    enabled = canSave,
                    onClick = {

                        if (
                            amount != null &&
                            selectedAccount != null &&
                            selectedCategory != null &&
                            validBalance
                        ) {

                            movementVm.createMovement(
                                selectedAccount.id,
                                selectedCategory.id,
                                type,
                                amount,
                                noteText.trim()
                            )
                        }
                    }
                ) {

                    if (movementState.isSaving) {

                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = PrimaryGreen,
                            strokeWidth = 2.dp
                        )

                    } else {

                        Text(
                            text = "Guardar",
                            color = PrimaryGreen
                        )
                    }
                }
            },
            dismissButton = {

                TextButton(
                    onClick = {
                        showForm = false
                    },
                    enabled = !movementState.isSaving
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
private fun MovementDropdownVector(
    selectedText: String,
    options: List<Pair<String, String>>,
    onSelect: (String) -> Unit,
    isCategory: Boolean = false,
    categoryLookup: List<Category> = emptyList()
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
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = TextPrimary
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

                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {

                            if (isCategory) {

                                val selected = categoryLookup
                                    .firstOrNull {
                                        it.id == option.first
                                    }

                                Icon(
                                    imageVector = GastifyIcons.category(
                                        selected?.icon ?: ""
                                    ),
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp)
                                )

                                Spacer(Modifier.width(10.dp))
                            }

                            Text(
                                text = option.second,
                                color = TextPrimary
                            )
                        }
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
                            text = "No hay opciones disponibles",
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

@Composable
private fun MovementSummaryVector(
    title: String,
    amount: String,
    color: Color,
    isIncome: Boolean,
    modifier: Modifier = Modifier
) {

    Card(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = SurfaceDark
        )
    ) {

        Column(
            modifier = Modifier.padding(18.dp)
        ) {

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {

                Icon(
                    imageVector = if (isIncome) {
                        GastifyIcons.income
                    } else {
                        GastifyIcons.expense
                    },
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(18.dp)
                )

                Spacer(Modifier.width(6.dp))

                Text(
                    text = title,
                    fontSize = 13.sp,
                    color = TextSecondary
                )
            }

            Spacer(Modifier.height(12.dp))

            Text(
                text = amount,
                fontSize = 19.sp,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }
    }
}

@Composable
private fun MovementCardVector(
    movement: Movement,
    account: Account?,
    category: Category?
) {

    val income = movement.type == MovementType.INCOME.name

    val color = if (income) {
        IncomeGreen
    } else {
        ExpenseRed
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = SurfaceDark
        )
    ) {

        Column(
            modifier = Modifier.padding(18.dp)
        ) {

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {

                Surface(
                    color = CardDark,
                    shape = RoundedCornerShape(14.dp)
                ) {

                    Box(
                        modifier = Modifier.size(48.dp),
                        contentAlignment = Alignment.Center
                    ) {

                        Icon(
                            imageVector = GastifyIcons.category(
                                category?.icon ?: ""
                            ),
                            contentDescription = null,
                            tint = PrimaryGreen,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                }

                Spacer(Modifier.width(12.dp))

                Column(
                    modifier = Modifier.weight(1f)
                ) {

                    Text(
                        text = category?.name
                            ?: "Categoría no disponible",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(Modifier.height(5.dp))

                    Text(
                        text = account?.name
                            ?: "Cuenta no disponible",
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                }

                Spacer(Modifier.width(8.dp))

                Text(
                    text = (
                        if (income) "+" else "-"
                    ) + formatMovementMoney(movement.amountCents),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = color
                )
            }

            if (movement.note.isNotBlank()) {

                Spacer(Modifier.height(14.dp))

                Text(
                    text = movement.note,
                    fontSize = 13.sp,
                    color = TextSecondary
                )
            }

            Spacer(Modifier.height(16.dp))

            HorizontalDivider(
                color = BorderDark
            )

            Spacer(Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {

                Text(
                    text = formatMovementDate(movement),
                    fontSize = 11.sp,
                    color = TextSecondary
                )

                Text(
                    text = if (income) {
                        "Ingreso"
                    } else {
                        "Gasto"
                    },
                    color = color,
                    fontSize = 11.sp
                )
            }

            Spacer(Modifier.height(10.dp))

            Text(
                text = "Saldo: ${
                    formatMovementMoney(movement.previousBalanceCents)
                } → ${
                    formatMovementMoney(movement.balanceAfterCents)
                }",
                fontSize = 12.sp,
                color = TextSecondary
            )
        }
    }
}
