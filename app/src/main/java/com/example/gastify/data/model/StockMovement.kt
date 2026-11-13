package com.example.gastify.data.model

import com.google.firebase.Timestamp

enum class StockMovementType {
    ENTRY,
    EXIT,
    ADJUSTMENT
}

data class StockMovement(
    val id: String = "",
    val productId: String = "",
    val productName: String = "",
    val type: String = StockMovementType.ENTRY.name,
    val quantityDelta: Int = 0,
    val previousQuantity: Int = 0,
    val resultingQuantity: Int = 0,
    val note: String = "",
    val createdAt: Timestamp? = null
)
