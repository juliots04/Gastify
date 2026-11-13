package com.example.gastify.ui.components

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.NotificationsActive

import androidx.compose.material3.*

import androidx.compose.runtime.*

import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat

import com.example.gastify.data.local.NotificationPreferencesStore

import com.example.gastify.notifications.BudgetAlertWorker
import com.example.gastify.notifications.ReminderScheduler
import com.example.gastify.notifications.StockAlertWorker

import com.example.gastify.ui.theme.*

private fun notificationsAllowed(
    context: Context
): Boolean {

    if (
        Build.VERSION.SDK_INT >=
        Build.VERSION_CODES.TIRAMISU
    ) {

        val permission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS
        )

        if (
            permission !=
            PackageManager.PERMISSION_GRANTED
        ) {
            return false
        }
    }

    return NotificationManagerCompat
        .from(context)
        .areNotificationsEnabled()
}

@Composable
fun NotificationSettingsSection(
    userId: String
) {

    val context = LocalContext.current

    val store = remember(
        context,
        userId
    ) {
        NotificationPreferencesStore(
            context,
            userId
        )
    }

    var settings by remember(
        userId
    ) {
        mutableStateOf(
            store.read()
        )
    }

    var permissionGranted by remember {
        mutableStateOf(
            notificationsAllowed(context)
        )
    }

    var errorMessage by remember {
        mutableStateOf<String?>(null)
    }

    // ==========================================
    // SOLICITAR PERMISO
    // ==========================================

    val permissionLauncher =
        rememberLauncherForActivityResult(
            contract =
                ActivityResultContracts.RequestPermission()
        ) { granted ->

            permissionGranted = granted &&
                notificationsAllowed(context)

            if (permissionGranted) {

                if (settings.budgetsEnabled) {

                    BudgetAlertWorker.ensureScheduled(
                        context
                    )

                    BudgetAlertWorker.checkNow(
                        context
                    )
                }

                if (settings.stockEnabled) {

                    StockAlertWorker.ensureScheduled(
                        context
                    )

                    StockAlertWorker.checkNow(
                        context
                    )
                }
            }
        }

    // ==========================================
    // CAMBIAR RECORDATORIOS
    // ==========================================

    fun changeReminders(
        enabled: Boolean
    ) {

        val saved = store.setRemindersEnabled(
            enabled
        )

        if (!saved) {

            errorMessage =
                "No se pudo guardar la configuración."

            return
        }

        settings = settings.copy(
            remindersEnabled = enabled
        )

        errorMessage = null

        if (enabled) {

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
    }

    // ==========================================
    // CAMBIAR PRESUPUESTOS
    // ==========================================

    fun changeBudgets(
        enabled: Boolean
    ) {

        val saved = store.setBudgetsEnabled(
            enabled
        )

        if (!saved) {

            errorMessage =
                "No se pudo guardar la configuración."

            return
        }

        settings = settings.copy(
            budgetsEnabled = enabled
        )

        errorMessage = null

        if (enabled) {

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
    }

    // ==========================================
    // CAMBIAR INVENTARIO
    // ==========================================

    fun changeStock(
        enabled: Boolean
    ) {

        val saved = store.setStockEnabled(
            enabled
        )

        if (!saved) {

            errorMessage =
                "No se pudo guardar la configuración."

            return
        }

        settings = settings.copy(
            stockEnabled = enabled
        )

        errorMessage = null

        if (enabled) {

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

    // ==========================================
    // ABRIR AJUSTES DE ANDROID
    // ==========================================

    fun openAndroidNotificationSettings() {

        val intent = Intent(
            Settings.ACTION_APP_NOTIFICATION_SETTINGS
        ).apply {

            putExtra(
                Settings.EXTRA_APP_PACKAGE,
                context.packageName
            )
        }

        context.startActivity(intent)
    }

    // ==========================================
    // INTERFAZ
    // ==========================================

    Column(
        modifier = Modifier.fillMaxWidth()
    ) {

        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {

            Icon(
                imageVector =
                    Icons.Outlined.NotificationsActive,

                contentDescription = null,

                tint = PrimaryGreen,

                modifier = Modifier.size(25.dp)
            )

            Spacer(
                Modifier.width(10.dp)
            )

            Text(
                text = "Notificaciones",

                fontSize = 20.sp,

                fontWeight = FontWeight.Bold,

                color = TextPrimary
            )
        }

        Spacer(
            Modifier.height(8.dp)
        )

        Text(
            text = "Selecciona las alertas que deseas recibir.",

            fontSize = 13.sp,

            color = TextSecondary
        )

        Spacer(
            Modifier.height(18.dp)
        )

        // ======================================
        // ESTADO DEL PERMISO
        // ======================================

        if (!permissionGranted) {

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
                        text = "Notificaciones bloqueadas",

                        fontWeight = FontWeight.SemiBold,

                        color = ExpenseRed
                    )

                    Spacer(
                        Modifier.height(8.dp)
                    )

                    Text(
                        text = "Android no está permitiendo mostrar notificaciones de GASTIFY. Tus preferencias pueden estar activadas, pero no recibirás los avisos hasta autorizar el permiso.",

                        color = TextSecondary,

                        fontSize = 12.sp,

                        lineHeight = 18.sp
                    )

                    Spacer(
                        Modifier.height(14.dp)
                    )

                    Button(
                        onClick = {

                            if (
                                Build.VERSION.SDK_INT >=
                                Build.VERSION_CODES.TIRAMISU &&
                                ContextCompat.checkSelfPermission(
                                    context,
                                    Manifest.permission.POST_NOTIFICATIONS
                                ) != PackageManager.PERMISSION_GRANTED
                            ) {

                                permissionLauncher.launch(
                                    Manifest.permission.POST_NOTIFICATIONS
                                )

                            } else {

                                openAndroidNotificationSettings()
                            }
                        },

                        colors = ButtonDefaults.buttonColors(
                            containerColor = PrimaryGreen,
                            contentColor = AmoledBlack
                        )
                    ) {

                        Text(
                            text = "Permitir notificaciones"
                        )
                    }
                }
            }

            Spacer(
                Modifier.height(14.dp)
            )
        }

        // ======================================
        // RECORDATORIOS
        // ======================================

        NotificationPreferenceCard(
            title = "Recordatorios",

            description = "Avisos programados para tus pasajes, pagos y otros gastos.",

            icon = Icons.Outlined.NotificationsActive,

            checked = settings.remindersEnabled,

            onCheckedChange = {
                changeReminders(it)
            }
        )

        Spacer(
            Modifier.height(12.dp)
        )

        // ======================================
        // PRESUPUESTOS
        // ======================================

        NotificationPreferenceCard(
            title = "Alertas de presupuestos",

            description = "Avisos cuando tus gastos alcancen el 80%, 90% o 100% del presupuesto.",

            icon = Icons.Outlined.AccountBalanceWallet,

            checked = settings.budgetsEnabled,

            onCheckedChange = {
                changeBudgets(it)
            }
        )

        Spacer(
            Modifier.height(12.dp)
        )

        // ======================================
        // INVENTARIO
        // ======================================

        NotificationPreferenceCard(
            title = "Alertas de inventario",

            description = "Notificaciones cuando un producto tenga stock bajo o se agote.",

            icon = Icons.Outlined.Inventory2,

            checked = settings.stockEnabled,

            onCheckedChange = {
                changeStock(it)
            }
        )

        // ======================================
        // ERRORES
        // ======================================

        if (errorMessage != null) {

            Spacer(
                Modifier.height(12.dp)
            )

            Text(
                text = errorMessage.orEmpty(),

                color = ExpenseRed,

                fontSize = 12.sp
            )
        }

        Spacer(
            Modifier.height(16.dp)
        )

        Text(
            text = "Los cambios se guardan automáticamente. Las alertas en segundo plano pueden retrasarse por las restricciones de batería de Android.",

            color = TextSecondary,

            fontSize = 12.sp,

            lineHeight = 19.sp
        )
    }
}

// ==========================================
// TARJETA CON INTERRUPTOR
// ==========================================

@Composable
private fun NotificationPreferenceCard(
    title: String,
    description: String,
    icon: ImageVector,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
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
                shape = RoundedCornerShape(12.dp),

                color = CardDark
            ) {

                Box(
                    modifier = Modifier.size(44.dp),

                    contentAlignment = Alignment.Center
                ) {

                    Icon(
                        imageVector = icon,

                        contentDescription = null,

                        tint = PrimaryGreen,

                        modifier = Modifier.size(23.dp)
                    )
                }
            }

            Spacer(
                Modifier.width(12.dp)
            )

            Column(
                modifier = Modifier.weight(1f)
            ) {

                Text(
                    text = title,

                    fontSize = 15.sp,

                    fontWeight = FontWeight.SemiBold,

                    color = TextPrimary
                )

                Spacer(
                    Modifier.height(6.dp)
                )

                Text(
                    text = description,

                    fontSize = 12.sp,

                    lineHeight = 18.sp,

                    color = TextSecondary
                )
            }

            Spacer(
                Modifier.width(8.dp)
            )

            Switch(
                checked = checked,

                onCheckedChange = onCheckedChange,

                colors = SwitchDefaults.colors(
                    checkedThumbColor = AmoledBlack,
                    checkedTrackColor = PrimaryGreen,
                    uncheckedThumbColor = TextSecondary,
                    uncheckedTrackColor = CardDark
                )
            )
        }
    }
}
