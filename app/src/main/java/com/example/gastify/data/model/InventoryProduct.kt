package com.example.gastify.data.model

import com.google.firebase.Timestamp

data class InventoryProduct(
    val id: String = "",
    val name: String = "",
    val sku: String = "",
    val description: String = "",
    val quantity: Int = 0,
    val minQuantity: Int = 0,
    val costCents: Long = 0L,
    val priceCents: Long = 0L,
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
) {
    val isOutOfStock: Boolean
        get() = quantity == 0

    val isLowStock: Boolean
        get() = quantity > 0 && quantity <= minQuantity
}
