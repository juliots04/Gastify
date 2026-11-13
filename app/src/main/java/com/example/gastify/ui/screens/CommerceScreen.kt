package com.example.gastify.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.PointOfSale
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.gastify.data.model.Account
import com.example.gastify.data.model.Category
import com.example.gastify.data.model.CommerceOperation
import com.example.gastify.data.model.CommerceOperationType
import com.example.gastify.data.model.CommerceRequest
import com.example.gastify.data.model.InventoryProduct
import com.example.gastify.data.model.Workspace
import com.example.gastify.ui.components.GastifyIcons
import com.example.gastify.ui.theme.*
import com.example.gastify.viewmodel.AccountViewModel
import com.example.gastify.viewmodel.CategoryViewModel
import com.example.gastify.viewmodel.CommerceViewModel
import com.example.gastify.viewmodel.InventoryViewModel
import java.math.BigDecimal
import java.text.SimpleDateFormat
import java.util.Locale

private const val COMMERCE_MAX_CENTS = 999_999_999_999L
private const val COMMERCE_MAX_UNITS = 1_000_000

private fun commerceMoney(cents: Long): String =
    "S/ " + BigDecimal.valueOf(cents, 2).toPlainString()

private fun commerceParseMoney(input: String): Long? {
    val value = input.trim()
    if (!Regex("""^\d{1,10}([.,]\d{1,2})?$""").matches(value)) return null
    return runCatching {
        BigDecimal(value.replace(',', '.'))
            .movePointRight(2)
            .longValueExact()
            .takeIf { it in 1L..COMMERCE_MAX_CENTS }
    }.getOrNull()
}

