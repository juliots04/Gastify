package com.example.gastify.ui.screens

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.ViewAgenda
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
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
import com.example.gastify.data.model.Account
import com.example.gastify.data.model.CategoryType
import com.example.gastify.data.model.PassageReminder
import com.example.gastify.data.model.Workspace
import com.example.gastify.notifications.ReminderScheduler
import com.example.gastify.ui.components.rememberPreferredAccount
import com.example.gastify.ui.components.rememberSavedViewPreference
import com.example.gastify.ui.components.GastifyIcons
import com.example.gastify.ui.components.GlassOptionsMenu
import com.example.gastify.ui.components.GlassOptionsItem
import com.example.gastify.ui.components.GlassOptionsDivider
import dev.chrisbanes.haze.HazeInput
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.blur.HazeBlurStyle
import dev.chrisbanes.haze.blur.HazeColorEffect
import dev.chrisbanes.haze.blur.hazeBlur
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState
import com.example.gastify.ui.theme.*
import com.example.gastify.viewmodel.AccountViewModel
import com.example.gastify.viewmodel.CategoryViewModel

import kotlinx.coroutines.launch
import kotlinx.coroutines.delay

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

private fun reminderTime(hour: Int, minute: Int): String {
    val displayHour = (hour % 12).let { if (it == 0) 12 else it }
    return String.format(Locale.forLanguageTag("es-PE"), "%d:%02d %s", displayHour, minute, if (hour < 12) "AM" else "PM")
}

