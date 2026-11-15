package com.example.gastify.ui

import androidx.activity.compose.BackHandler

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Tune
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

    val activeWorkspace =
        workspaceState.workspaces
            .firstOrNull {
                it.id == effectiveHomeWorkspaceId
            }
            ?: workspaceState.workspaces.firstOrNull()

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

    var settingsPage by rememberSaveable { mutableStateOf<String?>(null) }
    var settingsReturnSection by rememberSaveable { mutableStateOf(MainSection.HOME.name) }

    val currentSection = MainSection.entries
        .firstOrNull {

            it.name == selectedSection

        } ?: MainSection.HOME

    val openedWorkspace =
        workspaceState.workspaces
            .firstOrNull {

                it.id == selectedWorkspaceId
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
                currentSection != MainSection.HOME

    ) {

        when {
            currentSection == MainSection.SETTINGS && settingsPage != null -> { settingsPage = null }
            currentSection == MainSection.SETTINGS -> { selectedSection = settingsReturnSection }

            openedWorkspace != null -> {

                selectedWorkspaceId = null
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
            currentSection == MainSection.SETTINGS

    // ======================================
    // INTERFAZ PRINCIPAL
    // ======================================

    val accountsEdgeToEdge = openedWorkspace != null && currentWorkspaceSection in listOf(WorkspaceSection.ACCOUNTS, WorkspaceSection.CATEGORIES, WorkspaceSection.REMINDERS, WorkspaceSection.BUDGETS)

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

                        MainSection.entries.filter { it != MainSection.SETTINGS }.forEach {
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
                .padding(if (accountsEdgeToEdge) PaddingValues(bottom = innerPadding.calculateBottomPadding()) else innerPadding)

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

                GastifyHeader(
                    photoUrl = FirebaseAuth.getInstance().currentUser?.photoUrl?.toString(),
                    workspaces = workspaceState.workspaces,
                    activeWorkspace = activeWorkspace,
                    onSelectWorkspace = { workspace ->
                        recordWorkspaceUsage(workspace.id)
                    },
                    onManageWorkspaces = {
                        settingsPage = "Espacios"
                        settingsReturnSection = selectedSection
                        selectedSection = MainSection.SETTINGS.name
                    },
                    onOpenAccount = {
                        settingsPage = null
                        settingsReturnSection = selectedSection
                        selectedSection = MainSection.SETTINGS.name
                    }
                )
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
                                },
                                onOpenMovements = {
                                    recordWorkspaceUsage(openedWorkspace.id)
                                    selectedWorkspaceId = null
                                    selectedSection = MainSection.MOVEMENTS.name
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
                                modifier = Modifier.fillMaxSize(),
                                userId = userId,
                                workspaces = workspaceState.workspaces,
                                selectedWorkspaceId = effectiveHomeWorkspaceId,
                                onSelectWorkspace = { workspaceId ->
                                    recordWorkspaceUsage(workspaceId)
                                },
                                onOpenSpaces = {
                                    settingsPage = "Espacios"
                                    settingsReturnSection = selectedSection
                                    selectedSection = MainSection.SETTINGS.name
                                },
                                onOpenCategories = { workspace ->
                                    recordWorkspaceUsage(workspace.id)
                                    selectedWorkspaceSection = WorkspaceSection.CATEGORIES.name
                                    selectedWorkspaceId = workspace.id
                                },
                                onOpenAccounts = { workspace ->
                                    recordWorkspaceUsage(workspace.id)
                                    selectedWorkspaceSection = WorkspaceSection.ACCOUNTS.name
                                    selectedWorkspaceId = workspace.id
                                },
                                onOpenMovements = { workspace ->
                                    recordWorkspaceUsage(workspace.id)
                                    selectedSection = MainSection.MOVEMENTS.name
                                },
                                onOpenReminders = { workspace ->
                                    recordWorkspaceUsage(workspace.id)
                                    selectedWorkspaceSection = WorkspaceSection.REMINDERS.name
                                    selectedWorkspaceId = workspace.id
                                },
                                onOpenBudgets = { workspace ->
                                    recordWorkspaceUsage(workspace.id)
                                    selectedWorkspaceSection = WorkspaceSection.BUDGETS.name
                                    selectedWorkspaceId = workspace.id
                                },
                                onOpenInventory = { workspace ->
                                    recordWorkspaceUsage(workspace.id)
                                    selectedWorkspaceSection = WorkspaceSection.INVENTORY.name
                                    selectedWorkspaceId = workspace.id
                                },
                                onOpenCommerce = { workspace ->
                                    recordWorkspaceUsage(workspace.id)
                                    selectedWorkspaceSection = WorkspaceSection.COMMERCE.name
                                    selectedWorkspaceId = workspace.id
                                },
                                onOpenProfitability = { workspace ->
                                    recordWorkspaceUsage(workspace.id)
                                    selectedWorkspaceSection = WorkspaceSection.PROFITABILITY.name
                                    selectedWorkspaceId = workspace.id
                                }
                            )
                        }

                        // ==============================
                        // MOVIMIENTOS
                        // ==============================

                        MainSection.MOVEMENTS -> {
                            if (activeWorkspace != null) {
                                MovementsScreen(
                                    userId = userId,
                                    workspace = activeWorkspace,
                                    showBackButton = false,
                                    onBack = {
                                        selectedSection = MainSection.HOME.name
                                    }
                                )
                            } else {
                                NoActiveWorkspaceView(
                                    title = "Movimientos",
                                    message = "Crea un espacio para comenzar a registrar tus movimientos.",
                                    onCreateSpace = {
                                        settingsPage = "Espacios"
                                        settingsReturnSection = selectedSection
                                        selectedSection = MainSection.SETTINGS.name
                                    }
                                )
                            }
                        }

                        // ==============================
                        // ESTADÍSTICAS
                        // ==============================

                        MainSection.STATISTICS -> {
                            if (activeWorkspace != null) {
                                StatisticsScreen(
                                    userId = userId,
                                    workspace = activeWorkspace,
                                    showBackButton = false,
                                    onBack = {
                                        selectedSection = MainSection.HOME.name
                                    }
                                )
                            } else {
                                NoActiveWorkspaceView(
                                    title = "Estadísticas",
                                    message = "Crea un espacio para consultar las estadísticas de tus finanzas.",
                                    onCreateSpace = {
                                        settingsPage = "Espacios"
                                        settingsReturnSection = selectedSection
                                        selectedSection = MainSection.SETTINGS.name
                                    }
                                )
                            }
                        }

                        // ==============================
                        // AJUSTES
                        // ==============================

                        MainSection.SETTINGS -> {

                            SettingsScreen(
                                modifier = Modifier.fillMaxSize(),
                                page = settingsPage,
                                onOpenPage = { settingsPage = it },
                                onBack = {
                                    if (settingsPage != null) {
                                        settingsPage = null
                                    } else {
                                        selectedSection = settingsReturnSection
                                    }
                                },
                                userId = userId,
                                workspaces = workspaceState.workspaces,
                                isSavingWorkspace = workspaceState.isSaving,
                                onAddWorkspace = { name, type ->
                                    workspaceVm.createWorkspace(name, type)
                                },
                                onUpdateWorkspace = { id, name, type ->
                                    workspaceVm.updateWorkspace(id, name, type)
                                },
                                onDeleteWorkspace = { name ->
                                    workspaceVm.deleteWorkspace(name)
                                },
                                onOpenCategories = { workspace ->
                                    recordWorkspaceUsage(workspace.id)
                                    selectedWorkspaceSection = WorkspaceSection.CATEGORIES.name
                                    selectedWorkspaceId = workspace.id
                                },
                                onOpenAccounts = { workspace ->
                                    recordWorkspaceUsage(workspace.id)
                                    selectedWorkspaceSection = WorkspaceSection.ACCOUNTS.name
                                    selectedWorkspaceId = workspace.id
                                },
                                onOpenReminders = { workspace ->
                                    recordWorkspaceUsage(workspace.id)
                                    selectedWorkspaceSection = WorkspaceSection.REMINDERS.name
                                    selectedWorkspaceId = workspace.id
                                },
                                onOpenBudgets = { workspace ->
                                    recordWorkspaceUsage(workspace.id)
                                    selectedWorkspaceSection = WorkspaceSection.BUDGETS.name
                                    selectedWorkspaceId = workspace.id
                                },
                                onOpenInventory = { workspace ->
                                    recordWorkspaceUsage(workspace.id)
                                    selectedWorkspaceSection = WorkspaceSection.INVENTORY.name
                                    selectedWorkspaceId = workspace.id
                                },
                                onOpenCommerce = { workspace ->
                                    recordWorkspaceUsage(workspace.id)
                                    selectedWorkspaceSection = WorkspaceSection.COMMERCE.name
                                    selectedWorkspaceId = workspace.id
                                },
                                onOpenProfitability = { workspace ->
                                    recordWorkspaceUsage(workspace.id)
                                    selectedWorkspaceSection = WorkspaceSection.PROFITABILITY.name
                                    selectedWorkspaceId = workspace.id
                                },
                                preferences = appPreferences,
                                preferencesStore = preferencesStore,
                                onPreferencesChanged = { updated ->
                                    onPreferencesUpdated(updated)
                                },
                                onSignOut = {
                                    ReminderScheduler.deactivateUser(context)
                                    BudgetAlertWorker.cancelScheduled(context)
                                    StockAlertWorker.cancelScheduled(context)
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
    page: String?,
    onOpenPage: (String) -> Unit,
    onBack: () -> Unit,
    userId: String,
    workspaces: List<Workspace>,
    isSavingWorkspace: Boolean = false,
    onAddWorkspace: (name: String, type: String) -> Unit = { _, _ -> },
    onUpdateWorkspace: (id: String, name: String, type: String) -> Unit = { _, _, _ -> },
    onDeleteWorkspace: (String) -> Unit = {},
    onOpenCategories: (Workspace) -> Unit = {},
    onOpenAccounts: (Workspace) -> Unit = {},
    onOpenReminders: (Workspace) -> Unit = {},
    onOpenBudgets: (Workspace) -> Unit = {},
    onOpenInventory: (Workspace) -> Unit = {},
    onOpenCommerce: (Workspace) -> Unit = {},
    onOpenProfitability: (Workspace) -> Unit = {},
    preferences: AppPreferences,
    preferencesStore: AppPreferencesStore,
    onPreferencesChanged: (AppPreferences) -> Unit,
    onSignOut: () -> Unit
) {
    Column(modifier = modifier.background(AmoledBlack)) {
        Row(Modifier.fillMaxWidth().height(64.dp).padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(GastifyIcons.back, contentDescription = "Volver", tint = Color.White) }
            Text(page ?: "Cuenta", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        }
        if (page == "Espacios") {
            WorkspacesScreen(
                modifier = Modifier.fillMaxSize(),
                workspaces = workspaces,
                isSaving = isSavingWorkspace,
                onAddWorkspace = onAddWorkspace,
                onUpdateWorkspace = onUpdateWorkspace,
                onDeleteWorkspace = onDeleteWorkspace,
                onOpenCategories = onOpenCategories,
                onOpenAccounts = onOpenAccounts,
                onOpenReminders = onOpenReminders,
                onOpenBudgets = onOpenBudgets,
                onOpenInventory = onOpenInventory,
                onOpenCommerce = onOpenCommerce,
                onOpenProfitability = onOpenProfitability
            )
        } else {
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 16.dp)) {
                when (page) {
                    "Perfil" -> ProfileSettingsSection(userId = userId)
                    "Notificaciones" -> NotificationSettingsSection(userId = userId)
                    "Preferencias" -> AppPreferencesSection(workspaces = workspaces, preferences = preferences, store = preferencesStore, onPreferencesChanged = onPreferencesChanged)
                    else -> {
                        Text("Configuración de la cuenta", color = TextSecondary, fontSize = 13.sp, modifier = Modifier.padding(bottom = 18.dp))
                        AccountSettingsRow("Espacios", "Configura tus espacios Personal y Negocio", GastifyIcons.workspaces) { onOpenPage("Espacios") }
                        Spacer(Modifier.height(10.dp))
                        AccountSettingsRow("Perfil", "Información de tu cuenta", Icons.Outlined.Person) { onOpenPage("Perfil") }
                        Spacer(Modifier.height(10.dp))
                        AccountSettingsRow("Notificaciones", "Administra tus avisos", Icons.Outlined.Notifications) { onOpenPage("Notificaciones") }
                        Spacer(Modifier.height(10.dp))
                        AccountSettingsRow("Preferencias", "Personaliza la aplicación", Icons.Outlined.Tune) { onOpenPage("Preferencias") }
                        Spacer(Modifier.height(24.dp))
                        OutlinedButton(onClick = onSignOut, modifier = Modifier.fillMaxWidth()) {
                            Icon(GastifyIcons.logout, contentDescription = null, tint = ExpenseRed)
                            Spacer(Modifier.width(8.dp))
                            Text("Cerrar sesión", color = ExpenseRed, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun NoActiveWorkspaceView(
    title: String,
    message: String,
    onCreateSpace: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize().background(AmoledBlack),
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
                modifier = Modifier.size(52.dp)
            )
            Spacer(Modifier.height(16.dp))
            Text(
                text = title,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = message,
                fontSize = 14.sp,
                color = TextSecondary,
                lineHeight = 20.sp,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            Spacer(Modifier.height(20.dp))
            Button(
                onClick = onCreateSpace,
                colors = ButtonDefaults.buttonColors(
                    containerColor = PrimaryGreen,
                    contentColor = AmoledBlack
                )
            ) {
                Icon(GastifyIcons.add, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Crear o configurar espacio", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun AccountSettingsRow(title: String, subtitle: String, icon: ImageVector, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Color.White.copy(alpha = 0.06f)).clickable(onClick = onClick).padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(44.dp).clip(RoundedCornerShape(13.dp)).background(PrimaryGreen.copy(alpha = 0.12f)), contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = null, tint = PrimaryGreen, modifier = Modifier.size(22.dp))
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(title, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(4.dp))
            Text(subtitle, color = TextSecondary, fontSize = 12.sp)
        }
        Icon(GastifyIcons.next, contentDescription = null, tint = TextSecondary)
    }
}