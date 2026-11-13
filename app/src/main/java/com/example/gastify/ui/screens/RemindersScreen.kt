package com.example.gastify.ui.screens

import android.Manifest
import android.app.TimePickerDialog
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material3.*

import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable

import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat

import androidx.lifecycle.viewmodel.compose.viewModel

import com.example.gastify.data.local.ReminderStore

import com.example.gastify.data.model.PassageReminder
import com.example.gastify.data.model.Workspace
import com.example.gastify.data.model.CategoryType

import com.example.gastify.notifications.ReminderScheduler

import com.example.gastify.ui.components.GastifyIcons
import com.example.gastify.ui.theme.*

import com.example.gastify.viewmodel.AccountViewModel
import com.example.gastify.viewmodel.CategoryViewModel

import kotlinx.coroutines.launch

import java.math.BigDecimal
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

// ==========================================
// CONFIGURACIÓN DE DÍAS
// ==========================================

private val reminderDays = listOf(
    1 to "Lu",
    2 to "Ma",
    3 to "Mi",
    4 to "Ju",
    5 to "Vi",
    6 to "Sá",
    7 to "Do"
)

// ==========================================
// FORMATO DE MONEDA
// ==========================================

private fun formatReminderMoney(
    cents: Long
): String {

    val formatter = NumberFormat.getNumberInstance(
        Locale.forLanguageTag("es-PE")
    )

    formatter.minimumFractionDigits = 2
    formatter.maximumFractionDigits = 2

    val amount = BigDecimal.valueOf(cents, 2)

    return "S/ ${formatter.format(amount)}"
}

// ==========================================
// CONVERTIR MONEDA
// ==========================================

private fun parseReminderMoney(
    text: String
): Long? {

    val clean = text.trim()

    if (
        !Regex("""^\d{1,10}([.,]\d{1,2})?$""")
            .matches(clean)
    ) {
        return null
    }

    return try {

        val cents = BigDecimal(
            clean.replace(',', '.')
        ).movePointRight(2).longValueExact()

        cents.takeIf {
            it in 1L..999_999_999_999L
        }

    } catch (_: Exception) {
        null
    }
}

// ==========================================
// FORMATO DE HORA
// ==========================================

