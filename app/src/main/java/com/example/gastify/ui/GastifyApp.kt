package com.example.gastify.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll

import androidx.compose.material3.*

import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable

import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

import androidx.lifecycle.viewmodel.compose.viewModel

import com.example.gastify.data.model.Workspace

import com.example.gastify.ui.components.GastifyIcons

import com.example.gastify.ui.screens.AccountsScreen
import com.example.gastify.ui.screens.CategoriesScreen
import com.example.gastify.ui.screens.GastifyHomeScreen
import com.example.gastify.ui.screens.MovementsScreen
import com.example.gastify.ui.screens.RemindersScreen
import com.example.gastify.ui.screens.WorkspacesScreen

import com.example.gastify.ui.theme.*

import com.example.gastify.viewmodel.WorkspaceViewModel

import com.google.firebase.auth.FirebaseAuth

// ==========================================
// NAVEGACIÓN PRINCIPAL
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
// PANTALLAS DE ESPACIOS
// ==========================================

private enum class WorkspaceSection {

    CATEGORIES,

    ACCOUNTS,

    REMINDERS
}

// ==========================================
// APLICACIÓN PRINCIPAL
// ==========================================

@Composable
fun GastifyApp(
    onSignOut: () -> Unit
) {

    val userId = FirebaseAuth
        .getInstance()
        .currentUser
        ?.uid
        ?: return

    // ======================================
    // VIEWMODEL DE ESPACIOS
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
    // NAVEGACIÓN
    // ======================================

    var selectedSection by rememberSaveable {
        mutableStateOf(MainSection.HOME.name)
    }

    var selectedWorkspaceId by rememberSaveable {
        mutableStateOf<String?>(null)
    }

    var selectedWorkspaceSection by rememberSaveable {
        mutableStateOf(
            WorkspaceSection.CATEGORIES.name
        )
    }

    var selectedMovementWorkspaceId by rememberSaveable {
        mutableStateOf<String?>(null)
    }

    val currentSection = MainSection.entries.firstOrNull {
        it.name == selectedSection
    } ?: MainSection.HOME

    val openedWorkspace = workspaceState.workspaces.firstOrNull {
        it.id == selectedWorkspaceId
    }

    val openedMovementWorkspace = workspaceState.workspaces.firstOrNull {
        it.id == selectedMovementWorkspaceId
    }

    val currentWorkspaceSection =
        WorkspaceSection.entries.firstOrNull {

            it.name == selectedWorkspaceSection

        } ?: WorkspaceSection.CATEGORIES

    // ======================================
    // VALIDAR ESPACIOS
    // ======================================

    LaunchedEffect(
        selectedWorkspaceId,
        selectedMovementWorkspaceId,
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
        }
    }

    // ======================================
    // MENSAJES
    // ======================================

    val snackbar = remember {
        SnackbarHostState()
    }

    LaunchedEffect(workspaceState.errorMessage) {

        workspaceState.errorMessage?.let {

            snackbar.showSnackbar(it)

            workspaceVm.clearError()
        }
    }

    val workspaceNames = workspaceState.workspaces.map {
        it.name
    }

    val inSubscreen =
        openedWorkspace != null ||
            openedMovementWorkspace != null

    // ======================================
    // INTERFAZ
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
                        color = BorderDark,
                        thickness = 1.dp
                    )

                    NavigationBar(
                        containerColor = AmoledBlack,
                        tonalElevation = 0.dp
                    ) {

                        MainSection.entries.forEach { section ->

                            NavigationBarItem(
                                selected = currentSection == section,

                                onClick = {

                                    selectedSection = section.name
                                },

                                icon = {

                                    Icon(
                                        imageVector = section.icon,

                                        contentDescription = section.title,

                                        modifier = Modifier.size(24.dp)
                                    )
                                },

                                label = {

                                    Text(
                                        text = section.title,
                                        fontSize = 10.sp
                                    )
                                },

                                alwaysShowLabel = true,

                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = PrimaryGreen,

                                    selectedTextColor = PrimaryGreen,

                                    indicatorColor = SurfaceDark,

                                    unselectedIconColor = TextSecondary,

                                    unselectedTextColor = TextSecondary
                                )
                            )
                        }
                    }
                }
            }
        }

    ) { innerPadding ->

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {

            // ==================================
            // SUBPANTALLAS DEL ESPACIO
            // ==================================

            if (openedWorkspace != null) {

                when (currentWorkspaceSection) {

                    // CATEGORÍAS

                    WorkspaceSection.CATEGORIES -> {

                        CategoriesScreen(
                            userId = userId,

                            workspace = openedWorkspace,

                            onBack = {

                                selectedWorkspaceId = null
                            }
                        )
                    }

                    // CUENTAS

                    WorkspaceSection.ACCOUNTS -> {

                        AccountsScreen(
                            userId = userId,

                            workspace = openedWorkspace,

                            onBack = {

                                selectedWorkspaceId = null
                            }
                        )
                    }

                    // RECORDATORIOS

                    WorkspaceSection.REMINDERS -> {

                        RemindersScreen(
                            userId = userId,

                            workspace = openedWorkspace,

                            onBack = {

                                selectedWorkspaceId = null
                            }
                        )
                    }
                }

            } else if (openedMovementWorkspace != null) {

                // ==================================
                // MOVIMIENTOS
                // ==================================

                MovementsScreen(
                    userId = userId,

                    workspace = openedMovementWorkspace,

                    onBack = {

                        selectedMovementWorkspaceId = null
                    }
                )

            } else {

                // ==================================
                // NAVEGACIÓN PRINCIPAL
                // ==================================

                when (currentSection) {

                    // INICIO

                    MainSection.HOME -> {

                        GastifyHomeScreen(
                            modifier = Modifier.fillMaxSize(),

                            workspaces = workspaceNames
                        )
                    }

                    // MOVIMIENTOS

                    MainSection.MOVEMENTS -> {

                        MovementWorkspacePicker(
                            modifier = Modifier.fillMaxSize(),

                            workspaces = workspaceState.workspaces,

                            onOpenWorkspace = { workspace ->

                                selectedMovementWorkspaceId =
                                    workspace.id
                            }
                        )
                    }

                    // ESPACIOS

                    MainSection.WORKSPACES -> {

                        WorkspacesScreen(
                            modifier = Modifier.fillMaxSize(),

                            workspaces = workspaceState.workspaces,

                            isSaving = workspaceState.isSaving,

                            onAddWorkspace = { name ->

                                workspaceVm.createWorkspace(
                                    name
                                )
                            },

                            onDeleteWorkspace = { name ->

                                workspaceVm.deleteWorkspace(
                                    name
                                )
                            },

                            onOpenCategories = { workspace ->

                                selectedWorkspaceSection =
                                    WorkspaceSection.CATEGORIES.name

                                selectedWorkspaceId = workspace.id
                            },

                            onOpenAccounts = { workspace ->

                                selectedWorkspaceSection =
                                    WorkspaceSection.ACCOUNTS.name

                                selectedWorkspaceId = workspace.id
                            },

                            onOpenReminders = { workspace ->

                                selectedWorkspaceSection =
                                    WorkspaceSection.REMINDERS.name

                                selectedWorkspaceId = workspace.id
                            }
                        )
                    }

                    // ESTADÍSTICAS

                    MainSection.STATISTICS -> {

                        SectionPlaceholder(
                            modifier = Modifier.fillMaxSize(),

                            title = "Estadísticas",

                            subtitle = "Aquí visualizarás gráficos, reportes y análisis financieros.",

                            icon = GastifyIcons.statistics
                        )
                    }

                    // CONFIGURACIÓN

                    MainSection.SETTINGS -> {

                        SettingsScreen(
                            modifier = Modifier.fillMaxSize(),

                            onSignOut = onSignOut
                        )
                    }
                }
            }

            // ==================================
            // CARGA INICIAL
            // ==================================

            if (workspaceState.isLoading) {

                Box(
                    modifier = Modifier.fillMaxSize(),

                    contentAlignment = Alignment.Center
                ) {

                    CircularProgressIndicator(
                        color = PrimaryGreen
                    )
                }
            }
        }
    }
}

