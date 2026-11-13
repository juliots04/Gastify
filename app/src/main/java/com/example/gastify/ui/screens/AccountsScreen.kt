package com.example.gastify.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import com.example.gastify.ui.components.GastifyIcons
import com.example.gastify.ui.theme.*
import com.example.gastify.viewmodel.AccountViewModel

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
fun AccountsScreen(
    userId: String,
    workspace: Workspace,
    onBack: () -> Unit
) {

    val vm: AccountViewModel = viewModel(
        key = "accounts_${userId}_${workspace.id}",
        factory = AccountViewModel.factory(
            userId,
            workspace.id
        )
    )

    val state by vm.uiState.collectAsState()

    DisposableEffect(vm) {

        vm.startListening()

        onDispose {
            vm.stopListening()
        }
    }

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

    val cards = state.accounts.count {
        it.type == AccountType.TRANSPORT.name
    }

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
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {

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

                Text(
                    text = "Mis cuentas",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )

                Spacer(Modifier.height(6.dp))

                Text(
                    text = workspace.name,
                    fontSize = 14.sp,
                    color = TextSecondary
                )

                Spacer(Modifier.height(26.dp))
            }

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
                                tint = PrimaryGreen
                            )

                            Spacer(Modifier.width(10.dp))

                            Text(
                                text = "Balance de tus cuentas",
                                fontSize = 14.sp,
                                color = TextSecondary
                            )
                        }

                        Spacer(Modifier.height(12.dp))

                        Text(
                            text = formatAccountMoney(total),
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(Modifier.height(16.dp))

                        HorizontalDivider(
                            color = BorderDark
                        )

                        Spacer(Modifier.height(16.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {

                            Column {

                                Text(
                                    text = "Cuentas",
                                    fontSize = 12.sp,
                                    color = TextSecondary
                                )

                                Text(
                                    text = "${state.accounts.size}",
                                    fontSize = 19.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Column {

                                Text(
                                    text = "Tarjetas de transporte",
                                    fontSize = 12.sp,
                                    color = TextSecondary
                                )

                                Text(
                                    text = "$cards",
                                    fontSize = 19.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = PrimaryGreen
                                )
                            }
                        }
                    }
                }
            }

            item {

                Spacer(Modifier.height(8.dp))

                Button(
                    onClick = {

                        accountName = ""
                        accountType = AccountType.CASH.name
                        balanceText = "0"
                        showCreate = true
                    },
                    enabled = !state.isSaving,
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
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )

                    Spacer(Modifier.width(8.dp))

                    Text(
                        text = "Agregar cuenta",
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(Modifier.height(22.dp))

                Text(
                    text = "Cuentas registradas",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold
                )
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

                        AccountType.entries.forEach { type ->

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
    isSaving: Boolean,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {

    val isTransport = account.type == AccountType.TRANSPORT.name

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
                        modifier = Modifier.size(52.dp),
                        contentAlignment = Alignment.Center
                    ) {

                        Icon(
                            imageVector = GastifyIcons.account(
                                account.type
                            ),
                            contentDescription = null,
                            tint = PrimaryGreen,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }

                Spacer(Modifier.width(14.dp))

                Column(
                    modifier = Modifier.weight(1f)
                ) {

                    Text(
                        text = account.name,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(Modifier.height(5.dp))

                    Text(
                        text = accountTypeName(account.type),
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                }
            }

            Spacer(Modifier.height(22.dp))

            Text(
                text = if (isTransport) {
                    "Saldo estimado"
                } else {
                    "Saldo disponible"
                },
                fontSize = 13.sp,
                color = TextSecondary
            )

            Spacer(Modifier.height(8.dp))

            Text(
                text = formatAccountMoney(
                    account.balanceCents
                ),
                fontSize = 29.sp,
                fontWeight = FontWeight.Bold,
                color = if (isTransport) {
                    PrimaryGreen
                } else {
                    TextPrimary
                }
            )

            Spacer(Modifier.height(8.dp))

            Text(
                text = "Saldo inicial: ${
                    formatAccountMoney(account.openingBalanceCents)
                }",
                fontSize = 12.sp,
                color = TextSecondary
            )

            if (isTransport) {

                Spacer(Modifier.height(14.dp))

                Text(
                    text = "Este saldo es estimado. No consulta directamente la tarjeta física.",
                    fontSize = 12.sp,
                    lineHeight = 18.sp,
                    color = TextSecondary
                )
            }

            Spacer(Modifier.height(18.dp))

            HorizontalDivider(
                color = BorderDark
            )

            Row {

                TextButton(
                    onClick = onEdit,
                    enabled = !isSaving
                ) {

                    Icon(
                        imageVector = GastifyIcons.edit,
                        contentDescription = null,
                        tint = PrimaryGreen,
                        modifier = Modifier.size(18.dp)
                    )

                    Spacer(Modifier.width(5.dp))

                    Text(
                        text = "Editar",
                        color = PrimaryGreen
                    )
                }

                TextButton(
                    onClick = onDelete,
                    enabled = !isSaving
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
