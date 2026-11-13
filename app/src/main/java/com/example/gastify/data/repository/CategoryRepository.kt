package com.example.gastify.data.repository

import com.example.gastify.data.model.Category
import com.example.gastify.data.model.CategoryType

import com.google.android.gms.tasks.Task

import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query

import kotlinx.coroutines.suspendCancellableCoroutine

import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class CategoryRepository(
    userId: String,
    workspaceId: String
) {

    private val database = FirebaseFirestore.getInstance()

    private val collection = database
        .collection("users")
        .document(userId)
        .collection("workspaces")
        .document(workspaceId)
        .collection("categories")

    // CONSULTAR CATEGORÍAS EN TIEMPO REAL

    fun listenToCategories(
        onSuccess: (List<Category>) -> Unit,
        onError: (Exception) -> Unit
    ): ListenerRegistration {

        return collection
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->

                if (error != null) {
                    onError(error)
                    return@addSnapshotListener
                }

                val categories = snapshot?.documents
                    ?.mapNotNull { document ->

                        val name = document.getString("name")
                            ?: return@mapNotNull null

                        Category(
                            id = document.id,
                            name = name,
                            type = document.getString("type")
                                ?: CategoryType.EXPENSE.name,
                            icon = document.getString("icon")
                                ?: "💸",
                            color = document.getString("color")
                                ?: "#00E0A4",
                            createdAt = document.getTimestamp(
                                "createdAt"
                            )
                        )
                    }
                    ?: emptyList()

                onSuccess(categories)
            }
    }

    // CREAR CATEGORÍA

    suspend fun createCategory(
        name: String,
        type: String,
        icon: String,
        color: String
    ) {

        validateCategory(name, type, icon, color)

        val data = hashMapOf<String, Any>(
            "name" to name.trim(),
            "type" to type,
            "icon" to icon,
            "color" to color,
            "createdAt" to FieldValue.serverTimestamp()
        )

        awaitWrite(
            collection.add(data)
        )
    }

    // EDITAR CATEGORÍA

    suspend fun updateCategory(
        categoryId: String,
        name: String,
        type: String,
        icon: String,
        color: String
    ) {

        require(categoryId.isNotBlank()) {
            "Categoría inválida."
        }

        validateCategory(name, type, icon, color)

        val data = mapOf(
            "name" to name.trim(),
            "type" to type,
            "icon" to icon,
            "color" to color
        )

        awaitWrite(
            collection
                .document(categoryId)
                .update(data)
        )
    }

    // ELIMINAR CATEGORÍA

    suspend fun deleteCategory(
        categoryId: String
    ) {

        require(categoryId.isNotBlank()) {
            "Categoría inválida."
        }

        awaitWrite(
            collection
                .document(categoryId)
                .delete()
        )
    }

    // VALIDACIONES

    private fun validateCategory(
        name: String,
        type: String,
        icon: String,
        color: String
    ) {

        require(name.trim().isNotEmpty()) {
            "Introduce un nombre."
        }

        require(name.trim().length <= 60) {
            "El nombre no puede superar 60 caracteres."
        }

        require(
            type == CategoryType.INCOME.name ||
            type == CategoryType.EXPENSE.name
        ) {
            "Tipo de categoría inválido."
        }

        require(icon.isNotBlank() && icon.length <= 10) {
            "Icono inválido."
        }

        require(
            Regex("^#[0-9A-Fa-f]{6}$").matches(color)
        ) {
            "Color inválido."
        }
    }

    // ESPERAR RESULTADO DE FIREBASE

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
                    continuation.resumeWithException(exception)
                }
            }
        }
    }
}
