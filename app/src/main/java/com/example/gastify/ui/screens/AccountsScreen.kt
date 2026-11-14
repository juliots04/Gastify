package com.example.gastify.ui.screens

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Savings
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel

import com.example.gastify.data.model.Account
import com.example.gastify.data.model.AccountType
import com.example.gastify.data.model.Workspace
import com.example.gastify.data.repository.AccountRepository
import com.example.gastify.ui.components.GlassOptionsDivider
import com.example.gastify.ui.components.GlassOptionsItem
import com.example.gastify.ui.components.rememberSavedViewPreference
import com.example.gastify.ui.components.rememberPreferredAccount
import com.example.gastify.ui.components.GlassOptionsMenu
import com.example.gastify.ui.components.BalanceSummaryCard
import com.example.gastify.ui.components.GastifyIcons
import com.example.gastify.ui.theme.*
import com.example.gastify.viewmodel.AccountViewModel

import dev.chrisbanes.haze.HazeInput
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.blur.HazeBlurStyle
import dev.chrisbanes.haze.blur.HazeColorEffect
import dev.chrisbanes.haze.blur.hazeBlur
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState

import java.math.BigDecimal
import java.text.NumberFormat
import java.util.Locale

private fun accountTypeName(type: String): String {

    return when (type) {
        AccountType.CASH.name -> "Efectivo"
        AccountType.BANK.name -> "Banco"
        AccountType.WALLET.name -> "Billetera digital"
        AccountType.TRANSPORT.name -> "Transporte"
        else -> "Otra cuenta"
    }
}

private fun formatAccountMoney(cents: Long): String {

    val formatter = NumberFormat.getNumberInstance(
        Locale.forLanguageTag("es-PE")
    )

    formatter.minimumFractionDigits = 2
    formatter.maximumFractionDigits = 2

    return "S/ ${
        formatter.format(BigDecimal.valueOf(cents, 2))
    }"
}

private fun parseAccountMoney(text: String): Long? {

    val clean = text.trim()

    if (!Regex("""^\d{1,10}([.,]\d{1,2})?$""").matches(clean)) {
        return null
    }

    return try {

        val cents = BigDecimal(
            clean.replace(',', '.')
        ).movePointRight(2).longValueExact()

        cents.takeIf {
            it in 0L..AccountRepository.MAX_BALANCE_CENTS
        }

    } catch (_: Exception) {
        null
    }
}

