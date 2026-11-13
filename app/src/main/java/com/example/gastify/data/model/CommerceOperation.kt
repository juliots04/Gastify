package com.example.gastify.data.model

import com.google.firebase.Timestamp

enum class CommerceOperationType {
    PURCHASE,
    SALE
}

data class CommerceRequest(
    val type: CommerceOperationType,
    val productId: String,
    val accountId: String,
    val categoryId: String,
    val quantity: Int,
    val unitPriceCents: Long,
    val note: String = ""
)

data class CommerceOperation(
    val id: String = "",
    val type: String = CommerceOperationType.SALE.name,
    val productId: String = "",
    val productName: String = "",
    val productSku: String = "",
    val accountId: String = "",
    val categoryId: String = "",
    val quantity: Int = 0,
    val unitPriceCents: Long = 0L,
    val totalCents: Long = 0L,
    val previousQuantity: Int = 0,
    val resultingQuantity: Int = 0,
    val previousBalanceCents: Long = 0L,
    val balanceAfterCents: Long = 0L,
    val note: String = "",
    val createdAt: Timestamp? = null,

    // Fase 16:
    // Costo del producto cuando se registró
    // la operación. Null en registros antiguos.

    val historicalUnitCostCents: Long? = null,
    val historicalTotalCostCents: Long? = null
) {

    val hasHistoricalCost: Boolean
        get() =
            historicalUnitCostCents != null &&
                historicalTotalCostCents != null &&
                historicalUnitCostCents > 0L

    val historicalGrossProfitCents: Long?
        get() {

            if (
                type != CommerceOperationType.SALE.name ||
                !hasHistoricalCost
            ) {
                return null
            }

            return totalCents -
                (historicalTotalCostCents ?: 0L)
        }
}
