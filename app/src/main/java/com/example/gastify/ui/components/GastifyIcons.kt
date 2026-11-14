package com.example.gastify.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

object GastifyIcons {

    // NAVEGACIÓN

    val home: ImageVector = Icons.Outlined.Home
    val movements: ImageVector = Icons.Outlined.SwapHoriz
    val workspaces: ImageVector = Icons.Outlined.GridView
    val statistics: ImageVector = Icons.Outlined.BarChart
    val settings: ImageVector = Icons.Outlined.Settings

    // ACCIONES

    val add: ImageVector = Icons.Outlined.Add
    val back: ImageVector = Icons.AutoMirrored.Outlined.ArrowBack
    val next: ImageVector = Icons.Outlined.ChevronRight
    val edit: ImageVector = Icons.Outlined.Edit
    val delete: ImageVector = Icons.Outlined.DeleteOutline
    val logout: ImageVector = Icons.AutoMirrored.Outlined.Logout
    val expand: ImageVector = Icons.Outlined.ExpandMore
    val fingerprint: ImageVector = Icons.Outlined.Fingerprint

    // FINANZAS

    val accounts: ImageVector = Icons.Outlined.AccountBalanceWallet
    val categories: ImageVector = Icons.Outlined.Category
    val income: ImageVector = Icons.Outlined.ArrowDownward
    val expense: ImageVector = Icons.Outlined.ArrowUpward

    // TIPOS DE CUENTAS

    fun account(type: String): ImageVector {

        return when (type) {

            "CASH" -> Icons.Outlined.Payments

            "BANK" -> Icons.Outlined.AccountBalance

            "WALLET" -> Icons.Outlined.AccountBalanceWallet

            "TRANSPORT" -> Icons.Outlined.DirectionsBus

            else -> Icons.Outlined.CreditCard
        }
    }

    // CATEGORÍAS
    // También reconoce emojis antiguos almacenados en Firebase.

    fun category(value: String): ImageVector {

        return when (value) {

            "withdraw" -> Icons.Outlined.Atm
            "mobilepay" -> Icons.Outlined.PhoneAndroid
            "deposit" -> Icons.Outlined.AccountBalanceWallet
            "receive" -> Icons.Outlined.SouthWest
            "salary" -> Icons.Outlined.Payments
            "refund" -> Icons.Outlined.CurrencyExchange
            "gift" -> Icons.Outlined.Redeem
            "recharge" -> Icons.Outlined.AddCard
            "cardpay" -> Icons.Outlined.CreditCard
            "tap" -> Icons.Outlined.Contactless
            "transfer" -> Icons.Outlined.SwapHoriz
            "train" -> Icons.Outlined.Train
            "education" -> Icons.Outlined.School
            "bus", "🚌" -> Icons.Outlined.DirectionsBus

            "taxi", "🚕" -> Icons.Outlined.LocalTaxi

            "car" -> Icons.Outlined.DirectionsCar

            "food", "🍔" -> Icons.Outlined.Restaurant

            "shopping", "🛍️" -> Icons.Outlined.ShoppingCart

            "bills", "💡" -> Icons.Outlined.Lightbulb

            "work", "💼" -> Icons.Outlined.Work

            "money", "💰", "💸" -> Icons.Outlined.AttachMoney

            "stock", "📦" -> Icons.Outlined.Inventory

            "home", "🏠" -> Icons.Outlined.Home

            "receipt", "🧾" -> Icons.Outlined.Receipt

            "gaming", "🎮" -> Icons.Outlined.SportsEsports

            "health", "💊" -> Icons.Outlined.LocalHospital

            "bank" -> Icons.Outlined.AccountBalance

            "wallet" -> Icons.Outlined.AccountBalanceWallet

            "savings" -> Icons.Outlined.Savings

            else -> Icons.Outlined.Category
        }
    }
}

data class CategoryIconOption(
    val key: String,
    val label: String
)

val categoryIconOptions = listOf(
    CategoryIconOption("cardpay", "Pago con tarjeta"),
    CategoryIconOption("recharge", "Recarga de tarjeta"),
    CategoryIconOption("mobilepay", "Pago con Yape o Plin"),
    CategoryIconOption("bills", "Pago de servicios"),
    CategoryIconOption("bus", "Pago de pasaje"),
    CategoryIconOption("taxi", "Pago de taxi"),
    CategoryIconOption("food", "Pago de comida"),
    CategoryIconOption("money", "Pago en efectivo"),
    CategoryIconOption("receive", "Recibir dinero"),
    CategoryIconOption("salary", "Recibir sueldo"),
    CategoryIconOption("deposit", "Depósito de dinero"),
    CategoryIconOption("withdraw", "Retiro de efectivo"),
    CategoryIconOption("transfer", "Transferencia"),
    CategoryIconOption("tap", "Pago sin contacto"),
    CategoryIconOption("health", "Pago de salud"),
    CategoryIconOption("home", "Pago de vivienda"),
    CategoryIconOption("receipt", "Otros pagos"),
    CategoryIconOption("wallet", "Billetera digital"),
    CategoryIconOption("savings", "Ahorro"),
    CategoryIconOption("work", "Trabajo"),
    CategoryIconOption("stock", "Inventario"),
    CategoryIconOption("bank", "Cuenta bancaria"),
    CategoryIconOption("car", "Vehículo"),
    CategoryIconOption("gaming", "Entretenimiento")
)