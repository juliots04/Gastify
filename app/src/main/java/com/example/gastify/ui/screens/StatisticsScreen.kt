package com.example.gastify.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape

import androidx.compose.material3.*

import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable

import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

import androidx.lifecycle.viewmodel.compose.viewModel

import com.example.gastify.data.model.Category
import com.example.gastify.data.model.Movement
import com.example.gastify.data.model.Workspace

import com.example.gastify.ui.components.GastifyIcons
import com.example.gastify.ui.theme.*

import com.example.gastify.viewmodel.AccountViewModel
import com.example.gastify.viewmodel.CategoryViewModel
import com.example.gastify.viewmodel.StatisticsViewModel

import java.math.BigDecimal
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

// ==========================================
// MODELOS VISUALES
// ==========================================

private data class CategoryStatistic(

    val id: String,

    val name: String,

    val icon: String,

    val color: Color,

    val amountCents: Long
)

// ==========================================
// FORMATEAR MONEDA
// ==========================================

private fun statisticsMoney(
    cents: Long
): String {

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

// ==========================================
// COLOR DE CATEGORÍA
// ==========================================

private fun statisticsCategoryColor(
    hex: String
): Color {

    return try {

        Color(
            android.graphics.Color.parseColor(hex)
        )

    } catch (_: Exception) {

        PrimaryGreen
    }
}

// ==========================================
// CALCULAR DÍA DEL MES
// ==========================================

private fun movementDay(
    movement: Movement
): Int? {

    val date = movement.createdAt
        ?.toDate()
        ?: return null

    return Calendar.getInstance().apply {

        time = date

    }.get(Calendar.DAY_OF_MONTH)
}

// ==========================================
// PANTALLA PRINCIPAL
// ==========================================

@Composable
fun StatisticsScreen(
    userId: String,
    workspace: Workspace,
    onBack: () -> Unit
) {

    // ======================================
    // MES SELECCIONADO
    // ======================================

    var monthOffset by rememberSaveable(
        workspace.id
    ) {

        mutableIntStateOf(0)
    }

    val monthCalendar = remember(monthOffset) {

        Calendar.getInstance().apply {

            set(Calendar.DAY_OF_MONTH, 1)

            set(Calendar.HOUR_OF_DAY, 0)

            set(Calendar.MINUTE, 0)

            set(Calendar.SECOND, 0)

            set(Calendar.MILLISECOND, 0)

            add(
                Calendar.MONTH,
                -monthOffset
            )
        }
    }

    val startMillis = monthCalendar.timeInMillis

    val endMillis = remember(startMillis) {

        (monthCalendar.clone() as Calendar).apply {

            add(Calendar.MONTH, 1)

        }.timeInMillis
    }

    val monthTitle = SimpleDateFormat(
        "MMMM yyyy",
        Locale.forLanguageTag("es-PE")
    ).format(monthCalendar.time).replaceFirstChar {

        if (it.isLowerCase()) {
            it.titlecase(Locale.forLanguageTag("es-PE"))
        } else {
            it.toString()
        }
    }

    val daysInMonth = monthCalendar.getActualMaximum(
        Calendar.DAY_OF_MONTH
    )

    // ======================================
    // VIEWMODELS
    // ======================================

    val statisticsVm: StatisticsViewModel = viewModel(
        key = "statistics_${userId}_${workspace.id}",
        factory = StatisticsViewModel.factory(
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

    val statisticsState by statisticsVm.uiState.collectAsState()

    val accountState by accountVm.uiState.collectAsState()

    val categoryState by categoryVm.uiState.collectAsState()

    // ======================================
    // FIREBASE
    // ======================================

    DisposableEffect(
        statisticsVm,
        startMillis,
        endMillis
    ) {

        statisticsVm.startListening(
            startMillis,
            endMillis
        )

        onDispose {

            statisticsVm.stopListening()
        }
    }

    DisposableEffect(
        accountVm,
        categoryVm
    ) {

        accountVm.startListening()

        categoryVm.startListening()

        onDispose {

            accountVm.stopListening()

            categoryVm.stopListening()
        }
    }

    // ======================================
    // FILTROS
    // ======================================

    var selectedCategoryId by rememberSaveable(
        workspace.id
    ) {

        mutableStateOf("ALL")
    }

    var chartType by rememberSaveable(
        workspace.id
    ) {

        mutableStateOf("EXPENSE")
    }

    val movements = statisticsState.movements.filter {

        selectedCategoryId == "ALL" ||
            it.categoryId == selectedCategoryId
    }

    // ======================================
    // TOTALES DEL MES
    // ======================================

    val totalIncome = movements
        .filter {

            it.type == "INCOME"
        }
        .sumOf {

            it.amountCents
        }

    val totalExpense = movements
        .filter {

            it.type == "EXPENSE"
        }
        .sumOf {

            it.amountCents
        }

    val balance = totalIncome - totalExpense

    // ======================================
    // GASTO DE TRANSPORTE
    // ======================================

    val transportAccountIds = accountState.accounts
        .filter {

            it.type == "TRANSPORT"
        }
        .map {

            it.id
        }
        .toSet()

    val transportMovements = movements.filter {

        it.type == "EXPENSE" &&
            it.accountId in transportAccountIds
    }

    val transportExpense = transportMovements.sumOf {

        it.amountCents
    }

    val transportTrips = transportMovements.size

    // ======================================
    // TOTALES POR CATEGORÍA
    // ======================================

    val categoryStatistics = movements
        .filter {

            it.type == chartType
        }
        .groupBy {

            it.categoryId
        }
        .map { (categoryId, items) ->

            val category = categoryState.categories
                .firstOrNull {

                    it.id == categoryId
                }

            CategoryStatistic(
                id = categoryId,

                name = category?.name
                    ?: "Categoría no disponible",

                icon = category?.icon ?: "",

                color = statisticsCategoryColor(
                    category?.color ?: "#00E0A4"
                ),

                amountCents = items.sumOf {
                    it.amountCents
                }
            )
        }
        .sortedByDescending {

            it.amountCents
        }

    // ======================================
    // GRÁFICO DIARIO
    // ======================================

    val dailyAmounts = List(daysInMonth) { index ->

        val day = index + 1

        movements
            .filter {

                it.type == chartType &&
                    movementDay(it) == day
            }
            .sumOf {

                it.amountCents
            }
    }

    // ======================================
    // MENSAJES
    // ======================================

    val snackbar = remember {
        SnackbarHostState()
    }

    LaunchedEffect(
        statisticsState.errorMessage,
        accountState.errorMessage,
        categoryState.errorMessage
    ) {

        val message = statisticsState.errorMessage
            ?: accountState.errorMessage
            ?: categoryState.errorMessage

        if (message != null) {

            snackbar.showSnackbar(message)

            statisticsVm.clearError()

            accountVm.clearError()

            categoryVm.clearError()
        }
    }

    // ======================================
    // INTERFAZ AMOLED
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

            verticalArrangement = Arrangement.spacedBy(14.dp)
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
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
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
                        text = "Estadísticas",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }

                Spacer(Modifier.height(8.dp))

                Text(
                    text = workspace.name,
                    fontSize = 14.sp,
                    color = TextSecondary
                )

                Spacer(Modifier.height(26.dp))
            }

            // ==================================
            // SELECCIONAR MES
            // ==================================

            item {

                Card(
                    modifier = Modifier.fillMaxWidth(),

                    shape = RoundedCornerShape(20.dp),

                    colors = CardDefaults.cardColors(
                        containerColor = SurfaceDark
                    )
                ) {

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),

                        verticalAlignment = Alignment.CenterVertically,

                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {

                        IconButton(
                            onClick = {

                                monthOffset = (
                                    monthOffset + 1
                                ).coerceAtMost(11)
                            },

                            enabled = monthOffset < 11
                        ) {

                            Icon(
                                imageVector = GastifyIcons.back,
                                contentDescription = "Mes anterior",
                                tint = PrimaryGreen
                            )
                        }

                        Text(
                            text = monthTitle,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary
                        )

                        IconButton(
                            onClick = {

                                monthOffset = (
                                    monthOffset - 1
                                ).coerceAtLeast(0)
                            },

                            enabled = monthOffset > 0
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
            // FILTRO DE CATEGORÍAS
            // ==================================

            item {

                Text(
                    text = "Filtrar por categoría",
                    fontSize = 13.sp,
                    color = TextSecondary
                )

                Spacer(Modifier.height(8.dp))

                StatisticsCategoryFilter(
                    categories = categoryState.categories,

                    selectedCategoryId = selectedCategoryId,

                    onSelect = {

                        selectedCategoryId = it
                    }
                )
            }

            // ==================================
            // CARGANDO
            // ==================================

            if (
                statisticsState.isLoading ||
                accountState.isLoading ||
                categoryState.isLoading
            ) {

                item {

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(40.dp),

                        contentAlignment = Alignment.Center
                    ) {

                        CircularProgressIndicator(
                            color = PrimaryGreen
                        )
                    }
                }

            } else {

                // ==================================
                // RESUMEN DEL MES
                // ==================================

                item {

                    Text(
                        text = "Resumen mensual",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )

                    Spacer(Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),

                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {

                        StatisticsAmountCard(
                            modifier = Modifier.weight(1f),

                            title = "Ingresos",

                            amount = totalIncome,

                            color = IncomeGreen
                        )

                        StatisticsAmountCard(
                            modifier = Modifier.weight(1f),

                            title = "Gastos",

                            amount = totalExpense,

                            color = ExpenseRed
                        )
                    }

                    Spacer(Modifier.height(12.dp))

                    Card(
                        modifier = Modifier.fillMaxWidth(),

                        shape = RoundedCornerShape(20.dp),

                        colors = CardDefaults.cardColors(
                            containerColor = SurfaceDark
                        )
                    ) {

                        Column(
                            modifier = Modifier.padding(22.dp)
                        ) {

                            Text(
                                text = "Diferencia del mes",
                                fontSize = 13.sp,
                                color = TextSecondary
                            )

                            Spacer(Modifier.height(10.dp))

                            Text(
                                text = statisticsMoney(balance),

                                fontSize = 30.sp,

                                fontWeight = FontWeight.Bold,

                                color = if (balance >= 0L) {
                                    IncomeGreen
                                } else {
                                    ExpenseRed
                                }
                            )

                            Spacer(Modifier.height(10.dp))

                            Text(
                                text = "${movements.size} movimientos en el período seleccionado",

                                fontSize = 12.sp,

                                color = TextSecondary
                            )
                        }
                    }
                }

                // ==================================
                // GRÁFICO DE MOVIMIENTOS
                // ==================================

                item {

                    Spacer(Modifier.height(12.dp))

                    Text(
                        text = "Evolución diaria",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )

                    Spacer(Modifier.height(12.dp))

                    StatisticsTypeSelector(
                        selectedType = chartType,

                        onSelected = {

                            chartType = it
                        }
                    )

                    Spacer(Modifier.height(14.dp))

                    Card(
                        modifier = Modifier.fillMaxWidth(),

                        shape = RoundedCornerShape(22.dp),

                        colors = CardDefaults.cardColors(
                            containerColor = SurfaceDark
                        )
                    ) {

                        Column(
                            modifier = Modifier.padding(20.dp)
                        ) {

                            Text(
                                text = if (chartType == "EXPENSE") {
                                    "Gastos por día"
                                } else {
                                    "Ingresos por día"
                                },

                                fontSize = 14.sp,

                                color = TextSecondary
                            )

                            Spacer(Modifier.height(20.dp))

                            StatisticsDailyChart(
                                amounts = dailyAmounts,

                                color = if (chartType == "EXPENSE") {
                                    ExpenseRed
                                } else {
                                    IncomeGreen
                                }
                            )

                            Spacer(Modifier.height(14.dp))

                            Text(
                                text = "Días del mes: 1 al $daysInMonth",

                                fontSize = 11.sp,

                                color = TextSecondary
                            )
                        }
                    }
                }

                // ==================================
                // DISTRIBUCIÓN POR CATEGORÍA
                // ==================================

                item {

                    Spacer(Modifier.height(12.dp))

                    Text(
                        text = "Por categorías",
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
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(22.dp),

                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {

                            StatisticsDonutChart(
                                categories = categoryStatistics
                            )

                            Spacer(Modifier.height(16.dp))

                            Text(
                                text = if (chartType == "EXPENSE") {
                                    "Distribución de gastos"
                                } else {
                                    "Distribución de ingresos"
                                },

                                fontSize = 13.sp,

                                color = TextSecondary
                            )
                        }
                    }
                }

                if (categoryStatistics.isEmpty()) {

                    item {

                        StatisticsEmptyCard(
                            text = "No hay movimientos de este tipo para el período seleccionado."
                        )
                    }

                } else {

                    items(
                        items = categoryStatistics,

                        key = {
                            it.id
                        }
                    ) { item ->

                        val categoryTotal = categoryStatistics.sumOf {
                            it.amountCents
                        }

                        StatisticsCategoryCard(
                            item = item,
                            total = categoryTotal
                        )
                    }
                }

                // ==================================
                // GASTOS EN TRANSPORTE
                // ==================================

                item {

                    Spacer(Modifier.height(14.dp))

                    Text(
                        text = "Análisis de transporte",
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

                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {

                                Icon(
                                    imageVector = GastifyIcons.account(
                                        "TRANSPORT"
                                    ),

                                    contentDescription = null,

                                    tint = PrimaryGreen,

                                    modifier = Modifier.size(28.dp)
                                )

                                Spacer(Modifier.width(12.dp))

                                Text(
                                    text = "Gastos de transporte",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = TextPrimary
                                )
                            }

                            Spacer(Modifier.height(20.dp))

                            Text(
                                text = statisticsMoney(
                                    transportExpense
                                ),

                                fontSize = 30.sp,

                                fontWeight = FontWeight.Bold,

                                color = PrimaryGreen
                            )

                            Spacer(Modifier.height(10.dp))

                            Text(
                                text = "$transportTrips pagos registrados",

                                fontSize = 13.sp,

                                color = TextSecondary
                            )

                            Spacer(Modifier.height(18.dp))

                            HorizontalDivider(
                                color = BorderDark
                            )

                            Spacer(Modifier.height(14.dp))

                            Text(
                                text = "Se incluyen los gastos realizados desde cuentas de tipo Transporte. Cada gasto cuenta como un pago registrado.",

                                fontSize = 12.sp,

                                lineHeight = 19.sp,

                                color = TextSecondary
                            )
                        }
                    }
                }

                // ==================================
                // SIN MOVIMIENTOS
                // ==================================

                if (movements.isEmpty()) {

                    item {

                        StatisticsEmptyCard(
                            text = "Todavía no tienes movimientos para este mes y filtro."
                        )
                    }
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
// FILTRO POR CATEGORÍA
// ==========================================

@Composable
private fun StatisticsCategoryFilter(
    categories: List<Category>,
    selectedCategoryId: String,
    onSelect: (String) -> Unit
) {

    var expanded by remember {
        mutableStateOf(false)
    }

    val selectedCategory = categories.firstOrNull {
        it.id == selectedCategoryId
    }

    Box(
        modifier = Modifier.fillMaxWidth()
    ) {

        OutlinedButton(
            onClick = {
                expanded = true
            },

            modifier = Modifier.fillMaxWidth(),

            shape = RoundedCornerShape(14.dp)
        ) {

            Text(
                text = selectedCategory?.name
                    ?: "Todas las categorías",

                modifier = Modifier.weight(1f),

                color = TextPrimary,

                maxLines = 1,

                overflow = TextOverflow.Ellipsis
            )

            Icon(
                imageVector = GastifyIcons.expand,

                contentDescription = "Seleccionar categoría",

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

            DropdownMenuItem(
                text = {

                    Text(
                        text = "Todas las categorías",
                        color = TextPrimary
                    )
                },

                onClick = {

                    onSelect("ALL")

                    expanded = false
                }
            )

            categories.forEach { category ->

                DropdownMenuItem(
                    text = {

                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {

                            Icon(
                                imageVector = GastifyIcons.category(
                                    category.icon
                                ),

                                contentDescription = null,

                                tint = statisticsCategoryColor(
                                    category.color
                                ),

                                modifier = Modifier.size(20.dp)
                            )

                            Spacer(Modifier.width(10.dp))

                            Column {

                                Text(
                                    text = category.name,
                                    color = TextPrimary
                                )

                                Text(
                                    text = if (
                                        category.type == "INCOME"
                                    ) {
                                        "Ingreso"
                                    } else {
                                        "Gasto"
                                    },

                                    fontSize = 11.sp,

                                    color = TextSecondary
                                )
                            }
                        }
                    },

                    onClick = {

                        onSelect(category.id)

                        expanded = false
                    }
                )
            }
        }
    }
}

// ==========================================
// TARJETA DE IMPORTE
// ==========================================

@Composable
private fun StatisticsAmountCard(
    modifier: Modifier = Modifier,
    title: String,
    amount: Long,
    color: Color
) {

    Card(
        modifier = modifier,

        shape = RoundedCornerShape(20.dp),

        colors = CardDefaults.cardColors(
            containerColor = SurfaceDark
        )
    ) {

        Column(
            modifier = Modifier.padding(17.dp)
        ) {

            Text(
                text = title,
                fontSize = 13.sp,
                color = TextSecondary
            )

            Spacer(Modifier.height(12.dp))

            Text(
                text = statisticsMoney(amount),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = color,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

// ==========================================
// TIPO DEL GRÁFICO
// ==========================================

@Composable
private fun StatisticsTypeSelector(
    selectedType: String,
    onSelected: (String) -> Unit
) {

    Row(
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {

        FilterChip(
            selected = selectedType == "EXPENSE",

            onClick = {
                onSelected("EXPENSE")
            },

            label = {
                Text("Gastos")
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
            selected = selectedType == "INCOME",

            onClick = {
                onSelected("INCOME")
            },

            label = {
                Text("Ingresos")
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
}

// ==========================================
// GRÁFICO DE BARRAS DIARIAS
// ==========================================

@Composable
private fun StatisticsDailyChart(
    amounts: List<Long>,
    color: Color
) {

    val maximum = (
        amounts.maxOrNull() ?: 0L
    ).coerceAtLeast(1L)

    Column {

        Text(
            text = "Máximo diario: ${
                statisticsMoney(
                    amounts.maxOrNull() ?: 0L
                )
            }",

            fontSize = 12.sp,

            color = TextSecondary
        )

        Spacer(Modifier.height(14.dp))

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(150.dp)
        ) {

            val count = amounts.size

            if (count == 0) {
                return@Canvas
            }

            val gap = 3.dp.toPx()

            val barWidth = (
                size.width - gap * (count - 1)
            ) / count

            val chartHeight = size.height

            amounts.forEachIndexed { index, value ->

                if (value <= 0L) {
                    return@forEachIndexed
                }

                val fraction = (
                    value.toDouble() / maximum.toDouble()
                ).toFloat().coerceIn(0f, 1f)

                val barHeight = (
                    chartHeight * fraction
                ).coerceAtLeast(3.dp.toPx())

                val x = index * (
                    barWidth + gap
                )

                val y = chartHeight - barHeight

                drawRoundRect(
                    color = color,

                    topLeft = Offset(
                        x,
                        y
                    ),

                    size = Size(
                        barWidth.coerceAtLeast(1f),
                        barHeight
                    ),

                    cornerRadius = CornerRadius(
                        3.dp.toPx()
                    )
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),

            horizontalArrangement = Arrangement.SpaceBetween
        ) {

            Text(
                text = "Día 1",
                fontSize = 11.sp,
                color = TextSecondary
            )

            Text(
                text = "Día ${amounts.size}",
                fontSize = 11.sp,
                color = TextSecondary
            )
        }
    }
}

// ==========================================
// GRÁFICO CIRCULAR
// ==========================================

@Composable
private fun StatisticsDonutChart(
    categories: List<CategoryStatistic>
) {

    val total = categories.sumOf {
        it.amountCents
    }

    Box(
        modifier = Modifier.size(170.dp),

        contentAlignment = Alignment.Center
    ) {

        Canvas(
            modifier = Modifier.fillMaxSize()
        ) {

            val strokeWidth = 23.dp.toPx()

            drawArc(
                color = BorderDark,

                startAngle = 0f,

                sweepAngle = 360f,

                useCenter = false,

                style = Stroke(
                    width = strokeWidth
                )
            )

            if (total > 0L) {

                var startAngle = -90f

                categories.forEach { category ->

                    val sweepAngle = (
                        category.amountCents.toDouble() /
                            total.toDouble() * 360.0
                    ).toFloat()

                    drawArc(
                        color = category.color,

                        startAngle = startAngle,

                        sweepAngle = sweepAngle,

                        useCenter = false,

                        style = Stroke(
                            width = strokeWidth,
                            cap = StrokeCap.Butt
                        )
                    )

                    startAngle += sweepAngle
                }
            }
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Text(
                text = if (total > 0L) {
                    "${categories.size}"
                } else {
                    "0"
                },

                fontSize = 28.sp,

                fontWeight = FontWeight.Bold,

                color = TextPrimary
            )

            Text(
                text = "Categorías",
                fontSize = 11.sp,
                color = TextSecondary
            )
        }
    }
}

// ==========================================
// DISTRIBUCIÓN POR CATEGORÍA
// ==========================================

@Composable
private fun StatisticsCategoryCard(
    item: CategoryStatistic,
    total: Long
) {

    val fraction = if (total > 0L) {

        (
            item.amountCents.toDouble() /
                total.toDouble()
        ).toFloat().coerceIn(0f, 1f)

    } else {

        0f
    }

    val percentage = fraction * 100f

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

                Surface(
                    color = CardDark,

                    shape = RoundedCornerShape(12.dp)
                ) {

                    Box(
                        modifier = Modifier.size(44.dp),

                        contentAlignment = Alignment.Center
                    ) {

                        Icon(
                            imageVector = GastifyIcons.category(
                                item.icon
                            ),

                            contentDescription = null,

                            tint = item.color,

                            modifier = Modifier.size(25.dp)
                        )
                    }
                }

                Spacer(Modifier.width(12.dp))

                Column(
                    modifier = Modifier.weight(1f)
                ) {

                    Text(
                        text = item.name,

                        fontSize = 15.sp,

                        fontWeight = FontWeight.SemiBold,

                        color = TextPrimary,

                        maxLines = 1,

                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(Modifier.height(5.dp))

                    Text(
                        text = String.format(
                            Locale.US,
                            "%.1f%%",
                            percentage
                        ),

                        fontSize = 12.sp,

                        color = TextSecondary
                    )
                }

                Text(
                    text = statisticsMoney(
                        item.amountCents
                    ),

                    fontSize = 14.sp,

                    fontWeight = FontWeight.Bold,

                    color = item.color
                )
            }

            Spacer(Modifier.height(16.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(7.dp)
                    .clip(CircleShape)
                    .background(BorderDark)
            ) {

                Box(
                    modifier = Modifier
                        .fillMaxWidth(fraction)
                        .fillMaxHeight()
                        .clip(CircleShape)
                        .background(item.color)
                )
            }
        }
    }
}

// ==========================================
// TARJETA VACÍA
// ==========================================

@Composable
private fun StatisticsEmptyCard(
    text: String
) {

    Card(
        modifier = Modifier.fillMaxWidth(),

        shape = RoundedCornerShape(18.dp),

        colors = CardDefaults.cardColors(
            containerColor = SurfaceDark
        )
    ) {

        Text(
            text = text,

            modifier = Modifier.padding(22.dp),

            fontSize = 13.sp,

            lineHeight = 20.sp,

            color = TextSecondary
        )
    }
}
