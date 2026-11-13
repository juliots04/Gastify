package com.example.gastify.data.model

data class QuickAction(
    val id: String = "",
    val userId: String = "",
    val workspaceId: String = "",
    val name: String = "",
    val accountId: String = "",
    val categoryId: String = "",
    val type: String = MovementType.EXPENSE.name,
    val amountCents: Long = 280L,
    val note: String = ""
)