private fun formatReminderTime(
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

// ==========================================
// FORMATO DE PRÓXIMO RECORDATORIO
// ==========================================

private fun formatNextReminder(
    reminder: PassageReminder
): String {

    if (!reminder.enabled) {
        return "Desactivado"
    }

    val nextTime = ReminderScheduler.calculateNextTime(
        reminder
    ) ?: return "Sin programación"

    val formatter = SimpleDateFormat(
        "EEE dd MMM • HH:mm",
        Locale.forLanguageTag("es-PE")
    )

    return formatter.format(
        Date(nextTime)
    )
}

// ==========================================
// FORMATO DE DÍAS
// ==========================================

private fun formatReminderDays(
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

// ==========================================
// PERMISO DE NOTIFICACIONES
// ==========================================

private fun notificationsEnabled(
    context: Context
): Boolean {

    val systemEnabled = NotificationManagerCompat
        .from(context)
        .areNotificationsEnabled()

    if (!systemEnabled) {
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

// ==========================================
// PANTALLA PRINCIPAL
// ==========================================

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

    // ======================================
    // VIEWMODELS
    // ======================================

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

    // ======================================
    // LISTA DE RECORDATORIOS
    // ======================================

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

    fun refreshReminders() {

        reminders = store.getByWorkspace(
            userId,
            workspace.id
        )
    }

    // ======================================
    // NOTIFICACIONES
    // ======================================

    var notificationsAllowed by remember {
        mutableStateOf(
            notificationsEnabled(context)
        )
    }

    val snackbar = remember {
        SnackbarHostState()
    }

    val notificationPermissionLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestPermission()
        ) { granted ->

            notificationsAllowed = granted &&
                notificationsEnabled(context)

            if (!notificationsAllowed) {

                scope.launch {

                    snackbar.showSnackbar(
                        "Debes permitir las notificaciones para recibir avisos."
                    )
                }
            }
        }

    fun requestNotificationPermission() {

        if (
            Build.VERSION.SDK_INT >=
            Build.VERSION_CODES.TIRAMISU
        ) {

            notificationPermissionLauncher.launch(
                Manifest.permission.POST_NOTIFICATIONS
            )
        }
    }

    // ======================================
    // ESTADOS DE FORMULARIO
    // ======================================

    var showEditor by rememberSaveable {
        mutableStateOf(false)
    }

    var editingId by rememberSaveable {
        mutableStateOf<String?>(null)
    }

    var reminderTitle by rememberSaveable {
        mutableStateOf("")
    }

    var amountText by rememberSaveable {
        mutableStateOf("2.80")
    }

    var selectedAccountId by rememberSaveable {
        mutableStateOf("")
    }

    var selectedCategoryId by rememberSaveable {
        mutableStateOf("")
    }

    var selectedHour by rememberSaveable {
        mutableStateOf(7)
    }

    var selectedMinute by rememberSaveable {
        mutableStateOf(30)
    }

    // Bits 1-6: lunes a sábado.
    // Cada bit representa un día.

    var selectedDaysMask by rememberSaveable {
        mutableStateOf(63)
    }

    var reminderEnabled by rememberSaveable {
        mutableStateOf(true)
    }

    var reminderToDeleteId by rememberSaveable {
        mutableStateOf<String?>(null)
    }

    // ======================================
    // CATEGORÍAS DE GASTOS
    // ======================================

    val expenseCategories = categoryState.categories.filter {
        it.type == CategoryType.EXPENSE.name
    }

    // ======================================
    // ABRIR FORMULARIO
    // ======================================

    fun openEditor(
        reminder: PassageReminder?
    ) {

        editingId = reminder?.id

        reminderTitle = reminder?.title ?: ""

        amountText = reminder?.let {

            BigDecimal.valueOf(
                it.amountCents,
                2
            ).toPlainString()

        } ?: "2.80"

        selectedAccountId = reminder?.accountId
            ?: accountState.accounts.firstOrNull()?.id.orEmpty()

        selectedCategoryId = reminder?.categoryId
            ?: expenseCategories.firstOrNull()?.id.orEmpty()

        selectedHour = reminder?.hour ?: 7

        selectedMinute = reminder?.minute ?: 30

        selectedDaysMask = if (reminder != null) {

            reminder.daysOfWeek.fold(0) { mask, day ->
                mask or (1 shl (day - 1))
            }

        } else {
            63
        }

        reminderEnabled = reminder?.enabled ?: true

        showEditor = true
    }

    // ======================================
    // GUARDAR RECORDATORIO
    // ======================================

    fun saveReminder(
        reminder: PassageReminder
    ) {

        try {

            store.save(reminder)

            ReminderScheduler.schedule(
                context,
                reminder
            )

            refreshReminders()

            showEditor = false

            scope.launch {

                snackbar.showSnackbar(
                    "Recordatorio guardado correctamente."
                )
            }

            if (!notificationsAllowed) {
                requestNotificationPermission()
            }

        } catch (exception: Exception) {

            scope.launch {

                snackbar.showSnackbar(
                    exception.localizedMessage
                        ?: "No se pudo guardar el recordatorio."
                )
            }
        }
    }

    // ======================================
    // ACTIVAR / DESACTIVAR
    // ======================================

    fun changeReminderStatus(
        reminder: PassageReminder,
        enabled: Boolean
    ) {

        try {

            val updated = reminder.copy(
                enabled = enabled
            )

            store.save(updated)

            ReminderScheduler.schedule(
                context,
                updated
            )

            refreshReminders()

            if (enabled && !notificationsAllowed) {
                requestNotificationPermission()
            }

        } catch (exception: Exception) {

            scope.launch {

                snackbar.showSnackbar(
                    exception.localizedMessage
                        ?: "No se pudo actualizar el recordatorio."
                )
            }
        }
    }

    // ======================================
    // ELIMINAR RECORDATORIO
    // ======================================

    fun deleteReminder(
        reminderId: String
    ) {

        try {

            ReminderScheduler.cancel(
                context,
                reminderId
            )

            store.delete(reminderId)

            refreshReminders()

            reminderToDeleteId = null

            scope.launch {

                snackbar.showSnackbar(
                    "Recordatorio eliminado."
                )
            }

        } catch (exception: Exception) {

            scope.launch {

                snackbar.showSnackbar(
                    exception.localizedMessage
                        ?: "No se pudo eliminar el recordatorio."
                )
            }
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

            verticalArrangement = Arrangement.spacedBy(12.dp)
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

                Spacer(Modifier.height(14.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {

                    Icon(
                        imageVector = Icons.Outlined.NotificationsActive,
                        contentDescription = null,
                        tint = PrimaryGreen,
                        modifier = Modifier.size(30.dp)
                    )

                    Spacer(Modifier.width(12.dp))

                    Text(
                        text = "Recordatorios",
                        fontSize = 27.sp,
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
            // INFORMACIÓN
            // ==================================

            item {

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

                        Text(
                            text = "Controla tus pasajes",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary
                        )

                        Spacer(Modifier.height(8.dp))

                        Text(
                            text = "Configura horarios según tus turnos de trabajo. GASTIFY te recordará confirmar si pagaste tu pasaje.",
                            fontSize = 13.sp,
                            lineHeight = 20.sp,
                            color = TextSecondary
                        )

                        Spacer(Modifier.height(12.dp))

                        Text(
                            text = "Los horarios se guardan en este dispositivo.",
                            fontSize = 12.sp,
                            color = PrimaryGreen
                        )
                    }
                }
            }

            // ==================================
            // PERMISO DE NOTIFICACIONES
            // ==================================

            if (!notificationsAllowed) {

                item {

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = CardDark
                        )
                    ) {

                        Column(
                            modifier = Modifier.padding(18.dp)
                        ) {

                            Text(
                                text = "Notificaciones desactivadas",
                                fontWeight = FontWeight.SemiBold,
                                color = ExpenseRed
                            )

                            Spacer(Modifier.height(8.dp))

                            Text(
                                text = "Activa las notificaciones para recibir recordatorios de tus pasajes.",
                                fontSize = 12.sp,
                                lineHeight = 18.sp,
                                color = TextSecondary
                            )

                            Spacer(Modifier.height(10.dp))

                            TextButton(
                                onClick = {
                                    requestNotificationPermission()
                                }
                            ) {

                                Text(
                                    text = "Permitir notificaciones",
                                    color = PrimaryGreen
                                )
                            }
                        }
                    }
                }
            }

            // ==================================
            // CREAR RECORDATORIO
            // ==================================

            item {

                Spacer(Modifier.height(8.dp))

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
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )

                    Spacer(Modifier.width(8.dp))

                    Text(
                        text = "Nuevo recordatorio",
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(Modifier.height(16.dp))

                // COMPROBAR CUENTAS

                if (accountState.accounts.isEmpty()) {

                    Text(
                        text = "Primero crea una cuenta en este espacio.",
                        fontSize = 12.sp,
                        color = ExpenseRed
                    )

                    Spacer(Modifier.height(8.dp))
                }

                // COMPROBAR CATEGORÍAS

                if (expenseCategories.isEmpty()) {

                    Text(
                        text = "Primero crea una categoría de tipo Gasto.",
                        fontSize = 12.sp,
                        color = ExpenseRed
                    )

                    Spacer(Modifier.height(8.dp))
                }

                Spacer(Modifier.height(18.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {

                    Text(
                        text = "Mis recordatorios",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )

                    Text(
                        text = "${reminders.size}",
                        fontSize = 16.sp,
                        color = PrimaryGreen
                    )
                }

                Spacer(Modifier.height(10.dp))
            }

            // ==================================
            // SIN RECORDATORIOS
            // ==================================

            if (reminders.isEmpty()) {

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

                            Icon(
                                imageVector = Icons.Outlined.NotificationsActive,
                                contentDescription = null,
                                tint = TextSecondary,
                                modifier = Modifier.size(32.dp)
                            )

                            Spacer(Modifier.height(14.dp))

                            Text(
                                text = "Sin recordatorios",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.SemiBold
                            )

                            Spacer(Modifier.height(8.dp))

                            Text(
                                text = "Crea tu primer recordatorio para controlar tus pasajes.",
                                fontSize = 13.sp,
                                color = TextSecondary,
                                lineHeight = 20.sp
                            )
                        }
                    }
                }

            } else {

                // ==================================
                // LISTADO DE RECORDATORIOS
                // ==================================

                items(
                    items = reminders,
                    key = { it.id }
                ) { reminder ->

                    val accountName = accountState.accounts
                        .firstOrNull {
                            it.id == reminder.accountId
                        }
                        ?.name
                        ?: "Cuenta no disponible"

                    val categoryName = categoryState.categories
                        .firstOrNull {
                            it.id == reminder.categoryId
                        }
                        ?.name
                        ?: "Categoría no disponible"

                    ReminderCard(
                        reminder = reminder,
                        accountName = accountName,
                        categoryName = categoryName,

                        onEdit = {
                            openEditor(reminder)
                        },

                        onDelete = {
                            reminderToDeleteId = reminder.id
                        },

                        onEnabledChange = { enabled ->

                            changeReminderStatus(
                                reminder,
                                enabled
                            )
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

    // ==========================================
    // FORMULARIO CREAR / EDITAR
    // ==========================================

    if (showEditor) {

        val amountCents = parseReminderMoney(
            amountText
        )

        val selectedAccount = accountState.accounts
            .firstOrNull {
                it.id == selectedAccountId
            }

        val selectedCategory = expenseCategories
            .firstOrNull {
                it.id == selectedCategoryId
            }

        val selectedDays = (1..7)
            .filter { day ->

                selectedDaysMask and
                    (1 shl (day - 1)) != 0

            }
            .toSet()

        val canSave =
            reminderTitle.trim().isNotEmpty() &&
                amountCents != null &&
                selectedAccount != null &&
                selectedCategory != null &&
                selectedDays.isNotEmpty()

        AlertDialog(
            onDismissRequest = {
                showEditor = false
            },

            containerColor = SurfaceDark,

            title = {

                Text(
                    text = if (editingId == null) {
                        "Nuevo recordatorio"
                    } else {
                        "Editar recordatorio"
                    },
                    color = TextPrimary
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

                    // NOMBRE

                    OutlinedTextField(
                        value = reminderTitle,

                        onValueChange = {
                            reminderTitle = it.take(60)
                        },

                        label = {
                            Text("Nombre")
                        },

                        placeholder = {
                            Text("Ejemplo: Pasaje de ida")
                        },

                        singleLine = true,

                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(Modifier.height(18.dp))

                    // CUENTA

                    Text(
                        text = "Cuenta",
                        fontWeight = FontWeight.Medium
                    )

                    Spacer(Modifier.height(8.dp))

                    ReminderDropdown(
                        selectedText = selectedAccount?.name
                            ?: "Seleccionar cuenta",

                        options = accountState.accounts.map {
                            it.id to it.name
                        },

                        onSelect = {
                            selectedAccountId = it
                        }
                    )

                    Spacer(Modifier.height(18.dp))

                    // CATEGORÍA

                    Text(
                        text = "Categoría del gasto",
                        fontWeight = FontWeight.Medium
                    )

                    Spacer(Modifier.height(8.dp))

                    ReminderDropdown(
                        selectedText = selectedCategory?.name
                            ?: "Seleccionar categoría",

                        options = expenseCategories.map {
                            it.id to it.name
                        },

                        onSelect = {
                            selectedCategoryId = it
                        }
                    )

                    Spacer(Modifier.height(18.dp))

                    // IMPORTE

                    OutlinedTextField(
                        value = amountText,

                        onValueChange = {
                            amountText = it.take(16)
                        },

                        label = {
                            Text("Precio del pasaje (S/)")
                        },

                        placeholder = {
                            Text("2.80")
                        },

                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Decimal
                        ),

                        singleLine = true,

                        isError = amountText.isNotBlank() &&
                            amountCents == null,

                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(Modifier.height(22.dp))

                    // HORARIO

                    Text(
                        text = "Horario",
                        fontWeight = FontWeight.Medium
                    )

                    Spacer(Modifier.height(10.dp))

                    OutlinedButton(
                        onClick = {

                            TimePickerDialog(
                                context,

                                { _, hour, minute ->

                                    selectedHour = hour
                                    selectedMinute = minute
                                },

                                selectedHour,
                                selectedMinute,
                                true
                            ).show()
                        },

                        modifier = Modifier.fillMaxWidth(),

                        shape = RoundedCornerShape(14.dp)
                    ) {

                        Text(
                            text = formatReminderTime(
                                selectedHour,
                                selectedMinute
                            ),

                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = PrimaryGreen
                        )
                    }

                    Spacer(Modifier.height(22.dp))

                    // DÍAS

                    Text(
                        text = "Días de la semana",
                        fontWeight = FontWeight.Medium
                    )

                    Spacer(Modifier.height(10.dp))

                    // PRIMERA FILA

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {

                        reminderDays.take(4).forEach { (day, label) ->

                            val bit = 1 shl (day - 1)

                            val selected =
                                selectedDaysMask and bit != 0

                            FilterChip(
                                selected = selected,

                                onClick = {

                                    selectedDaysMask =
                                        selectedDaysMask xor bit
                                },

                                label = {
                                    Text(label)
                                }
                            )
                        }
                    }

                    // SEGUNDA FILA

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {

                        reminderDays.drop(4).forEach { (day, label) ->

                            val bit = 1 shl (day - 1)

                            val selected =
                                selectedDaysMask and bit != 0

                            FilterChip(
                                selected = selected,

                                onClick = {

                                    selectedDaysMask =
                                        selectedDaysMask xor bit
                                },

                                label = {
                                    Text(label)
                                }
                            )
                        }
                    }

                    if (selectedDays.isEmpty()) {

                        Spacer(Modifier.height(6.dp))

                        Text(
                            text = "Selecciona al menos un día.",
                            color = ExpenseRed,
                            fontSize = 12.sp
                        )
                    }

                    Spacer(Modifier.height(18.dp))

                    // ACTIVAR RECORDATORIO

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {

                        Text(
                            text = "Recordatorio activo",
                            fontWeight = FontWeight.Medium
                        )

                        Switch(
                            checked = reminderEnabled,

                            onCheckedChange = {
                                reminderEnabled = it
                            }
                        )
                    }

                    Spacer(Modifier.height(12.dp))

                    Text(
                        text = "El aviso se repetirá en los días seleccionados.",
                        fontSize = 12.sp,
                        lineHeight = 18.sp,
                        color = TextSecondary
                    )
                }
            },

            confirmButton = {

                TextButton(
                    enabled = canSave,

                    onClick = {

                        if (
                            amountCents != null &&
                            selectedAccount != null &&
                            selectedCategory != null &&
                            selectedDays.isNotEmpty()
                        ) {

                            val reminder = PassageReminder(

                                id = editingId
                                    ?: UUID.randomUUID().toString(),

                                userId = userId,

                                workspaceId = workspace.id,

                                accountId = selectedAccount.id,

                                categoryId = selectedCategory.id,

                                title = reminderTitle.trim(),

                                amountCents = amountCents,

                                hour = selectedHour,

                                minute = selectedMinute,

                                daysOfWeek = selectedDays,

                                enabled = reminderEnabled
                            )

                            saveReminder(reminder)
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
    // CONFIRMACIÓN DE ELIMINACIÓN
    // ==========================================

    val reminderToDelete = reminders.firstOrNull {
        it.id == reminderToDeleteId
    }

    if (reminderToDelete != null) {

        AlertDialog(
            onDismissRequest = {
                reminderToDeleteId = null
            },

            containerColor = SurfaceDark,

            title = {

                Text(
                    text = "Eliminar recordatorio",
                    color = TextPrimary
                )
            },

            text = {

                Text(
                    text = "¿Seguro que deseas eliminar \"${reminderToDelete.title}\"?",
                    color = TextSecondary
                )
            },

            confirmButton = {

                TextButton(
                    onClick = {

                        deleteReminder(
                            reminderToDelete.id
                        )
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
                        reminderToDeleteId = null
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

// ==========================================
// TARJETA DE RECORDATORIO
// ==========================================

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

            // ENCABEZADO

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {

                Surface(
                    color = CardDark,
                    shape = RoundedCornerShape(14.dp)
                ) {

                    Box(
                        modifier = Modifier.size(50.dp),
                        contentAlignment = Alignment.Center
                    ) {

                        Icon(
                            imageVector = Icons.Outlined.NotificationsActive,
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
                        text = reminder.title,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(Modifier.height(5.dp))

                    Text(
                        text = formatReminderTime(
                            reminder.hour,
                            reminder.minute
                        ),

                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryGreen
                    )
                }

                Switch(
                    checked = reminder.enabled,

                    onCheckedChange = onEnabledChange
                )
            }

            Spacer(Modifier.height(18.dp))

            HorizontalDivider(
                color = BorderDark
            )

            Spacer(Modifier.height(14.dp))

            // IMPORTE

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {

                Text(
                    text = "Importe",
                    color = TextSecondary,
                    fontSize = 13.sp
                )

                Text(
                    text = formatReminderMoney(
                        reminder.amountCents
                    ),

                    color = TextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
            }

            Spacer(Modifier.height(12.dp))

            // CUENTA

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {

                Text(
                    text = "Cuenta",
                    color = TextSecondary,
                    fontSize = 13.sp
                )

                Text(
                    text = accountName,
                    color = TextPrimary,
                    fontSize = 13.sp
                )
            }

            Spacer(Modifier.height(12.dp))

            // CATEGORÍA

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {

                Text(
                    text = "Categoría",
                    color = TextSecondary,
                    fontSize = 13.sp
                )

                Text(
                    text = categoryName,
                    color = TextPrimary,
                    fontSize = 13.sp
                )
            }

            Spacer(Modifier.height(14.dp))

            // DÍAS

            Text(
                text = formatReminderDays(
                    reminder.daysOfWeek
                ),

                fontSize = 12.sp,
                color = TextSecondary
            )

            Spacer(Modifier.height(10.dp))

            // SIGUIENTE AVISO

            Text(
                text = "Próximo aviso: ${
                    formatNextReminder(reminder)
                }",

                fontSize = 12.sp,
                color = PrimaryGreen
            )

            Spacer(Modifier.height(16.dp))

            HorizontalDivider(
                color = BorderDark
            )

            Spacer(Modifier.height(8.dp))

            // ACCIONES

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {

                TextButton(
                    onClick = onEdit
                ) {

                    Icon(
                        imageVector = GastifyIcons.edit,
                        contentDescription = null,
                        tint = PrimaryGreen,
                        modifier = Modifier.size(18.dp)
                    )

                    Spacer(Modifier.width(6.dp))

                    Text(
                        text = "Editar",
                        color = PrimaryGreen
                    )
                }

                TextButton(
                    onClick = onDelete
                ) {

                    Icon(
                        imageVector = GastifyIcons.delete,
                        contentDescription = null,
                        tint = ExpenseRed,
                        modifier = Modifier.size(18.dp)
                    )

                    Spacer(Modifier.width(6.dp))

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
// SELECTOR DE CUENTA Y CATEGORÍA
// ==========================================

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

            modifier = Modifier.fillMaxWidth(),

            shape = RoundedCornerShape(12.dp)
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

                        Text(
                            text = "No hay opciones disponibles.",
                            color = TextSecondary
                        )
                    },

                    onClick = {
                        expanded = false
                    }
                )
            }
        }
    }
}
