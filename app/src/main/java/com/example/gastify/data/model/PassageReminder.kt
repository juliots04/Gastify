package com.example.gastify.data.model

data class PassageReminder(

    val id: String = "",

    val userId: String = "",

    val workspaceId: String = "",

    val accountId: String = "",

    val categoryId: String = "",

    val title: String = "",

    val amountCents: Long = 280L,

    val hour: Int = 7,

    val minute: Int = 30,

    // 1 = Lunes, 7 = Domingo
    val daysOfWeek: Set<Int> = setOf(
        1, 2, 3, 4, 5, 6
    ),

    val enabled: Boolean = true
)
