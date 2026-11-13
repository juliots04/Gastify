package com.example.gastify.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape

import androidx.compose.material3.*

import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable

import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

import androidx.lifecycle.viewmodel.compose.viewModel

import com.example.gastify.data.model.CommerceOperation
import com.example.gastify.data.model.CommerceOperationType
import com.example.gastify.data.model.InventoryProduct
import com.example.gastify.data.model.Workspace

import com.example.gastify.ui.components.GastifyIcons
import com.example.gastify.ui.theme.*

import com.example.gastify.viewmodel.InventoryViewModel
import com.example.gastify.viewmodel.ProfitabilityViewModel

import java.math.BigDecimal
import java.math.RoundingMode
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

private val ProfitWarningColor =
    Color(0xFFFFBF69)

// ==========================================
// MODELOS INTERNOS
// ==========================================

private data class HistoricalProductReport(

    val id: String,

    val name: String,

    val unitsSold: Long,

    val salesCount: Int,

    val salesWithCost: Int,

    val revenueCents: BigDecimal,

    val knownRevenueCents: BigDecimal,

    val historicalCostCents: BigDecimal,

    val historicalProfitCents: BigDecimal
)

private data class HistoricalProfitReport(

    val salesRevenueCents: BigDecimal,

    val purchasePaymentsCents: BigDecimal,

    val cashFlowCents: BigDecimal,

    val knownRevenueCents: BigDecimal,

    val historicalCostCents: BigDecimal,

    val historicalGrossProfitCents: BigDecimal,

    val historicalMargin: Double,

    val inventoryValueCents: BigDecimal,

    val totalSales: Int,

    val totalPurchases: Int,

    val salesWithCost: Int,

    val salesWithoutCost: Int,

    val soldUnits: Long,

    val purchasedUnits: Long,

    val lossProducts: Int,

    val products: List<HistoricalProductReport>
)

// ==========================================
// FORMATEAR DINERO
// ==========================================

private fun profitMoney(
    cents: BigDecimal
): String {

    val value = cents.abs()
        .movePointLeft(2)
        .setScale(
            2,
            RoundingMode.HALF_UP
        )
        .toPlainString()

    return if (cents.signum() < 0) {
        "-S/ $value"
    } else {
        "S/ $value"
    }
}

// ==========================================
// PORCENTAJE
// ==========================================

private fun profitPercentage(
    value: Double
): String {

    return String.format(
        Locale.forLanguageTag("es-PE"),
        "%.1f%%",
        value
    )
}

// ==========================================
// FECHAS
// ==========================================

private fun profitMonthRange(
    offset: Int
): Pair<Long, Long> {

    val start = Calendar.getInstance().apply {

        set(Calendar.DAY_OF_MONTH, 1)

        set(Calendar.HOUR_OF_DAY, 0)

        set(Calendar.MINUTE, 0)

        set(Calendar.SECOND, 0)

        set(Calendar.MILLISECOND, 0)

        add(Calendar.MONTH, offset)
    }

    val end = (
        start.clone() as Calendar
    ).apply {

        add(Calendar.MONTH, 1)
    }

    return start.timeInMillis to
        end.timeInMillis
}

private fun profitMonthName(
    millis: Long
): String {

    val text = SimpleDateFormat(
        "MMMM yyyy",
        Locale.forLanguageTag("es-PE")
    ).format(
        java.util.Date(millis)
    )

    return text.replaceFirstChar {
        it.uppercase()
    }
}

// ==========================================
// SUMAR TOTALES
// ==========================================

private fun sumCommerceTotals(
    operations: List<CommerceOperation>
): BigDecimal {

    return operations.fold(
        BigDecimal.ZERO
    ) { total, operation ->

        total.add(
            BigDecimal.valueOf(
                operation.totalCents
            )
        )
    }
}

// ==========================================
// CREAR REPORTE
// ==========================================

