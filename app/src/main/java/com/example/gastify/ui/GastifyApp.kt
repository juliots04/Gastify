package com.example.gastify.ui

import androidx.activity.compose.BackHandler

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll

import androidx.compose.material3.*

import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable

import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

import androidx.lifecycle.viewmodel.compose.viewModel

import com.example.gastify.data.local.AppPreferences
import com.example.gastify.data.local.AppPreferencesStore
import com.example.gastify.data.local.NotificationPreferencesStore

import com.example.gastify.data.model.Workspace

import com.example.gastify.data.repository.FirestoreSyncMonitor

import com.example.gastify.notifications.BudgetAlertWorker
import com.example.gastify.notifications.ReminderScheduler
import com.example.gastify.notifications.StockAlertWorker

import com.example.gastify.ui.components.AppPreferencesSection
import com.example.gastify.ui.components.GastifyHeader
import com.example.gastify.ui.components.GastifyIcons
import com.example.gastify.ui.components.NotificationSettingsSection
import com.example.gastify.ui.components.OfflineSyncBanner
import com.example.gastify.ui.components.ProfileSettingsSection

import com.example.gastify.ui.screens.AccountsScreen
import com.example.gastify.ui.screens.BudgetsScreen
import com.example.gastify.ui.screens.CategoriesScreen
import com.example.gastify.ui.screens.CommerceScreen
import com.example.gastify.ui.screens.GastifyHomeScreen
import com.example.gastify.ui.screens.InventoryScreen
import com.example.gastify.ui.screens.MovementsScreen
import com.example.gastify.ui.screens.ProfitabilityScreen
import com.example.gastify.ui.screens.RemindersScreen
import com.example.gastify.ui.screens.StatisticsScreen
import com.example.gastify.ui.screens.WorkspacesScreen

import com.example.gastify.ui.theme.*

import com.example.gastify.viewmodel.WorkspaceViewModel

import com.google.firebase.auth.FirebaseAuth

// ==========================================
// PESTAÑAS PRINCIPALES
// ==========================================

private enum class MainSection(
    val title: String,
    val icon: ImageVector
) {

    HOME(
        "Inicio",
        GastifyIcons.home
    ),

    MOVEMENTS(
        "Movimientos",
        GastifyIcons.movements
    ),

    WORKSPACES(
        "Espacios",
        GastifyIcons.workspaces
    ),

    STATISTICS(
        "Estadísticas",
        GastifyIcons.statistics
    ),

    SETTINGS(
        "Ajustes",
        GastifyIcons.settings
    )
}

// ==========================================
// SUBPANTALLAS
// ==========================================

private enum class WorkspaceSection {

    CATEGORIES,

    ACCOUNTS,

    REMINDERS,

    BUDGETS,

    INVENTORY,

    COMMERCE,

    PROFITABILITY
}

// ==========================================
// APLICACIÓN PRINCIPAL
// ==========================================

