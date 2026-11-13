package com.example.gastify.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.SwapVert
import androidx.compose.material.icons.outlined.WarningAmber

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

import com.example.gastify.data.model.InventoryProduct
import com.example.gastify.data.model.StockMovement
import com.example.gastify.data.model.StockMovementType
import com.example.gastify.data.model.Workspace

import com.example.gastify.ui.components.GastifyIcons
import com.example.gastify.ui.theme.*

import com.example.gastify.viewmodel.InventoryViewModel

import java.math.BigDecimal
import java.text.SimpleDateFormat
import java.util.Locale

private val InventoryWarning =
    Color(0xFFFFBF69)

private const val MAX_INVENTORY_QUANTITY =
    1_000_000

private const val MAX_INVENTORY_AMOUNT =
    999_999_999_999L

// ==========================================
// FORMATOS
// ==========================================

private fun inventoryMoney(
    cents: Long
): String {

    return "S/ " + BigDecimal.valueOf(
        cents,
        2
    ).toPlainString()
}

private fun inventoryTotalMoney(
    cents: BigDecimal
): String {

    return "S/ " + cents
        .movePointLeft(2)
        .setScale(2)
        .toPlainString()
}

private fun parseInventoryMoney(
    text: String
): Long? {

    val clean = text.trim()

    if (
        !Regex("""^\d{1,10}([.,]\d{1,2})?$""")
            .matches(clean)
    ) {
        return null
    }

    return try {

        BigDecimal(
            clean.replace(',', '.')
        )
            .movePointRight(2)
            .longValueExact()
            .takeIf {
                it in 0L..MAX_INVENTORY_AMOUNT
            }

    } catch (_: Exception) {

        null
    }
}

private fun stockTypeName(
    type: StockMovementType
): String {

    return when (type) {

        StockMovementType.ENTRY -> "Entrada"

        StockMovementType.EXIT -> "Salida"

        StockMovementType.ADJUSTMENT -> "Ajuste"
    }
}

// ==========================================
// PANTALLA PRINCIPAL
// ==========================================