@Composable
fun CommerceScreen(
    userId: String,
    workspace: Workspace,
    onBack: () -> Unit
) {
    val inventoryVm: InventoryViewModel = viewModel(
        key = "inventory_${userId}_${workspace.id}",
        factory = InventoryViewModel.factory(userId, workspace.id)
    )
    val accountVm: AccountViewModel = viewModel(
        key = "accounts_${userId}_${workspace.id}",
        factory = AccountViewModel.factory(userId, workspace.id)
    )
    val categoryVm: CategoryViewModel = viewModel(
        key = "categories_${userId}_${workspace.id}",
        factory = CategoryViewModel.factory(userId, workspace.id)
    )
    val commerceVm: CommerceViewModel = viewModel(
        key = "commerce_${userId}_${workspace.id}",
        factory = CommerceViewModel.factory(userId, workspace.id)
    )

    val inventoryState by inventoryVm.uiState.collectAsState()
    val accountState by accountVm.uiState.collectAsState()
    val categoryState by categoryVm.uiState.collectAsState()
    val commerceState by commerceVm.uiState.collectAsState()

    DisposableEffect(inventoryVm, accountVm, categoryVm, commerceVm) {
        inventoryVm.startListening()
        accountVm.startListening()
        categoryVm.startListening()
        commerceVm.startListening()
        onDispose {
            inventoryVm.stopListening()
            accountVm.stopListening()
            categoryVm.stopListening()
            commerceVm.stopListening()
        }
    }

    val snackbar = remember { SnackbarHostState() }
    LaunchedEffect(
        commerceState.errorMessage,
        commerceState.successMessage,
        inventoryState.errorMessage,
        accountState.errorMessage,
        categoryState.errorMessage
    ) {
        val message = commerceState.errorMessage ?: commerceState.successMessage
            ?: inventoryState.errorMessage ?: accountState.errorMessage
            ?: categoryState.errorMessage
        if (message != null) {
            snackbar.showSnackbar(message)
            commerceVm.clearMessages()
            inventoryVm.clearMessages()
            accountVm.clearError()
            categoryVm.clearError()
        }
    }

    var typeName by rememberSaveable(workspace.id) {
        mutableStateOf(CommerceOperationType.SALE.name)
    }
    val type = CommerceOperationType.entries.firstOrNull { it.name == typeName }
        ?: CommerceOperationType.SALE
    var productId by rememberSaveable(workspace.id) { mutableStateOf("") }
    var accountId by rememberSaveable(workspace.id) { mutableStateOf("") }
    var categoryId by rememberSaveable(workspace.id) { mutableStateOf("") }
    var quantityText by rememberSaveable(workspace.id) { mutableStateOf("1") }
    var unitPriceText by rememberSaveable(workspace.id) { mutableStateOf("") }
    var note by rememberSaveable(workspace.id) { mutableStateOf("") }
    var confirmation by remember { mutableStateOf<CommerceRequest?>(null) }

    val expenseOperation = type == CommerceOperationType.PURCHASE
    val matchingCategories = categoryState.categories.filter {
        it.type == if (expenseOperation) "EXPENSE" else "INCOME"
    }

    LaunchedEffect(inventoryState.products) {
        if (inventoryState.products.none { it.id == productId }) {
            productId = inventoryState.products.firstOrNull()?.id.orEmpty()
        }
    }
    LaunchedEffect(accountState.accounts) {
        if (accountState.accounts.none { it.id == accountId }) {
            accountId = accountState.accounts.firstOrNull()?.id.orEmpty()
        }
    }
    LaunchedEffect(typeName, categoryState.categories) {
        if (matchingCategories.none { it.id == categoryId }) {
            categoryId = matchingCategories.firstOrNull()?.id.orEmpty()
        }
    }
    val product = inventoryState.products.firstOrNull { it.id == productId }
    val account = accountState.accounts.firstOrNull { it.id == accountId }
    val category = matchingCategories.firstOrNull { it.id == categoryId }

    LaunchedEffect(typeName, productId) {
        unitPriceText = product?.let {
            BigDecimal.valueOf(
                if (expenseOperation) it.costCents else it.priceCents,
                2
            ).toPlainString()
        } ?: ""
    }

    val quantity = quantityText.toIntOrNull()
    val unitPrice = if (expenseOperation) {
        product?.costCents?.takeIf { it > 0L }
    } else {
        commerceParseMoney(unitPriceText)
    }
    val total = if (
        quantity != null && quantity in 1..COMMERCE_MAX_UNITS &&
        unitPrice != null && unitPrice in 1L..COMMERCE_MAX_CENTS
    ) quantity.toLong() * unitPrice else null
    val resultingStock = if (quantity != null && product != null) {
        product.quantity.toLong() + if (expenseOperation) quantity else -quantity
    } else null
    val resultingBalance = if (total != null && account != null) {
        account.balanceCents + if (expenseOperation) -total else total
    } else null
    val canConfirm = !commerceState.isSaving &&
        product != null && account != null && category != null &&
        total != null && total in 1L..COMMERCE_MAX_CENTS &&
        resultingStock != null && resultingStock in 0L..COMMERCE_MAX_UNITS.toLong() &&
        resultingBalance != null && resultingBalance in 0L..COMMERCE_MAX_CENTS &&
        note.length <= 200

    Box(modifier = Modifier.fillMaxSize().background(AmoledBlack)) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                TextButton(onClick = onBack) {
                    Icon(GastifyIcons.back, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("Volver")
                }
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.PointOfSale, null, tint = PrimaryGreen)
                    Spacer(Modifier.width(12.dp))
                    Text("Compras y ventas", fontSize = 26.sp, fontWeight = FontWeight.Bold)
                }
                Text(workspace.name, color = TextSecondary, fontSize = 13.sp)
            }

            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    FilterChip(
                        selected = !expenseOperation,
                        onClick = { typeName = CommerceOperationType.SALE.name },
                        label = { Text("Venta") },
                        leadingIcon = { Icon(Icons.Outlined.PointOfSale, null, Modifier.size(18.dp)) }
                    )
                    FilterChip(
                        selected = expenseOperation,
                        onClick = { typeName = CommerceOperationType.PURCHASE.name },
                        label = { Text("Compra") },
                        leadingIcon = { Icon(Icons.Outlined.ShoppingCart, null, Modifier.size(18.dp)) }
                    )
                }
            }

            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceDark)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Text(
                            if (expenseOperation) "Registrar compra" else "Registrar venta",
                            fontSize = 19.sp,
                            fontWeight = FontWeight.Bold
                        )
                        CommerceDropdown(
                            title = "Producto",
                            value = product?.name ?: "Seleccionar producto",
                            options = inventoryState.products.map { it.id to it.name },
                            onSelect = { productId = it }
                        )
                        if (product != null) {
                            Text(
                                "Stock disponible: ${product.quantity} unidades",
                                color = TextSecondary,
                                fontSize = 12.sp
                            )
                        }
                        CommerceDropdown(
                            title = "Cuenta financiera",
                            value = account?.name ?: "Seleccionar cuenta",
                            options = accountState.accounts.map { it.id to it.name },
                            onSelect = { accountId = it }
                        )
                        if (account != null) {
                            Text(
                                "Saldo: ${commerceMoney(account.balanceCents)}",
                                color = TextSecondary,
                                fontSize = 12.sp
                            )
                        }
                        CommerceDropdown(
                            title = "Categoría ${if (expenseOperation) "de gasto" else "de ingreso"}",
                            value = category?.name ?: "Seleccionar categoría",
                            options = matchingCategories.map { it.id to it.name },
                            onSelect = { categoryId = it }
                        )
                        OutlinedTextField(
                            value = quantityText,
                            onValueChange = { quantityText = it.take(7) },
                            label = { Text("Cantidad de unidades") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = unitPriceText,
                            onValueChange = { if (!expenseOperation) unitPriceText = it.take(16) },
                            enabled = !expenseOperation,
                            label = { Text("Precio unitario (S/)") },
                            supportingText = {
                                if (expenseOperation) {
                                    Text("La compra usa el costo configurado en Inventario.")
                                }
                            },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = note,
                            onValueChange = { note = it.take(200) },
                            label = { Text("Nota (opcional)") },
                            maxLines = 3,
                            modifier = Modifier.fillMaxWidth()
                        )
                        HorizontalDivider(color = BorderDark)
                        Text("Total: ${total?.let(::commerceMoney) ?: "—"}", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = PrimaryGreen)
                        Text(
                            "Stock después: ${resultingStock?.takeIf { it in 0L..COMMERCE_MAX_UNITS.toLong() } ?: "—"}",
                            color = TextSecondary,
                            fontSize = 13.sp
                        )
                        Text(
                            "Saldo después: ${resultingBalance?.takeIf { it in 0L..COMMERCE_MAX_CENTS }?.let(::commerceMoney) ?: "—"}",
                            color = TextSecondary,
                            fontSize = 13.sp
                        )
                        if (!canConfirm && product != null && account != null) {
                            Text(
                                "Revisa la cantidad, el precio, el stock y el saldo disponibles.",
                                color = ExpenseRed,
                                fontSize = 12.sp
                            )
                        }
                        Button(
                            onClick = {
                                if (canConfirm && quantity != null && unitPrice != null &&
                                    product != null && account != null && category != null) {
                                    confirmation = CommerceRequest(
                                        type = type,
                                        productId = product.id,
                                        accountId = account.id,
                                        categoryId = category.id,
                                        quantity = quantity,
                                        unitPriceCents = unitPrice,
                                        note = note.trim()
                                    )
                                }
                            },
                            enabled = canConfirm,
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = PrimaryGreen,
                                contentColor = AmoledBlack
                            )
                        ) {
                            Text("Revisar ${if (expenseOperation) "compra" else "venta"}")
                        }
                    }
                }
            }

            item {
                Spacer(Modifier.height(8.dp))
                Text("Últimas operaciones", fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
            }
            if (commerceState.isLoading) {
                item {
                    Box(Modifier.fillMaxWidth().padding(20.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = PrimaryGreen)
                    }
                }
            } else if (commerceState.operations.isEmpty()) {
                item {
                    Text("Todavía no hay compras o ventas registradas.", color = TextSecondary)
                }
            } else {
                items(commerceState.operations, key = { it.id }) { operation ->
                    CommerceHistoryCard(operation)
                }
            }
        }
        SnackbarHost(hostState = snackbar, modifier = Modifier.align(Alignment.BottomCenter))
    }

    confirmation?.let { request ->
        val amount = request.quantity.toLong() * request.unitPriceCents
        val requestProduct = inventoryState.products.firstOrNull { it.id == request.productId }
        val requestAccount = accountState.accounts.firstOrNull { it.id == request.accountId }
        val requestCategory = categoryState.categories.firstOrNull { it.id == request.categoryId }
        AlertDialog(
            onDismissRequest = { if (!commerceState.isSaving) confirmation = null },
            containerColor = SurfaceDark,
            title = { Text("Confirmar ${if (request.type == CommerceOperationType.PURCHASE) "compra" else "venta"}") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(requestProduct?.name ?: "Producto no disponible", fontWeight = FontWeight.Bold)
                    Text("Cantidad: ${request.quantity}")
                    Text("Total: ${commerceMoney(amount)}", color = PrimaryGreen, fontSize = 22.sp)
                    Text("Cuenta: ${requestAccount?.name ?: "No disponible"}")
                    Text("Categoría: ${requestCategory?.name ?: "No disponible"}")
                    Text("Se actualizarán el inventario, la cuenta y ambos historiales.", color = TextSecondary, fontSize = 12.sp)
                }
            },
            confirmButton = {
                TextButton(
                    enabled = !commerceState.isSaving,
                    onClick = {
                        commerceVm.registerOperation(request) {
                            confirmation = null
                            quantityText = "1"
                            note = ""
                        }
                    }
                ) {
                    Text(if (commerceState.isSaving) "Guardando..." else "Confirmar", color = PrimaryGreen)
                }
            },
            dismissButton = {
                TextButton(
                    enabled = !commerceState.isSaving,
                    onClick = { confirmation = null }
                ) { Text("Cancelar", color = TextSecondary) }
            }
        )
    }
}