@Composable
internal fun AccountsLightsBackground(modifier: Modifier = Modifier, animated: Boolean = true) {
    val movement = remember { androidx.compose.runtime.mutableFloatStateOf(0f) }
    LaunchedEffect(animated) {
        val start = android.os.SystemClock.elapsedRealtime()
        while (animated) {
            val elapsed = android.os.SystemClock.elapsedRealtime() - start
            movement.floatValue = kotlin.math.sin(elapsed * Math.PI / 9000.0).toFloat()
            kotlinx.coroutines.delay(125L)
        }
    }
    Canvas(modifier = modifier) {
        val lights = listOf(
            Triple(Color(0xFF22C99A), Offset(size.width * (0.12f + movement.floatValue * 0.12f), size.height * 0.16f), 0.9f),
            Triple(Color(0xFF8658F5), Offset(size.width * (0.9f - movement.floatValue * 0.12f), size.height * 0.37f), 0.85f),
            Triple(Color(0xFF168FCC), Offset(size.width * (0.15f - movement.floatValue * 0.1f), size.height * 0.65f), 0.9f),
            Triple(Color(0xFFD34890), Offset(size.width * (0.85f + movement.floatValue * 0.1f), size.height * 0.88f), 0.8f)
        )
        lights.forEach { (color, center, scale) ->
            val radius = size.width * scale
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(color.copy(alpha = 0.32f), color.copy(alpha = 0.1f), Color.Transparent),
                    center = center,
                    radius = radius
                ),
                radius = radius,
                center = center
            )
        }
    }
}
@Composable
fun AccountsScreen(
    userId: String,
    workspace: Workspace,
    onBack: () -> Unit,
    onOpenMovements: () -> Unit
) {

    val vm: AccountViewModel = viewModel(
        key = "accounts_${userId}_${workspace.id}",
        factory = AccountViewModel.factory(
            userId,
            workspace.id
        )
    )

    val state by vm.uiState.collectAsState()
    val (preferredAccountId, setPreferredAccount) = rememberPreferredAccount(userId, workspace.id)

    DisposableEffect(vm) {

        vm.startListening()

        onDispose {
            vm.stopListening()
        }
    }

    val (balancesVisible, setBalancesVisible) = rememberSavedViewPreference(userId, "account_balances_visible", true)

    var showCreate by rememberSaveable {
        mutableStateOf(false)
    }

    var accountName by rememberSaveable {
        mutableStateOf("")
    }

    var accountType by rememberSaveable {
        mutableStateOf(AccountType.CASH.name)
    }

    var balanceText by rememberSaveable {
        mutableStateOf("0")
    }

    var toEditId by rememberSaveable {
        mutableStateOf<String?>(null)
    }

    var editName by rememberSaveable {
        mutableStateOf("")
    }

    var toDeleteId by rememberSaveable {
        mutableStateOf<String?>(null)
    }

    val snackbar = remember {
        SnackbarHostState()
    }

    LaunchedEffect(state.errorMessage) {

        state.errorMessage?.let {

            snackbar.showSnackbar(it)

            vm.clearError()
        }
    }

    val total = state.accounts.sumOf {
        it.balanceCents
    }

    val glassState = rememberHazeState()
    val backgroundGlassState = rememberHazeState()
    val statusBarHeight = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val listState = rememberLazyListState()
    val fadeDistance = with(LocalDensity.current) { 100.dp.toPx() }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AmoledBlack)
    ) {
        Box(Modifier.fillMaxSize().hazeSource(glassState)) {
        Box(Modifier.fillMaxSize().hazeSource(backgroundGlassState).background(AmoledBlack)) {
            AccountsLightsBackground(Modifier.fillMaxSize(), animated = false)
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
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            item {
                BalanceSummaryCard(
                    title = "Balance de tus cuentas",
                    amount = if (balancesVisible) formatAccountMoney(total) else "S/ ••••••",
                    backgroundState = backgroundGlassState
                ) {
                    BoxWithConstraints(Modifier.fillMaxWidth()) {
                        val cellWidth = maxWidth / 3
                        Row(
                            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.width(cellWidth).padding(end = 8.dp)) {
                                Text("Cuentas", fontSize = 12.sp, color = TextSecondary, maxLines = 1)
                                Text("${state.accounts.size}", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                            }
                            AccountType.entries.forEach { type ->
                                val count = state.accounts.count { it.type == type.name }
                                if (count > 0) {
                                    VerticalDivider(modifier = Modifier.height(36.dp), color = BorderDark)
                                    Column(modifier = Modifier.width(cellWidth - 1.dp).padding(horizontal = 10.dp)) {
                                        Text(accountTypeName(type.name), fontSize = 12.sp, color = TextSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        Text("$count", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            item {

                Spacer(Modifier.height(20.dp))
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Cuentas registradas",
                        modifier = Modifier.weight(1f),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    IconButton(onClick = { setBalancesVisible(!balancesVisible) }) {
                        Icon(
                            imageVector = if (balancesVisible) Icons.Outlined.Visibility else Icons.Outlined.VisibilityOff,
                            contentDescription = if (balancesVisible) "Ocultar saldos" else "Mostrar saldos",
                            tint = TextSecondary,
                            modifier = Modifier.size(23.dp)
                        )
                    }
                }
            }

            if (state.isLoading) {

                item {

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {

                        CircularProgressIndicator(
                            color = PrimaryGreen
                        )
                    }
                }

            } else if (state.accounts.isEmpty()) {

                item {

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
                                text = "Todavía no tienes cuentas",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.SemiBold
                            )

                            Spacer(Modifier.height(10.dp))

                            Text(
                                text = "Agrega efectivo, bancos, billeteras o tarjetas de transporte.",
                                color = TextSecondary,
                                fontSize = 13.sp
                            )
                        }
                    }
                }

            } else {

                items(
                    items = state.accounts,
                    key = { it.id }
                ) { account ->

                    AccountCardVector(
                        account = account,
                        glassState = glassState,
                        balancesVisible = balancesVisible,
                        isPreferred = account.id == preferredAccountId,
                        onTogglePreferred = { setPreferredAccount(if (account.id == preferredAccountId) null else account.id) },
                        onMovements = onOpenMovements,
                        isSaving = state.isSaving,
                        onEdit = {

                            toEditId = account.id
                            editName = account.name
                        },
                        onDelete = {
                            toDeleteId = account.id
                        }
                    )
                }
            }
        }

        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(statusBarHeight + 64.dp)
                .drawBehind {
                    val alpha = if (listState.firstVisibleItemIndex > 0) 1f else
                        (listState.firstVisibleItemScrollOffset / fadeDistance).coerceIn(0f, 1f)
                    drawRect(AmoledBlack.copy(alpha = alpha))
                }
                .padding(start = 12.dp, end = 12.dp, top = statusBarHeight),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack, modifier = Modifier.size(48.dp)) {
                Icon(
                    GastifyIcons.back,
                    contentDescription = "Volver",
                    tint = Color.White,
                    modifier = Modifier.size(30.dp)
                )
            }
            Column(modifier = Modifier.weight(1f).padding(start = 4.dp, end = 8.dp)) {
                Text(
                    text = "Mis cuentas",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = workspace.name,
                    fontSize = 13.sp,
                    color = Color.White.copy(alpha = 0.8f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Button(
                onClick = {
                    accountName = ""
                    accountType = AccountType.CASH.name
                    balanceText = "0"
                    showCreate = true
                },
                enabled = !state.isSaving,
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White.copy(alpha = 0.16f),
                    contentColor = Color.White,
                    disabledContainerColor = Color.White.copy(alpha = 0.08f),
                    disabledContentColor = Color.White.copy(alpha = 0.4f)
                ),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp)
            ) {
                Icon(GastifyIcons.add, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(5.dp))
                Text("Agregar cuenta", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
            }
        }
        SnackbarHost(
            hostState = snackbar,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }

    // CREAR CUENTA

    if (showCreate) {

        val cleanName = accountName.trim()

        val balance = parseAccountMoney(balanceText)

        AlertDialog(
            onDismissRequest = {

                if (!state.isSaving) {
                    showCreate = false
                }
            },
            containerColor = SurfaceDark,
            title = {
                Text("Nueva cuenta")
            },
            text = {

                Column(
                    modifier = Modifier
                        .heightIn(max = 440.dp)
                        .verticalScroll(rememberScrollState())
                ) {

                    OutlinedTextField(
                        value = accountName,
                        onValueChange = {
                            accountName = it.take(60)
                        },
                        label = {
                            Text("Nombre")
                        },
                        placeholder = {
                            Text("Ejemplo: Tarjeta Morado")
                        },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(Modifier.height(18.dp))

                    Text(
                        text = "Tipo de cuenta",
                        fontWeight = FontWeight.Medium
                    )

                    Spacer(Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.horizontalScroll(
                            rememberScrollState()
                        ),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {

                        listOf(AccountType.CASH, AccountType.TRANSPORT, AccountType.WALLET, AccountType.OTHER).forEach { type ->

                            FilterChip(
                                selected = accountType == type.name,
                                onClick = {
                                    accountType = type.name
                                },
                                leadingIcon = {

                                    Icon(
                                        imageVector = GastifyIcons.account(
                                            type.name
                                        ),
                                        contentDescription = null,
                                        modifier = Modifier.size(20.dp)
                                    )
                                },
                                label = {
                                    Text(accountTypeName(type.name))
                                }
                            )
                        }
                    }

                    Spacer(Modifier.height(18.dp))

                    OutlinedTextField(
                        value = balanceText,
                        onValueChange = {
                            balanceText = it.take(16)
                        },
                        label = {
                            Text("Saldo inicial (S/)")
                        },
                        placeholder = {
                            Text("Ejemplo: 30.00")
                        },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Decimal
                        ),
                        isError = balance == null,
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (balance == null) {

                        Spacer(Modifier.height(6.dp))

                        Text(
                            text = "Introduce un importe válido.",
                            color = ExpenseRed,
                            fontSize = 12.sp
                        )
                    }

                    Spacer(Modifier.height(12.dp))

                    Text(
                        text = if (accountType == "TRANSPORT") {
                            "Este será el saldo estimado de tu tarjeta de transporte."
                        } else {
                            "Introduce el dinero disponible actualmente en esta cuenta."
                        },
                        fontSize = 12.sp,
                        lineHeight = 18.sp,
                        color = TextSecondary
                    )
                }
            },
            confirmButton = {

                TextButton(
                    enabled = cleanName.isNotEmpty() &&
                        balance != null &&
                        !state.isSaving,
                    onClick = {

                        if (balance != null) {

                            vm.createAccount(
                                cleanName,
                                accountType,
                                balance
                            )

                            showCreate = false
                        }
                    }
                ) {

                    Text(
                        text = "Crear",
                        color = PrimaryGreen
                    )
                }
            },
            dismissButton = {

                TextButton(
                    onClick = {
                        showCreate = false
                    },
                    enabled = !state.isSaving
                ) {

                    Text(
                        text = "Cancelar",
                        color = TextSecondary
                    )
                }
            }
        )
    }

    // EDITAR CUENTA

    val editAccount = state.accounts.firstOrNull {
        it.id == toEditId
    }

    if (editAccount != null) {

        AlertDialog(
            onDismissRequest = {
                toEditId = null
            },
            containerColor = SurfaceDark,
            title = {
                Text("Editar cuenta")
            },
            text = {

                OutlinedTextField(
                    value = editName,
                    onValueChange = {
                        editName = it.take(60)
                    },
                    label = {
                        Text("Nombre")
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {

                TextButton(
                    enabled = editName.trim().isNotEmpty() &&
                        !state.isSaving,
                    onClick = {

                        vm.renameAccount(
                            editAccount.id,
                            editName.trim()
                        )

                        toEditId = null
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
                        toEditId = null
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

    // ELIMINAR CUENTA

    val deleteAccount = state.accounts.firstOrNull {
        it.id == toDeleteId
    }

    if (deleteAccount != null) {

        AlertDialog(
            onDismissRequest = {
                toDeleteId = null
            },
            containerColor = SurfaceDark,
            title = {
                Text("Eliminar cuenta")
            },
            text = {

                Text(
                    text = "¿Seguro que deseas eliminar \"${deleteAccount.name}\"?\n\nLa eliminación no transfiere dinero a otra cuenta. Las cuentas con movimientos no pueden eliminarse.",
                    color = TextSecondary,
                    lineHeight = 21.sp
                )
            },
            confirmButton = {

                TextButton(
                    enabled = !state.isSaving,
                    onClick = {

                        vm.deleteAccount(deleteAccount.id)

                        toDeleteId = null
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
                        toDeleteId = null
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
}

@Composable
private fun AccountCardVector(
    account: Account,
    glassState: HazeState,
    balancesVisible: Boolean,
    isPreferred: Boolean,
    onTogglePreferred: () -> Unit,
    isSaving: Boolean,
    onMovements: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    if (account.type == AccountType.TRANSPORT.name) {
        TransportAccountCard(account, glassState, balancesVisible, isPreferred, onTogglePreferred, isSaving, onMovements, onEdit, onDelete)
    } else if (account.type == AccountType.WALLET.name) {
        WalletAccountCard(account, glassState, balancesVisible, isPreferred, onTogglePreferred, isSaving, onMovements, onEdit, onDelete)
    } else {
        CashAccountPanel(account, glassState, balancesVisible, isPreferred, onTogglePreferred, isSaving, onMovements, onEdit, onDelete)
    }
}
@Composable
private fun TransportAccountCard(
    account: Account,
    glassState: HazeState,
    balancesVisible: Boolean,
    isPreferred: Boolean,
    onTogglePreferred: () -> Unit,
    isSaving: Boolean,
    onMovements: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }
    val whiteMuted = Color.White.copy(alpha = 0.72f)

    Card(
        modifier = Modifier.fillMaxWidth().aspectRatio(1.586f),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1A2450))
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        listOf(Color(0xFF263A73), Color(0xFF252353), Color(0xFF143C46))
                    )
                )
        ) {
            Canvas(Modifier.matchParentSize()) {
                drawCircle(
                    color = Color.White.copy(alpha = 0.045f),
                    radius = size.width * 0.42f,
                    center = Offset(size.width * 0.94f, size.height * 0.8f)
                )
                drawCircle(
                    color = Color.White.copy(alpha = 0.035f),
                    radius = size.width * 0.31f,
                    center = Offset(size.width * 0.98f, size.height * 0.82f)
                )
            }
            Column(Modifier.padding(start = 20.dp, end = 4.dp, top = 12.dp, bottom = 20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        color = Color.White.copy(alpha = 0.14f),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Box(Modifier.size(48.dp), contentAlignment = Alignment.Center) {
                            Icon(
                                GastifyIcons.account(AccountType.TRANSPORT.name),
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            account.name,
                            color = Color.White,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text("Transporte", color = whiteMuted, fontSize = 12.sp)
                    }
                    PreferredAccountStar(isPreferred, isSaving, onTogglePreferred)
                    Box {
                        IconButton(onClick = { menuExpanded = true }, enabled = !isSaving, modifier = Modifier.size(40.dp)) {
                            Icon(Icons.Outlined.MoreVert, contentDescription = "Más opciones", tint = Color.White)
                        }
                        GlassOptionsMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }, glassState = glassState) {
                            GlassOptionsItem("Ver movimientos", GastifyIcons.movements, onClick = { menuExpanded = false; onMovements() })
                            GlassOptionsDivider()
                            GlassOptionsItem(
                                label = "Editar cuenta",
                                icon = GastifyIcons.edit,
                                onClick = { menuExpanded = false; onEdit() }
                            )
                            GlassOptionsDivider()
                            GlassOptionsItem(
                                label = "Eliminar cuenta",
                                icon = GastifyIcons.delete,
                                destructive = true,
                                onClick = { menuExpanded = false; onDelete() }
                            )
                        }
                    }
                }
                Spacer(Modifier.height(24.dp))
                Text("Saldo estimado", fontSize = 12.sp, color = whiteMuted)
                Spacer(Modifier.height(4.dp))
                Text(
                    if (balancesVisible) formatAccountMoney(account.balanceCents) else "S/ ••••••",
                    fontSize = 29.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    "Este saldo es estimado. No consulta directamente la tarjeta física.",
                    fontSize = 12.sp,
                    lineHeight = 18.sp,
                    color = whiteMuted
                )
            }
        }
    }
}
@Composable
private fun CashAccountPanel(
    account: Account,
    glassState: HazeState,
    balancesVisible: Boolean,
    isPreferred: Boolean,
    onTogglePreferred: () -> Unit,
    isSaving: Boolean,
    onMovements: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }
    val isWallet = account.type == AccountType.WALLET.name
    val isCash = account.type == AccountType.CASH.name
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1.68f)
            .clip(RoundedCornerShape(12.dp))
            .background(Brush.linearGradient(if (isWallet) listOf(Color(0xFF6B2CC1), Color(0xFF55249B), Color(0xFF303985)) else if (isCash) listOf(Color(0xFF1D5744), Color(0xFF164438), Color(0xFF123A31)) else listOf(Color(0xFF263441), Color(0xFF18232B))))
            .drawBehind {
                val line = Color(0xFF8EE8B5).copy(alpha = 0.13f)
                val coinRadius = size.minDimension * 0.22f
                val coinCenter = Offset(size.width * 0.82f, size.height * 0.74f)
                drawCircle(line, coinRadius, coinCenter, style = Stroke(width = 2.dp.toPx()))
                drawCircle(line, coinRadius * 0.78f, coinCenter, style = Stroke(width = 1.dp.toPx()))
                drawCircle(line.copy(alpha = 0.07f), coinRadius * 0.75f, Offset(size.width * 0.96f, size.height * 0.47f), style = Stroke(width = 2.dp.toPx()))
            }
            .padding(start = 20.dp, end = 4.dp, top = 10.dp, bottom = 12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Surface(color = PrimaryGreen.copy(alpha = 0.14f), shape = RoundedCornerShape(50)) {
                Box(Modifier.size(48.dp), contentAlignment = Alignment.Center) {
                    Icon(
                        GastifyIcons.account(account.type),
                        contentDescription = null,
                        tint = if (isCash) PrimaryGreen else Color.White,
                        modifier = Modifier.size(25.dp)
                    )
                }
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(account.name, color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(accountTypeName(account.type), color = Color.White.copy(alpha = 0.65f), fontSize = 12.sp)
            }
            PreferredAccountStar(isPreferred, isSaving, onTogglePreferred)
            Box {
                IconButton(onClick = { menuExpanded = true }, enabled = !isSaving, modifier = Modifier.size(40.dp)) {
                    Icon(Icons.Outlined.MoreVert, contentDescription = "Más opciones", tint = Color.White)
                }
                GlassOptionsMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }, glassState = glassState) {
                    GlassOptionsItem("Ver movimientos", GastifyIcons.movements, onClick = { menuExpanded = false; onMovements() })
                    GlassOptionsDivider()
                    GlassOptionsItem("Editar cuenta", GastifyIcons.edit, onClick = { menuExpanded = false; onEdit() })
                    GlassOptionsDivider()
                    GlassOptionsItem("Eliminar cuenta", GastifyIcons.delete, onClick = { menuExpanded = false; onDelete() }, destructive = true)
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        Text("Saldo disponible", color = Color.White.copy(alpha = 0.65f), fontSize = 12.sp)
        Spacer(Modifier.height(4.dp))
        Text(if (balancesVisible) formatAccountMoney(account.balanceCents) else "S/ ••••••", color = Color.White, fontSize = 29.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(4.dp))
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("Este saldo es estimado. No se sincroniza con tu cuenta.", modifier = Modifier.weight(1f), color = Color.White.copy(alpha = 0.65f), fontSize = 11.sp, lineHeight = 14.sp, maxLines = 2)
            if (isCash) Icon(Icons.Outlined.Savings, contentDescription = null, tint = Color.White.copy(alpha = 0.25f), modifier = Modifier.size(26.dp))
        }
    }
}
@Composable
private fun WalletAccountCard(
    account: Account,
    glassState: HazeState,
    balancesVisible: Boolean,
    isPreferred: Boolean,
    onTogglePreferred: () -> Unit,
    isSaving: Boolean,
    onMovements: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }
    Box(
        modifier = Modifier.fillMaxWidth().aspectRatio(1.68f)
            .clip(RoundedCornerShape(12.dp))
            .background(Brush.linearGradient(listOf(Color(0xFF742396), Color(0xFF531377), Color(0xFF3D145F))))
    ) {
        Canvas(Modifier.matchParentSize()) {
            val accent = Color(0xFFE9B5FA).copy(alpha = 0.11f)
            val stroke = 20.dp.toPx()
            drawLine(accent, Offset(size.width * 0.65f, size.height * 1.1f), Offset(size.width * 1.1f, size.height * 0.15f), strokeWidth = stroke)
            drawLine(accent.copy(alpha = 0.06f), Offset(size.width * 0.8f, size.height * 1.1f), Offset(size.width * 1.2f, size.height * 0.3f), strokeWidth = stroke * 1.6f)
        }
        Column(Modifier.fillMaxSize().padding(start = 20.dp, end = 4.dp, top = 12.dp, bottom = 14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(color = Color.White.copy(alpha = 0.14f), shape = RoundedCornerShape(14.dp)) {
                    Box(Modifier.size(48.dp), contentAlignment = Alignment.Center) {
                        Icon(GastifyIcons.account(AccountType.WALLET.name), contentDescription = null, tint = Color.White, modifier = Modifier.size(26.dp))
                    }
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(account.name, color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text("Billetera digital", color = Color.White.copy(alpha = 0.72f), fontSize = 12.sp)
                }
                PreferredAccountStar(isPreferred, isSaving, onTogglePreferred)
                Box {
                    IconButton(onClick = { menuExpanded = true }, enabled = !isSaving, modifier = Modifier.size(40.dp)) {
                        Icon(Icons.Outlined.MoreVert, contentDescription = "Más opciones", tint = Color.White)
                    }
                    GlassOptionsMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }, glassState = glassState) {
                        GlassOptionsItem("Ver movimientos", GastifyIcons.movements, onClick = { menuExpanded = false; onMovements() })
                        GlassOptionsDivider()
                        GlassOptionsItem("Editar cuenta", GastifyIcons.edit, onClick = { menuExpanded = false; onEdit() })
                        GlassOptionsDivider()
                        GlassOptionsItem("Eliminar cuenta", GastifyIcons.delete, onClick = { menuExpanded = false; onDelete() }, destructive = true)
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            Text("Saldo estimado", color = Color.White.copy(alpha = 0.72f), fontSize = 12.sp)
            Spacer(Modifier.height(4.dp))
            Text(if (balancesVisible) formatAccountMoney(account.balanceCents) else "S/ ••••••", color = Color.White, fontSize = 29.sp, fontWeight = FontWeight.Bold, maxLines = 1)
            Spacer(Modifier.height(8.dp))
            Text("Este saldo es estimado. No se sincroniza con tu billetera.", color = Color.White.copy(alpha = 0.72f), fontSize = 12.sp, lineHeight = 16.sp, maxLines = 2)
        }
    }
}
@Composable
private fun PreferredAccountStar(isPreferred: Boolean, isSaving: Boolean, onClick: () -> Unit) {
    IconButton(onClick = onClick, enabled = !isSaving, modifier = Modifier.size(36.dp)) {
        Icon(
            imageVector = if (isPreferred) Icons.Filled.Star else Icons.Outlined.StarOutline,
            contentDescription = if (isPreferred) "Quitar cuenta preferida" else "Marcar como cuenta preferida",
            tint = if (isPreferred) Color(0xFFFFD66B) else Color.White,
            modifier = Modifier.size(23.dp)
        )
    }
}