@Composable
fun GastifyApp(
    onSignOut: () -> Unit
) {

    val context = LocalContext.current

    val userId = FirebaseAuth
        .getInstance()
        .currentUser
        ?.uid
        ?: return

    // ======================================
    // PREFERENCIAS
    // ======================================

    val preferencesStore = remember(
        context,
        userId
    ) {

        AppPreferencesStore(
            context,
            userId
        )
    }

    var appPreferences by remember(userId) {

        mutableStateOf(
            preferencesStore.read()
        )
    }

    // ======================================
    // ESPACIOS
    // ======================================

    val workspaceVm: WorkspaceViewModel = viewModel(

        key = "workspaces_$userId",

        factory = WorkspaceViewModel.factory(
            userId
        )
    )

    val workspaceState by workspaceVm
        .uiState
        .collectAsState()

    DisposableEffect(workspaceVm) {

        workspaceVm.startListening()

        onDispose {

            workspaceVm.stopListening()
        }
    }

    // ======================================
    // MONITOR DE FIRESTORE - FASE 19.2
    // ======================================

    val syncMonitor = remember(userId) {

        FirestoreSyncMonitor(
            userId
        )
    }

    val syncState by syncMonitor
        .state
        .collectAsState()

    DisposableEffect(syncMonitor) {

        syncMonitor.start()

        onDispose {

            syncMonitor.stop()
        }
    }

    // ======================================
    // NOTIFICACIONES
    // ======================================

    LaunchedEffect(userId) {

        val notificationSettings =
            NotificationPreferencesStore(
                context,
                userId
            ).read()

        // RECORDATORIOS

        if (notificationSettings.remindersEnabled) {

            ReminderScheduler.activateUser(
                context,
                userId
            )

            ReminderScheduler.scheduleAll(
                context
            )

        } else {

            ReminderScheduler.deactivateUser(
                context
            )
        }

        // PRESUPUESTOS

        if (notificationSettings.budgetsEnabled) {

            BudgetAlertWorker.ensureScheduled(
                context
            )

            BudgetAlertWorker.checkNow(
                context
            )

        } else {

            BudgetAlertWorker.cancelScheduled(
                context
            )
        }

        // INVENTARIO

        if (notificationSettings.stockEnabled) {

            StockAlertWorker.ensureScheduled(
                context
            )

            StockAlertWorker.checkNow(
                context
            )

        } else {

            StockAlertWorker.cancelScheduled(
                context
            )
        }
    }

    // ======================================
    // NAVEGACIÓN
    // ======================================

    var selectedSection by rememberSaveable(userId) {

        mutableStateOf(
            appPreferences.startSection.name
        )
    }

    var selectedWorkspaceId by rememberSaveable(userId) {

        mutableStateOf<String?>(null)
    }

    var selectedWorkspaceSection by rememberSaveable(userId) {

        mutableStateOf(
            WorkspaceSection.CATEGORIES.name
        )
    }

    var selectedMovementWorkspaceId by rememberSaveable(userId) {

        mutableStateOf<String?>(null)
    }

    var selectedStatisticsWorkspaceId by rememberSaveable(userId) {

        mutableStateOf<String?>(null)
    }

    var selectedHomeWorkspaceId by remember(userId) {

        mutableStateOf<String?>(null)
    }

    // ======================================
    // ESPACIO PREFERIDO
    // ======================================

    val availableWorkspaceIds =
        workspaceState.workspaces
            .map {
                it.id
            }
            .toSet()

    val preferredWorkspaceId =
        appPreferences.preferredWorkspaceId(
            availableWorkspaceIds
        )

    val effectiveHomeWorkspaceId =
        selectedHomeWorkspaceId
            ?.takeIf {
                it in availableWorkspaceIds
            }
            ?: preferredWorkspaceId

    // ======================================
    // REGISTRAR ESPACIO UTILIZADO
    // ======================================

    fun recordWorkspaceUsage(
        workspaceId: String
    ) {

        selectedHomeWorkspaceId =
            workspaceId

        if (
            workspaceId in availableWorkspaceIds &&
            appPreferences.rememberLastWorkspace
        ) {

            val saved = preferencesStore
                .recordWorkspaceOpened(
                    workspaceId
                )

            if (saved) {

                appPreferences =
                    preferencesStore.read()
            }
        }
    }

    // ======================================
    // ACTUALIZAR PREFERENCIAS
    // ======================================

    fun onPreferencesUpdated(
        updated: AppPreferences
    ) {

        appPreferences = updated

        selectedHomeWorkspaceId = null
    }

    // ======================================
    // SECCIÓN ACTUAL
    // ======================================

    val currentSection = MainSection.entries
        .firstOrNull {

            it.name == selectedSection

        } ?: MainSection.HOME

    val openedWorkspace =
        workspaceState.workspaces
            .firstOrNull {

                it.id == selectedWorkspaceId
            }

    val openedMovementWorkspace =
        workspaceState.workspaces
            .firstOrNull {

                it.id == selectedMovementWorkspaceId
            }

    val openedStatisticsWorkspace =
        workspaceState.workspaces
            .firstOrNull {

                it.id == selectedStatisticsWorkspaceId
            }

    val currentWorkspaceSection =
        WorkspaceSection.entries
            .firstOrNull {

                it.name == selectedWorkspaceSection

            } ?: WorkspaceSection.CATEGORIES

    // ======================================
    // BOTÓN ATRÁS
    // ======================================

    BackHandler(

        enabled =
            openedWorkspace != null ||
                openedMovementWorkspace != null ||
                openedStatisticsWorkspace != null ||
                currentSection != MainSection.HOME

    ) {

        when {

            openedWorkspace != null -> {

                selectedWorkspaceId = null
            }

            openedMovementWorkspace != null -> {

                selectedMovementWorkspaceId = null
            }

            openedStatisticsWorkspace != null -> {

                selectedStatisticsWorkspaceId = null
            }

            else -> {

                selectedSection =
                    MainSection.HOME.name
            }
        }
    }

    // ======================================
    // VALIDAR ESPACIOS
    // ======================================

    LaunchedEffect(
        selectedWorkspaceId,
        selectedMovementWorkspaceId,
        selectedStatisticsWorkspaceId,
        workspaceState.isLoading,
        workspaceState.workspaces
    ) {

        if (!workspaceState.isLoading) {

            if (
                selectedWorkspaceId != null &&
                openedWorkspace == null
            ) {

                selectedWorkspaceId = null
            }

            if (
                selectedMovementWorkspaceId != null &&
                openedMovementWorkspace == null
            ) {

                selectedMovementWorkspaceId = null
            }

            if (
                selectedStatisticsWorkspaceId != null &&
                openedStatisticsWorkspace == null
            ) {

                selectedStatisticsWorkspaceId = null
            }
        }
    }

    // ======================================
    // MENSAJES
    // ======================================

    val snackbar = remember {
        SnackbarHostState()
    }

    LaunchedEffect(
        workspaceState.errorMessage
    ) {

        workspaceState.errorMessage?.let {

            snackbar.showSnackbar(it)

            workspaceVm.clearError()
        }
    }

    // ======================================
    // ESTADO DE NAVEGACIÓN
    // ======================================

    val inSubscreen =
        openedWorkspace != null ||
            openedMovementWorkspace != null ||
            openedStatisticsWorkspace != null

    // ======================================
    // INTERFAZ PRINCIPAL
    // ======================================

    Scaffold(

        modifier = Modifier.fillMaxSize(),

        containerColor = AmoledBlack,

        snackbarHost = {

            SnackbarHost(
                hostState = snackbar
            )
        },

        bottomBar = {

            if (!inSubscreen) {

                Column {

                    HorizontalDivider(
                        color = BorderDark
                    )

                    NavigationBar(

                        containerColor = AmoledBlack,

                        tonalElevation = 0.dp

                    ) {

                        MainSection.entries.forEach {
                                section ->

                            NavigationBarItem(

                                selected =
                                    currentSection == section,

                                onClick = {

                                    selectedSection =
                                        section.name
                                },

                                icon = {

                                    Icon(

                                        imageVector =
                                            section.icon,

                                        contentDescription =
                                            section.title,

                                        modifier =
                                            Modifier.size(24.dp)
                                    )
                                },

                                label = {

                                    Text(
                                        text = section.title,
                                        fontSize = 10.sp
                                    )
                                },

                                alwaysShowLabel = true,

                                colors =
                                    NavigationBarItemDefaults.colors(

                                        selectedIconColor =
                                            PrimaryGreen,

                                        selectedTextColor =
                                            PrimaryGreen,

                                        indicatorColor =
                                            SurfaceDark,

                                        unselectedIconColor =
                                            TextSecondary,

                                        unselectedTextColor =
                                            TextSecondary
                                    )
                            )
                        }
                    }
                }
            }
        }

    ) { innerPadding ->

        // ======================================
        // CONTENEDOR PRINCIPAL
        // ======================================

        Column(

            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)

        ) {

            // ==============================
            // AVISO DE CONEXIÓN
            // ==============================

            OfflineSyncBanner(
                state = syncState
            )

            // ==============================
            // HEADER GLOBAL
            // ==============================

            if (!inSubscreen) {

                GastifyHeader()
            }

            // ==============================
            // CONTENIDO DE GASTIFY
            // ==============================

            Box(

                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)

            ) {

                // ==================================
                // SUBPANTALLAS DE ESPACIOS
                // ==================================

                if (openedWorkspace != null) {

                    when (currentWorkspaceSection) {

                        WorkspaceSection.CATEGORIES -> {

                            CategoriesScreen(
                                userId = userId,

                                workspace = openedWorkspace,

                                onBack = {
                                    selectedWorkspaceId = null
                                }
                            )
                        }

                        WorkspaceSection.ACCOUNTS -> {

                            AccountsScreen(
                                userId = userId,

                                workspace = openedWorkspace,

                                onBack = {
                                    selectedWorkspaceId = null
                                }
                            )
                        }

                        WorkspaceSection.REMINDERS -> {

                            RemindersScreen(
                                userId = userId,

                                workspace = openedWorkspace,

                                onBack = {
                                    selectedWorkspaceId = null
                                }
                            )
                        }

                        WorkspaceSection.BUDGETS -> {

                            BudgetsScreen(
                                userId = userId,

                                workspace = openedWorkspace,

                                onBack = {
                                    selectedWorkspaceId = null
                                }
                            )
                        }

                        WorkspaceSection.INVENTORY -> {

                            InventoryScreen(
                                userId = userId,

                                workspace = openedWorkspace,

                                onBack = {
                                    selectedWorkspaceId = null
                                }
                            )
                        }

                        WorkspaceSection.COMMERCE -> {

                            CommerceScreen(
                                userId = userId,

                                workspace = openedWorkspace,

                                onBack = {
                                    selectedWorkspaceId = null
                                }
                            )
                        }

                        WorkspaceSection.PROFITABILITY -> {

                            ProfitabilityScreen(
                                userId = userId,

                                workspace = openedWorkspace,

                                onBack = {
                                    selectedWorkspaceId = null
                                }
                            )
                        }
                    }

                } else if (
                    openedMovementWorkspace != null
                ) {

                    // ==================================
                    // MOVIMIENTOS FINANCIEROS
                    // ==================================

                    MovementsScreen(
                        userId = userId,

                        workspace =
                            openedMovementWorkspace,

                        onBack = {
                            selectedMovementWorkspaceId = null
                        }
                    )

                } else if (
                    openedStatisticsWorkspace != null
                ) {

                    // ==================================
                    // ESTADÍSTICAS
                    // ==================================

                    StatisticsScreen(
                        userId = userId,

                        workspace =
                            openedStatisticsWorkspace,

                        onBack = {
                            selectedStatisticsWorkspaceId = null
                        }
                    )

                } else {

                    // ==================================
                    // PESTAÑAS PRINCIPALES
                    // ==================================

                    when (currentSection) {

                        // ==============================
                        // INICIO
                        // ==============================

                        MainSection.HOME -> {

                            GastifyHomeScreen(

                                modifier =
                                    Modifier.fillMaxSize(),

                                userId = userId,

                                workspaces =
                                    workspaceState.workspaces,

                                selectedWorkspaceId =
                                    effectiveHomeWorkspaceId,

                                onSelectWorkspace = {
                                        workspaceId ->

                                    selectedHomeWorkspaceId =
                                        workspaceId

                                    if (
                                        workspaceId != null &&
                                        workspaceId in
                                            availableWorkspaceIds
                                    ) {

                                        recordWorkspaceUsage(
                                            workspaceId
                                        )
                                    }
                                },

                                onOpenSpaces = {

                                    selectedSection =
                                        MainSection.WORKSPACES.name
                                },

                                onOpenAccounts = {
                                        workspace ->

                                    recordWorkspaceUsage(
                                        workspace.id
                                    )

                                    selectedWorkspaceSection =
                                        WorkspaceSection.ACCOUNTS.name

                                    selectedWorkspaceId =
                                        workspace.id
                                },

                                onOpenMovements = {
                                        workspace ->

                                    recordWorkspaceUsage(
                                        workspace.id
                                    )

                                    selectedMovementWorkspaceId =
                                        workspace.id
                                },

                                onOpenReminders = {
                                        workspace ->

                                    recordWorkspaceUsage(
                                        workspace.id
                                    )

                                    selectedWorkspaceSection =
                                        WorkspaceSection.REMINDERS.name

                                    selectedWorkspaceId =
                                        workspace.id
                                }
                            )
                        }

                        // ==============================
                        // MOVIMIENTOS
                        // ==============================

                        MainSection.MOVEMENTS -> {

                            WorkspacePicker(

                                modifier =
                                    Modifier.fillMaxSize(),

                                title = "Movimientos",

                                subtitle =
                                    "Selecciona un espacio para consultar ingresos y gastos.",

                                buttonText =
                                    "Ver movimientos",

                                buttonIcon =
                                    GastifyIcons.movements,

                                workspaces =
                                    workspaceState.workspaces,

                                preferredWorkspaceId =
                                    preferredWorkspaceId,

                                onOpenWorkspace = {
                                        workspace ->

                                    recordWorkspaceUsage(
                                        workspace.id
                                    )

                                    selectedMovementWorkspaceId =
                                        workspace.id
                                }
                            )
                        }

                        // ==============================
                        // ESPACIOS
                        // ==============================

                        MainSection.WORKSPACES -> {

                            WorkspacesScreen(

                                modifier =
                                    Modifier.fillMaxSize(),

                                workspaces =
                                    workspaceState.workspaces,

                                isSaving =
                                    workspaceState.isSaving,

                                onAddWorkspace = {

                                    workspaceVm.createWorkspace(
                                        it
                                    )
                                },

                                onDeleteWorkspace = {

                                    workspaceVm.deleteWorkspace(
                                        it
                                    )
                                },

                                onOpenCategories = {
                                        workspace ->

                                    recordWorkspaceUsage(
                                        workspace.id
                                    )

                                    selectedWorkspaceSection =
                                        WorkspaceSection.CATEGORIES.name

                                    selectedWorkspaceId =
                                        workspace.id
                                },

                                onOpenAccounts = {
                                        workspace ->

                                    recordWorkspaceUsage(
                                        workspace.id
                                    )

                                    selectedWorkspaceSection =
                                        WorkspaceSection.ACCOUNTS.name

                                    selectedWorkspaceId =
                                        workspace.id
                                },

                                onOpenReminders = {
                                        workspace ->

                                    recordWorkspaceUsage(
                                        workspace.id
                                    )

                                    selectedWorkspaceSection =
                                        WorkspaceSection.REMINDERS.name

                                    selectedWorkspaceId =
                                        workspace.id
                                },

                                onOpenBudgets = {
                                        workspace ->

                                    recordWorkspaceUsage(
                                        workspace.id
                                    )

                                    selectedWorkspaceSection =
                                        WorkspaceSection.BUDGETS.name

                                    selectedWorkspaceId =
                                        workspace.id
                                },

                                onOpenInventory = {
                                        workspace ->

                                    recordWorkspaceUsage(
                                        workspace.id
                                    )

                                    selectedWorkspaceSection =
                                        WorkspaceSection.INVENTORY.name

                                    selectedWorkspaceId =
                                        workspace.id
                                },

                                onOpenCommerce = {
                                        workspace ->

                                    recordWorkspaceUsage(
                                        workspace.id
                                    )

                                    selectedWorkspaceSection =
                                        WorkspaceSection.COMMERCE.name

                                    selectedWorkspaceId =
                                        workspace.id
                                },

                                onOpenProfitability = {
                                        workspace ->

                                    recordWorkspaceUsage(
                                        workspace.id
                                    )

                                    selectedWorkspaceSection =
                                        WorkspaceSection.PROFITABILITY.name

                                    selectedWorkspaceId =
                                        workspace.id
                                }
                            )
                        }

                        // ==============================
                        // ESTADÍSTICAS
                        // ==============================

                        MainSection.STATISTICS -> {

                            WorkspacePicker(

                                modifier =
                                    Modifier.fillMaxSize(),

                                title = "Estadísticas",

                                subtitle =
                                    "Selecciona un espacio para analizar tus finanzas.",

                                buttonText =
                                    "Ver estadísticas",

                                buttonIcon =
                                    GastifyIcons.statistics,

                                workspaces =
                                    workspaceState.workspaces,

                                preferredWorkspaceId =
                                    preferredWorkspaceId,

                                onOpenWorkspace = {
                                        workspace ->

                                    recordWorkspaceUsage(
                                        workspace.id
                                    )

                                    selectedStatisticsWorkspaceId =
                                        workspace.id
                                }
                            )
                        }

                        // ==============================
                        // AJUSTES
                        // ==============================

                        MainSection.SETTINGS -> {

                            SettingsScreen(

                                modifier =
                                    Modifier.fillMaxSize(),

                                userId = userId,

                                workspaces =
                                    workspaceState.workspaces,

                                preferences =
                                    appPreferences,

                                preferencesStore =
                                    preferencesStore,

                                onPreferencesChanged = {
                                        updated ->

                                    onPreferencesUpdated(
                                        updated
                                    )
                                },

                                onSignOut = {

                                    ReminderScheduler.deactivateUser(
                                        context
                                    )

                                    BudgetAlertWorker.cancelScheduled(
                                        context
                                    )

                                    StockAlertWorker.cancelScheduled(
                                        context
                                    )

                                    onSignOut()
                                }
                            )
                        }
                    }
                }

                // ==================================
                // INDICADOR DE CARGA
                // ==================================

                if (workspaceState.isLoading) {

                    Box(

                        modifier = Modifier.fillMaxSize(),

                        contentAlignment =
                            Alignment.Center

                    ) {

                        CircularProgressIndicator(
                            color = PrimaryGreen
                        )
                    }
                }
            }
        }
    }
}