@Composable
fun InventoryScreen(
    userId: String,
    workspace: Workspace,
    onBack: () -> Unit
) {

    val inventoryVm: InventoryViewModel = viewModel(
        key = "inventory_${userId}_${workspace.id}",

        factory = InventoryViewModel.factory(
            userId,
            workspace.id
        )
    )

    val state by inventoryVm.uiState.collectAsState()

    DisposableEffect(inventoryVm) {

        inventoryVm.startListening()

        onDispose {

            inventoryVm.stopListening()
        }
    }

    val snackbar = remember {
        SnackbarHostState()
    }

    LaunchedEffect(
        state.errorMessage,
        state.successMessage
    ) {

        val message = state.errorMessage
            ?: state.successMessage

        if (message != null) {

            snackbar.showSnackbar(message)

            inventoryVm.clearMessages()
        }
    }

    // ======================================
    // BÚSQUEDA
    // ======================================

    var search by rememberSaveable(
        workspace.id
    ) {
        mutableStateOf("")
    }

    var stockFilter by rememberSaveable(
        workspace.id
    ) {
        mutableStateOf("ALL")
    }

    val filteredProducts = state.products.filter {
            product ->

        val matchesSearch =

            product.name.contains(
                search,
                ignoreCase = true
            ) ||

            product.sku.contains(
                search,
                ignoreCase = true
            )

        val matchesStock = when (stockFilter) {

            "LOW" -> product.isLowStock

            "OUT" -> product.isOutOfStock

            else -> true
        }

        matchesSearch && matchesStock
    }

    // ======================================
    // TOTALES
    // ======================================

    val stockUnits = state.products.sumOf {
        it.quantity.toLong()
    }

    val lowStockCount = state.products.count {
        it.isLowStock
    }

    val outOfStockCount = state.products.count {
        it.isOutOfStock
    }

    val inventoryValue = state.products.fold(
        BigDecimal.ZERO
    ) { total, product ->

        total + BigDecimal.valueOf(
            product.costCents
        ).multiply(
            BigDecimal.valueOf(
                product.quantity.toLong()
            )
        )
    }

    // ======================================
    // ESTADOS DEL EDITOR
    // ======================================

    var showEditor by remember {
        mutableStateOf(false)
    }

    var editingProduct by remember {
        mutableStateOf<InventoryProduct?>(null)
    }

    var deletingProduct by remember {
        mutableStateOf<InventoryProduct?>(null)
    }

    var stockProductId by remember {
        mutableStateOf<String?>(null)
    }

    var stockTypeName by remember {
        mutableStateOf(
            StockMovementType.ENTRY.name
        )
    }

    var stockQuantityText by remember {
        mutableStateOf("1")
    }

    var stockNote by remember {
        mutableStateOf("")
    }

    var name by rememberSaveable {
        mutableStateOf("")
    }

    var sku by rememberSaveable {
        mutableStateOf("")
    }

    var description by rememberSaveable {
        mutableStateOf("")
    }

    var quantityText by rememberSaveable {
        mutableStateOf("0")
    }

    var minQuantityText by rememberSaveable {
        mutableStateOf("0")
    }

    var costText by rememberSaveable {
        mutableStateOf("0.00")
    }

    var priceText by rememberSaveable {
        mutableStateOf("0.00")
    }

    // ======================================
    // ABRIR EDITOR
    // ======================================

    fun openEditor(
        product: InventoryProduct?
    ) {

        editingProduct = product

        name = product?.name ?: ""

        sku = product?.sku ?: ""

        description = product?.description ?: ""

        quantityText = product?.quantity
            ?.toString() ?: "0"

        minQuantityText = product?.minQuantity
            ?.toString() ?: "0"

        costText = product?.let {

            BigDecimal.valueOf(
                it.costCents,
                2
            ).toPlainString()

        } ?: "0.00"

        priceText = product?.let {

            BigDecimal.valueOf(
                it.priceCents,
                2
            ).toPlainString()

        } ?: "0.00"

        showEditor = true
    }

    // ======================================
    // ABRIR MOVIMIENTO
    // ======================================

    fun openStockMovement(
        product: InventoryProduct
    ) {

        stockProductId = product.id

        stockTypeName =
            StockMovementType.ENTRY.name

        stockQuantityText = "1"

        stockNote = ""
    }

    // ======================================
    // INTERFAZ
    // ======================================

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

            verticalArrangement = Arrangement.spacedBy(
                14.dp
            )
        ) {

            // ENCABEZADO

            item {

                Spacer(Modifier.height(16.dp))

                TextButton(
                    onClick = onBack
                ) {

                    Icon(
                        imageVector = GastifyIcons.back,
                        contentDescription = null
                    )

                    Spacer(Modifier.width(6.dp))

                    Text("Volver")
                }

                Spacer(Modifier.height(12.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {

                    Icon(
                        imageVector =
                            Icons.Outlined.Inventory2,

                        contentDescription = null,

                        tint = PrimaryGreen,

                        modifier = Modifier.size(30.dp)
                    )

                    Spacer(Modifier.width(12.dp))

                    Text(
                        text = "Inventario",

                        fontSize = 28.sp,

                        fontWeight = FontWeight.Bold,

                        color = TextPrimary
                    )
                }

                Spacer(Modifier.height(8.dp))

                Text(
                    text = workspace.name,

                    color = TextSecondary
                )

                Spacer(Modifier.height(20.dp))
            }

            // CARGA

            if (state.isLoading) {

                item {

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(30.dp),

                        contentAlignment = Alignment.Center
                    ) {

                        CircularProgressIndicator(
                            color = PrimaryGreen
                        )
                    }
                }

            } else {

                // VALOR DEL INVENTARIO

                item {

                    Card(
                        modifier = Modifier.fillMaxWidth(),

                        shape = RoundedCornerShape(22.dp),

                        colors = CardDefaults.cardColors(
                            containerColor = SurfaceDark
                        )
                    ) {

                        Column(
                            modifier = Modifier.padding(22.dp)
                        ) {

                            Text(
                                text = "Valor del inventario",

                                color = TextSecondary,

                                fontSize = 13.sp
                            )

                            Spacer(Modifier.height(12.dp))

                            Text(
                                text = inventoryTotalMoney(
                                    inventoryValue
                                ),

                                fontSize = 29.sp,

                                fontWeight = FontWeight.Bold,

                                color = PrimaryGreen
                            )

                            Spacer(Modifier.height(14.dp))

                            HorizontalDivider(
                                color = BorderDark
                            )

                            Spacer(Modifier.height(14.dp))

                            Text(
                                text = "${state.products.size} productos · $stockUnits unidades",

                                color = TextSecondary,

                                fontSize = 12.sp
                            )
                        }
                    }
                }

                // ALERTAS DE STOCK

                item {

                    Row(
                        modifier = Modifier.fillMaxWidth(),

                        horizontalArrangement =
                            Arrangement.spacedBy(12.dp)
                    ) {

                        InventorySummaryCard(
                            modifier = Modifier.weight(1f),

                            title = "Stock bajo",

                            value = lowStockCount.toString(),

                            color = InventoryWarning
                        )

                        InventorySummaryCard(
                            modifier = Modifier.weight(1f),

                            title = "Agotados",

                            value = outOfStockCount.toString(),

                            color = ExpenseRed
                        )
                    }
                }
            }

            // NUEVO PRODUCTO

            item {

                Button(
                    onClick = {
                        openEditor(null)
                    },

                    enabled = !state.isSaving,

                    modifier = Modifier.fillMaxWidth(),

                    shape = RoundedCornerShape(16.dp),

                    colors = ButtonDefaults.buttonColors(
                        containerColor = PrimaryGreen,
                        contentColor = AmoledBlack
                    )
                ) {

                    Icon(
                        imageVector = GastifyIcons.add,
                        contentDescription = null
                    )

                    Spacer(Modifier.width(8.dp))

                    Text("Nuevo producto")
                }
            }

            // BUSCADOR

            item {

                OutlinedTextField(
                    value = search,

                    onValueChange = {
                        search = it
                    },

                    label = {
                        Text("Buscar producto")
                    },

                    leadingIcon = {

                        Icon(
                            imageVector = Icons.Outlined.Search,
                            contentDescription = null
                        )
                    },

                    singleLine = true,

                    modifier = Modifier.fillMaxWidth()
                )
            }

            // FILTROS

            item {

                Row(
                    horizontalArrangement =
                        Arrangement.spacedBy(8.dp)
                ) {

                    FilterChip(
                        selected = stockFilter == "ALL",

                        onClick = {
                            stockFilter = "ALL"
                        },

                        label = {
                            Text("Todos")
                        }
                    )

                    FilterChip(
                        selected = stockFilter == "LOW",

                        onClick = {
                            stockFilter = "LOW"
                        },

                        label = {
                            Text("Stock bajo")
                        }
                    )

                    FilterChip(
                        selected = stockFilter == "OUT",

                        onClick = {
                            stockFilter = "OUT"
                        },

                        label = {
                            Text("Agotados")
                        }
                    )
                }
            }

            item {

                Text(
                    text = "Productos (${filteredProducts.size})",

                    fontSize = 20.sp,

                    fontWeight = FontWeight.SemiBold,

                    color = TextPrimary
                )
            }

            // PRODUCTOS

            if (
                filteredProducts.isEmpty() &&
                !state.isLoading
            ) {

                item {

                    InventoryEmptyCard(
                        text = if (state.products.isEmpty()) {

                            "Todavía no tienes productos registrados."

                        } else {

                            "No se encontraron productos."
                        }
                    )
                }

            } else {

                items(
                    items = filteredProducts,

                    key = {
                        it.id
                    }
                ) { product ->

                    InventoryProductCard(
                        product = product,

                        enabled = !state.isSaving,

                        onEdit = {
                            openEditor(product)
                        },

                        onDelete = {
                            deletingProduct = product
                        },

                        onStock = {
                            openStockMovement(product)
                        }
                    )
                }
            }

            // ==================================
            // HISTORIAL DE MOVIMIENTOS
            // ==================================

            item {

                Spacer(Modifier.height(20.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {

                    Icon(
                        imageVector = Icons.Outlined.History,

                        contentDescription = null,

                        tint = PrimaryGreen
                    )

                    Spacer(Modifier.width(10.dp))

                    Text(
                        text = "Historial de stock",

                        fontSize = 20.sp,

                        fontWeight = FontWeight.SemiBold,

                        color = TextPrimary
                    )
                }

                Spacer(Modifier.height(8.dp))

                Text(
                    text = "Últimos 50 movimientos registrados.",

                    fontSize = 12.sp,

                    color = TextSecondary
                )
            }

            if (state.isHistoryLoading) {

                item {

                    CircularProgressIndicator(
                        color = PrimaryGreen,

                        modifier = Modifier.padding(20.dp)
                    )
                }

            } else if (
                state.stockMovements.isEmpty()
            ) {

                item {

                    InventoryEmptyCard(
                        text = "Todavía no tienes movimientos de inventario."
                    )
                }

            } else {

                items(
                    items = state.stockMovements,

                    key = {
                        "stock_${it.id}"
                    }
                ) { movement ->

                    StockHistoryCard(
                        movement = movement
                    )
                }
            }
        }

        SnackbarHost(
            hostState = snackbar,

            modifier = Modifier.align(
                Alignment.BottomCenter
            )
        )
    }

    // ==========================================
    // CREAR / EDITAR PRODUCTO
    // ==========================================

    if (showEditor) {

        val currentProduct = editingProduct

        val quantity = quantityText.toIntOrNull()

        val minimum = minQuantityText.toIntOrNull()

        val cost = parseInventoryMoney(costText)

        val price = parseInventoryMoney(priceText)

        val duplicateSku = sku.isNotBlank() &&
            state.products.any {

                it.id != currentProduct?.id &&
                    it.sku.equals(
                        sku.trim(),
                        ignoreCase = true
                    )
            }

        val valid =
            name.trim().isNotEmpty() &&
                quantity != null &&
                quantity in 0..MAX_INVENTORY_QUANTITY &&
                minimum != null &&
                minimum in 0..MAX_INVENTORY_QUANTITY &&
                cost != null &&
                price != null &&
                !duplicateSku &&
                !state.isSaving

        AlertDialog(
            onDismissRequest = {

                if (!state.isSaving) {
                    showEditor = false
                }
            },

            containerColor = SurfaceDark,

            title = {

                Text(
                    text = if (currentProduct == null) {
                        "Nuevo producto"
                    } else {
                        "Editar producto"
                    }
                )
            },

            text = {

                Column(
                    modifier = Modifier
                        .heightIn(max = 480.dp)
                        .verticalScroll(
                            rememberScrollState()
                        )
                ) {

                    OutlinedTextField(
                        value = name,

                        onValueChange = {
                            name = it.take(80)
                        },

                        label = {
                            Text("Nombre")
                        },

                        singleLine = true,

                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(Modifier.height(14.dp))

                    OutlinedTextField(
                        value = sku,

                        onValueChange = {
                            sku = it.take(40)
                        },

                        label = {
                            Text("Código SKU")
                        },

                        singleLine = true,

                        isError = duplicateSku,

                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(Modifier.height(14.dp))

                    OutlinedTextField(
                        value = description,

                        onValueChange = {
                            description = it.take(200)
                        },

                        label = {
                            Text("Descripción")
                        },

                        maxLines = 3,

                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(Modifier.height(18.dp))

                    Text(
                        text = "Control de stock",

                        fontWeight = FontWeight.SemiBold
                    )

                    Spacer(Modifier.height(12.dp))

                    if (currentProduct == null) {

                        OutlinedTextField(
                            value = quantityText,

                            onValueChange = {
                                quantityText = it.take(7)
                            },

                            label = {
                                Text("Cantidad inicial")
                            },

                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Number
                            ),

                            singleLine = true,

                            modifier = Modifier.fillMaxWidth()
                        )

                    } else {

                        Text(
                            text = "Stock actual: ${
                                state.products.firstOrNull {
                                    it.id == currentProduct.id
                                }?.quantity ?: currentProduct.quantity
                            } unidades",

                            color = PrimaryGreen,

                            fontWeight = FontWeight.SemiBold
                        )

                        Spacer(Modifier.height(6.dp))

                        Text(
                            text = "Para cambiar la cantidad utiliza «Mover stock».",

                            color = TextSecondary,

                            fontSize = 12.sp
                        )
                    }

                    Spacer(Modifier.height(14.dp))

                    OutlinedTextField(
                        value = minQuantityText,

                        onValueChange = {
                            minQuantityText = it.take(7)
                        },

                        label = {
                            Text("Stock mínimo")
                        },

                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number
                        ),

                        singleLine = true,

                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(Modifier.height(18.dp))

                    Text(
                        text = "Precios",

                        fontWeight = FontWeight.SemiBold
                    )

                    Spacer(Modifier.height(12.dp))

                    OutlinedTextField(
                        value = costText,

                        onValueChange = {
                            costText = it.take(16)
                        },

                        label = {
                            Text("Costo de compra (S/)")
                        },

                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Decimal
                        ),

                        singleLine = true,

                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(Modifier.height(12.dp))

                    OutlinedTextField(
                        value = priceText,

                        onValueChange = {
                            priceText = it.take(16)
                        },

                        label = {
                            Text("Precio de venta (S/)")
                        },

                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Decimal
                        ),

                        singleLine = true,

                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },

            confirmButton = {

                TextButton(
                    enabled = valid,

                    onClick = {

                        if (
                            quantity != null &&
                            minimum != null &&
                            cost != null &&
                            price != null
                        ) {

                            inventoryVm.saveProduct(

                                product = InventoryProduct(
                                    id = currentProduct?.id ?: "",

                                    name = name.trim(),

                                    sku = sku.trim(),

                                    description =
                                        description.trim(),

                                    quantity = if (
                                        currentProduct == null
                                    ) {
                                        quantity
                                    } else {
                                        currentProduct.quantity
                                    },

                                    minQuantity = minimum,

                                    costCents = cost,

                                    priceCents = price,

                                    createdAt =
                                        currentProduct?.createdAt
                                ),

                                onSuccess = {

                                    showEditor = false
                                }
                            )
                        }
                    }
                ) {

                    Text(
                        text = if (state.isSaving) {
                            "Guardando..."
                        } else {
                            "Guardar"
                        },

                        color = PrimaryGreen
                    )
                }
            },

            dismissButton = {

                TextButton(
                    enabled = !state.isSaving,

                    onClick = {
                        showEditor = false
                    }
                ) {

                    Text("Cancelar")
                }
            }
        )
    }

    // ==========================================
    // MOVIMIENTO DE STOCK
    // ==========================================

    val selectedStockProduct = state.products
        .firstOrNull {
            it.id == stockProductId
        }

    if (selectedStockProduct != null) {

        val type = StockMovementType.entries
            .firstOrNull {
                it.name == stockTypeName
            } ?: StockMovementType.ENTRY

        val inputQuantity = stockQuantityText.toIntOrNull()

        val currentQuantity = selectedStockProduct.quantity

        val resultQuantity = inputQuantity?.let {

            when (type) {

                StockMovementType.ENTRY -> {

                    currentQuantity.toLong() +
                        it.toLong()
                }

                StockMovementType.EXIT -> {

                    currentQuantity.toLong() -
                        it.toLong()
                }

                StockMovementType.ADJUSTMENT -> {

                    it.toLong()
                }
            }
        }

        val validMovement =
            inputQuantity != null &&
                inputQuantity in 0..MAX_INVENTORY_QUANTITY &&
                (
                    type == StockMovementType.ADJUSTMENT ||
                        inputQuantity > 0
                ) &&
                resultQuantity != null &&
                resultQuantity in
                    0L..MAX_INVENTORY_QUANTITY.toLong() &&
                resultQuantity != currentQuantity.toLong() &&
                !state.isSaving

        AlertDialog(
            onDismissRequest = {

                if (!state.isSaving) {
                    stockProductId = null
                }
            },

            containerColor = SurfaceDark,

            title = {

                Text("Movimiento de stock")
            },

            text = {

                Column(
                    modifier = Modifier
                        .heightIn(max = 450.dp)
                        .verticalScroll(
                            rememberScrollState()
                        )
                ) {

                    Text(
                        text = selectedStockProduct.name,

                        fontSize = 17.sp,

                        fontWeight = FontWeight.Bold,

                        color = TextPrimary
                    )

                    Spacer(Modifier.height(8.dp))

                    Text(
                        text = "Stock actual: $currentQuantity unidades",

                        color = PrimaryGreen,

                        fontSize = 14.sp
                    )

                    Spacer(Modifier.height(20.dp))

                    Text(
                        text = "Tipo de movimiento",

                        fontWeight = FontWeight.Medium
                    )

                    Spacer(Modifier.height(10.dp))

                    Column {

                        Row(
                            horizontalArrangement =
                                Arrangement.spacedBy(8.dp)
                        ) {

                            FilterChip(
                                selected =
                                    type == StockMovementType.ENTRY,

                                onClick = {

                                    stockTypeName =
                                        StockMovementType.ENTRY.name

                                    stockQuantityText = "1"
                                },

                                label = {
                                    Text("Entrada")
                                }
                            )

                            FilterChip(
                                selected =
                                    type == StockMovementType.EXIT,

                                onClick = {

                                    stockTypeName =
                                        StockMovementType.EXIT.name

                                    stockQuantityText = "1"
                                },

                                label = {
                                    Text("Salida")
                                }
                            )
                        }

                        FilterChip(
                            selected =
                                type == StockMovementType.ADJUSTMENT,

                            onClick = {

                                stockTypeName =
                                    StockMovementType.ADJUSTMENT.name

                                stockQuantityText =
                                    currentQuantity.toString()
                            },

                            label = {
                                Text("Ajustar stock")
                            }
                        )
                    }

                    Spacer(Modifier.height(18.dp))

                    OutlinedTextField(
                        value = stockQuantityText,

                        onValueChange = {
                            stockQuantityText = it.take(7)
                        },

                        label = {

                            Text(
                                if (
                                    type ==
                                    StockMovementType.ADJUSTMENT
                                ) {

                                    "Cantidad final correcta"

                                } else {

                                    "Unidades a mover"
                                }
                            )
                        },

                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number
                        ),

                        singleLine = true,

                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(Modifier.height(16.dp))

                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = CardDark
                        )
                    ) {

                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {

                            Text(
                                text = "Resultado estimado",

                                fontSize = 12.sp,

                                color = TextSecondary
                            )

                            Spacer(Modifier.height(10.dp))

                            Text(
                                text = if (
                                    resultQuantity != null &&
                                    resultQuantity in
                                        0L..MAX_INVENTORY_QUANTITY.toLong()
                                ) {

                                    "$resultQuantity unidades"

                                } else {

                                    "Cantidad inválida"
                                },

                                fontSize = 22.sp,

                                fontWeight = FontWeight.Bold,

                                color = if (
                                    validMovement
                                ) {
                                    PrimaryGreen
                                } else {
                                    ExpenseRed
                                }
                            )
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    OutlinedTextField(
                        value = stockNote,

                        onValueChange = {
                            stockNote = it.take(200)
                        },

                        label = {
                            Text("Motivo o descripción")
                        },

                        placeholder = {

                            Text(
                                when (type) {

                                    StockMovementType.ENTRY ->
                                        "Compra de mercadería"

                                    StockMovementType.EXIT ->
                                        "Salida de almacén"

                                    StockMovementType.ADJUSTMENT ->
                                        "Corrección de inventario"
                                }
                            )
                        },

                        maxLines = 3,

                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(Modifier.height(10.dp))

                    Text(
                        text = "La cantidad definitiva se calculará en Firebase al confirmar la operación.",

                        color = TextSecondary,

                        fontSize = 12.sp,

                        lineHeight = 18.sp
                    )
                }
            },

            confirmButton = {

                TextButton(
                    enabled = validMovement,

                    onClick = {

                        if (inputQuantity != null) {

                            inventoryVm.registerStockMovement(

                                productId =
                                    selectedStockProduct.id,

                                type = type,

                                quantityValue = inputQuantity,

                                note = stockNote.trim(),

                                onSuccess = {

                                    stockProductId = null
                                }
                            )
                        }
                    }
                ) {

                    Text(
                        text = if (state.isSaving) {

                            "Registrando..."

                        } else {

                            "Confirmar"
                        },

                        color = PrimaryGreen
                    )
                }
            },

            dismissButton = {

                TextButton(
                    enabled = !state.isSaving,

                    onClick = {
                        stockProductId = null
                    }
                ) {

                    Text("Cancelar")
                }
            }
        )
    }

    // ==========================================
    // ELIMINAR PRODUCTO
    // ==========================================

    deletingProduct?.let { product ->

        AlertDialog(
            onDismissRequest = {
                deletingProduct = null
            },

            containerColor = SurfaceDark,

            title = {
                Text("Eliminar producto")
            },

            text = {

                Text(
                    text = "¿Eliminar \"${product.name}\"?\n\nLos movimientos de stock ya registrados permanecerán en el historial.",

                    color = TextSecondary
                )
            },

            confirmButton = {

                TextButton(
                    enabled = !state.isSaving,

                    onClick = {

                        inventoryVm.deleteProduct(
                            product.id
                        )

                        deletingProduct = null
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
                        deletingProduct = null
                    }
                ) {

                    Text("Cancelar")
                }
            }
        )
    }
}

// ==========================================
// TARJETA DE RESUMEN
// ==========================================

@Composable
private fun InventorySummaryCard(
    modifier: Modifier,
    title: String,
    value: String,
    color: Color
) {

    Card(
        modifier = modifier,

        shape = RoundedCornerShape(18.dp),

        colors = CardDefaults.cardColors(
            containerColor = SurfaceDark
        )
    ) {

        Column(
            modifier = Modifier.padding(18.dp)
        ) {

            Text(
                text = title,

                color = TextSecondary,

                fontSize = 12.sp
            )

            Spacer(Modifier.height(12.dp))

            Text(
                text = value,

                color = color,

                fontSize = 25.sp,

                fontWeight = FontWeight.Bold
            )
        }
    }
}

// ==========================================
// TARJETA DE PRODUCTO
// ==========================================

@Composable
private fun InventoryProductCard(
    product: InventoryProduct,
    enabled: Boolean,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onStock: () -> Unit
) {

    val stockColor = when {

        product.isOutOfStock -> ExpenseRed

        product.isLowStock -> InventoryWarning

        else -> PrimaryGreen
    }

    val stockText = when {

        product.isOutOfStock -> "Agotado"

        product.isLowStock -> "Stock bajo"

        else -> "Disponible"
    }

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

                Surface(
                    color = CardDark,

                    shape = RoundedCornerShape(14.dp)
                ) {

                    Box(
                        modifier = Modifier.size(50.dp),

                        contentAlignment = Alignment.Center
                    ) {

                        Icon(
                            imageVector = Icons.Outlined.Inventory2,

                            contentDescription = null,

                            tint = PrimaryGreen,

                            modifier = Modifier.size(28.dp)
                        )
                    }
                }

                Spacer(Modifier.width(12.dp))

                Column(
                    modifier = Modifier.weight(1f)
                ) {

                    Text(
                        text = product.name,

                        color = TextPrimary,

                        fontSize = 17.sp,

                        fontWeight = FontWeight.SemiBold,

                        maxLines = 2,

                        overflow = TextOverflow.Ellipsis
                    )

                    if (product.sku.isNotBlank()) {

                        Spacer(Modifier.height(5.dp))

                        Text(
                            text = "SKU: ${product.sku}",

                            color = TextSecondary,

                            fontSize = 12.sp
                        )
                    }
                }
            }

            if (product.description.isNotBlank()) {

                Spacer(Modifier.height(12.dp))

                Text(
                    text = product.description,

                    color = TextSecondary,

                    fontSize = 12.sp
                )
            }

            Spacer(Modifier.height(16.dp))

            HorizontalDivider(
                color = BorderDark
            )

            Spacer(Modifier.height(14.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {

                Icon(
                    imageVector = Icons.Outlined.WarningAmber,

                    contentDescription = null,

                    tint = stockColor,

                    modifier = Modifier.size(18.dp)
                )

                Spacer(Modifier.width(6.dp))

                Text(
                    text = stockText,

                    color = stockColor,

                    fontWeight = FontWeight.SemiBold
                )

                Spacer(Modifier.weight(1f))

                Text(
                    text = "${product.quantity} unidades",

                    color = TextPrimary,

                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(Modifier.height(10.dp))

            Text(
                text = "Stock mínimo: ${product.minQuantity}",

                color = TextSecondary,

                fontSize = 12.sp
            )

            Spacer(Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),

                horizontalArrangement =
                    Arrangement.SpaceBetween
            ) {

                Column {

                    Text(
                        text = "Costo",

                        color = TextSecondary,

                        fontSize = 12.sp
                    )

                    Spacer(Modifier.height(6.dp))

                    Text(
                        text = inventoryMoney(
                            product.costCents
                        ),

                        color = TextPrimary,

                        fontWeight = FontWeight.SemiBold
                    )
                }

                Column {

                    Text(
                        text = "Venta",

                        color = TextSecondary,

                        fontSize = 12.sp
                    )

                    Spacer(Modifier.height(6.dp))

                    Text(
                        text = inventoryMoney(
                            product.priceCents
                        ),

                        color = PrimaryGreen,

                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(Modifier.height(18.dp))

            Button(
                onClick = onStock,

                enabled = enabled,

                modifier = Modifier.fillMaxWidth(),

                shape = RoundedCornerShape(12.dp),

                colors = ButtonDefaults.buttonColors(
                    containerColor = PrimaryGreen,
                    contentColor = AmoledBlack
                )
            ) {

                Icon(
                    imageVector = Icons.Outlined.SwapVert,

                    contentDescription = null
                )

                Spacer(Modifier.width(8.dp))

                Text(
                    text = "Mover stock",

                    fontWeight = FontWeight.Bold
                )
            }

            Row {

                TextButton(
                    onClick = onEdit,

                    enabled = enabled
                ) {

                    Icon(
                        imageVector = GastifyIcons.edit,

                        contentDescription = null,

                        modifier = Modifier.size(18.dp)
                    )

                    Spacer(Modifier.width(5.dp))

                    Text("Editar")
                }

                TextButton(
                    onClick = onDelete,

                    enabled = enabled
                ) {

                    Icon(
                        imageVector = GastifyIcons.delete,

                        contentDescription = null,

                        tint = ExpenseRed,

                        modifier = Modifier.size(18.dp)
                    )

                    Spacer(Modifier.width(5.dp))

                    Text(
                        text = "Eliminar",

                        color = ExpenseRed
                    )
                }
            }
        }
    }
}

// ==========================================
// HISTORIAL
// ==========================================

@Composable
private fun StockHistoryCard(
    movement: StockMovement
) {

    val type = StockMovementType.entries
        .firstOrNull {
            it.name == movement.type
        } ?: StockMovementType.ADJUSTMENT

    val movementColor = when {

        movement.quantityDelta > 0 -> IncomeGreen

        movement.quantityDelta < 0 -> ExpenseRed

        else -> PrimaryGreen
    }

    val date = movement.createdAt?.let {

        SimpleDateFormat(
            "dd MMM yyyy · HH:mm",
            Locale.forLanguageTag("es-PE")
        ).format(it.toDate())

    } ?: "Sincronizando..."

    Card(
        modifier = Modifier.fillMaxWidth(),

        shape = RoundedCornerShape(18.dp),

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
                    imageVector = Icons.Outlined.History,

                    contentDescription = null,

                    tint = movementColor
                )

                Spacer(Modifier.width(10.dp))

                Column(
                    modifier = Modifier.weight(1f)
                ) {

                    Text(
                        text = movement.productName,

                        color = TextPrimary,

                        fontWeight = FontWeight.SemiBold,

                        maxLines = 1,

                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(Modifier.height(5.dp))

                    Text(
                        text = stockTypeName(type),

                        color = TextSecondary,

                        fontSize = 12.sp
                    )
                }

                Text(
                    text = if (
                        movement.quantityDelta > 0
                    ) {

                        "+${movement.quantityDelta}"

                    } else {

                        movement.quantityDelta.toString()
                    },

                    color = movementColor,

                    fontWeight = FontWeight.Bold,

                    fontSize = 20.sp
                )
            }

            Spacer(Modifier.height(14.dp))

            Text(
                text = "Stock: ${movement.previousQuantity} → ${movement.resultingQuantity}",

                color = TextPrimary,

                fontSize = 13.sp
            )

            if (movement.note.isNotBlank()) {

                Spacer(Modifier.height(8.dp))

                Text(
                    text = movement.note,

                    color = TextSecondary,

                    fontSize = 12.sp
                )
            }

            Spacer(Modifier.height(10.dp))

            Text(
                text = date,

                color = TextSecondary,

                fontSize = 11.sp
            )
        }
    }
}

// ==========================================
// TARJETA VACÍA
// ==========================================

@Composable
private fun InventoryEmptyCard(
    text: String
) {

    Card(
        modifier = Modifier.fillMaxWidth(),

        colors = CardDefaults.cardColors(
            containerColor = SurfaceDark
        )
    ) {

        Text(
            text = text,

            modifier = Modifier.padding(22.dp),

            color = TextSecondary,

            fontSize = 13.sp
        )
    }
}