private fun reminderNext(reminder: PassageReminder, now: Long): String {
    if (!reminder.enabled) return "Desactivado"
    val next = ReminderScheduler.calculateNextTime(reminder) ?: return "Sin programación"
    val date = SimpleDateFormat("EEE d MMM", Locale.forLanguageTag("es-PE"))
        .format(Date(next)).replace(".", "")
    val minutes = ((next - now).coerceAtLeast(0L) + 59_999L) / 60_000L
    val remaining = when {
        minutes < 1 -> "menos de un minuto"
        minutes < 60 -> "$minutes ${if (minutes == 1L) "minuto" else "minutos"}"
        minutes < 1440 -> {
            val hours = minutes / 60
            val extraMinutes = minutes % 60
            "$hours ${if (hours == 1L) "hora" else "horas"}" +
                if (extraMinutes > 0) " y $extraMinutes ${if (extraMinutes == 1L) "minuto" else "minutos"}" else ""
        }
        else -> {
            val days = minutes / 1440
            val hours = (minutes % 1440) / 60
            "$days ${if (days == 1L) "día" else "días"}" +
                if (hours > 0) " y $hours ${if (hours == 1L) "hora" else "horas"}" else ""
        }
    }
    return "$date · en $remaining"
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
    val (preferredAccountId) = rememberPreferredAccount(userId, workspace.id)

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
            ?: (accountState.accounts.firstOrNull { it.id == preferredAccountId }
                ?: accountState.accounts.firstOrNull())?.id.orEmpty()

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

    val (gridView, setGridView) = rememberSavedViewPreference(userId, "reminders_grid", false)
    val glassState = rememberHazeState()
    val backgroundGlassState = rememberHazeState()
    val permissionBlurStyle = remember {
        HazeBlurStyle {
            blurRadius(24.dp)
            colorEffects(listOf(HazeColorEffect.tint(Color(0xFF0C1119).copy(alpha = 0.87f))))
        }
    }
    val statusBarHeight = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val listState = rememberLazyGridState()
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

        LazyVerticalGrid(
            columns = GridCells.Fixed(if (gridView) 2 else 1),
            modifier = Modifier.fillMaxSize(),
            state = listState,
            contentPadding = PaddingValues(
                top = statusBarHeight + 76.dp,
                start = 20.dp,
                end = 20.dp,
                bottom = 32.dp
            ),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement =
                Arrangement.spacedBy(12.dp)
        ) {

            // PERMISO DE NOTIFICACIONES

            if (!notificationsAllowed) {

                item(span = { GridItemSpan(maxLineSpan) }) {

                    Card(
                        modifier = Modifier.fillMaxWidth()
                            .clip(RoundedCornerShape(24.dp))
                            .hazeBlur(input = HazeInput.Sources(backgroundGlassState), style = permissionBlurStyle),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
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

                item(span = { GridItemSpan(maxLineSpan) }) {

                    Card(
                        modifier = Modifier.fillMaxWidth()
                            .clip(RoundedCornerShape(24.dp))
                            .hazeBlur(input = HazeInput.Sources(backgroundGlassState), style = permissionBlurStyle),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
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

            item(span = { GridItemSpan(maxLineSpan) }) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("Mis recordatorios", modifier = Modifier.weight(1f), fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
                    IconButton(onClick = { setGridView(!gridView) }) {
                        Icon(
                            imageVector = if (gridView) Icons.Outlined.ViewAgenda else Icons.Outlined.GridView,
                            contentDescription = if (gridView) "Ver en filas" else "Ver en cuadrícula",
                            tint = Color.White
                        )
                    }
                }
            }
            if (reminders.isEmpty()) {

                item(span = { GridItemSpan(maxLineSpan) }) {

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
                gridItems(items = reminders, key = { it.id }) { reminder ->
                    Box(Modifier.animateItem().animateContentSize()) {
                        val accountName = accountState.accounts.firstOrNull { it.id == reminder.accountId }?.name ?: "Cuenta no disponible"
                        val categoryName = categoryState.categories.firstOrNull { it.id == reminder.categoryId }?.name ?: "Categoría no disponible"
                        if (gridView) {
                            CompactReminderCard(
                                reminder = reminder, glassState = glassState,
                                accountName = accountName, categoryName = categoryName,
                                onEdit = { openEditor(reminder) }, onDelete = { deleteId = reminder.id },
                                onEnabledChange = { changeStatus(reminder, it) }
                            )
                        } else {
                            ReminderCard(
                                reminder = reminder, glassState = glassState,
                                accountName = accountName, categoryName = categoryName,
                                onEdit = { openEditor(reminder) }, onDelete = { deleteId = reminder.id },
                                onEnabledChange = { changeStatus(reminder, it) }
                            )
                        }
                    }
                }
            }
        }

        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(statusBarHeight + 64.dp)
                .background(AmoledBlack.copy(alpha = headerAlpha))
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
                    text = "Mis recordatorios",
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
                onClick = { openEditor(null) },
                enabled = !accountState.isLoading && !categoryState.isLoading,
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

                    Spacer(Modifier.height(8.dp))
                    ReminderAccountSelector(
                        accounts = accountState.accounts,
                        selectedId = accountId,
                        preferredAccountId = preferredAccountId,
                        onSelect = { accountId = it }
                    )
                    Spacer(Modifier.height(16.dp))

                    Text(
                        text = "Categoría de gasto",
                        fontWeight = FontWeight.Medium
                    )

                    Spacer(Modifier.height(8.dp))
                    ReminderCategorySelector(
                        selectedId = categoryId,
                        options = expenseCategories.map {
                            val tint = try { Color(android.graphics.Color.parseColor(it.color)) } catch (_: Exception) { PrimaryGreen }
                            ReminderOption(it.id, it.name, GastifyIcons.category(it.icon), tint)
                        },
                        onSelect = { categoryId = it }
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

                    ReminderTimeSelector(
                        hour = hour,
                        minute = minute,
                        onHourChange = { hour = it },
                        onMinuteChange = { minute = it }
                    )
                    Spacer(Modifier.height(20.dp))

                    Text(
                        text = "Días de la semana",
                        fontWeight = FontWeight.Medium
                    )

                    Spacer(Modifier.height(8.dp))

                    ReminderDaySelector(
                        selectedDays = selectedDays,
                        onDayClick = { day -> daysMask = daysMask xor (1 shl (day - 1)) }
                    )
                    Spacer(Modifier.height(16.dp))


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
    glassState: HazeState,
    accountName: String,
    categoryName: String,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onEnabledChange: (Boolean) -> Unit
) {
    var menuExpanded by remember(reminder.id) { mutableStateOf(false) }
    val now by produceState(initialValue = System.currentTimeMillis(), key1 = reminder.id) {
        while (true) {
            value = System.currentTimeMillis()
            delay(60_000L)
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.06f))
    ) {
        Column(Modifier.padding(start = 18.dp, top = 14.dp, end = 12.dp, bottom = 18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(color = PrimaryGreen.copy(alpha = 0.14f), shape = RoundedCornerShape(14.dp)) {
                    Box(Modifier.size(48.dp), contentAlignment = Alignment.Center) {
                        Icon(Icons.Outlined.NotificationsActive, contentDescription = null, tint = PrimaryGreen, modifier = Modifier.size(24.dp))
                    }
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(reminder.title, color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Spacer(Modifier.height(4.dp))
                    Text(if (reminder.enabled) "Activo" else "Desactivado", color = if (reminder.enabled) PrimaryGreen else TextSecondary, fontSize = 12.sp)
                }
                Box {
                    IconButton(onClick = { menuExpanded = true }, modifier = Modifier.size(40.dp)) {
                        Icon(Icons.Outlined.MoreVert, contentDescription = "Más opciones", tint = Color.White)
                    }
                    GlassOptionsMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }, glassState = glassState) {
                        GlassOptionsItem("Editar recordatorio", GastifyIcons.edit, onClick = { menuExpanded = false; onEdit() })
                        GlassOptionsDivider()
                        GlassOptionsItem("Eliminar recordatorio", GastifyIcons.delete, onClick = { menuExpanded = false; onDelete() }, destructive = true)
                    }
                }
            }
            Spacer(Modifier.height(18.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Hora del aviso", color = TextSecondary, fontSize = 12.sp)
                    Spacer(Modifier.height(4.dp))
                    Text(reminderTime(reminder.hour, reminder.minute), color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("Monto", color = TextSecondary, fontSize = 12.sp)
                    Spacer(Modifier.height(4.dp))
                    Text(reminderMoney(reminder.amountCents), color = Color.White, fontSize = 19.sp, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(Modifier.height(14.dp))
            HorizontalDivider(color = Color.White.copy(alpha = 0.1f))
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Column(Modifier.weight(1f)) {
                    Text("Cuenta", color = TextSecondary, fontSize = 12.sp)
                    Spacer(Modifier.height(4.dp))
                    Text(accountName, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Column(Modifier.weight(1f)) {
                    Text("Categoría", color = TextSecondary, fontSize = 12.sp)
                    Spacer(Modifier.height(4.dp))
                    Text(categoryName, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
            Spacer(Modifier.height(16.dp))
            HorizontalDivider(color = Color.White.copy(alpha = 0.1f))
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Próximo aviso", color = TextSecondary, fontSize = 12.sp)
                    Spacer(Modifier.height(4.dp))
                    Text(reminderNext(reminder, now), color = if (reminder.enabled) PrimaryGreen else TextSecondary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                }
                Spacer(Modifier.width(8.dp))
                Switch(
                    checked = reminder.enabled,
                    onCheckedChange = onEnabledChange,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = PrimaryGreen,
                        checkedBorderColor = PrimaryGreen,
                        uncheckedThumbColor = Color.White.copy(alpha = 0.65f),
                        uncheckedTrackColor = Color.White.copy(alpha = 0.10f),
                        uncheckedBorderColor = Color.White.copy(alpha = 0.18f)
                    )
                )
            }
        }
    }
}
@Composable
private fun CompactReminderCard(
    reminder: PassageReminder,
    glassState: HazeState,
    accountName: String,
    categoryName: String,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onEnabledChange: (Boolean) -> Unit
) {
    var menuExpanded by remember(reminder.id) { mutableStateOf(false) }
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.06f))
    ) {
        Column(Modifier.fillMaxWidth().padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.NotificationsActive, contentDescription = null, tint = PrimaryGreen, modifier = Modifier.size(22.dp))
                Spacer(Modifier.weight(1f))
                Box {
                    IconButton(onClick = { menuExpanded = true }, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Outlined.MoreVert, contentDescription = "Más opciones", tint = Color.White)
                    }
                    GlassOptionsMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }, glassState = glassState) {
                        GlassOptionsItem("Editar recordatorio", GastifyIcons.edit, onClick = { menuExpanded = false; onEdit() })
                        GlassOptionsDivider()
                        GlassOptionsItem("Eliminar recordatorio", GastifyIcons.delete, onClick = { menuExpanded = false; onDelete() }, destructive = true)
                    }
                }
            }
            Text(reminder.title, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(reminderTime(reminder.hour, reminder.minute), modifier = Modifier.weight(1f), color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.width(4.dp))
                Text(reminderMoney(reminder.amountCents), color = PrimaryGreen, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
            }
            Spacer(Modifier.height(8.dp))
            Text(accountName, color = TextSecondary, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(categoryName, color = TextSecondary, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(if (reminder.enabled) "Activo" else "Desactivado", modifier = Modifier.weight(1f), color = if (reminder.enabled) PrimaryGreen else TextSecondary, fontSize = 11.sp)
                Switch(
                    checked = reminder.enabled,
                    onCheckedChange = onEnabledChange,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White, checkedTrackColor = PrimaryGreen, checkedBorderColor = PrimaryGreen,
                        uncheckedThumbColor = Color.White.copy(alpha = 0.65f), uncheckedTrackColor = Color.White.copy(alpha = 0.10f), uncheckedBorderColor = Color.White.copy(alpha = 0.18f)
                    )
                )
            }
        }
    }
}
@Composable
private fun ReminderDaySelector(selectedDays: Set<Int>, onDayClick: ((Int) -> Unit)? = null) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
        reminderDays.forEach { (day, label) ->
            val active = day in selectedDays
            var modifier = Modifier.weight(1f).height(32.dp).clip(RoundedCornerShape(10.dp))
                .background(if (active) PrimaryGreen.copy(alpha = 0.18f) else Color.White.copy(alpha = 0.04f))
            if (onDayClick != null) modifier = modifier.clickable { onDayClick(day) }
            Box(modifier, contentAlignment = Alignment.Center) {
                Text(label, color = if (active) PrimaryGreen else TextSecondary.copy(alpha = 0.55f), fontSize = 12.sp, fontWeight = if (active) FontWeight.Bold else FontWeight.Normal)
            }
        }
    }
}

@Composable
private fun ReminderAccountSelector(accounts: List<Account>, selectedId: String, preferredAccountId: String?, onSelect: (String) -> Unit) {
    val listState = rememberLazyListState()
    val orderedAccounts = remember(accounts, selectedId) { accounts.sortedBy { it.id != selectedId } }
    LaunchedEffect(selectedId) { listState.animateScrollToItem(0) }
    if (accounts.isEmpty()) {
        Text("No hay cuentas disponibles", color = TextSecondary, fontSize = 13.sp)
    } else {
        Column {
            Text("Desliza para ver todas las cuentas", color = TextSecondary, fontSize = 11.sp)
            Spacer(Modifier.height(6.dp))
        LazyRow(state = listState, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            items(orderedAccounts, key = { it.id }) { account ->
                val selected = account.id == selectedId
                val colors = when (account.type) {
                    "TRANSPORT" -> listOf(Color(0xFF263A73), Color(0xFF252353), Color(0xFF143C46))
                    "WALLET" -> listOf(Color(0xFF742396), Color(0xFF531377), Color(0xFF3D145F))
                    "CASH" -> listOf(Color(0xFF1D5744), Color(0xFF164438), Color(0xFF123A31))
                    else -> listOf(Color(0xFF263441), Color(0xFF18232B))
                }
                val type = when (account.type) {
                    "TRANSPORT" -> "Transporte"
                    "WALLET" -> "Billetera digital"
                    "CASH" -> "Efectivo"
                    "BANK" -> "Banco"
                    else -> "Otra cuenta"
                }
                Box(
                    Modifier.width(188.dp).aspectRatio(1.68f).clip(RoundedCornerShape(12.dp))
                        .background(Brush.linearGradient(colors))
                        .border(if (selected) 1.dp else 0.dp, if (selected) Color.White.copy(alpha = 0.7f) else Color.Transparent, RoundedCornerShape(12.dp))
                        .clickable { onSelect(account.id) }
                ) {
                    Canvas(Modifier.matchParentSize()) {
                        val accent = Color.White.copy(alpha = 0.08f)
                        when (account.type) {
                            "WALLET" -> drawLine(accent, Offset(size.width * 0.65f, size.height), Offset(size.width, 0f), strokeWidth = 16.dp.toPx())
                            "TRANSPORT" -> drawCircle(accent, size.width * 0.32f, Offset(size.width, size.height))
                            else -> drawCircle(accent, size.height * 0.25f, Offset(size.width * 0.82f, size.height * 0.72f), style = Stroke(width = 1.dp.toPx()))
                        }
                    }
                    if (selected) {
                        Box(
                            Modifier.align(Alignment.TopEnd).padding(4.dp).size(19.dp)
                                .clip(RoundedCornerShape(50)).background(Color.White.copy(alpha = 0.9f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Filled.Check, contentDescription = "Cuenta seleccionada", tint = Color(0xFF263441), modifier = Modifier.size(14.dp))
                        }
                    }
                    Column(Modifier.fillMaxSize().padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(GastifyIcons.account(account.type), contentDescription = null, tint = Color.White, modifier = Modifier.size(22.dp))
                            Spacer(Modifier.width(8.dp))
                            Column(Modifier.weight(1f)) {
                                Text(account.name, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(type, color = Color.White.copy(alpha = 0.7f), fontSize = 10.sp, maxLines = 1)
                            }
                            Icon(
                                imageVector = if (account.id == preferredAccountId) Icons.Filled.Star else Icons.Outlined.StarOutline,
                                contentDescription = if (account.id == preferredAccountId) "Cuenta preferida" else "Cuenta no preferida",
                                tint = if (account.id == preferredAccountId) Color(0xFFFFD66B) else Color.White.copy(alpha = 0.65f),
                                modifier = Modifier.padding(end = if (selected) 14.dp else 0.dp).size(17.dp)
                            )
                        }
                        Spacer(Modifier.weight(1f))
                        Text(if (account.type == "CASH") "Saldo disponible" else "Saldo estimado", color = Color.White.copy(alpha = 0.7f), fontSize = 10.sp)
                        Text(reminderMoney(account.balanceCents), color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                    }
                }
            }
        }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReminderTimeSelector(hour: Int, minute: Int, onHourChange: (Int) -> Unit, onMinuteChange: (Int) -> Unit) {
    var showPicker by remember { mutableStateOf(false) }
    Surface(
        onClick = { showPicker = true },
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = Color.White.copy(alpha = 0.06f)
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.NotificationsActive, contentDescription = null, tint = PrimaryGreen)
            Spacer(Modifier.width(12.dp))
            Text(reminderTime(hour, minute), modifier = Modifier.weight(1f), color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Text("Cambiar", color = PrimaryGreen, fontSize = 13.sp)
        }
    }
    if (showPicker) {
        val pickerState = rememberTimePickerState(initialHour = hour, initialMinute = minute, is24Hour = false)
        AlertDialog(
            onDismissRequest = { showPicker = false },
            containerColor = SurfaceDark,
            title = { Text("Seleccionar hora") },
            text = {
                TimeInput(
                    state = pickerState,
                    colors = TimePickerDefaults.colors(
                        timeSelectorSelectedContainerColor = Color(0xFFD7D7D7),
                        timeSelectorUnselectedContainerColor = Color(0xFF4A4A4A),
                        timeSelectorSelectedContentColor = Color(0xFF202020),
                        timeSelectorUnselectedContentColor = Color.White,
                        periodSelectorSelectedContainerColor = Color(0xFFD7D7D7),
                        periodSelectorUnselectedContainerColor = Color(0xFF4A4A4A),
                        periodSelectorSelectedContentColor = Color(0xFF202020),
                        periodSelectorUnselectedContentColor = Color.White,
                        periodSelectorBorderColor = Color(0xFF8A8A8A),
                        selectorColor = Color(0xFF9B9B9B)
                    )
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    onHourChange(pickerState.hour)
                    onMinuteChange(pickerState.minute)
                    showPicker = false
                }) { Text("Aceptar", color = PrimaryGreen) }
            },
            dismissButton = { TextButton(onClick = { showPicker = false }) { Text("Cancelar") } }
        )
    }
}
private data class ReminderOption(val id: String, val name: String, val icon: ImageVector, val tint: Color)

@Composable
private fun ReminderCategorySelector(selectedId: String, options: List<ReminderOption>, onSelect: (String) -> Unit) {
    if (options.isEmpty()) {
        Text("No hay categorías de gasto disponibles", color = TextSecondary, fontSize = 13.sp)
    } else {
        Column {
            Text("Desliza para ver todas las categorías de gasto", color = TextSecondary, fontSize = 11.sp)
            Spacer(Modifier.height(6.dp))
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                options.forEach { option ->
                    val selected = option.id == selectedId
                    Row(
                        Modifier.clip(RoundedCornerShape(14.dp))
                            .background(Color.White.copy(alpha = 0.06f))
                            .border(1.dp, if (selected) Color.White else Color.Transparent, RoundedCornerShape(14.dp))
                            .clickable { onSelect(option.id) }.padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(option.tint.copy(alpha = 0.14f)), contentAlignment = Alignment.Center) {
                            Icon(option.icon, contentDescription = null, tint = option.tint, modifier = Modifier.size(22.dp))
                        }
                        Spacer(Modifier.width(8.dp))
                        Text(option.name, color = Color.White, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.widthIn(max = 120.dp))
                    }
                }
            }
        }
    }
}