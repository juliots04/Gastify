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
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.CloudDone
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.NotificationsActive

import androidx.compose.material3.*

import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable

import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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

import com.example.gastify.ui.components.GastifyIcons
import com.example.gastify.ui.theme.*

import com.example.gastify.viewmodel.CategoryViewModel
import com.example.gastify.viewmodel.StatisticsViewModel

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

    val range = remember(selectedPeriod) {

        budgetPeriodRange(
            selectedPeriod
        )
    }

    // ======================================
    // VIEWMODELS EXISTENTES
    // ======================================

    val statisticsVm: StatisticsViewModel = viewModel(

        key = "statistics_${userId}_${workspace.id}",

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
        range.first,
        range.second
    ) {

        statisticsVm.startListening(
            range.first,
            range.second
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

    val expenses = statisticsState.movements.filter {

        it.type == "EXPENSE"
    }

    val totalExpense = expenses.sumOf {

        it.amountCents
    }

    fun spentForBudget(
        budget: Budget
    ): Long {

        return if (budget.categoryId.isEmpty()) {

            totalExpense

        } else {

            expenses.filter {

                it.categoryId == budget.categoryId

            }.sumOf {

                it.amountCents
            }
        }
    }

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
                "Presupuesto guardado. Se sincronizará con Firebase."
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

                Spacer(Modifier.height(14.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {

                    Icon(
                        imageVector =
                            Icons.Outlined.AccountBalanceWallet,

                        contentDescription = null,

                        tint = PrimaryGreen,

                        modifier = Modifier.size(30.dp)
                    )

                    Spacer(Modifier.width(12.dp))

                    Text(
                        text = "Presupuestos",
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

                Spacer(Modifier.height(22.dp))
            }

            // ==================================
            // ESTADO DE SINCRONIZACIÓN
            // ==================================

            item {

                Card(
                    modifier = Modifier.fillMaxWidth(),

                    shape = RoundedCornerShape(16.dp),

                    colors = CardDefaults.cardColors(
                        containerColor = SurfaceDark
                    )
                ) {

                    Row(
                        modifier = Modifier.padding(18.dp),

                        verticalAlignment = Alignment.CenterVertically
                    ) {

                        Icon(
                            imageVector = if (
                                migrated &&
                                remoteReady &&
                                !remoteFromCache &&
                                !remotePendingWrites
                            ) {

                                Icons.Outlined.CloudDone

                            } else {

                                Icons.Outlined.CloudOff
                            },

                            contentDescription = null,

                            tint = if (
                                migrated &&
                                remoteReady &&
                                !remoteFromCache &&
                                !remotePendingWrites
                            ) {

                                PrimaryGreen

                            } else {

                                BudgetWarningColor
                            },

                            modifier = Modifier.size(25.dp)
                        )

                        Spacer(Modifier.width(12.dp))

                        Column(
                            modifier = Modifier.weight(1f)
                        ) {

                            Text(
                                text = when {

                                    !migrated ->
                                        "Migración pendiente"

                                    remotePendingWrites ->
                                        "Sincronizando cambios"

                                    !remoteReady ->
                                        "Conectando con Firebase"

                                    remoteFromCache ->
                                        "Mostrando datos locales"

                                    else ->
                                        "Sincronizado con Firebase"
                                },

                                fontWeight = FontWeight.SemiBold,

                                color = TextPrimary,

                                fontSize = 14.sp
                            )

                            Spacer(Modifier.height(5.dp))

                            Text(
                                text = when {

                                    !migrated ->
                                        "Se necesita conexión para migrar los presupuestos anteriores."

                                    remotePendingWrites ->
                                        "Tus cambios están pendientes de confirmación."

                                    remoteFromCache ->
                                        "Los datos se actualizarán al recuperar internet."

                                    else ->
                                        "Tus presupuestos están disponibles en la nube."
                                },

                                color = TextSecondary,

                                fontSize = 12.sp,

                                lineHeight = 18.sp
                            )
                        }
                    }
                }
            }

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

                Spacer(Modifier.height(12.dp))

                Text(
                    text = "Período",
                    color = TextSecondary,
                    fontSize = 14.sp
                )

                Spacer(Modifier.height(10.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(
                        8.dp
                    )
                ) {

                    BudgetPeriod.entries.forEach { period ->

                        FilterChip(
                            selected = selectedPeriod == period,

                            onClick = {

                                selectedPeriodName = period.name
                            },

                            label = {

                                Text(
                                    budgetPeriodName(period)
                                )
                            }
                        )
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

                    Card(
                        modifier = Modifier.fillMaxWidth(),

                        shape = RoundedCornerShape(24.dp),

                        colors = CardDefaults.cardColors(
                            containerColor = SurfaceDark
                        )
                    ) {

                        Column(
                            modifier = Modifier.padding(24.dp)
                        ) {

                            Text(
                                text = "Gastos del período",

                                color = TextSecondary,

                                fontSize = 13.sp
                            )

                            Spacer(Modifier.height(12.dp))

                            Text(
                                text = budgetMoney(totalExpense),

                                fontSize = 32.sp,

                                fontWeight = FontWeight.Bold,

                                color = ExpenseRed
                            )

                            Spacer(Modifier.height(16.dp))

                            HorizontalDivider(
                                color = BorderDark
                            )

                            Spacer(Modifier.height(16.dp))

                            Text(
                                text = "${visibleBudgets.size} presupuestos configurados",

                                color = TextSecondary,

                                fontSize = 12.sp
                            )
                        }
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

                    Button(
                        onClick = {

                            openEditor(null)
                        },

                        enabled = migrated,

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
                            text = "Nuevo presupuesto",
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(Modifier.height(16.dp))

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
                        text = "Los presupuestos se renuevan al comenzar un nuevo período. Los importes gastados se consultan desde Firebase.",

                        color = TextSecondary,

                        fontSize = 12.sp,

                        lineHeight = 19.sp
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

        val alreadyExists = visibleBudgets.any {

            it.categoryId == selectedCategoryId &&
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

                    Text(
                        text = "Período: ${
                            budgetPeriodName(selectedPeriod)
                        }",

                        color = PrimaryGreen,

                        fontWeight = FontWeight.SemiBold
                    )

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

                    Spacer(Modifier.height(12.dp))

                    Text(
                        text = "Los cambios se guardarán en Firebase y se reflejarán en tus otros dispositivos.",

                        color = TextSecondary,

                        fontSize = 12.sp,

                        lineHeight = 18.sp
                    )
                }
            },

            confirmButton = {

                TextButton(
                    enabled = valid,

                    onClick = {

                        if (amount != null) {

                            val budgetId = editingId
                                ?: repository.documentId(
                                    selectedPeriod.name,
                                    selectedCategoryId
                                )

                            saveBudget(
                                Budget(
                                    id = budgetId,

                                    userId = userId,

                                    workspaceId = workspace.id,

                                    categoryId = selectedCategoryId,

                                    period = selectedPeriod.name,

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
    category: Category?,
    spentCents: Long,
    daysRemaining: Int,
    actionsEnabled: Boolean,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {

    val progress = spentCents.toDouble() /
        budget.limitCents.toDouble()

    val fraction = progress
        .toFloat()
        .coerceIn(0f, 1f)

    val percentage = progress * 100.0

    val remaining = (
        budget.limitCents - spentCents
    ).coerceAtLeast(0L)

    val exceeded = (
        spentCents - budget.limitCents
    ).coerceAtLeast(0L)

    val statusColor = when {

        progress >= 1.0 -> ExpenseRed

        progress >= 0.80 -> BudgetWarningColor

        else -> PrimaryGreen
    }

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
                            imageVector = if (
                                budget.categoryId.isEmpty()
                            ) {

                                GastifyIcons.accounts

                            } else {

                                GastifyIcons.category(
                                    category?.icon ?: ""
                                )
                            },

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
                        text = if (
                            budget.categoryId.isEmpty()
                        ) {

                            "Presupuesto general"

                        } else {

                            category?.name
                                ?: "Categoría no disponible"
                        },

                        fontSize = 17.sp,

                        fontWeight = FontWeight.SemiBold,

                        color = TextPrimary,

                        maxLines = 1,

                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(Modifier.height(6.dp))

                    Text(
                        text = when (budget.period) {

                            BudgetPeriod.DAILY.name ->
                                "Diario"

                            BudgetPeriod.WEEKLY.name ->
                                "Semanal"

                            else ->
                                "Mensual"
                        },

                        color = TextSecondary,

                        fontSize = 12.sp
                    )
                }

                Icon(
                    imageVector =
                        Icons.Outlined.NotificationsActive,

                    contentDescription = null,

                    tint = statusColor
                )
            }

            Spacer(Modifier.height(22.dp))

            Text(
                text = "Gastado",

                fontSize = 12.sp,

                color = TextSecondary
            )

            Spacer(Modifier.height(8.dp))

            Text(
                text = budgetMoney(spentCents),

                fontSize = 27.sp,

                fontWeight = FontWeight.Bold,

                color = TextPrimary
            )

            Spacer(Modifier.height(6.dp))

            Text(
                text = "Límite: ${
                    budgetMoney(budget.limitCents)
                }",

                color = TextSecondary,

                fontSize = 13.sp
            )

            Spacer(Modifier.height(18.dp))

            LinearProgressIndicator(
                progress = {
                    fraction
                },

                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp),

                color = statusColor,

                trackColor = BorderDark
            )

            Spacer(Modifier.height(10.dp))

            Text(
                text = String.format(
                    Locale.US,
                    "%.1f%% utilizado",
                    percentage
                ),

                color = statusColor,

                fontWeight = FontWeight.SemiBold,

                fontSize = 13.sp
            )

            Spacer(Modifier.height(12.dp))

            Text(
                text = when {

                    progress >= 1.0 ->
                        "Límite alcanzado o superado."

                    progress >= 0.90 ->
                        "Alerta: superaste el 90%."

                    progress >= 0.80 ->
                        "Atención: superaste el 80%."

                    else ->
                        "Dentro del presupuesto."
                },

                color = statusColor,

                fontSize = 12.sp
            )

            Spacer(Modifier.height(18.dp))

            HorizontalDivider(
                color = BorderDark
            )

            Spacer(Modifier.height(16.dp))

            Text(
                text = if (exceeded > 0L) {

                    "Excedido por ${
                        budgetMoney(exceeded)
                    }"

                } else {

                    "Disponible: ${
                        budgetMoney(remaining)
                    }"
                },

                fontSize = 17.sp,

                fontWeight = FontWeight.Bold,

                color = if (exceeded > 0L) {
                    ExpenseRed
                } else {
                    PrimaryGreen
                }
            )

            if (
                budget.period !=
                BudgetPeriod.DAILY.name
            ) {

                Spacer(Modifier.height(12.dp))

                Text(
                    text = "Gasto diario sugerido: ${
                        budgetMoney(
                            remaining / daysRemaining
                        )
                    }",

                    color = TextSecondary,

                    fontSize = 12.sp
                )
            }

            Spacer(Modifier.height(16.dp))

            Row {

                TextButton(
                    enabled = actionsEnabled,

                    onClick = onEdit
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
                    enabled = actionsEnabled,

                    onClick = onDelete
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
