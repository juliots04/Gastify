package com.example.gastify.data.repository

import com.example.gastify.data.model.Movement

import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query

import java.util.Date

class StatisticsRepository(
    userId: String,
    workspaceId: String
) {

    private val collection = FirebaseFirestore
        .getInstance()
        .collection("users")
        .document(userId)
        .collection("workspaces")
        .document(workspaceId)
        .collection("movements")

    fun listenToMonth(
        startMillis: Long,
        endMillis: Long,
        onSuccess: (List<Movement>) -> Unit,
        onError: (Exception) -> Unit
    ): ListenerRegistration {

        require(endMillis > startMillis) {
            "El período seleccionado no es válido."
        }

        val startDate = Timestamp(Date(startMillis))
        val endDate = Timestamp(Date(endMillis))

        return collection
            .whereGreaterThanOrEqualTo(
                "createdAt",
                startDate
            )
            .whereLessThan(
                "createdAt",
                endDate
            )
            .orderBy(
                "createdAt",
                Query.Direction.DESCENDING
            )
            .addSnapshotListener { snapshot, error ->

                if (error != null) {

                    onError(error)

                    return@addSnapshotListener
                }

                val movements = snapshot
                    ?.documents
                    ?.mapNotNull { document ->

                        val accountId = document
                            .getString("accountId")
                            ?: return@mapNotNull null

                        val categoryId = document
                            .getString("categoryId")
                            ?: return@mapNotNull null

                        val type = document
                            .getString("type")
                            ?: return@mapNotNull null

                        val amount = document
                            .getLong("amountCents")
                            ?: return@mapNotNull null

                        if (
                            type !in listOf("INCOME", "EXPENSE") ||
                            amount <= 0L
                        ) {

                            return@mapNotNull null
                        }

                        Movement(
                            id = document.id,

                            accountId = accountId,

                            categoryId = categoryId,

                            type = type,

                            amountCents = amount,

                            note = document
                                .getString("note")
                                ?: "",

                            previousBalanceCents = document
                                .getLong("previousBalanceCents")
                                ?: 0L,

                            balanceAfterCents = document
                                .getLong("balanceAfterCents")
                                ?: 0L,

                            createdAt = document
                                .getTimestamp("createdAt")
                        )
                    }
                    ?: emptyList()

                onSuccess(movements)
            }
    }
}