private fun calculateHistoricalReport(
    operations: List<CommerceOperation>,
    products: List<InventoryProduct>
): HistoricalProfitReport {

    val sales = operations.filter {

        it.type == CommerceOperationType.SALE.name
    }

    val purchases = operations.filter {

        it.type == CommerceOperationType.PURCHASE.name
    }

    val salesRevenue = sumCommerceTotals(
        sales
    )

    val purchasePayments = sumCommerceTotals(
        purchases
    )

    val cashFlow = salesRevenue.subtract(
        purchasePayments
    )

    // ======================================
    // VENTAS CON COSTO HISTÓRICO
    // ======================================

    val salesWithCost = sales.filter {

        it.hasHistoricalCost
    }

    val salesWithoutCost = sales.size -
        salesWithCost.size

    val knownRevenue = sumCommerceTotals(
        salesWithCost
    )

    val totalHistoricalCost = salesWithCost.fold(
        BigDecimal.ZERO
    ) { total, sale ->

        total.add(
            BigDecimal.valueOf(
                sale.historicalTotalCostCents ?: 0L
            )
        )
    }

    val historicalProfit = knownRevenue.subtract(
        totalHistoricalCost
    )

    val margin = if (
        knownRevenue > BigDecimal.ZERO
    ) {

        historicalProfit
            .divide(
                knownRevenue,
                6,
                RoundingMode.HALF_UP
            )
            .multiply(
                BigDecimal.valueOf(100)
            )
            .toDouble()

    } else {

        0.0
    }

    // ======================================
    // RENTABILIDAD POR PRODUCTO
    // ======================================

    val productReports = sales
        .groupBy {
            it.productId
        }
        .map { (productId, productSales) ->

            val withCost = productSales.filter {

                it.hasHistoricalCost
            }

            val productRevenue = sumCommerceTotals(
                productSales
            )

            val revenueWithCost = sumCommerceTotals(
                withCost
            )

            val historicalCost = withCost.fold(
                BigDecimal.ZERO
            ) { total, sale ->

                total.add(
                    BigDecimal.valueOf(
                        sale.historicalTotalCostCents ?: 0L
                    )
                )
            }

            val productProfit = revenueWithCost.subtract(
                historicalCost
            )

            HistoricalProductReport(

                id = productId,

                name = productSales.firstOrNull()
                    ?.productName
                    ?.ifBlank { "Producto" }
                    ?: "Producto",

                unitsSold = productSales.sumOf {
                    it.quantity.toLong()
                },

                salesCount = productSales.size,

                salesWithCost = withCost.size,

                revenueCents = productRevenue,

                knownRevenueCents = revenueWithCost,

                historicalCostCents = historicalCost,

                historicalProfitCents = productProfit
            )
        }
        .sortedByDescending {
            it.revenueCents
        }

    // ======================================
    // INVENTARIO AL COSTO ACTUAL
    // ======================================

    val inventoryValue = products.fold(
        BigDecimal.ZERO
    ) { total, product ->

        total.add(
            BigDecimal.valueOf(
                product.costCents
            ).multiply(
                BigDecimal.valueOf(
                    product.quantity.toLong()
                )
            )
        )
    }

    // ======================================
    // PRODUCTOS CON PÉRDIDAS
    // ======================================

    val lossProducts = productReports.count {

        it.salesWithCost > 0 &&
            it.historicalProfitCents.signum() < 0
    }

    return HistoricalProfitReport(

        salesRevenueCents = salesRevenue,

        purchasePaymentsCents = purchasePayments,

        cashFlowCents = cashFlow,

        knownRevenueCents = knownRevenue,

        historicalCostCents = totalHistoricalCost,

        historicalGrossProfitCents = historicalProfit,

        historicalMargin = margin,

        inventoryValueCents = inventoryValue,

        totalSales = sales.size,

        totalPurchases = purchases.size,

        salesWithCost = salesWithCost.size,

        salesWithoutCost = salesWithoutCost,

        soldUnits = sales.sumOf {
            it.quantity.toLong()
        },

        purchasedUnits = purchases.sumOf {
            it.quantity.toLong()
        },

        lossProducts = lossProducts,

        products = productReports
    )
}

// ==========================================
// PANTALLA PRINCIPAL
// ==========================================