// ==========================================
// SELECTOR DE ESPACIOS PARA MOVIMIENTOS
// ==========================================

@Composable
private fun MovementWorkspacePicker(
    modifier: Modifier = Modifier,
    workspaces: List<Workspace>,
    onOpenWorkspace: (Workspace) -> Unit
) {

    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(20.dp)
    ) {

        Spacer(Modifier.height(16.dp))

        Text(
            text = "Movimientos",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(Modifier.height(8.dp))

        Text(
            text = "Selecciona un espacio para consultar o registrar movimientos.",
            fontSize = 14.sp,
            lineHeight = 21.sp,
            color = TextSecondary
        )

        Spacer(Modifier.height(30.dp))

        Text(
            text = "Mis espacios",
            fontSize = 20.sp,
            fontWeight = FontWeight.SemiBold
        )

        Spacer(Modifier.height(18.dp))

        if (workspaces.isEmpty()) {

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
                        text = "Todavía no tienes espacios",
                        fontWeight = FontWeight.SemiBold
                    )

                    Spacer(Modifier.height(10.dp))

                    Text(
                        text = "Crea un espacio desde la pestaña Espacios.",
                        color = TextSecondary,
                        fontSize = 13.sp
                    )
                }
            }

        } else {

            workspaces.forEach { workspace ->

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),

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

                            Icon(
                                imageVector = GastifyIcons.workspaces,
                                contentDescription = null,
                                tint = PrimaryGreen,
                                modifier = Modifier.size(24.dp)
                            )

                            Spacer(Modifier.width(10.dp))

                            Text(
                                text = workspace.name,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        Spacer(Modifier.height(8.dp))

                        Text(
                            text = "Consultar ingresos y gastos",
                            fontSize = 13.sp,
                            color = TextSecondary
                        )

                        Spacer(Modifier.height(18.dp))

                        Button(
                            onClick = {
                                onOpenWorkspace(workspace)
                            },

                            modifier = Modifier.fillMaxWidth(),

                            shape = RoundedCornerShape(12.dp),

                            colors = ButtonDefaults.buttonColors(
                                containerColor = CardDark,
                                contentColor = PrimaryGreen
                            )
                        ) {

                            Icon(
                                imageVector = GastifyIcons.movements,
                                contentDescription = null,
                                modifier = Modifier.size(19.dp)
                            )

                            Spacer(Modifier.width(8.dp))

                            Text(
                                text = "Ver movimientos",
                                modifier = Modifier.weight(1f),
                                fontWeight = FontWeight.Medium
                            )

                            Icon(
                                imageVector = GastifyIcons.next,
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
// PANTALLAS PENDIENTES
// ==========================================

@Composable
private fun SectionPlaceholder(
    modifier: Modifier = Modifier,
    title: String,
    subtitle: String,
    icon: ImageVector
) {

    Column(
        modifier = modifier.padding(28.dp),

        verticalArrangement = Arrangement.Center,

        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Surface(
            color = SurfaceDark,
            shape = RoundedCornerShape(28.dp)
        ) {

            Box(
                modifier = Modifier.size(92.dp),

                contentAlignment = Alignment.Center
            ) {

                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = PrimaryGreen,
                    modifier = Modifier.size(42.dp)
                )
            }
        }

        Spacer(Modifier.height(24.dp))

        Text(
            text = title,
            fontSize = 25.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(Modifier.height(12.dp))

        Text(
            text = subtitle,
            fontSize = 14.sp,
            color = TextSecondary,
            textAlign = TextAlign.Center,
            lineHeight = 22.sp
        )

        Spacer(Modifier.height(20.dp))

        Text(
            text = "Próximamente",
            fontSize = 12.sp,
            color = PrimaryGreen
        )
    }
}

// ==========================================
// CONFIGURACIÓN
// ==========================================

@Composable
private fun SettingsScreen(
    modifier: Modifier = Modifier,
    onSignOut: () -> Unit
) {

    val user = FirebaseAuth
        .getInstance()
        .currentUser

    Column(
        modifier = modifier.padding(24.dp)
    ) {

        Spacer(Modifier.height(16.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {

            Icon(
                imageVector = GastifyIcons.settings,
                contentDescription = null,
                tint = PrimaryGreen,
                modifier = Modifier.size(27.dp)
            )

            Spacer(Modifier.width(10.dp))

            Text(
                text = "Configuración",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(Modifier.height(30.dp))

        Text(
            text = "Mi cuenta",
            fontSize = 20.sp,
            fontWeight = FontWeight.SemiBold
        )

        Spacer(Modifier.height(20.dp))

        Text(
            text = user?.displayName ?: "Usuario",
            fontSize = 17.sp,
            fontWeight = FontWeight.Medium
        )

        Spacer(Modifier.height(8.dp))

        Text(
            text = user?.email ?: "",
            fontSize = 13.sp,
            color = TextSecondary
        )

        Spacer(Modifier.height(32.dp))

        HorizontalDivider(
            color = BorderDark
        )

        Spacer(Modifier.height(20.dp))

        OutlinedButton(
            onClick = onSignOut,
            modifier = Modifier.fillMaxWidth()
        ) {

            Icon(
                imageVector = GastifyIcons.logout,
                contentDescription = null,
                tint = ExpenseRed
            )

            Spacer(Modifier.width(8.dp))

            Text(
                text = "Cerrar sesión",
                fontWeight = FontWeight.SemiBold,
                color = ExpenseRed
            )
        }
    }
}
