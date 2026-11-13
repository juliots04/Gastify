package com.example.gastify.ui.screens

import android.Manifest
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel

import com.example.gastify.data.local.ReminderStore
import com.example.gastify.data.model.CategoryType
import com.example.gastify.data.model.PassageReminder
import com.example.gastify.data.model.Workspace
import com.example.gastify.notifications.ReminderScheduler
import com.example.gastify.ui.components.GastifyIcons
import com.example.gastify.ui.theme.*
import com.example.gastify.viewmodel.AccountViewModel
import com.example.gastify.viewmodel.CategoryViewModel

import kotlinx.coroutines.launch

import java.math.BigDecimal
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

private val reminderDays = listOf(
    1 to "Lu",
    2 to "Ma",
    3 to "Mi",
    4 to "Ju",
    5 to "Vi",
    6 to "Sá",
    7 to "Do"
)

private fun reminderMoney(cents: Long): String {

    return "S/ " + BigDecimal.valueOf(
        cents,
        2
    ).toPlainString()
}

private fun parseReminderMoney(
    value: String
): Long? {

    val text = value.trim()

    if (
        !Regex("""^\d{1,10}([.,]\d{1,2})?$""")
            .matches(text)
    ) {
        return null
    }

    return try {

        BigDecimal(
            text.replace(',', '.')
        ).movePointRight(2)
            .longValueExact()
            .takeIf {
                it in 1L..999_999_999_999L
            }

    } catch (_: Exception) {
        null
    }
}

private fun reminderTime(
    hour: Int,
    minute: Int
): String {

    return String.format(
        Locale.forLanguageTag("es-PE"),
        "%02d:%02d",
        hour,
        minute
    )
}

private fun reminderNext(
    reminder: PassageReminder
): String {

    if (!reminder.enabled) {
        return "Desactivado"
    }

    val next = ReminderScheduler
        .calculateNextTime(reminder)
        ?: return "Sin programación"

    return SimpleDateFormat(
        "EEE dd MMM • HH:mm",
        Locale.forLanguageTag("es-PE")
    ).format(Date(next))
}

private fun reminderDaysText(
    days: Set<Int>
): String {

    if (days.size == 7) {
        return "Todos los días"
    }

    return reminderDays
        .filter {
            it.first in days
        }
        .joinToString(" · ") {
            it.second
        }
}

private fun notificationsEnabled(
    context: Context
): Boolean {

    if (
        !NotificationManagerCompat
            .from(context)
            .areNotificationsEnabled()
    ) {
        return false
    }

    if (
        Build.VERSION.SDK_INT >=
        Build.VERSION_CODES.TIRAMISU
    ) {

        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
    }

    return true
}

