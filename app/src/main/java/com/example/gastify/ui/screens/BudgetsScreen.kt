package com.example.gastify.ui.screens

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.animation.core.animateDp
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.MoreVert

import androidx.compose.material3.*

import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable

import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

import androidx.lifecycle.viewmodel.compose.viewModel

import com.example.gastify.data.local.BudgetStore

import com.example.gastify.data.model.Budget
import com.example.gastify.data.model.BudgetPeriod
import com.example.gastify.data.model.Category
import com.example.gastify.data.model.Workspace

import com.example.gastify.data.repository.BudgetCloudRepository

import com.example.gastify.notifications.BudgetAlertWorker

import com.example.gastify.ui.components.GlassOptionsMenu
import com.example.gastify.ui.components.GlassOptionsItem
import com.example.gastify.ui.components.GlassOptionsDivider
import com.example.gastify.ui.components.BalanceSummaryCard
import com.example.gastify.ui.components.GastifyIcons
import com.example.gastify.ui.theme.*

import com.example.gastify.viewmodel.CategoryViewModel
import com.example.gastify.viewmodel.StatisticsViewModel

import dev.chrisbanes.haze.HazeInput
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState
import dev.chrisbanes.haze.blur.HazeBlurStyle
import dev.chrisbanes.haze.blur.HazeColorEffect
import dev.chrisbanes.haze.blur.hazeBlur

import kotlinx.coroutines.launch

import java.math.BigDecimal
import java.text.NumberFormat
import java.util.Calendar
import java.util.Locale

private const val MAX_BUDGET_AMOUNT =
    999_999_999_999L

private val BudgetWarningColor =
    Color(0xFFFFBF69)

// ==========================================
// FORMATO DE DINERO
// ==========================================

private fun budgetMoney(
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
// CONVERTIR IMPORTE
// ==========================================

private fun parseBudgetMoney(
    value: String
): Long? {

    val clean = value.trim()

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
                it in 1L..MAX_BUDGET_AMOUNT
            }

    } catch (_: Exception) {

        null
    }
}

// ==========================================
// NOMBRE DEL PERÍODO
// ==========================================

private fun budgetPeriodName(
    period: BudgetPeriod
): String {

    return when (period) {

        BudgetPeriod.DAILY -> "Diario"

        BudgetPeriod.WEEKLY -> "Semanal"

        BudgetPeriod.MONTHLY -> "Mensual"
    }
}

// ==========================================
// RANGO DEL PERÍODO
// ==========================================

private fun budgetPeriodRange(
    period: BudgetPeriod
): Pair<Long, Long> {

    val start = Calendar.getInstance().apply {

        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }

    when (period) {

        BudgetPeriod.DAILY -> Unit

        BudgetPeriod.WEEKLY -> {

            val daysSinceMonday = (
                start.get(Calendar.DAY_OF_WEEK) + 5
            ) % 7

            start.add(
                Calendar.DAY_OF_MONTH,
                -daysSinceMonday
            )
        }

        BudgetPeriod.MONTHLY -> {

            start.set(
                Calendar.DAY_OF_MONTH,
                1
            )
        }
    }

    val end = (
        start.clone() as Calendar
    ).apply {

        when (period) {

            BudgetPeriod.DAILY -> {

                add(Calendar.DAY_OF_MONTH, 1)
            }

            BudgetPeriod.WEEKLY -> {

                add(Calendar.DAY_OF_MONTH, 7)
            }

            BudgetPeriod.MONTHLY -> {

                add(Calendar.MONTH, 1)
            }
        }
    }

    return start.timeInMillis to end.timeInMillis
}

// ==========================================
// DÍAS RESTANTES
// ==========================================

private fun budgetRemainingDays(
    endMillis: Long
): Int {

    val current = Calendar.getInstance().apply {

        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }

    var days = 0

    while (
        current.timeInMillis < endMillis &&
        days < 32
    ) {

        days++

        current.add(
            Calendar.DAY_OF_MONTH,
            1
        )
    }

    return days.coerceAtLeast(1)
}

// ==========================================
// PANTALLA PRINCIPAL
// ==========================================