@Composable
private fun CommerceDropdown(
    title: String,
    value: String,
    options: List<Pair<String, String>>,
    onSelect: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    Column {
        Text(title, color = TextSecondary, fontSize = 12.sp)
        Spacer(Modifier.height(6.dp))
        Box(modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(
                onClick = { expanded = true },
                enabled = options.isNotEmpty(),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(value, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                Icon(GastifyIcons.expand, contentDescription = "Desplegar")
            }
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                containerColor = SurfaceDark
            ) {
                options.forEach { (id, name) ->
                    DropdownMenuItem(
                        text = { Text(name, maxLines = 2) },
                        onClick = {
                            onSelect(id)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun CommerceHistoryCard(operation: CommerceOperation) {
    val isSale = operation.type == CommerceOperationType.SALE.name
    val date = operation.createdAt?.let {
        SimpleDateFormat("dd MMM yyyy · HH:mm", Locale.forLanguageTag("es-PE"))
            .format(it.toDate())
    } ?: "Sincronizando..."
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceDark)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    if (isSale) "Venta" else "Compra",
                    color = if (isSale) IncomeGreen else ExpenseRed,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.weight(1f))
                Text(
                    (if (isSale) "+" else "−") + commerceMoney(operation.totalCents),
                    color = if (isSale) IncomeGreen else ExpenseRed,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(operation.productName, color = TextPrimary, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(5.dp))
            Text("${operation.quantity} × ${commerceMoney(operation.unitPriceCents)}", color = TextSecondary, fontSize = 12.sp)
            Text("Stock: ${operation.previousQuantity} → ${operation.resultingQuantity}", color = TextSecondary, fontSize = 12.sp)
            if (operation.note.isNotBlank()) {
                Text(operation.note, color = TextSecondary, fontSize = 12.sp)
            }
            Spacer(Modifier.height(5.dp))
            Text(date, color = TextSecondary, fontSize = 11.sp)
        }
    }
}
