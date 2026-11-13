package com.example.gastify.data.repository

import com.example.gastify.data.model.Movement
import com.example.gastify.data.model.MovementType

import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query

import kotlinx.coroutines.suspendCancellableCoroutine

import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class MovementRepository(
    userId: String,
    workspaceId: String
) {

    private val database = FirebaseFirestore.getInstance()

    private val workspaceDocument = database
        .collection("users")
        .document(userId)
        .collection("workspaces")
        .document(workspaceId)

    private val movementCollection = workspaceDocument
        .collection("movements")

    private val accountCollection = workspaceDocument
        .collection("accounts")

    private val categoryCollection = workspaceDocument
        .collection("categories")

    companion object {
        const val MAX_AMOUNT_CENTS = 999_999_999_999L
    }

    // ========================================
    // CONSULTAR MOVIMIENTOS
    // ========================================

    fun listenToMovements(
        onSuccess: (List<Movement>) -> Unit,
        onError: (Exception) -> Unit
    ): ListenerRegistration {

        return movementCollection
            .orderBy(
                "createdAt",
                Query.Direction.DESCENDING
            )
            .limit(100)
            .addSnapshotListener { snapshot, error ->

                if (error != null) {
                    onError(error)
                    return@addSnapshotListener
                }

                val movements = snapshot?.documents
                    ?.mapNotNull { document ->

                        val accountId =
                            document.getString("accountId")
                                ?: return@mapNotNull null

                        val categoryId =
                            document.getString("categoryId")
                                ?: return@mapNotNull null

                        Movement(
                            id = document.id,

                            accountId = accountId,

                            categoryId = categoryId,

                            type = document.getString("type")
                                ?: MovementType.EXPENSE.name,

                            amountCents =
                                document.getLong("amountCents")
                                    ?: 0L,

                            note = document.getString("note")
                                ?: "",

                            previousBalanceCents =
                                document.getLong(
                                    "previousBalanceCents"
                                ) ?: 0L,

                            balanceAfterCents =
                                document.getLong(
                                    "balanceAfterCents"
                                ) ?: 0L,

                            createdAt =
                                document.getTimestamp("createdAt")
                        )
                    }
                    ?: emptyList()

                onSuccess(movements)
            }
    }

    // ========================================
    // REGISTRAR MOVIMIENTO
    // ========================================

    suspend fun createMovement(
        accountId: String,
        categoryId: String,
        type: String,
        amountCents: Long,
        note: String
    ) {

        // VALIDACIONES

        require(accountId.isNotBlank()) {
            "Selecciona una cuenta."
        }

        require(categoryId.isNotBlank()) {
            "Selecciona una categoría."
        }

        require(
            MovementType.entries.any {
                it.name == type
            }
        ) {
            "Tipo de movimiento inválido."
        }

        require(
            amountCents in 1L..MAX_AMOUNT_CENTS
        ) {
            "Introduce un importe válido."
        }

        require(note.length <= 200) {
            "La descripción no puede superar 200 caracteres."
        }

        // REFERENCIAS FIREBASE

        val accountDocument = accountCollection
            .document(accountId)

        val categoryDocument = categoryCollection
            .document(categoryId)

        val movementDocument = movementCollection
            .document()

        // TRANSACCIÓN ATÓMICA

        val task = database.runTransaction { transaction ->

            // Primero leer todos los documentos.

            val accountSnapshot = transaction.get(
                accountDocument
            )

            val categorySnapshot = transaction.get(
                categoryDocument
            )

            // VALIDAR CUENTA

            if (!accountSnapshot.exists()) {

                throw IllegalStateException(
                    "La cuenta seleccionada no existe."
                )
            }

            // VALIDAR CATEGORÍA

            if (!categorySnapshot.exists()) {

                throw IllegalStateException(
                    "La categoría seleccionada no existe."
                )
            }

            val categoryType = categorySnapshot
                .getString("type")

            if (categoryType != type) {

                throw IllegalStateException(
                    "La categoría no corresponde al tipo de movimiento."
                )
            }

            // OBTENER SALDO ACTUAL

            val currentBalance = accountSnapshot
                .getLong("balanceCents")
                ?: throw IllegalStateException(
                    "La cuenta no tiene un saldo válido."
                )

            // CALCULAR NUEVO SALDO

            val newBalance = when (type) {

                MovementType.INCOME.name -> {

                    Math.addExact(
                        currentBalance,
                        amountCents
                    )
                }

                MovementType.EXPENSE.name -> {

                    Math.subtractExact(
                        currentBalance,
                        amountCents
                    )
                }

                else -> {

                    throw IllegalStateException(
                        "Tipo de movimiento inválido."
                    )
                }
            }

            // EVITAR SALDOS NEGATIVOS

            if (newBalance < 0L) {

                throw IllegalStateException(
                    "Saldo insuficiente en la cuenta."
                )
            }

            // VALIDAR LÍMITE

            if (
                newBalance > MAX_AMOUNT_CENTS
            ) {

                throw IllegalStateException(
                    "El saldo supera el límite permitido."
                )
            }

            // ACTUALIZAR CUENTA

            transaction.update(
                accountDocument,

                mapOf(
                    "balanceCents" to newBalance,
                    "lastMovementId" to movementDocument.id
                )
            )

            // DATOS DEL MOVIMIENTO

            val movementData = hashMapOf<String, Any>(

                "accountId" to accountId,

                "categoryId" to categoryId,

                "type" to type,

                "amountCents" to amountCents,

                "note" to note.trim(),

                "previousBalanceCents" to currentBalance,

                "balanceAfterCents" to newBalance,

                "createdAt" to FieldValue.serverTimestamp()
            )

            // REGISTRAR MOVIMIENTO

            transaction.set(
                movementDocument,
                movementData
            )

            Unit
        }

        // ESPERAR CONFIRMACIÓN

        suspendCancellableCoroutine<Unit> { continuation ->

            task.addOnSuccessListener {

                if (continuation.isActive) {

                    continuation.resume(Unit)
                }
            }

            task.addOnFailureListener { exception ->

                if (continuation.isActive) {

                    continuation.resumeWithException(
                        exception
                    )
                }
            }
        }
    }
}