@Composable
fun BudgetsScreen(
    userId: String,
    workspace: Workspace,
    onBack: () -> Unit
) {

    val context = LocalContext.current

    val store = remember(context) {

        BudgetStore(context)
    }

    val repository = remember(
        userId,
        workspace.id
    ) {

        BudgetCloudRepository(
            userId,
            workspace.id
        )
    }

    val scope = rememberCoroutineScope()

    val snackbar = remember {
        SnackbarHostState()
    }

    fun showMessage(message: String) {

        scope.launch {

            snackbar.showSnackbar(message)
        }
    }

    // ======================================
    // FIREBASE: PRESUPUESTOS
    // ======================================

    var cachedBudgets by remember(
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

    var remoteBudgets by remember(
        userId,
        workspace.id
    ) {

        mutableStateOf<List<Budget>>(
            emptyList()
        )
    }

    var remoteReady by remember(
        userId,
        workspace.id
    ) {

        mutableStateOf(false)
    }

    var remoteFromCache by remember(
        userId,
        workspace.id
    ) {

        mutableStateOf(true)
    }

    var remotePendingWrites by remember(
        userId,
        workspace.id
    ) {

        mutableStateOf(false)
    }

    var migrated by remember(
        userId,
        workspace.id
    ) {

        mutableStateOf(
            store.isMigrated(
                userId,
                workspace.id
            )
        )
    }

    var syncError by remember(
        userId,
        workspace.id
    ) {

        mutableStateOf<String?>(null)
    }

    var retryCounter by remember {
        mutableIntStateOf(0)
    }

    // ======================================
    // ESCUCHAR PRESUPUESTOS EN TIEMPO REAL
    // ======================================

    DisposableEffect(
        repository,
        retryCounter
    ) {

        var active = true

        val listener = repository.listen(

            onChange = {
                    budgets,
                    fromCache,
                    pendingWrites ->

                if (active) {

                    remoteBudgets = budgets

                    remoteFromCache = fromCache

                    remotePendingWrites = pendingWrites

                    remoteReady = true

                    syncError = null
                }
            },

            onError = { exception ->

                if (active) {

                    syncError = exception.localizedMessage
                        ?: "No se pudo conectar con Firebase."
                }
            }
        )

        // Migrar una sola vez los presupuestos
        // guardados antes de esta fase.

        if (!migrated) {

            repository.migrateLegacy(

                localBudgets = store.getByWorkspace(
                    userId,
                    workspace.id
                ),

                onSuccess = {

                    try {

                        store.markMigrated(
                            userId,
                            workspace.id
                        )

                        if (active) {

                            migrated = true

                            syncError = null
                        }

                    } catch (exception: Exception) {

                        if (active) {

                            syncError =
                                exception.localizedMessage
                                    ?: "Error al completar la migración."
                        }
                    }
                },

                onError = { exception ->

                    if (active) {

                        syncError = exception.localizedMessage
                            ?: "No se pudieron migrar los presupuestos."
                    }
                }
            )
        }

        onDispose {

            active = false

            listener.remove()
        }
    }

    // ======================================
    // ACTUALIZAR CACHÉ LOCAL
    // ======================================

    LaunchedEffect(
        migrated,
        remoteReady,
        remoteBudgets,
        remoteFromCache,
        remotePendingWrites
    ) {

        if (
            migrated &&
            remoteReady &&
            (
                !remoteFromCache ||
                    remoteBudgets.isNotEmpty() ||
                    remotePendingWrites
            )
        ) {

            val changed = store.replaceWorkspace(
                userId,
                workspace.id,
                remoteBudgets
            )

            cachedBudgets = store.getByWorkspace(
                userId,
                workspace.id
            )

            if (remoteBudgets.isNotEmpty()) {

                BudgetAlertWorker.ensureScheduled(
                    context
                )

                if (changed) {

                    BudgetAlertWorker.checkNow(
                        context
                    )
                }
            }
        }
    }

    // ======================================
    // PRESUPUESTOS PARA MOSTRAR
    // ======================================

    val budgets = if (migrated && remoteReady) {

        if (
            remoteFromCache &&
            remoteBudgets.isEmpty() &&
            !remotePendingWrites &&
            cachedBudgets.isNotEmpty()
        ) {

            cachedBudgets

        } else {

            remoteBudgets
        }

    } else {

        // Durante la migración damos prioridad
        // a los datos existentes en Firebase.

        val remoteScopes = remoteBudgets.map {

            "${it.period}|${it.categoryId}"
        }.toSet()

        remoteBudgets + cachedBudgets.filter {

            "${it.period}|${it.categoryId}" !in remoteScopes
        }
    }

    // ======================================
    // PERÍODO SELECCIONADO
    // ======================================

    var selectedPeriodName by rememberSaveable(
        workspace.id
    ) {

        mutableStateOf(
            BudgetPeriod.MONTHLY.name
        )
    }

    val selectedPeriod = BudgetPeriod.entries
        .firstOrNull {

            it.name == selectedPeriodName

        } ?: BudgetPeriod.MONTHLY

    val periodRanges = remember(workspace.id) {
        BudgetPeriod.entries.associateWith { budgetPeriodRange(it) }
    }
    val range = periodRanges.getValue(selectedPeriod)
    val queryStart = periodRanges.values.minOf { it.first }
    val queryEnd = periodRanges.values.maxOf { it.second }
    // ======================================
    // VIEWMODELS EXISTENTES
    // ======================================

    val statisticsVm: StatisticsViewModel = viewModel(

        key = "budget_statistics_${userId}_${workspace.id}",

        factory = StatisticsViewModel.factory(
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

    val statisticsState by statisticsVm
        .uiState
        .collectAsState()

    val categoryState by categoryVm
        .uiState
        .collectAsState()

    DisposableEffect(
        statisticsVm,
        queryStart,
        queryEnd
    ) {

        statisticsVm.startListening(
            queryStart,
            queryEnd
        )

        onDispose {

            statisticsVm.stopListening()
        }
    }

    DisposableEffect(categoryVm) {

        categoryVm.startListening()

        onDispose {

            categoryVm.stopListening()
        }
    }

    // ======================================
    // FILTRAR PRESUPUESTOS
    // ======================================

    val visibleBudgets = budgets.filter {

        it.period == selectedPeriod.name
    }.sortedWith(

        compareBy<Budget> {
            it.categoryId.isNotEmpty()
        }.thenBy {
            it.categoryId
        }
    )

    val expenseCategories = categoryState.categories
        .filter {

            it.type == "EXPENSE"
        }

    // ======================================
    // MOVIMIENTOS DEL PERÍODO
    // ======================================

    val totalsByPeriod = remember(statisticsState.movements, periodRanges) {
        val totals = BudgetPeriod.entries.associateWith { 0L }.toMutableMap()
        val categories = BudgetPeriod.entries.associateWith { mutableMapOf<String, Long>() }
        statisticsState.movements.forEach { movement ->
            val timestamp = movement.createdAt?.toDate()?.time
            if (movement.type == "EXPENSE" && timestamp != null) {
                periodRanges.forEach { (period, bounds) ->
                    if (timestamp >= bounds.first && timestamp < bounds.second) {
                        totals[period] = totals.getValue(period) + movement.amountCents
                        val byCategory = categories.getValue(period)
                        byCategory[movement.categoryId] = (byCategory[movement.categoryId] ?: 0L) + movement.amountCents
                    }
                }
            }
        }
        BudgetPeriod.entries.associateWith { period -> totals.getValue(period) to categories.getValue(period) }
    }
    val expenseTotals = totalsByPeriod.getValue(selectedPeriod)
    val totalExpense = expenseTotals.first

    fun spentForBudget(budget: Budget): Long =
        if (budget.categoryId.isEmpty()) totalExpense else expenseTotals.second[budget.categoryId] ?: 0L
    val exceededCount = visibleBudgets.count {

        spentForBudget(it) >= it.limitCents
    }

    val warningCount = visibleBudgets.count {

        val spent = spentForBudget(it)

        spent < it.limitCents &&
            spent.toDouble() / it.limitCents >= 0.80
    }

    // ======================================
    // ESTADOS DEL FORMULARIO
    // ======================================

    var showEditor by rememberSaveable {
        mutableStateOf(false)
    }

    var editingId by rememberSaveable {
        mutableStateOf<String?>(null)
    }

    var editorPeriodName by rememberSaveable { mutableStateOf(BudgetPeriod.MONTHLY.name) }

    var selectedCategoryId by rememberSaveable {
        mutableStateOf("")
    }

    var limitText by rememberSaveable {
        mutableStateOf("")
    }

    var deleteId by rememberSaveable {
        mutableStateOf<String?>(null)
    }

    // ======================================
    // ABRIR EDITOR
    // ======================================

    fun openEditor(
        budget: Budget?
    ) {

        editingId = budget?.id
        editorPeriodName = budget?.period ?: selectedPeriod.name

        selectedCategoryId =
            budget?.categoryId ?: ""

        limitText = budget?.let {

            BigDecimal.valueOf(
                it.limitCents,
                2
            ).toPlainString()

        } ?: ""

        showEditor = true
    }

    // ======================================
    // GUARDAR EN FIREBASE
    // ======================================

    fun saveBudget(
        budget: Budget
    ) {

        try {

            repository.save(
                budget = budget,

                onError = { exception ->

                    showMessage(
                        "No se pudo sincronizar: ${
                            exception.localizedMessage
                        }"
                    )
                }
            )

            showEditor = false

            showMessage(
                "Presupuesto guardado."
            )

        } catch (exception: Exception) {

            showMessage(
                exception.localizedMessage
                    ?: "No se pudo guardar."
            )
        }
    }

    // ======================================
    // ELIMINAR EN FIREBASE
    // ======================================

    fun deleteBudget(
        budget: Budget
    ) {

        try {

            repository.delete(
                budget = budget,

                onError = { exception ->

                    showMessage(
                        "No se pudo eliminar: ${
                            exception.localizedMessage
                        }"
                    )
                }
            )

            deleteId = null

            showMessage(
                "Eliminación solicitada."
            )

        } catch (exception: Exception) {

            showMessage(
                exception.localizedMessage
                    ?: "No se pudo eliminar."
            )
        }
    }

    // ======================================
    // ERRORES EXISTENTES
    // ======================================

    LaunchedEffect(
        statisticsState.errorMessage,
        categoryState.errorMessage
    ) {

        val error = statisticsState.errorMessage
            ?: categoryState.errorMessage

        if (error != null) {

            showMessage(error)

            statisticsVm.clearError()

            categoryVm.clearError()
        }
    }

    // ======================================
    // INTERFAZ AMOLED
    // ======================================

    val glassState = rememberHazeState()
    val backgroundGlassState = rememberHazeState()
    val statusBarHeight = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val listState = rememberLazyListState()
    val fadeDistance = with(LocalDensity.current) { 100.dp.toPx() }
    val headerAlpha = if (listState.firstVisibleItemIndex > 0) 1f else
        (listState.firstVisibleItemScrollOffset / fadeDistance).coerceIn(0f, 1f)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AmoledBlack)
    ) {

        Box(Modifier.fillMaxSize().hazeSource(glassState)) {
        Box(Modifier.fillMaxSize().hazeSource(backgroundGlassState).background(AmoledBlack)) {
            AccountsLightsBackground(Modifier.fillMaxSize())
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),

            state = listState,
            contentPadding = PaddingValues(
                top = statusBarHeight + 76.dp,
                start = 20.dp,
                end = 20.dp,
                bottom = 32.dp
            ),

            verticalArrangement = Arrangement.spacedBy(
                14.dp
            )
        ) {

            // ==================================
            // ERROR DE FIREBASE
            // ==================================

            if (syncError != null) {

                item {

                    Card(
                        modifier = Modifier.fillMaxWidth(),

                        colors = CardDefaults.cardColors(
                            containerColor = CardDark
                        )
                    ) {

                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {

                            Text(
                                text = syncError.orEmpty(),

                                color = ExpenseRed,

                                fontSize = 12.sp,

                                lineHeight = 19.sp
                            )

                            Spacer(Modifier.height(8.dp))

                            TextButton(
                                onClick = {

                                    syncError = null

                                    retryCounter++
                                }
                            ) {

                                Text(
                                    text = "Reintentar",
                                    color = PrimaryGreen
                                )
                            }
                        }
                    }
                }
            }

            // ==================================
            // SELECCIONAR PERÍODO
            // ==================================

            item {
                Text("Período", color = TextSecondary, fontSize = 14.sp)
                Spacer(Modifier.height(10.dp))
                BoxWithConstraints(
                    modifier = Modifier.widthIn(max = 270.dp).fillMaxWidth().height(40.dp)
                        .clip(RoundedCornerShape(12.dp)).background(Color.White.copy(alpha = 0.08f))
                ) {
                    val options = listOf(BudgetPeriod.MONTHLY, BudgetPeriod.WEEKLY, BudgetPeriod.DAILY)
                    val segmentWidth = maxWidth / options.size
                    val indicatorOffset by animateDpAsState(
                        targetValue = segmentWidth * options.indexOf(selectedPeriod),
                        animationSpec = tween(durationMillis = 280), label = "period indicator"
                    )
                    Box(Modifier.offset(x = indicatorOffset).width(segmentWidth).fillMaxHeight().clip(RoundedCornerShape(12.dp)).background(Color.White.copy(alpha = 0.18f)))
                    Row(Modifier.fillMaxSize()) {
                        options.forEach { period ->
                            Box(
                                Modifier.weight(1f).fillMaxHeight().clip(RoundedCornerShape(12.dp)).clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { selectedPeriodName = period.name },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(budgetPeriodName(period), color = if (period == selectedPeriod) Color.White else TextSecondary, fontSize = 12.sp, fontWeight = if (period == selectedPeriod) FontWeight.SemiBold else FontWeight.Normal)
                            }
                        }
                    }
                }
            }
            // ==================================
            // CARGANDO GASTOS
            // ==================================

            if (
                statisticsState.isLoading ||
                categoryState.isLoading
            ) {

                item {

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(36.dp),

                        contentAlignment = Alignment.Center
                    ) {

                        CircularProgressIndicator(
                            color = PrimaryGreen
                        )
                    }
                }

            } else {

                // ==================================
                // RESUMEN GENERAL
                // ==================================

                item {

                    BalanceSummaryCard(
                        title = "Gastos del período",
                        amount = budgetMoney(totalExpense),
                        backgroundState = backgroundGlassState
                    ) {
                        Text(
                            text = "${visibleBudgets.size} presupuestos configurados",
                            color = TextSecondary,
                            fontSize = 12.sp
                        )
                    }
                }
                // ==================================
                // ALERTAS VISUALES
                // ==================================

                if (
                    exceededCount > 0 ||
                    warningCount > 0
                ) {

                    item {

                        Card(
                            modifier = Modifier.fillMaxWidth(),

                            colors = CardDefaults.cardColors(
                                containerColor = CardDark
                            )
                        ) {

                            Row(
                                modifier = Modifier.padding(18.dp),

                                verticalAlignment = Alignment.CenterVertically
                            ) {

                                Icon(
                                    imageVector =
                                        Icons.Outlined.NotificationsActive,

                                    contentDescription = null,

                                    tint = if (exceededCount > 0) {
                                        ExpenseRed
                                    } else {
                                        BudgetWarningColor
                                    }
                                )

                                Spacer(Modifier.width(12.dp))

                                Column {

                                    Text(
                                        text = "Alertas de presupuesto",

                                        fontWeight = FontWeight.Bold,

                                        color = TextPrimary
                                    )

                                    Spacer(Modifier.height(6.dp))

                                    Text(
                                        text = "$exceededCount límites alcanzados y $warningCount próximos al límite.",

                                        color = TextSecondary,

                                        fontSize = 12.sp,

                                        lineHeight = 18.sp
                                    )
                                }
                            }
                        }
                    }
                }

                // ==================================
                // CREAR PRESUPUESTO
                // ==================================

                item {

                    Text(
                        text = "Mis presupuestos",

                        fontSize = 20.sp,

                        fontWeight = FontWeight.SemiBold,

                        color = TextPrimary
                    )
                }

                // ==================================
                // PRESUPUESTOS
                // ==================================

                if (visibleBudgets.isEmpty()) {

                    item {

                        Card(
                            modifier = Modifier.fillMaxWidth(),

                            colors = CardDefaults.cardColors(
                                containerColor = SurfaceDark
                            )
                        ) {

                            Text(
                                text = "Todavía no tienes presupuestos para este período.",

                                modifier = Modifier.padding(24.dp),

                                color = TextSecondary,

                                fontSize = 13.sp
                            )
                        }
                    }

                } else {

                    items(
                        items = visibleBudgets,

                        key = {
                            it.id
                        }
                    ) { budget ->

                        val category = categoryState.categories
                            .firstOrNull {

                                it.id == budget.categoryId
                            }

                        CloudBudgetCard(
                            glassState = glassState,
                            budget = budget,

                            category = category,

                            spentCents = spentForBudget(budget),

                            daysRemaining = budgetRemainingDays(
                                range.second
                            ),

                            actionsEnabled = migrated,

                            onEdit = {

                                openEditor(budget)
                            },

                            onDelete = {

                                deleteId = budget.id
                            }
                        )
                    }
                }

                item {

                    Text(
                        text = "Los presupuestos se renuevan al comenzar un nuevo período.",

                        color = TextSecondary,

                        fontSize = 12.sp,

                        lineHeight = 19.sp
                    )
                }
            }
        }

        }

        Row(
            modifier = Modifier.fillMaxWidth().height(statusBarHeight + 64.dp)
                .background(AmoledBlack.copy(alpha = headerAlpha))
                .padding(start = 12.dp, end = 12.dp, top = statusBarHeight),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack, modifier = Modifier.size(48.dp)) {
                Icon(GastifyIcons.back, contentDescription = "Volver", tint = Color.White, modifier = Modifier.size(30.dp))
            }
            Column(modifier = Modifier.weight(1f).padding(start = 4.dp, end = 8.dp)) {
                Text("Mis presupuestos", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(workspace.name, fontSize = 13.sp, color = Color.White.copy(alpha = 0.8f), maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Button(
                onClick = { openEditor(null) }, enabled = migrated,
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White.copy(alpha = 0.16f), contentColor = Color.White,
                    disabledContainerColor = Color.White.copy(alpha = 0.08f), disabledContentColor = Color.White.copy(alpha = 0.4f)
                ),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp)
            ) {
                Icon(GastifyIcons.add, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(5.dp))
                Text("Agregar", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
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
    // FORMULARIO CREAR / EDITAR
    // ==========================================

    if (showEditor) {

        val amount = parseBudgetMoney(
            limitText
        )

        val editingBudget = budgets.firstOrNull {
            it.id == editingId
        }

        val selectedCategory = expenseCategories
            .firstOrNull {

                it.id == selectedCategoryId
            }

        val alreadyExists = budgets.any {

            it.categoryId == selectedCategoryId &&
                it.period == editorPeriodName &&
                it.id != editingId
        }

        val valid =
            migrated &&
                amount != null &&
                !alreadyExists &&
                (
                    selectedCategoryId.isEmpty() ||
                        selectedCategory != null
                )

        AlertDialog(
            onDismissRequest = {

                showEditor = false
            },

            containerColor = SurfaceDark,

            title = {

                Text(
                    text = if (editingBudget == null) {
                        "Nuevo presupuesto"
                    } else {
                        "Editar presupuesto"
                    },

                    color = TextPrimary
                )
            },

            text = {

                Column(
                    modifier = Modifier
                        .heightIn(max = 420.dp)
                        .verticalScroll(
                            rememberScrollState()
                        )
                ) {

                    Text("Período", fontWeight = FontWeight.Medium)
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(BudgetPeriod.MONTHLY, BudgetPeriod.WEEKLY, BudgetPeriod.DAILY).forEach { period ->
                            FilterChip(
                                selected = editorPeriodName == period.name,
                                onClick = { editorPeriodName = period.name },
                                enabled = editingBudget == null,
                                label = { Text(budgetPeriodName(period), fontSize = 12.sp) }
                            )
                        }
                    }
                    Spacer(Modifier.height(20.dp))
                    Text(
                        text = "Aplicar límite a",

                        fontWeight = FontWeight.Medium
                    )

                    Spacer(Modifier.height(10.dp))

                    if (editingBudget == null) {

                        CloudBudgetCategoryDropdown(
                            selectedText =
                                selectedCategory?.name
                                    ?: "Todos los gastos",

                            categories = expenseCategories,

                            onSelect = {

                                selectedCategoryId = it
                            }
                        )

                    } else {

                        Text(
                            text = selectedCategory?.name
                                ?: "Todos los gastos",

                            color = TextSecondary
                        )
                    }

                    Spacer(Modifier.height(20.dp))

                    OutlinedTextField(
                        value = limitText,

                        onValueChange = {

                            limitText = it.take(16)
                        },

                        label = {

                            Text("Límite (S/)")
                        },

                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Decimal
                        ),

                        singleLine = true,

                        isError =
                            limitText.isNotBlank() &&
                                amount == null,

                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(Modifier.height(16.dp))

                    if (alreadyExists) {

                        Text(
                            text = "Ya existe un presupuesto para esta categoría y período.",

                            color = ExpenseRed,

                            fontSize = 12.sp
                        )
                    }

                }
            },

            confirmButton = {

                TextButton(
                    enabled = valid,

                    onClick = {

                        if (amount != null) {

                            val budgetId = editingId
                                ?: repository.documentId(
                                    editorPeriodName,
                                    selectedCategoryId
                                )

                            selectedPeriodName = editorPeriodName
                            saveBudget(
                                Budget(
                                    id = budgetId,

                                    userId = userId,

                                    workspaceId = workspace.id,

                                    categoryId = selectedCategoryId,

                                    period = editorPeriodName,

                                    limitCents = amount
                                )
                            )
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

    // ==========================================
    // ELIMINAR PRESUPUESTO
    // ==========================================

    val budgetToDelete = budgets.firstOrNull {

        it.id == deleteId
    }

    if (budgetToDelete != null) {

        AlertDialog(
            onDismissRequest = {

                deleteId = null
            },

            containerColor = SurfaceDark,

            title = {

                Text("Eliminar presupuesto")
            },

            text = {

                Text(
                    text = "¿Eliminar este presupuesto?\n\nLos movimientos financieros no se eliminarán.",

                    color = TextSecondary
                )
            },

            confirmButton = {

                TextButton(
                    onClick = {

                        deleteBudget(budgetToDelete)
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

                        deleteId = null
                    }
                ) {

                    Text("Cancelar")
                }
            }
        )
    }
}

// ==========================================
// TARJETA DE PRESUPUESTO
// ==========================================

@Composable
private fun CloudBudgetCard(
    budget: Budget,
    glassState: HazeState,
    category: Category?,
    spentCents: Long,
    daysRemaining: Int,
    actionsEnabled: Boolean,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val progress = spentCents.toDouble() / budget.limitCents.toDouble()
    val fraction = progress.toFloat().coerceIn(0f, 1f)
    val remaining = (budget.limitCents - spentCents).coerceAtLeast(0L)
    val exceeded = (spentCents - budget.limitCents).coerceAtLeast(0L)
    val statusColor = when {
        progress >= 1.0 -> ExpenseRed
        progress >= 0.80 -> BudgetWarningColor
        else -> PrimaryGreen
    }
    var menuExpanded by remember(budget.id) { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.06f))
    ) {
        Column(Modifier.fillMaxWidth().padding(start = 18.dp, end = 12.dp, top = 14.dp, bottom = 18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(color = statusColor.copy(alpha = 0.14f), shape = RoundedCornerShape(14.dp)) {
                    Box(Modifier.size(48.dp), contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (budget.categoryId.isEmpty()) GastifyIcons.accounts else GastifyIcons.category(category?.icon.orEmpty()),
                            contentDescription = null,
                            tint = statusColor,
                            modifier = Modifier.size(25.dp)
                        )
                    }
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        text = if (budget.categoryId.isEmpty()) "Presupuesto general" else category?.name ?: "Categoría no disponible",
                        color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold,
                        maxLines = 1, overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = when (budget.period) {
                            BudgetPeriod.DAILY.name -> "Diario"
                            BudgetPeriod.WEEKLY.name -> "Semanal"
                            else -> "Mensual"
                        },
                        color = TextSecondary, fontSize = 12.sp
                    )
                }
                Box {
                    IconButton(onClick = { menuExpanded = true }, enabled = actionsEnabled, modifier = Modifier.size(40.dp)) {
                        Icon(Icons.Outlined.MoreVert, contentDescription = "Más opciones", tint = Color.White)
                    }
                    GlassOptionsMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }, glassState = glassState) {
                        GlassOptionsItem("Editar presupuesto", GastifyIcons.edit, onClick = { menuExpanded = false; onEdit() })
                        GlassOptionsDivider()
                        GlassOptionsItem("Eliminar presupuesto", GastifyIcons.delete, onClick = { menuExpanded = false; onDelete() }, destructive = true)
                    }
                }
            }
            Spacer(Modifier.height(20.dp))
            Row(Modifier.fillMaxWidth().padding(end = 6.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
                Column(Modifier.weight(1f)) {
                    Text("Gastado", color = TextSecondary, fontSize = 12.sp)
                    Spacer(Modifier.height(4.dp))
                    Text(budgetMoney(spentCents), color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Spacer(Modifier.width(8.dp))
                Column(horizontalAlignment = Alignment.End) {
                    Text("Límite", color = TextSecondary, fontSize = 12.sp)
                    Spacer(Modifier.height(4.dp))
                    Text(budgetMoney(budget.limitCents), color = Color.White.copy(alpha = 0.85f), fontSize = 14.sp, fontWeight = FontWeight.Medium, maxLines = 1)
                }
            }
            Spacer(Modifier.height(16.dp))
            LinearProgressIndicator(
                progress = { fraction },
                modifier = Modifier.fillMaxWidth().padding(end = 6.dp).height(8.dp),
                color = statusColor,
                trackColor = BorderDark
            )
            Spacer(Modifier.height(10.dp))
            Row(Modifier.fillMaxWidth().padding(end = 6.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(String.format(Locale.US, "%.1f%% utilizado", progress * 100.0), color = statusColor, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                Text(
                    text = when {
                        progress >= 1.0 -> "Límite alcanzado"
                        progress >= 0.90 -> "Más del 90%"
                        progress >= 0.80 -> "Más del 80%"
                        else -> "En orden"
                    },
                    color = statusColor, fontSize = 12.sp
                )
            }
            Spacer(Modifier.height(14.dp))
            HorizontalDivider(color = Color.White.copy(alpha = 0.08f))
            Spacer(Modifier.height(14.dp))
            Row(Modifier.fillMaxWidth().padding(end = 6.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(if (exceeded > 0L) "Excedido" else "Disponible", color = TextSecondary, fontSize = 12.sp)
                Text(budgetMoney(if (exceeded > 0L) exceeded else remaining), color = if (exceeded > 0L) ExpenseRed else PrimaryGreen, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
            if (budget.period != BudgetPeriod.DAILY.name) {
                Spacer(Modifier.height(8.dp))
                Text("Gasto diario sugerido: ${budgetMoney(remaining / daysRemaining)}", color = TextSecondary, fontSize = 12.sp)
            }
        }
    }
}
// ==========================================
// SELECTOR DE CATEGORÍAS
// ==========================================

@Composable
private fun CloudBudgetCategoryDropdown(
    selectedText: String,
    categories: List<Category>,
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

            modifier = Modifier.fillMaxWidth()
        ) {

            Text(
                text = selectedText,

                modifier = Modifier.weight(1f),

                maxLines = 1,

                overflow = TextOverflow.Ellipsis,

                color = TextPrimary
            )

            Icon(
                imageVector = GastifyIcons.expand,

                contentDescription = null
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
                        text = "Todos los gastos",

                        color = TextPrimary
                    )
                },

                onClick = {

                    onSelect("")

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

                                tint = PrimaryGreen,

                                modifier = Modifier.size(20.dp)
                            )

                            Spacer(Modifier.width(10.dp))

                            Text(
                                text = category.name,

                                color = TextPrimary
                            )
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
