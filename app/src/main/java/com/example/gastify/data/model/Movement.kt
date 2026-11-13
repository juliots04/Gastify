package com.example.gastify.data.model

import com.google.firebase.Timestamp

enum class MovementType {
    INCOME,
    EXPENSE
}

data class Movement(

    val id: String = "",

    val accountId: String = "",

    val categoryId: String = "",

    val type: String = MovementType.EXPENSE.name,

    val amountCents: Long = 0L,

    val note: String = "",

    val previousBalanceCents: Long = 0L,

    val balanceAfterCents: Long = 0L,

    val createdAt: Timestamp? = null
)
