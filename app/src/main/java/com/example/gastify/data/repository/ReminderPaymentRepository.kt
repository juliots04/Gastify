package com.example.gastify.data.repository

import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore

import kotlinx.coroutines.suspendCancellableCoroutine

import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class ReminderPaymentRepository(
    private val userId: String,
    private val workspaceId: String
) {

    private val database = FirebaseFirestore.getInstance()

    private val workspace = database
        .collection("users")
        .document(userId)
        .collection("workspaces")
        .document(workspaceId)

    suspend fun confirmPayment(
        reminderId: String,
        triggerTime: Long,
        accountId: String,
        categoryId: String,
        amountCents: Long,
        title: String
    ) {

        require(
            reminderId.matches(
                Regex("[a-zA-Z0-9_-]{1,128}")
            )
        ) {
            "Identificador del recordatorio inválido."
        }

        require(triggerTime > 0L) {
            "Fecha del recordatorio inválida."
        }

        require(accountId.isNotBlank()) {
            "Cuenta inválida."
        }

        require(categoryId.isNotBlank()) {
            "Categoría inválida."
        }

        require(amountCents in 1L..999_999_999_999L) {
            "Importe inválido."
        }

        val movementId =
            "rem_${reminderId}_${triggerTime}"

        val accountRef = workspace
            .collection("accounts")
            .document(accountId)

        val categoryRef = workspace
            .collection("categories")
            .document(categoryId)

        val movementRef = workspace
            .collection("movements")
            .document(movementId)

        val task = database.runTransaction { transaction ->

            // Primero comprobar si este aviso ya fue registrado.

            val existingMovement = transaction.get(
                movementRef
            )

            if (existingMovement.exists()) {

                // Ya fue procesado: no descontar otra vez.
                return@runTransaction Unit
            }

            // Todas las lecturas ocurren antes de escribir.

            val account = transaction.get(
                accountRef
            )

            val category = transaction.get(
                categoryRef
            )

            if (!account.exists()) {

                throw IllegalStateException(
                    "La cuenta del recordatorio ya no existe."
                )
            }

            if (!category.exists()) {

                throw IllegalStateException(
                    "La categoría del recordatorio ya no existe."
                )
            }

            if (
                category.getString("type") != "EXPENSE"
            ) {

                throw IllegalStateException(
                    "La categoría debe ser de tipo Gasto."
                )
            }

            val previousBalance = account
                .getLong("balanceCents")
                ?: throw IllegalStateException(
                    "La cuenta no tiene un saldo válido."
                )

            val newBalance = Math.subtractExact(
                previousBalance,
                amountCents
            )

            if (newBalance < 0L) {

                throw IllegalStateException(
                    "Saldo insuficiente para registrar el pasaje."
                )
            }

            val movementData = hashMapOf<String, Any>(

                "accountId" to accountId,

                "categoryId" to categoryId,

                "type" to "EXPENSE",

                "amountCents" to amountCents,

                "note" to (
                    "Pasaje confirmado: $title"
                ).take(200),

                "previousBalanceCents" to previousBalance,

                "balanceAfterCents" to newBalance,

                "createdAt" to FieldValue.serverTimestamp()
            )

            transaction.update(
                accountRef,
                mapOf(
                    "balanceCents" to newBalance,
                    "lastMovementId" to movementId
                )
            )

            transaction.set(
                movementRef,
                movementData
            )

            Unit
        }

        suspendCancellableCoroutine<Unit> { continuation ->

            task.addOnSuccessListener {

                if (continuation.isActive) {
                    continuation.resume(Unit)
                }
            }

            task.addOnFailureListener { exception ->

                if (continuation.isActive) {
                    continuation.resumeWithException(exception)
                }
            }
        }
    }
}
