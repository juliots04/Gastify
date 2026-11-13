package com.example.gastify.data.repository

import com.example.gastify.data.model.Account
import com.example.gastify.data.model.AccountType

import com.google.android.gms.tasks.Task

import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query

import kotlinx.coroutines.suspendCancellableCoroutine

import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class AccountRepository(
    userId: String,
    workspaceId: String
) {

    private val database = FirebaseFirestore.getInstance()

    private val collection = database
        .collection("users")
        .document(userId)
        .collection("workspaces")
        .document(workspaceId)
        .collection("accounts")

    companion object {
        const val MAX_BALANCE_CENTS = 999_999_999_999L
    }

    // ========================================
    // CONSULTAR CUENTAS EN TIEMPO REAL
    // ========================================

    fun listenToAccounts(
        onSuccess: (List<Account>) -> Unit,
        onError: (Exception) -> Unit
    ): ListenerRegistration {

        return collection
            .orderBy(
                "createdAt",
                Query.Direction.DESCENDING
            )
            .addSnapshotListener { snapshot, error ->

                if (error != null) {
                    onError(error)
                    return@addSnapshotListener
                }

                val accounts = snapshot?.documents
                    ?.mapNotNull { document ->

                        val name = document.getString("name")
                            ?: return@mapNotNull null

                        Account(
                            id = document.id,

                            name = name,

                            type = document.getString("type")
                                ?: AccountType.OTHER.name,

                            openingBalanceCents =
                                document.getLong("openingBalanceCents")
                                    ?: 0L,

                            balanceCents =
                                document.getLong("balanceCents")
                                    ?: 0L,

                            createdAt = document.getTimestamp(
                                "createdAt"
                            )
                        )
                    }
                    ?: emptyList()

                onSuccess(accounts)
            }
    }

    // ========================================
    // CREAR CUENTA
    // ========================================

    suspend fun createAccount(
        name: String,
        type: String,
        openingBalanceCents: Long
    ) {

        validateName(name)

        require(
            AccountType.entries.any {
                it.name == type
            }
        ) {
            "Tipo de cuenta inválido."
        }

        require(
            openingBalanceCents in 0L..MAX_BALANCE_CENTS
        ) {
            "El saldo inicial no es válido."
        }

        val data = hashMapOf<String, Any>(
            "name" to name.trim(),

            "type" to type,

            "openingBalanceCents" to openingBalanceCents,

            "balanceCents" to openingBalanceCents,

            "createdAt" to FieldValue.serverTimestamp()
        )

        awaitWrite(
            collection.add(data)
        )
    }

    // ========================================
    // RENOMBRAR CUENTA
    // ========================================

    suspend fun renameAccount(
        accountId: String,
        newName: String
    ) {

        require(accountId.isNotBlank()) {
            "Identificador de cuenta inválido."
        }

        validateName(newName)

        awaitWrite(
            collection
                .document(accountId)
                .update(
                    "name",
                    newName.trim()
                )
        )
    }

    // ========================================
    // ELIMINAR CUENTA
    // ========================================

    suspend fun deleteAccount(
        accountId: String
    ) {

        require(accountId.isNotBlank()) {
            "Identificador de cuenta inválido."
        }

        awaitWrite(
            collection
                .document(accountId)
                .delete()
        )
    }

    // ========================================
    // VALIDAR NOMBRE
    // ========================================

    private fun validateName(
        name: String
    ) {

        require(name.trim().isNotEmpty()) {
            "Introduce el nombre de la cuenta."
        }

        require(name.trim().length <= 60) {
            "El nombre no puede superar 60 caracteres."
        }
    }

    // ========================================
    // ESPERAR RESPUESTA DE FIREBASE
    // ========================================

    private suspend fun awaitWrite(
        task: Task<*>
    ) {

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
