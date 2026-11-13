package com.example.gastify.data.model

enum class BudgetPeriod {
    DAILY,
    WEEKLY,
    MONTHLY
}

data class Budget(
    val id: String = "",
    val userId: String = "",
    val workspaceId: String = "",
    val categoryId: String = "",
    val period: String = BudgetPeriod.MONTHLY.name,
    val limitCents: Long = 0L
)