@Composable
fun ProfitabilityScreen(
    userId: String,
    workspace: Workspace,
    onBack: () -> Unit
) {

    val profitabilityVm: ProfitabilityViewModel = viewModel(

        key = "profitability_${userId}_${workspace.id}",

        factory = ProfitabilityViewModel.factory(
            userId,
            workspace.id
        )
    )

    val inventoryVm: InventoryViewModel = viewModel(

        key = "inventory_${userId}_${workspace.id}",

        factory = InventoryViewModel.factory(
            userId,
            workspace.id
        )
    )

    val profitabilityState by profitabilityVm
        .uiState
        .collectAsState()

    val inventoryState by inventoryVm
        .uiState
        .collectAsState()

    // ======================================
    // MES SELECCIONADO
    // ======================================

    var monthOffset by rememberSaveable(
        workspace.id
    ) {

        mutableIntStateOf(0)
    }

    val monthRange = remember(
        monthOffset
    ) {

        profitMonthRange(monthOffset)
    }

    val monthName = remember(
        monthRange.first
    ) {

        profitMonthName(
            monthRange.first
        )
    }

    // ======================================
    // FIREBASE
    // ======================================

    DisposableEffect(
        profitabilityVm,
        monthRange.first,
        monthRange.second
    ) {

        profitabilityVm.startListening(
            monthRange.first,
            monthRange.second
        )

        onDispose {

            profitabilityVm.stopListening()
        }
    }

    DisposableEffect(inventoryVm) {

        inventoryVm.startListening()

        onDispose {

            inventoryVm.stopListening()
        }
    }

    // ======================================
    // ERRORES
    // ======================================

    val snackbar = remember {
        SnackbarHostState()
    }

    LaunchedEffect(
        profitabilityState.errorMessage,
        inventoryState.errorMessage
    ) {

        val error = profitabilityState.errorMessage
            ?: inventoryState.errorMessage

        if (error != null) {

            snackbar.showSnackbar(error)

            profitabilityVm.clearError()

            inventoryVm.clearMessages()
        }
    }

    // ======================================
    // CALCULAR REPORTE
    // ======================================

    val report = remember(
        profitabilityState.operations,
        inventoryState.products
    ) {

        calculateHistoricalReport(
            profitabilityState.operations,
            inventoryState.products
        )
    }

    val isLoading =
        profitabilityState.isLoading ||
            inventoryState.isLoading

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

            verticalArrangement =
                Arrangement.spacedBy(14.dp)

        ) {

            // ==================================
            // ENCABEZADO
            // ==================================

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
                        imageVector = GastifyIcons.statistics,
                        contentDescription = null,
                        tint = PrimaryGreen,
                        modifier = Modifier.size(30.dp)
                    )

                    Spacer(Modifier.width(12.dp))

                    Text(
                        text = "Rentabilidad",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }

                Spacer(Modifier.height(8.dp))

                Text(
                    text = workspace.name,
                    color = TextSecondary,
                    fontSize = 14.sp
                )

                Spacer(Modifier.height(18.dp))
            }

            // ==================================
            // SELECTOR DE MES
            // ==================================

            item {

                Card(
                    modifier = Modifier.fillMaxWidth(),

                    shape = RoundedCornerShape(18.dp),

                    colors = CardDefaults.cardColors(
                        containerColor = SurfaceDark
                    )
                ) {

                    Row(
                        modifier = Modifier.padding(12.dp),

                        verticalAlignment = Alignment.CenterVertically,

                        horizontalArrangement =
                            Arrangement.SpaceBetween
                    ) {

                        IconButton(
                            onClick = {

                                if (monthOffset > -11) {
                                    monthOffset--
                                }
                            },

                            enabled = monthOffset > -11
                        ) {

                            Icon(
                                imageVector = GastifyIcons.back,
                                contentDescription = "Mes anterior",
                                tint = PrimaryGreen
                            )
                        }

                        Text(
                            text = monthName,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary,
                            fontSize = 15.sp
                        )

                        IconButton(
                            onClick = {

                                if (monthOffset < 0) {
                                    monthOffset++
                                }
                            },

                            enabled = monthOffset < 0
                        ) {

                            Icon(
                                imageVector = GastifyIcons.next,
                                contentDescription = "Mes siguiente",
                                tint = PrimaryGreen
                            )
                        }
                    }
                }
            }

            // ==================================
            // ESTADO DE CONEXIÓN
            // ==================================

            if (profitabilityState.isFromCache) {

                item {

                    Text(
                        text = "Mostrando información almacenada localmente. Puede no incluir los últimos cambios.",
                        color = ProfitWarningColor,
                        fontSize = 12.sp,
                        lineHeight = 18.sp
                    )
                }
            }

            if (isLoading) {

                item {

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(35.dp),

                        contentAlignment = Alignment.Center
                    ) {

                        CircularProgressIndicator(
                            color = PrimaryGreen
                        )
                    }
                }

            } else {

                // ==================================
                // RESUMEN COMERCIAL
                // ==================================

                item {

                    Text(
                        text = "Resumen comercial",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )

                    Spacer(Modifier.height(14.dp))

                    HistoricalMetricCard(
                        title = "Ingresos por ventas",
                        value = profitMoney(
                            report.salesRevenueCents
                        ),
                        description = "${report.totalSales} ventas · ${report.soldUnits} unidades",
                        color = IncomeGreen,
                        icon = GastifyIcons.income
                    )

                    Spacer(Modifier.height(12.dp))

                    HistoricalMetricCard(
                        title = "Pagos por compras",
                        value = profitMoney(
                            report.purchasePaymentsCents
                        ),
                        description = "${report.totalPurchases} compras · ${report.purchasedUnits} unidades",
                        color = ExpenseRed,
                        icon = GastifyIcons.expense
                    )

                    Spacer(Modifier.height(12.dp))

                    HistoricalMetricCard(
                        title = "Flujo de caja comercial",
                        value = profitMoney(
                            report.cashFlowCents
                        ),
                        description = "Ventas cobradas menos compras pagadas.",
                        color = if (
                            report.cashFlowCents.signum() >= 0
                        ) {
                            IncomeGreen
                        } else {
                            ExpenseRed
                        },
                        icon = GastifyIcons.accounts
                    )
                }

                // ==================================
                // UTILIDAD HISTÓRICA
                // ==================================

                item {

                    Spacer(Modifier.height(12.dp))

                    Text(
                        text = "Ganancias por ventas",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )

                    Spacer(Modifier.height(12.dp))

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
                                text = "Utilidad bruta según costo registrado",
                                color = TextSecondary,
                                fontSize = 13.sp
                            )

                            Spacer(Modifier.height(12.dp))

                            Text(
                                text = if (
                                    report.salesWithCost > 0
                                ) {

                                    profitMoney(
                                        report.historicalGrossProfitCents
                                    )

                                } else {

                                    "Sin datos suficientes"
                                },

                                fontSize = 27.sp,

                                fontWeight = FontWeight.Bold,

                                color = if (
                                    report.historicalGrossProfitCents.signum() >= 0
                                ) {

                                    IncomeGreen

                                } else {

                                    ExpenseRed
                                }
                            )

                            Spacer(Modifier.height(16.dp))

                            HorizontalDivider(
                                color = BorderDark
                            )

                            Spacer(Modifier.height(16.dp))

                            HistoricalDetailRow(
                                label = "Ingresos con costo conocido",
                                value = profitMoney(
                                    report.knownRevenueCents
                                )
                            )

                            Spacer(Modifier.height(12.dp))

                            HistoricalDetailRow(
                                label = "Costo de productos vendidos",
                                value = profitMoney(
                                    report.historicalCostCents
                                )
                            )

                            Spacer(Modifier.height(18.dp))

                            Text(
                                text = "Margen bruto",
                                color = TextSecondary,
                                fontSize = 13.sp
                            )

                            Spacer(Modifier.height(8.dp))

                            Text(
                                text = if (
                                    report.salesWithCost > 0
                                ) {

                                    profitPercentage(
                                        report.historicalMargin
                                    )

                                } else {

                                    "—"
                                },

                                fontSize = 24.sp,

                                fontWeight = FontWeight.Bold,

                                color = PrimaryGreen
                            )

                            Spacer(Modifier.height(18.dp))

                            Text(
                                text = "${report.salesWithCost} de ${report.totalSales} ventas tienen un costo histórico válido.",

                                color = TextSecondary,

                                fontSize = 12.sp,

                                lineHeight = 19.sp
                            )
                        }
                    }
                }

                // ==================================
                // ADVERTENCIAS
                // ==================================

                if (
                    report.salesWithoutCost > 0 ||
                    report.lossProducts > 0
                ) {

                    item {

                        Card(
                            modifier = Modifier.fillMaxWidth(),

                            shape = RoundedCornerShape(18.dp),

                            colors = CardDefaults.cardColors(
                                containerColor = CardDark
                            )
                        ) {

                            Column(
                                modifier = Modifier.padding(18.dp)
                            ) {

                                Text(
                                    text = "Observaciones",
                                    fontWeight = FontWeight.Bold,
                                    color = ProfitWarningColor
                                )

                                if (
                                    report.salesWithoutCost > 0
                                ) {

                                    Spacer(Modifier.height(12.dp))

                                    Text(
                                        text = "${report.salesWithoutCost} ventas no tienen costo histórico válido. Sus ingresos aparecen en el total de ventas, pero no se incluyen en el cálculo de utilidad.",

                                        color = TextSecondary,

                                        fontSize = 12.sp,

                                        lineHeight = 19.sp
                                    )
                                }

                                if (
                                    report.lossProducts > 0
                                ) {

                                    Spacer(Modifier.height(12.dp))

                                    Text(
                                        text = "${report.lossProducts} productos presentan pérdidas brutas según sus ventas con costo registrado.",

                                        color = ExpenseRed,

                                        fontSize = 12.sp,

                                        lineHeight = 19.sp
                                    )
                                }
                            }
                        }
                    }
                }

                // ==================================
                // INVENTARIO
                // ==================================

                item {

                    Spacer(Modifier.height(10.dp))

                    Text(
                        text = "Inventario actual",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )

                    Spacer(Modifier.height(12.dp))

                    HistoricalMetricCard(
                        title = "Valor del inventario al costo",
                        value = profitMoney(
                            report.inventoryValueCents
                        ),
                        description = "${inventoryState.products.size} productos registrados.",
                        color = PrimaryGreen,
                        icon = GastifyIcons.accounts
                    )
                }

                // ==================================
                // RANKING
                // ==================================

                item {

                    Spacer(Modifier.height(12.dp))

                    Text(
                        text = "Rentabilidad por producto",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )

                    Spacer(Modifier.height(8.dp))

                    Text(
                        text = "Ordenados por ingresos generados durante el mes.",
                        color = TextSecondary,
                        fontSize = 12.sp
                    )
                }

                if (report.products.isEmpty()) {

                    item {

                        Card(
                            modifier = Modifier.fillMaxWidth(),

                            colors = CardDefaults.cardColors(
                                containerColor = SurfaceDark
                            )
                        ) {

                            Text(
                                text = "No se registraron ventas durante este período.",
                                modifier = Modifier.padding(22.dp),
                                color = TextSecondary
                            )
                        }
                    }

                } else {

                    items(
                        items = report.products,
                        key = {
                            it.id
                        }
                    ) { product ->

                        HistoricalProductCard(
                            product = product
                        )
                    }
                }

                // ==================================
                // ACLARACIÓN CONTABLE
                // ==================================

                item {

                    Spacer(Modifier.height(14.dp))

                    Text(
                        text = "El costo histórico corresponde al costo configurado en el producto cuando se registró la venta. No representa necesariamente el costo real por lote (FIFO o promedio ponderado). Las compras no se descuentan nuevamente de la utilidad bruta y todavía no se incluyen impuestos ni gastos operativos.",

                        color = TextSecondary,

                        fontSize = 12.sp,

                        lineHeight = 20.sp
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
}

// ==========================================
// TARJETA DE MÉTRICA
// ==========================================

@Composable
private fun HistoricalMetricCard(
    title: String,
    value: String,
    description: String,
    color: Color,
    icon: ImageVector
) {

    Card(
        modifier = Modifier.fillMaxWidth(),

        shape = RoundedCornerShape(20.dp),

        colors = CardDefaults.cardColors(
            containerColor = SurfaceDark
        )
    ) {

        Row(
            modifier = Modifier.padding(20.dp),

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
                        imageVector = icon,
                        contentDescription = null,
                        tint = color,
                        modifier = Modifier.size(25.dp)
                    )
                }
            }

            Spacer(Modifier.width(14.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {

                Text(
                    text = title,
                    color = TextSecondary,
                    fontSize = 13.sp
                )

                Spacer(Modifier.height(8.dp))

                Text(
                    text = value,
                    color = color,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(Modifier.height(8.dp))

                Text(
                    text = description,
                    color = TextSecondary,
                    fontSize = 12.sp,
                    lineHeight = 18.sp
                )
            }
        }
    }
}

// ==========================================
// DETALLE DE RENTABILIDAD
// ==========================================

@Composable
private fun HistoricalDetailRow(
    label: String,
    value: String
) {

    Row(
        modifier = Modifier.fillMaxWidth(),

        verticalAlignment = Alignment.CenterVertically,

        horizontalArrangement =
            Arrangement.SpaceBetween
    ) {

        Text(
            text = label,
            color = TextSecondary,
            fontSize = 12.sp,
            modifier = Modifier.weight(1f)
        )

        Spacer(Modifier.width(10.dp))

        Text(
            text = value,
            color = TextPrimary,
            fontWeight = FontWeight.SemiBold,
            fontSize = 13.sp
        )
    }
}

// ==========================================
// RENTABILIDAD POR PRODUCTO
// ==========================================

@Composable
private fun HistoricalProductCard(
    product: HistoricalProductReport
) {

    val profitColor = if (
        product.historicalProfitCents.signum() < 0
    ) {
        ExpenseRed
    } else {
        IncomeGreen
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

            Text(
                text = product.name,
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(Modifier.height(8.dp))

            Text(
                text = "${product.unitsSold} unidades vendidas · ${product.salesCount} operaciones",

                color = TextSecondary,

                fontSize = 12.sp
            )

            Spacer(Modifier.height(8.dp))

            Text(
                text = "Con costo histórico: ${product.salesWithCost}/${product.salesCount} ventas",

                color = PrimaryGreen,

                fontSize = 12.sp
            )

            Spacer(Modifier.height(16.dp))

            HorizontalDivider(
                color = BorderDark
            )

            Spacer(Modifier.height(14.dp))

            HistoricalDetailRow(
                label = "Ingresos totales",
                value = profitMoney(
                    product.revenueCents
                )
            )

            Spacer(Modifier.height(12.dp))

            HistoricalDetailRow(
                label = "Ingresos con costo registrado",
                value = profitMoney(
                    product.knownRevenueCents
                )
            )

            Spacer(Modifier.height(12.dp))

            HistoricalDetailRow(
                label = "Costo histórico",
                value = if (
                    product.salesWithCost > 0
                ) {

                    profitMoney(
                        product.historicalCostCents
                    )

                } else {

                    "No disponible"
                }
            )

            Spacer(Modifier.height(16.dp))

            HorizontalDivider(
                color = BorderDark
            )

            Spacer(Modifier.height(14.dp))

            Text(
                text = "Utilidad bruta",
                color = TextSecondary,
                fontSize = 12.sp
            )

            Spacer(Modifier.height(8.dp))

            Text(
                text = if (
                    product.salesWithCost > 0
                ) {

                    profitMoney(
                        product.historicalProfitCents
                    )

                } else {

                    "Sin costo histórico"
                },

                color = if (
                    product.salesWithCost > 0
                ) {
                    profitColor
                } else {
                    ProfitWarningColor
                },

                fontSize = 21.sp,

                fontWeight = FontWeight.Bold
            )

            if (
                product.salesWithCost > 0 &&
                product.historicalProfitCents.signum() < 0
            ) {

                Spacer(Modifier.height(12.dp))

                Text(
                    text = "Pérdida bruta registrada para las ventas con costo conocido.",
                    color = ExpenseRed,
                    fontSize = 12.sp,
                    lineHeight = 18.sp
                )
            }
        }
    }
}