@Composable
fun RemindersScreen(
    userId: String,
    workspace: Workspace,
    onBack: () -> Unit
) {

    val context = LocalContext.current

    val store = remember(context) {
        ReminderStore(context)
    }

    val scope = rememberCoroutineScope()

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

    val accountState by accountVm.uiState.collectAsState()

    val categoryState by categoryVm.uiState.collectAsState()

    DisposableEffect(accountVm, categoryVm) {

        accountVm.startListening()
        categoryVm.startListening()

        onDispose {

            accountVm.stopListening()
            categoryVm.stopListening()
        }
    }

    var reminders by remember(
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

    fun refresh() {

        reminders = store.getByWorkspace(
            userId,
            workspace.id
        )
    }

    val snackbar = remember {
        SnackbarHostState()
    }

    fun showMessage(message: String) {

        scope.launch {
            snackbar.showSnackbar(message)
        }
    }

    // ======================================
    // PERMISOS
    // ======================================

    var notificationsAllowed by remember {
        mutableStateOf(
            notificationsEnabled(context)
        )
    }

    var exactAllowed by remember {
        mutableStateOf(
            ReminderScheduler.canScheduleExact(context)
        )
    }

    val notificationLauncher =
        rememberLauncherForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) {

            notificationsAllowed =
                notificationsEnabled(context)

            if (!notificationsAllowed) {

                showMessage(
                    "Debes permitir las notificaciones."
                )
            }
        }

    val exactAlarmLauncher =
        rememberLauncherForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) {

            exactAllowed =
                ReminderScheduler.canScheduleExact(context)

            ReminderScheduler.scheduleAll(context)

            if (exactAllowed) {

                showMessage(
                    "Alarmas precisas activadas."
                )
            }
        }

    fun requestNotifications() {

        if (
            Build.VERSION.SDK_INT >=
            Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {

            notificationLauncher.launch(
                Manifest.permission.POST_NOTIFICATIONS
            )

        } else {

            try {

                val intent = Intent(
                    Settings.ACTION_APP_NOTIFICATION_SETTINGS
                ).apply {

                    putExtra(
                        Settings.EXTRA_APP_PACKAGE,
                        context.packageName
                    )
                }

                context.startActivity(intent)

            } catch (_: Exception) {

                showMessage(
                    "Activa las notificaciones desde Ajustes."
                )
            }
        }
    }

    fun requestExactAlarms() {

        if (
            Build.VERSION.SDK_INT >=
            Build.VERSION_CODES.S
        ) {

            try {

                val intent = Intent(
                    Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
                    Uri.parse(
                        "package:${context.packageName}"
                    )
                )

                exactAlarmLauncher.launch(intent)

            } catch (_: Exception) {

                showMessage(
                    "Activa Alarmas y recordatorios desde los ajustes de Android."
                )
            }
        }
    }

    // ======================================
    // FORMULARIO
    // ======================================

    var showEditor by rememberSaveable {
        mutableStateOf(false)
    }

    var editingId by rememberSaveable {
        mutableStateOf<String?>(null)
    }

    var title by rememberSaveable {
        mutableStateOf("")
    }

    var amountText by rememberSaveable {
        mutableStateOf("2.80")
    }

    var accountId by rememberSaveable {
        mutableStateOf("")
    }

    var categoryId by rememberSaveable {
        mutableStateOf("")
    }

    var hour by rememberSaveable {
        mutableStateOf(7)
    }

    var minute by rememberSaveable {
        mutableStateOf(30)
    }

    var daysMask by rememberSaveable {
        mutableStateOf(63)
    }

    var enabled by rememberSaveable {
        mutableStateOf(true)
    }

    var deleteId by rememberSaveable {
        mutableStateOf<String?>(null)
    }

    val expenseCategories =
        categoryState.categories.filter {
            it.type == CategoryType.EXPENSE.name
        }

    fun openEditor(reminder: PassageReminder?) {

        editingId = reminder?.id

        title = reminder?.title ?: ""

        amountText = reminder?.let {
            BigDecimal.valueOf(
                it.amountCents,
                2
            ).toPlainString()
        } ?: "2.80"

        accountId = reminder?.accountId
            ?: accountState.accounts.firstOrNull()
                ?.id.orEmpty()

        categoryId = reminder?.categoryId
            ?: expenseCategories.firstOrNull()
                ?.id.orEmpty()

        hour = reminder?.hour ?: 7

        minute = reminder?.minute ?: 30

        daysMask = reminder?.daysOfWeek?.fold(0) {
                mask, day ->

            mask or (1 shl (day - 1))

        } ?: 63

        enabled = reminder?.enabled ?: true

        showEditor = true
    }

    fun saveReminder(reminder: PassageReminder) {

        try {

            store.save(reminder)

            ReminderScheduler.schedule(
                context,
                reminder
            )

            refresh()

            showEditor = false

            showMessage(
                "Recordatorio guardado correctamente."
            )

            if (!notificationsAllowed) {
                requestNotifications()
            }

        } catch (exception: Exception) {

            showMessage(
                exception.localizedMessage
                    ?: "No se pudo guardar."
            )
        }
    }

    fun changeStatus(
        reminder: PassageReminder,
        isEnabled: Boolean
    ) {

        try {

            val updated = reminder.copy(
                enabled = isEnabled
            )

            store.save(updated)

            ReminderScheduler.schedule(
                context,
                updated
            )

            refresh()

        } catch (exception: Exception) {

            showMessage(
                exception.localizedMessage
                    ?: "No se pudo actualizar."
            )
        }
    }

    fun deleteReminder(reminderId: String) {

        try {

            ReminderScheduler.cancel(
                context,
                reminderId
            )

            store.delete(reminderId)

            refresh()

            deleteId = null

            showMessage("Recordatorio eliminado.")

        } catch (exception: Exception) {

            showMessage(
                exception.localizedMessage
                    ?: "No se pudo eliminar."
            )
        }
    }

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
                Arrangement.spacedBy(12.dp)
        ) {

            item {

                Spacer(Modifier.height(16.dp))

                TextButton(onClick = onBack) {

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
                            Icons.Outlined.NotificationsActive,
                        contentDescription = null,
                        tint = PrimaryGreen,
                        modifier = Modifier.size(29.dp)
                    )

                    Spacer(Modifier.width(12.dp))

                    Text(
                        text = "Recordatorios",
                        fontSize = 27.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(Modifier.height(8.dp))

                Text(
                    text = workspace.name,
                    color = TextSecondary
                )

                Spacer(Modifier.height(24.dp))
            }

            // PERMISO DE NOTIFICACIONES

            if (!notificationsAllowed) {

                item {

                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = SurfaceDark
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {

                        Column(
                            modifier = Modifier.padding(18.dp)
                        ) {

                            Text(
                                text = "Notificaciones desactivadas",
                                color = ExpenseRed,
                                fontWeight = FontWeight.Bold
                            )

                            Spacer(Modifier.height(8.dp))

                            Text(
                                text = "Debes permitirlas para recibir los avisos.",
                                color = TextSecondary,
                                fontSize = 13.sp
                            )

                            Spacer(Modifier.height(10.dp))

                            TextButton(
                                onClick = {
                                    requestNotifications()
                                }
                            ) {

                                Text(
                                    text = "Activar notificaciones",
                                    color = PrimaryGreen
                                )
                            }
                        }
                    }
                }
            }

            // PERMISO DE ALARMAS EXACTAS

            if (!exactAllowed) {

                item {

                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = SurfaceDark
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {

                        Column(
                            modifier = Modifier.padding(18.dp)
                        ) {

                            Text(
                                text = "Activar alarmas precisas",
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )

                            Spacer(Modifier.height(8.dp))

                            Text(
                                text = "Android puede retrasar tus avisos si no permites programarlos a una hora precisa.",
                                fontSize = 13.sp,
                                lineHeight = 20.sp,
                                color = TextSecondary
                            )

                            Spacer(Modifier.height(10.dp))

                            TextButton(
                                onClick = {
                                    requestExactAlarms()
                                }
                            ) {

                                Text(
                                    text = "Conceder permiso",
                                    color = PrimaryGreen
                                )
                            }
                        }
                    }
                }
            }

            item {

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = CardDark
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {

                    Column(
                        modifier = Modifier.padding(18.dp)
                    ) {

                        Text(
                            text = "Recordatorios inteligentes",
                            fontWeight = FontWeight.SemiBold,
                            color = PrimaryGreen
                        )

                        Spacer(Modifier.height(8.dp))

                        Text(
                            text = "Cuando aparezca el aviso podrás confirmar el pago o aplazarlo 10 minutos. Aplazarlo no descontará dinero.",
                            color = TextSecondary,
                            fontSize = 13.sp,
                            lineHeight = 20.sp
                        )

                        Spacer(Modifier.height(10.dp))

                        Text(
                            text = "En HyperOS, permite también el inicio automático y la actividad en segundo plano de GASTIFY.",
                            color = TextSecondary,
                            fontSize = 12.sp,
                            lineHeight = 18.sp
                        )
                    }
                }
            }

            item {

                Spacer(Modifier.height(12.dp))

                Button(
                    onClick = {
                        openEditor(null)
                    },
                    enabled = !accountState.isLoading &&
                        !categoryState.isLoading,
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
                        text = "Nuevo recordatorio",
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(Modifier.height(18.dp))

                Text(
                    text = "Mis recordatorios (${reminders.size})",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(Modifier.height(8.dp))
            }

            if (reminders.isEmpty()) {

                item {

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = SurfaceDark
                        )
                    ) {

                        Text(
                            text = "Todavía no tienes recordatorios.",
                            color = TextSecondary,
                            modifier = Modifier.padding(24.dp)
                        )
                    }
                }

            } else {

                items(
                    items = reminders,
                    key = { it.id }
                ) { reminder ->

                    val accountName =
                        accountState.accounts.firstOrNull {
                            it.id == reminder.accountId
                        }?.name ?: "Cuenta no disponible"

                    val categoryName =
                        categoryState.categories.firstOrNull {
                            it.id == reminder.categoryId
                        }?.name ?: "Categoría no disponible"

                    ReminderCard(
                        reminder = reminder,
                        accountName = accountName,
                        categoryName = categoryName,
                        onEdit = {
                            openEditor(reminder)
                        },
                        onDelete = {
                            deleteId = reminder.id
                        },
                        onEnabledChange = {
                            changeStatus(reminder, it)
                        }
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

    // ======================================
    // CREAR / EDITAR
    // ======================================

    if (showEditor) {

        val amount = parseReminderMoney(amountText)

        val account = accountState.accounts.firstOrNull {
            it.id == accountId
        }

        val category = expenseCategories.firstOrNull {
            it.id == categoryId
        }

        val selectedDays = (1..7)
            .filter {
                daysMask and (1 shl (it - 1)) != 0
            }
            .toSet()

        val valid =
            title.trim().isNotEmpty() &&
                amount != null &&
                account != null &&
                category != null &&
                selectedDays.isNotEmpty()

        AlertDialog(
            onDismissRequest = {
                showEditor = false
            },
            containerColor = SurfaceDark,
            title = {

                Text(
                    if (editingId == null) {
                        "Nuevo recordatorio"
                    } else {
                        "Editar recordatorio"
                    }
                )
            },
            text = {

                Column(
                    modifier = Modifier
                        .heightIn(max = 480.dp)
                        .verticalScroll(
                            rememberScrollState()
                        )
                ) {

                    OutlinedTextField(
                        value = title,
                        onValueChange = {
                            title = it.take(60)
                        },
                        label = {
                            Text("Nombre")
                        },
                        placeholder = {
                            Text("Pasaje de ida")
                        },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(Modifier.height(16.dp))

                    Text(
                        text = "Cuenta",
                        fontWeight = FontWeight.Medium
                    )

                    ReminderDropdown(
                        selectedText = account?.name
                            ?: "Seleccionar cuenta",
                        options = accountState.accounts.map {
                            it.id to it.name
                        },
                        onSelect = {
                            accountId = it
                        }
                    )

                    Spacer(Modifier.height(16.dp))

                    Text(
                        text = "Categoría de gasto",
                        fontWeight = FontWeight.Medium
                    )

                    ReminderDropdown(
                        selectedText = category?.name
                            ?: "Seleccionar categoría",
                        options = expenseCategories.map {
                            it.id to it.name
                        },
                        onSelect = {
                            categoryId = it
                        }
                    )

                    Spacer(Modifier.height(16.dp))

                    OutlinedTextField(
                        value = amountText,
                        onValueChange = {
                            amountText = it.take(16)
                        },
                        label = {
                            Text("Precio del pasaje (S/)")
                        },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Decimal
                        ),
                        singleLine = true,
                        isError = amountText.isNotBlank() &&
                            amount == null,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(Modifier.height(20.dp))

                    Text(
                        text = "Horario",
                        fontWeight = FontWeight.Medium
                    )

                    Spacer(Modifier.height(8.dp))

                    OutlinedButton(
                        onClick = {

                            TimePickerDialog(
                                context,
                                { _, h, m ->

                                    hour = h
                                    minute = m
                                },
                                hour,
                                minute,
                                true
                            ).show()
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {

                        Text(
                            text = reminderTime(hour, minute),
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = PrimaryGreen
                        )
                    }

                    Spacer(Modifier.height(20.dp))

                    Text(
                        text = "Días de la semana",
                        fontWeight = FontWeight.Medium
                    )

                    Spacer(Modifier.height(8.dp))

                    Column {

                        reminderDays.chunked(4).forEach { row ->

                            Row(
                                horizontalArrangement =
                                    Arrangement.spacedBy(6.dp)
                            ) {

                                row.forEach { (day, label) ->

                                    val bit =
                                        1 shl (day - 1)

                                    FilterChip(
                                        selected =
                                            daysMask and bit != 0,
                                        onClick = {
                                            daysMask =
                                                daysMask xor bit
                                        },
                                        label = {
                                            Text(label)
                                        }
                                    )
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement =
                            Arrangement.SpaceBetween
                    ) {

                        Text("Recordatorio activo")

                        Switch(
                            checked = enabled,
                            onCheckedChange = {
                                enabled = it
                            }
                        )
                    }
                }
            },
            confirmButton = {

                TextButton(
                    enabled = valid,
                    onClick = {

                        if (
                            amount != null &&
                            account != null &&
                            category != null
                        ) {

                            saveReminder(
                                PassageReminder(
                                    id = editingId
                                        ?: UUID.randomUUID()
                                            .toString(),
                                    userId = userId,
                                    workspaceId = workspace.id,
                                    accountId = account.id,
                                    categoryId = category.id,
                                    title = title.trim(),
                                    amountCents = amount,
                                    hour = hour,
                                    minute = minute,
                                    daysOfWeek = selectedDays,
                                    enabled = enabled
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

    // ======================================
    // ELIMINAR
    // ======================================

    val toDelete = reminders.firstOrNull {
        it.id == deleteId
    }

    if (toDelete != null) {

        AlertDialog(
            onDismissRequest = {
                deleteId = null
            },
            containerColor = SurfaceDark,
            title = {
                Text("Eliminar recordatorio")
            },
            text = {
                Text(
                    "¿Eliminar \"${toDelete.title}\"?"
                )
            },
            confirmButton = {

                TextButton(
                    onClick = {
                        deleteReminder(toDelete.id)
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

@Composable
private fun ReminderCard(
    reminder: PassageReminder,
    accountName: String,
    categoryName: String,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onEnabledChange: (Boolean) -> Unit
) {

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

                Icon(
                    imageVector =
                        Icons.Outlined.NotificationsActive,
                    contentDescription = null,
                    tint = PrimaryGreen,
                    modifier = Modifier.size(28.dp)
                )

                Spacer(Modifier.width(12.dp))

                Column(
                    modifier = Modifier.weight(1f)
                ) {

                    Text(
                        text = reminder.title,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(Modifier.height(6.dp))

                    Text(
                        text = reminderTime(
                            reminder.hour,
                            reminder.minute
                        ),
                        color = PrimaryGreen,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Switch(
                    checked = reminder.enabled,
                    onCheckedChange = onEnabledChange
                )
            }

            Spacer(Modifier.height(16.dp))

            HorizontalDivider(color = BorderDark)

            Spacer(Modifier.height(14.dp))

            Text(
                text = reminderMoney(
                    reminder.amountCents
                ),
                fontSize = 19.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(Modifier.height(10.dp))

            Text(
                text = "Cuenta: $accountName",
                color = TextSecondary,
                fontSize = 13.sp
            )

            Text(
                text = "Categoría: $categoryName",
                color = TextSecondary,
                fontSize = 13.sp
            )

            Spacer(Modifier.height(10.dp))

            Text(
                text = reminderDaysText(
                    reminder.daysOfWeek
                ),
                color = TextSecondary,
                fontSize = 12.sp
            )

            Spacer(Modifier.height(8.dp))

            Text(
                text = "Próximo aviso: ${
                    reminderNext(reminder)
                }",
                color = PrimaryGreen,
                fontSize = 12.sp
            )

            Spacer(Modifier.height(12.dp))

            HorizontalDivider(color = BorderDark)

            Row {

                TextButton(onClick = onEdit) {

                    Icon(
                        imageVector = GastifyIcons.edit,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )

                    Spacer(Modifier.width(5.dp))

                    Text("Editar")
                }

                TextButton(onClick = onDelete) {

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

@Composable
private fun ReminderDropdown(
    selectedText: String,
    options: List<Pair<String, String>>,
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

            Spacer(Modifier.width(8.dp))

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

            options.forEach { option ->

                DropdownMenuItem(
                    text = {
                        Text(
                            text = option.second,
                            color = TextPrimary
                        )
                    },
                    onClick = {

                        onSelect(option.first)

                        expanded = false
                    }
                )
            }

            if (options.isEmpty()) {

                DropdownMenuItem(
                    text = {
                        Text("Sin opciones disponibles")
                    },
                    onClick = {
                        expanded = false
                    }
                )
            }
        }
    }
}
