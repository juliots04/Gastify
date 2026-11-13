package com.example.gastify.data.model

import com.google.firebase.Timestamp

enum class CategoryType {
    INCOME,
    EXPENSE
}

data class Category(
    val id: String = "",
    val name: String = "",
    val type: String = CategoryType.EXPENSE.name,
    val icon: String = "💸",
    val color: String = "#00E0A4",
    val createdAt: Timestamp? = null
)
