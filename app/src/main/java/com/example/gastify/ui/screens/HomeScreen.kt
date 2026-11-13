package com.example.gastify.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape

import androidx.compose.material3.*

import androidx.compose.runtime.*

import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

import androidx.lifecycle.viewmodel.compose.viewModel

import com.example.gastify.data.local.ReminderStore

import com.example.gastify.data.model.Account
import com.example.gastify.data.model.AccountType
import com.example.gastify.data.model.Category
import com.example.gastify.data.model.Movement
import com.example.gastify.data.model.PassageReminder
import com.example.gastify.data.model.Workspace

import com.example.gastify.notifications.ReminderScheduler

import com.example.gastify.ui.components.GastifyIcons
import com.example.gastify.ui.components.QuickActionsSection

import com.example.gastify.ui.theme.*

import com.example.gastify.viewmodel.AccountViewModel
import com.example.gastify.viewmodel.CategoryViewModel
import com.example.gastify.viewmodel.MovementViewModel

import com.google.firebase.Timestamp

import java.math.BigDecimal
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private fun dashboardMoney(
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

private fun dashboardDate(
    timestamp: Timestamp?
): String {

    if (timestamp == null) {
        return "Sincronizando..."
    }

    return SimpleDateFormat(
        "dd MMM yyyy • HH:mm",
        Locale.forLanguageTag("es-PE")
    ).format(timestamp.toDate())
}

private fun dashboardNextDate(
    time: Long
): String {

    return SimpleDateFormat(
        "EEE dd MMM • HH:mm",
        Locale.forLanguageTag("es-PE")
    ).format(Date(time))
}

// ==========================================
// PANTALLA PRINCIPAL
// ==========================================

@Composable
fun GastifyHomeScreen(
    modifier: Modifier = Modifier,
    userId: String,
    workspaces: List<Workspace>,
    selectedWorkspaceId: String?,
    onSelectWorkspace: (String) -> Unit,
    onOpenSpaces: () -> Unit,
    onOpenAccounts: (Workspace) -> Unit,
    onOpenMovements: (Workspace) -> Unit,
    onOpenReminders: (Workspace) -> Unit
) {

    val workspace = workspaces.firstOrNull {
        it.id == selectedWorkspaceId
    } ?: workspaces.firstOrNull()

    if (workspace == null) {

        Box(
            modifier = modifier.background(AmoledBlack),
            contentAlignment = Alignment.Center
        ) {

            Column(
                modifier = Modifier.padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {

                Icon(
                    imageVector = GastifyIcons.workspaces,
                    contentDescription = null,
                    tint = PrimaryGreen,
                    modifier = Modifier.size(56.dp)
                )

                Spacer(Modifier.height(20.dp))

                Text(
                    text = "Bienvenido a GASTIFY",
                    fontSize = 25.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )

                Spacer(Modifier.height(12.dp))

                Text(
                    text = "Crea tu primer espacio para comenzar a controlar tus finanzas.",
                    fontSize = 14.sp,
                    lineHeight = 22.sp,
                    color = TextSecondary
                )

                Spacer(Modifier.height(24.dp))

                Button(
                    onClick = onOpenSpaces,
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

                    Text("Crear espacio")
                }
            }
        }

        return
    }

    DashboardWorkspaceContent(
        modifier = modifier,
        userId = userId,
        workspace = workspace,
        workspaces = workspaces,
        onSelectWorkspace = onSelectWorkspace,
        onOpenAccounts = {
            onOpenAccounts(workspace)
        },
        onOpenMovements = {
            onOpenMovements(workspace)
        },
        onOpenReminders = {
            onOpenReminders(workspace)
        }
    )
}

// ==========================================
// DASHBOARD DEL ESPACIO
// ==========================================

@Composable
private fun DashboardWorkspaceContent(
    modifier: Modifier,
    userId: String,
    workspace: Workspace,
    workspaces: List<Workspace>,
    onSelectWorkspace: (String) -> Unit,
    onOpenAccounts: () -> Unit,
    onOpenMovements: () -> Unit,
    onOpenReminders: () -> Unit
) {

    val context = LocalContext.current

    val accountVm: AccountViewModel = viewModel(
        key = "accounts_${userId}_${workspace.id}",
        factory = AccountViewModel.factory(
            userId,
            workspace.id
        )
    )

    val movementVm: MovementViewModel = viewModel(
        key = "movements_${userId}_${workspace.id}",
        factory = MovementViewModel.factory(
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

    val accountState by accountVm.uiState.collectAsState()

    val movementState by movementVm.uiState.collectAsState()

    val categoryState by categoryVm.uiState.collectAsState()

    // ======================================
    // FIREBASE EN TIEMPO REAL
    // ======================================

    DisposableEffect(
        accountVm,
        movementVm,
        categoryVm
    ) {

        accountVm.startListening()
        movementVm.startListening()
        categoryVm.startListening()

        onDispose {

            accountVm.stopListening()
            movementVm.stopListening()
            categoryVm.stopListening()
        }
    }

    // ======================================
    // RECORDATORIOS
    // ======================================

    val reminders = remember(
        userId,
        workspace.id
    ) {

        ReminderStore(context).getByWorkspace(
            userId,
            workspace.id
        )
    }

    val upcomingReminders = reminders
        .filter {
            it.enabled
        }
        .mapNotNull { reminder ->

            ReminderScheduler.calculateNextTime(
                reminder
            )?.let { time ->

                reminder to time
            }
        }
        .sortedBy {
            it.second
        }

    // ======================================
    // SALDOS
    // ======================================

    val totalBalance = accountState.accounts.sumOf {
        it.balanceCents
    }

    val transportAccounts = accountState.accounts.filter {
        it.type == AccountType.TRANSPORT.name
    }

    // ======================================
    // RESUMEN FINANCIERO
    // ======================================

    val totalIncome = movementState.movements
        .filter {
            it.type == "INCOME"
        }
        .sumOf {
            it.amountCents
        }

    val totalExpense = movementState.movements
        .filter {
            it.type == "EXPENSE"
        }
        .sumOf {
            it.amountCents
        }

    val recentMovements = movementState.movements.take(5)

    // ======================================
    // MENSAJES
    // ======================================

    val snackbar = remember {
        SnackbarHostState()
    }

    LaunchedEffect(
        accountState.errorMessage,
        movementState.errorMessage,
        movementState.successMessage,
        categoryState.errorMessage
    ) {

        val message = movementState.errorMessage
            ?: movementState.successMessage
            ?: accountState.errorMessage
            ?: categoryState.errorMessage

        if (message != null) {

            snackbar.showSnackbar(message)

            accountVm.clearError()
            movementVm.clearMessages()
            categoryVm.clearError()
        }
    }

    // ======================================
    // INTERFAZ
    // ======================================

    Box(
        modifier = modifier.background(AmoledBlack)
    ) {

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 20.dp,
                end = 20.dp,
                bottom = 30.dp
            ),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {

            // ENCABEZADO

            item {

                Spacer(Modifier.height(16.dp))

                Text(
                    text = "GASTIFY",
                    color = PrimaryGreen,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp
                )

                Spacer(Modifier.height(8.dp))

                Text(
                    text = "Panel de control",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )

                Spacer(Modifier.height(6.dp))

                Text(
                    text = "Tus finanzas en un solo lugar",
                    color = TextSecondary,
                    fontSize = 13.sp
                )

                Spacer(Modifier.height(24.dp))

                Text(
                    text = "Espacio financiero",
                    fontSize = 13.sp,
                    color = TextSecondary
                )

                Spacer(Modifier.height(10.dp))

                DashboardWorkspaceSelector(
                    workspace = workspace,
                    workspaces = workspaces,
                    onSelectWorkspace = onSelectWorkspace
                )
            }

            if (
                accountState.isLoading ||
                movementState.isLoading ||
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
                // SALDO TOTAL
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

                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {

                                Icon(
                                    imageVector = GastifyIcons.accounts,
                                    contentDescription = null,
                                    tint = PrimaryGreen,
                                    modifier = Modifier.size(23.dp)
                                )

                                Spacer(Modifier.width(10.dp))

                                Text(
                                    text = "Saldo total",
                                    fontSize = 14.sp,
                                    color = TextSecondary
                                )
                            }

                            Spacer(Modifier.height(16.dp))

                            Text(
                                text = dashboardMoney(totalBalance),
                                fontSize = 32.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )

                            Spacer(Modifier.height(18.dp))

                            HorizontalDivider(
                                color = BorderDark
                            )

                            Spacer(Modifier.height(16.dp))

                            Text(
                                text = "${accountState.accounts.size} cuentas registradas",
                                fontSize = 12.sp,
                                color = TextSecondary
                            )
                        }
                    }
                }

                // ==================================
                // NAVEGACIÓN RÁPIDA
                // ==================================

                item {

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {

                        DashboardQuickButton(
                            modifier = Modifier.weight(1f),
                            title = "Cuentas",
                            icon = GastifyIcons.accounts,
                            onClick = onOpenAccounts
                        )

                        DashboardQuickButton(
                            modifier = Modifier.weight(1f),
                            title = "Movimientos",
                            icon = GastifyIcons.movements,
                            onClick = onOpenMovements
                        )

                        DashboardQuickButton(
                            modifier = Modifier.weight(1f),
                            title = "Pasajes",
                            icon = GastifyIcons.account("TRANSPORT"),
                            onClick = onOpenReminders
                        )
                    }
                }

                // ==================================
                // NUEVO: ACCESOS RÁPIDOS
                // ==================================

                item {

                    Spacer(Modifier.height(14.dp))

                    key(workspace.id) {

                        QuickActionsSection(
                            userId = userId,
                            workspace = workspace,
                            accounts = accountState.accounts,
                            categories = categoryState.categories,
                            isSaving = movementState.isSaving,

                            onRegister = { action ->

                                movementVm.createMovement(
                                    accountId = action.accountId,
                                    categoryId = action.categoryId,
                                    type = action.type,
                                    amountCents = action.amountCents,
                                    note = action.note.ifBlank {
                                        action.name
                                    }
                                )
                            }
                        )
                    }
                }

                // ==================================
                // RESUMEN FINANCIERO
                // ==================================

                item {

                    Spacer(Modifier.height(16.dp))

                    Text(
                        text = "Resumen financiero",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )

                    Spacer(Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {

                        DashboardSummaryCard(
                            modifier = Modifier.weight(1f),
                            title = "Ingresos",
                            amount = totalIncome,
                            color = IncomeGreen,
                            icon = GastifyIcons.income
                        )

                        DashboardSummaryCard(
                            modifier = Modifier.weight(1f),
                            title = "Gastos",
                            amount = totalExpense,
                            color = ExpenseRed,
                            icon = GastifyIcons.expense
                        )
                    }

                    Spacer(Modifier.height(10.dp))

                    Text(
                        text = "Resumen basado en los últimos 100 movimientos registrados.",
                        fontSize = 11.sp,
                        lineHeight = 17.sp,
                        color = TextSecondary
                    )
                }

                // ==================================
                // TARJETAS DE TRANSPORTE
                // ==================================

                item {

                    Spacer(Modifier.height(16.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {

                        Icon(
                            imageVector = GastifyIcons.account(
                                "TRANSPORT"
                            ),
                            contentDescription = null,
                            tint = PrimaryGreen
                        )

                        Spacer(Modifier.width(10.dp))

                        Text(
                            text = "Mis tarjetas",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary
                        )
                    }

                    Spacer(Modifier.height(4.dp))

                    Text(
                        text = "Control de saldo estimado de transporte",
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                }

                if (transportAccounts.isEmpty()) {

                    item {

                        DashboardMessageCard(
                            message = "Todavía no tienes tarjetas de transporte. Crea una cuenta de tipo Transporte."
                        )
                    }

                } else {

                    items(
                        items = transportAccounts,
                        key = {
                            "transport_${it.id}"
                        }
                    ) { account ->

                        val nextReminder = upcomingReminders
                            .firstOrNull {
                                it.first.accountId == account.id
                            }

                        DashboardTransportCard(
                            account = account,
                            nextReminder = nextReminder
                        )
                    }
                }

                // ==================================
                // PRÓXIMOS RECORDATORIOS
                // ==================================

                item {

                    Spacer(Modifier.height(16.dp))

                    Text(
                        text = "Próximos recordatorios",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )
                }

                if (upcomingReminders.isEmpty()) {

                    item {

                        DashboardMessageCard(
                            message = "No tienes recordatorios activos para este espacio."
                        )
                    }

                } else {

                    items(
                        items = upcomingReminders.take(3),
                        key = {
                            "reminder_${it.first.id}"
                        }
                    ) { reminderItem ->

                        DashboardReminderCard(
                            reminder = reminderItem.first,
                            nextTime = reminderItem.second
                        )
                    }
                }

                item {

                    OutlinedButton(
                        onClick = onOpenReminders,
                        modifier = Modifier.fillMaxWidth(),
                        border = BorderStroke(
                            1.dp,
                            BorderDark
                        )
                    ) {

                        Text(
                            text = "Administrar recordatorios",
                            color = PrimaryGreen
                        )

                        Spacer(Modifier.width(8.dp))

                        Icon(
                            imageVector = GastifyIcons.next,
                            contentDescription = null,
                            tint = PrimaryGreen
                        )
                    }
                }

                // ==================================
                // MOVIMIENTOS RECIENTES
                // ==================================

                item {

                    Spacer(Modifier.height(16.dp))

                    Text(
                        text = "Movimientos recientes",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )
                }

                if (recentMovements.isEmpty()) {

                    item {

                        DashboardMessageCard(
                            message = "Todavía no hay movimientos financieros registrados."
                        )
                    }

                } else {

                    items(
                        items = recentMovements,
                        key = {
                            "movement_${it.id}"
                        }
                    ) { movement ->

                        val account = accountState.accounts
                            .firstOrNull {
                                it.id == movement.accountId
                            }

                        val category = categoryState.categories
                            .firstOrNull {
                                it.id == movement.categoryId
                            }

                        DashboardMovementCard(
                            movement = movement,
                            account = account,
                            category = category
                        )
                    }
                }

                item {

                    OutlinedButton(
                        onClick = onOpenMovements,
                        modifier = Modifier.fillMaxWidth(),
                        border = BorderStroke(
                            1.dp,
                            BorderDark
                        )
                    ) {

                        Text(
                            text = "Ver todos los movimientos",
                            color = PrimaryGreen
                        )

                        Spacer(Modifier.width(8.dp))

                        Icon(
                            imageVector = GastifyIcons.next,
                            contentDescription = null,
                            tint = PrimaryGreen
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
// SELECTOR DE ESPACIOS
// ==========================================

@Composable
private fun DashboardWorkspaceSelector(
    workspace: Workspace,
    workspaces: List<Workspace>,
    onSelectWorkspace: (String) -> Unit
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
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(
                1.dp,
                BorderDark
            )
        ) {

            Icon(
                imageVector = GastifyIcons.workspaces,
                contentDescription = null,
                tint = PrimaryGreen
            )

            Spacer(Modifier.width(10.dp))

            Text(
                text = workspace.name,
                color = TextPrimary,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Icon(
                imageVector = GastifyIcons.expand,
                contentDescription = null,
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

            workspaces.forEach { item ->

                DropdownMenuItem(
                    text = {

                        Text(
                            text = item.name,
                            color = TextPrimary
                        )
                    },
                    onClick = {

                        onSelectWorkspace(item.id)

                        expanded = false
                    }
                )
            }
        }
    }
}

// ==========================================
// BOTÓN DE NAVEGACIÓN
// ==========================================

@Composable
private fun DashboardQuickButton(
    modifier: Modifier = Modifier,
    title: String,
    icon: ImageVector,
    onClick: () -> Unit
) {

    OutlinedButton(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        contentPadding = PaddingValues(
            horizontal = 4.dp,
            vertical = 14.dp
        ),
        border = BorderStroke(
            1.dp,
            BorderDark
        )
    ) {

        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = PrimaryGreen,
                modifier = Modifier.size(23.dp)
            )

            Spacer(Modifier.height(8.dp))

            Text(
                text = title,
                color = TextPrimary,
                fontSize = 11.sp,
                maxLines = 1
            )
        }
    }
}

// ==========================================
// RESUMEN FINANCIERO
// ==========================================

@Composable
private fun DashboardSummaryCard(
    modifier: Modifier,
    title: String,
    amount: Long,
    color: Color,
    icon: ImageVector
) {

    Card(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = SurfaceDark
        )
    ) {

        Column(
            modifier = Modifier.padding(16.dp)
        ) {

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {

                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(19.dp)
                )

                Spacer(Modifier.width(6.dp))

                Text(
                    text = title,
                    color = TextSecondary,
                    fontSize = 12.sp
                )
            }

            Spacer(Modifier.height(14.dp))

            Text(
                text = dashboardMoney(amount),
                color = color,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

// ==========================================
// TARJETA DE TRANSPORTE
// ==========================================

@Composable
private fun DashboardTransportCard(
    account: Account,
    nextReminder: Pair<PassageReminder, Long>?
) {

    val fare = nextReminder?.first?.amountCents

    val availableTrips = if (
        fare != null &&
        fare > 0L
    ) {

        account.balanceCents / fare

    } else {

        null
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
                            imageVector = GastifyIcons.account(
                                "TRANSPORT"
                            ),
                            contentDescription = null,
                            tint = PrimaryGreen,
                            modifier = Modifier.size(27.dp)
                        )
                    }
                }

                Spacer(Modifier.width(12.dp))

                Text(
                    text = account.name,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(Modifier.height(20.dp))

            Text(
                text = "Saldo estimado",
                fontSize = 12.sp,
                color = TextSecondary
            )

            Spacer(Modifier.height(6.dp))

            Text(
                text = dashboardMoney(
                    account.balanceCents
                ),
                fontSize = 29.sp,
                fontWeight = FontWeight.Bold,
                color = PrimaryGreen
            )

            Spacer(Modifier.height(16.dp))

            HorizontalDivider(
                color = BorderDark
            )

            Spacer(Modifier.height(14.dp))

            if (availableTrips != null) {

                Text(
                    text = "$availableTrips pasajes disponibles",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )

                Spacer(Modifier.height(6.dp))

                Text(
                    text = "Calculados con un pasaje de ${
                        dashboardMoney(fare ?: 0L)
                    }.",
                    fontSize = 12.sp,
                    color = TextSecondary
                )

            } else {

                Text(
                    text = "Configura un recordatorio para calcular los pasajes disponibles.",
                    fontSize = 12.sp,
                    lineHeight = 19.sp,
                    color = TextSecondary
                )
            }

            if (nextReminder != null) {

                Spacer(Modifier.height(12.dp))

                Text(
                    text = "Próximo aviso: ${
                        dashboardNextDate(nextReminder.second)
                    }",
                    fontSize = 12.sp,
                    color = PrimaryGreen
                )
            }
        }
    }
}

// ==========================================
// RECORDATORIO
// ==========================================

@Composable
private fun DashboardReminderCard(
    reminder: PassageReminder,
    nextTime: Long
) {

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = SurfaceDark
        )
    ) {

        Row(
            modifier = Modifier.padding(18.dp),
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
                        imageVector = GastifyIcons.account(
                            "TRANSPORT"
                        ),
                        contentDescription = null,
                        tint = PrimaryGreen
                    )
                }
            }

            Spacer(Modifier.width(12.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {

                Text(
                    text = reminder.title,
                    color = TextPrimary,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(Modifier.height(6.dp))

                Text(
                    text = dashboardNextDate(nextTime),
                    color = TextSecondary,
                    fontSize = 12.sp
                )
            }

            Spacer(Modifier.width(8.dp))

            Text(
                text = dashboardMoney(
                    reminder.amountCents
                ),
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = PrimaryGreen
            )
        }
    }
}

// ==========================================
// MOVIMIENTO RECIENTE
// ==========================================

@Composable
private fun DashboardMovementCard(
    movement: Movement,
    account: Account?,
    category: Category?
) {

    val isIncome = movement.type == "INCOME"

    val movementColor = if (isIncome) {
        IncomeGreen
    } else {
        ExpenseRed
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = SurfaceDark
        )
    ) {

        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {

            Surface(
                color = CardDark,
                shape = RoundedCornerShape(12.dp)
            ) {

                Box(
                    modifier = Modifier.size(46.dp),
                    contentAlignment = Alignment.Center
                ) {

                    Icon(
                        imageVector = GastifyIcons.category(
                            category?.icon ?: ""
                        ),
                        contentDescription = null,
                        tint = PrimaryGreen,
                        modifier = Modifier.size(25.dp)
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
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(Modifier.height(5.dp))

                Text(
                    text = account?.name
                        ?: "Cuenta no disponible",
                    fontSize = 12.sp,
                    color = TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(Modifier.height(5.dp))

                Text(
                    text = dashboardDate(
                        movement.createdAt
                    ),
                    fontSize = 10.sp,
                    color = TextSecondary
                )
            }

            Spacer(Modifier.width(8.dp))

            Text(
                text = (
                    if (isIncome) "+" else "-"
                ) + dashboardMoney(
                    movement.amountCents
                ),
                color = movementColor,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp
            )
        }
    }
}

// ==========================================
// MENSAJE VACÍO
// ==========================================

@Composable
private fun DashboardMessageCard(
    message: String
) {

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = SurfaceDark
        )
    ) {

        Text(
            text = message,
            modifier = Modifier.padding(20.dp),
            fontSize = 13.sp,
            lineHeight = 20.sp,
            color = TextSecondary
        )
    }
}
