package com.example.gastify.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.ui.graphics.vector.ImageVector

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
    val logout: ImageVector = Icons.Outlined.Logout
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

    CategoryIconOption("bus", "Autobús"),

    CategoryIconOption("taxi", "Taxi"),

    CategoryIconOption("car", "Auto"),

    CategoryIconOption("food", "Comida"),

    CategoryIconOption("shopping", "Compras"),

    CategoryIconOption("bills", "Servicios"),

    CategoryIconOption("work", "Trabajo"),

    CategoryIconOption("money", "Dinero"),

    CategoryIconOption("stock", "Inventario"),

    CategoryIconOption("home", "Hogar"),

    CategoryIconOption("receipt", "Recibos"),

    CategoryIconOption("gaming", "Juegos"),

    CategoryIconOption("health", "Salud"),

    CategoryIconOption("bank", "Banco"),

    CategoryIconOption("wallet", "Billetera"),

    CategoryIconOption("savings", "Ahorros")
)
