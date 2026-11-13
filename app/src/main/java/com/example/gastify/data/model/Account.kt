package com.example.gastify.data.model

import com.google.firebase.Timestamp

enum class AccountType {
    CASH,
    BANK,
    WALLET,
    TRANSPORT,
    OTHER
}

data class Account(
    val id: String = "",
    val name: String = "",
    val type: String = AccountType.CASH.name,
    val openingBalanceCents: Long = 0L,
    val balanceCents: Long = 0L,
    val createdAt: Timestamp? = null
)