// ==========================================
// SELECTOR DE ESPACIOS
// ==========================================

@Composable
private fun WorkspacePicker(
    modifier: Modifier = Modifier,
    title: String,
    subtitle: String,
    buttonText: String,
    buttonIcon: ImageVector,
    workspaces: List<Workspace>,
    preferredWorkspaceId: String?,
    onOpenWorkspace: (Workspace) -> Unit
) {

    Column(

        modifier = modifier
            .verticalScroll(
                rememberScrollState()
            )
            .padding(20.dp)

    ) {

        Spacer(
            Modifier.height(16.dp)
        )

        Text(
            text = title,

            fontSize = 28.sp,

            fontWeight = FontWeight.Bold
        )

        Spacer(
            Modifier.height(8.dp)
        )

        Text(
            text = subtitle,

            fontSize = 14.sp,

            lineHeight = 21.sp,

            color = TextSecondary
        )

        Spacer(
            Modifier.height(30.dp)
        )

        Text(
            text = "Mis espacios",

            fontSize = 20.sp,

            fontWeight = FontWeight.SemiBold
        )

        Spacer(
            Modifier.height(18.dp)
        )

        // ======================================
        // SIN ESPACIOS
        // ======================================

        if (workspaces.isEmpty()) {

            Card(

                modifier = Modifier.fillMaxWidth(),

                colors = CardDefaults.cardColors(
                    containerColor = SurfaceDark
                )

            ) {

                Text(
                    text =
                        "Crea primero un espacio desde la pestaña Espacios.",

                    modifier = Modifier.padding(24.dp),

                    color = TextSecondary
                )
            }

        } else {

            // ======================================
            // ORDENAR ESPACIOS
            // ======================================

            val sortedWorkspaces = workspaces
                .sortedWith(

                    compareByDescending<Workspace> {

                        it.id == preferredWorkspaceId

                    }.thenBy {

                        it.name.lowercase()
                    }
                )

            // ======================================
            // MOSTRAR ESPACIOS
            // ======================================

            sortedWorkspaces.forEach { workspace ->

                val isPreferred =
                    workspace.id == preferredWorkspaceId

                Card(

                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),

                    colors = CardDefaults.cardColors(
                        containerColor = SurfaceDark
                    )

                ) {

                    Column(
                        modifier = Modifier.padding(20.dp)
                    ) {

                        Row(

                            verticalAlignment =
                                Alignment.CenterVertically

                        ) {

                            Icon(

                                imageVector =
                                    GastifyIcons.workspaces,

                                contentDescription = null,

                                tint = PrimaryGreen
                            )

                            Spacer(
                                Modifier.width(10.dp)
                            )

                            Text(

                                text = workspace.name,

                                fontSize = 18.sp,

                                fontWeight =
                                    FontWeight.SemiBold,

                                modifier =
                                    Modifier.weight(1f)
                            )
                        }

                        Spacer(
                            Modifier.height(8.dp)
                        )

                        Text(

                            text = if (isPreferred) {

                                "Espacio preferido"

                            } else {

                                "Espacio financiero"
                            },

                            color = if (isPreferred) {

                                PrimaryGreen

                            } else {

                                TextSecondary
                            },

                            fontSize = 13.sp
                        )

                        Spacer(
                            Modifier.height(18.dp)
                        )

                        Button(

                            onClick = {

                                onOpenWorkspace(
                                    workspace
                                )
                            },

                            modifier =
                                Modifier.fillMaxWidth(),

                            colors =
                                ButtonDefaults.buttonColors(

                                    containerColor =
                                        CardDark,

                                    contentColor =
                                        PrimaryGreen
                                )

                        ) {

                            Icon(
                                imageVector = buttonIcon,

                                contentDescription = null
                            )

                            Spacer(
                                Modifier.width(8.dp)
                            )

                            Text(

                                text = buttonText,

                                modifier =
                                    Modifier.weight(1f)
                            )

                            Icon(
                                imageVector =
                                    GastifyIcons.next,

                                contentDescription = null
                            )
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// CONFIGURACIÓN
// ==========================================

@Composable
private fun SettingsScreen(
    modifier: Modifier = Modifier,
    userId: String,
    workspaces: List<Workspace>,
    preferences: AppPreferences,
    preferencesStore: AppPreferencesStore,
    onPreferencesChanged: (AppPreferences) -> Unit,
    onSignOut: () -> Unit
) {

    Column(

        modifier = modifier
            .verticalScroll(
                rememberScrollState()
            )
            .padding(24.dp)

    ) {

        Spacer(
            Modifier.height(16.dp)
        )

        Row(

            verticalAlignment =
                Alignment.CenterVertically

        ) {

            Icon(

                imageVector =
                    GastifyIcons.settings,

                contentDescription = null,

                tint = PrimaryGreen
            )

            Spacer(
                Modifier.width(10.dp)
            )

            Text(

                text = "Configuración",

                fontSize = 28.sp,

                fontWeight = FontWeight.Bold
            )
        }

        Spacer(
            Modifier.height(30.dp)
        )

        // ======================================
        // PERFIL DE GOOGLE - FASE 18.3
        // ======================================

        ProfileSettingsSection(
            userId = userId
        )

        Spacer(
            Modifier.height(30.dp)
        )

        HorizontalDivider(
            color = BorderDark
        )

        Spacer(
            Modifier.height(28.dp)
        )

        // ======================================
        // NOTIFICACIONES - FASE 18.1
        // ======================================

        NotificationSettingsSection(
            userId = userId
        )

        Spacer(
            Modifier.height(32.dp)
        )

        HorizontalDivider(
            color = BorderDark
        )

        Spacer(
            Modifier.height(28.dp)
        )

        // ======================================
        // PREFERENCIAS - FASE 18.2
        // ======================================

        AppPreferencesSection(

            workspaces = workspaces,

            preferences = preferences,

            store = preferencesStore,

            onPreferencesChanged =
                onPreferencesChanged
        )

        Spacer(
            Modifier.height(32.dp)
        )

        HorizontalDivider(
            color = BorderDark
        )

        Spacer(
            Modifier.height(20.dp)
        )

        // ======================================
        // CERRAR SESIÓN
        // ======================================

        OutlinedButton(

            onClick = onSignOut,

            modifier = Modifier.fillMaxWidth()

        ) {

            Icon(

                imageVector = GastifyIcons.logout,

                contentDescription = null,

                tint = ExpenseRed
            )

            Spacer(
                Modifier.width(8.dp)
            )

            Text(

                text = "Cerrar sesión",

                color = ExpenseRed,

                fontWeight = FontWeight.SemiBold
            )
        }

        Spacer(
            Modifier.height(24.dp)
        )
    }
}
