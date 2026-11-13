package com.example.gastify.data.repository

import com.example.gastify.data.model.CommerceOperation
import com.example.gastify.data.model.CommerceOperationType

import com.google.firebase.Timestamp

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.MetadataChanges
import com.google.firebase.firestore.Query

import java.util.Date

class ProfitabilityRepository(
    userId: String,
    workspaceId: String
) {

    companion object {

        private const val MAX_AMOUNT_CENTS =
            999_999_999_999L

        private const val MAX_QUANTITY =
            1_000_000L
    }

    private val collection = FirebaseFirestore
        .getInstance()
        .collection("users")
        .document(userId)
        .collection("workspaces")
        .document(workspaceId)
        .collection("commerceOperations")

    fun listenMonth(
        startMillis: Long,
        endMillis: Long,
        onSuccess: (
            List<CommerceOperation>,
            Boolean
        ) -> Unit,
        onError: (Exception) -> Unit
    ): ListenerRegistration {

        require(startMillis < endMillis) {
            "El período no es válido."
        }

        val start = Timestamp(
            Date(startMillis)
        )

        val end = Timestamp(
            Date(endMillis)
        )

        return collection
            .whereGreaterThanOrEqualTo(
                "createdAt",
                start
            )
            .whereLessThan(
                "createdAt",
                end
            )
            .orderBy(
                "createdAt",
                Query.Direction.DESCENDING
            )
            .addSnapshotListener(
                MetadataChanges.INCLUDE
            ) { snapshot, error ->

                if (error != null) {

                    onError(error)

                    return@addSnapshotListener
                }

                if (snapshot == null) {
                    return@addSnapshotListener
                }

                val operations = snapshot.documents
                    .mapNotNull { document ->

                        val type = document.getString(
                            "type"
                        ) ?: return@mapNotNull null

                        if (
                            CommerceOperationType.entries.none {
                                it.name == type
                            }
                        ) {
                            return@mapNotNull null
                        }

                        val productId = document.getString(
                            "productId"
                        ) ?: return@mapNotNull null

                        val productName = document.getString(
                            "productName"
                        ) ?: return@mapNotNull null

                        val quantity = document.getLong(
                            "quantity"
                        ) ?: return@mapNotNull null

                        val unitPrice = document.getLong(
                            "unitPriceCents"
                        ) ?: return@mapNotNull null

                        val total = document.getLong(
                            "totalCents"
                        ) ?: return@mapNotNull null

                        if (
                            productId.isBlank() ||
                            quantity !in 1L..MAX_QUANTITY ||
                            unitPrice !in 1L..MAX_AMOUNT_CENTS ||
                            total !in 1L..MAX_AMOUNT_CENTS
                        ) {
                            return@mapNotNull null
                        }

                        val expectedTotal = Math.multiplyExact(
                            quantity,
                            unitPrice
                        )

                        if (expectedTotal != total) {
                            return@mapNotNull null
                        }

                        // Las operaciones antiguas no
                        // contienen estos campos.

                        val historicalUnitCost =
                            document.getLong(
                                "historicalUnitCostCents"
                            )

                        val historicalTotalCost =
                            document.getLong(
                                "historicalTotalCostCents"
                            )

                        val validHistoricalCost =
                            historicalUnitCost != null &&
                                historicalTotalCost != null &&
                                historicalUnitCost in
                                    0L..MAX_AMOUNT_CENTS &&
                                historicalTotalCost ==
                                    historicalUnitCost * quantity

                        CommerceOperation(

                            id = document.id,

                            type = type,

                            productId = productId,

                            productName = productName,

                            productSku = document.getString(
                                "productSku"
                            ) ?: "",

                            accountId = document.getString(
                                "accountId"
                            ) ?: "",

                            categoryId = document.getString(
                                "categoryId"
                            ) ?: "",

                            quantity = quantity.toInt(),

                            unitPriceCents = unitPrice,

                            totalCents = total,

                            createdAt = document.getTimestamp(
                                "createdAt"
                            ),

                            historicalUnitCostCents =
                                if (validHistoricalCost) {
                                    historicalUnitCost
                                } else {
                                    null
                                },

                            historicalTotalCostCents =
                                if (validHistoricalCost) {
                                    historicalTotalCost
                                } else {
                                    null
                                }
                        )
                    }

                onSuccess(
                    operations,
                    snapshot.metadata.isFromCache
                )
            }
    }
}
