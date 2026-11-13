package com.example.gastify.data.repository

import com.example.gastify.data.model.Budget
import com.example.gastify.data.model.BudgetPeriod

import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.MetadataChanges
import com.google.firebase.firestore.Source

class BudgetCloudRepository(
    private val userId: String,
    private val workspaceId: String
) {

    private val database = FirebaseFirestore.getInstance()

    private val collection = database
        .collection("users")
        .document(userId)
        .collection("workspaces")
        .document(workspaceId)
        .collection("budgets")

    companion object {

        private const val MAX_LIMIT_CENTS =
            999_999_999_999L
    }

    // ==========================================
    // IDENTIFICADOR ÚNICO POR PRESUPUESTO
    // ==========================================

    fun documentId(
        period: String,
        categoryId: String
    ): String {

        require(
            BudgetPeriod.entries.any {
                it.name == period
            }
        ) {
            "Período inválido."
        }

        require('/' !in categoryId) {
            "Categoría inválida."
        }

        return "b_${period}_${
            categoryId.ifEmpty { "ALL" }
        }"
    }

    // ==========================================
    // VALIDAR PRESUPUESTO
    // ==========================================

    private fun validate(
        budget: Budget
    ) {

        require(budget.userId == userId) {
            "El usuario no corresponde."
        }

        require(budget.workspaceId == workspaceId) {
            "El espacio no corresponde."
        }

        require(
            budget.limitCents in 1L..MAX_LIMIT_CENTS
        ) {
            "El límite no es válido."
        }

        documentId(
            budget.period,
            budget.categoryId
        )
    }

    // ==========================================
    // DATOS PARA FIREBASE
    // ==========================================

    private fun toFirestoreData(
        budget: Budget
    ): Map<String, Any> {

        return mapOf(
            "period" to budget.period,

            "categoryId" to budget.categoryId,

            "limitCents" to budget.limitCents,

            "updatedAt" to FieldValue.serverTimestamp()
        )
    }

    // ==========================================
    // ESCUCHAR EN TIEMPO REAL
    // ==========================================

    fun listen(
        onChange: (
            budgets: List<Budget>,
            fromCache: Boolean,
            hasPendingWrites: Boolean
        ) -> Unit,

        onError: (Exception) -> Unit
    ): ListenerRegistration {

        return collection.addSnapshotListener(
            MetadataChanges.INCLUDE
        ) { snapshot, error ->

            if (error != null) {

                onError(error)

                return@addSnapshotListener
            }

            if (snapshot == null) {
                return@addSnapshotListener
            }

            val budgets = snapshot.documents
                .mapNotNull { document ->

                    val period = document
                        .getString("period")
                        ?: return@mapNotNull null

                    val categoryId = document
                        .getString("categoryId")
                        ?: return@mapNotNull null

                    val limit = document
                        .getLong("limitCents")
                        ?: return@mapNotNull null

                    if (
                        BudgetPeriod.entries.none {
                            it.name == period
                        } ||
                        limit !in 1L..MAX_LIMIT_CENTS
                    ) {

                        return@mapNotNull null
                    }

                    Budget(
                        id = document.id,

                        userId = userId,

                        workspaceId = workspaceId,

                        categoryId = categoryId,

                        period = period,

                        limitCents = limit
                    )
                }

            onChange(
                budgets,
                snapshot.metadata.isFromCache,
                snapshot.metadata.hasPendingWrites()
            )
        }
    }

    // ==========================================
    // CREAR O ACTUALIZAR
    // ==========================================

    fun save(
        budget: Budget,
        onError: (Exception) -> Unit
    ) {

        validate(budget)

        val id = documentId(
            budget.period,
            budget.categoryId
        )

        collection.document(id)
            .set(
                toFirestoreData(budget)
            )
            .addOnFailureListener { exception ->

                onError(exception)
            }
    }

    // ==========================================
    // ELIMINAR
    // ==========================================

    fun delete(
        budget: Budget,
        onError: (Exception) -> Unit
    ) {

        validate(budget)

        collection.document(budget.id)
            .delete()
            .addOnFailureListener { exception ->

                onError(exception)
            }
    }

    // ==========================================
    // MIGRAR PRESUPUESTOS LOCALES
    // ==========================================

    fun migrateLegacy(
        localBudgets: List<Budget>,
        onSuccess: () -> Unit,
        onError: (Exception) -> Unit
    ) {

        // Si no existen presupuestos locales,
        // no necesitamos migrar nada.

        if (localBudgets.isEmpty()) {

            onSuccess()

            return
        }

        // Consultar el servidor para no sobrescribir
        // presupuestos existentes en otro dispositivo.

        collection.get(Source.SERVER)
            .addOnSuccessListener { snapshot ->

                val remoteScopes = snapshot.documents
                    .mapNotNull { document ->

                        val period = document
                            .getString("period")
                            ?: return@mapNotNull null

                        val categoryId = document
                            .getString("categoryId")
                            ?: return@mapNotNull null

                        "$period|$categoryId"
                    }
                    .toSet()

                val remoteIds = snapshot.documents
                    .map { it.id }
                    .toSet()

                val missing = localBudgets
                    .filter {

                        it.userId == userId &&
                            it.workspaceId == workspaceId
                    }
                    .mapNotNull { budget ->

                        try {

                            validate(budget)

                            val id = documentId(
                                budget.period,
                                budget.categoryId
                            )

                            budget.copy(id = id)

                        } catch (_: Exception) {

                            null
                        }
                    }
                    .distinctBy {
                        it.id
                    }
                    .filter { budget ->

                        val scope =
                            "${budget.period}|${budget.categoryId}"

                        budget.id !in remoteIds &&
                            scope !in remoteScopes
                    }

                if (missing.isEmpty()) {

                    onSuccess()

                    return@addOnSuccessListener
                }

                val batch = database.batch()

                missing.forEach { budget ->

                    batch.set(
                        collection.document(budget.id),
                        toFirestoreData(budget)
                    )
                }

                batch.commit()
                    .addOnSuccessListener {

                        onSuccess()
                    }
                    .addOnFailureListener { exception ->

                        onError(exception)
                    }
            }
            .addOnFailureListener { exception ->

                onError(exception)
            }
    }
}
