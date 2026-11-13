package com.example.gastify.data.repository

import com.example.gastify.data.model.Workspace

import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query

import kotlinx.coroutines.suspendCancellableCoroutine

import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class WorkspaceRepository(
    private val userId: String
) {

    private val database = FirebaseFirestore.getInstance()

    // Colección privada del usuario
    private val workspaceCollection = database
        .collection("users")
        .document(userId)
        .collection("workspaces")

    // ==============================
    // ESCUCHAR ESPACIOS EN TIEMPO REAL
    // ==============================

    fun listenToWorkspaces(
        onSuccess: (List<Workspace>) -> Unit,
        onError: (Exception) -> Unit
    ): ListenerRegistration {

        return workspaceCollection
            .orderBy(
                "createdAt",
                Query.Direction.DESCENDING
            )
            .addSnapshotListener { snapshot, error ->

                if (error != null) {
                    onError(error)
                    return@addSnapshotListener
                }

                val workspaces = snapshot?.documents
                    ?.mapNotNull { document ->

                        val name = document.getString("name")

                        if (name.isNullOrBlank()) {
                            null
                        } else {
                            Workspace(
                                id = document.id,
                                name = name,
                                createdAt = document.getTimestamp(
                                    "createdAt"
                                )
                            )
                        }
                    }
                    ?: emptyList()

                onSuccess(workspaces)
            }
    }

    // ==============================
    // CREAR ESPACIO
    // ==============================

    suspend fun createWorkspace(
        name: String
    ) {

        val cleanName = name.trim()

        require(cleanName.isNotEmpty()) {
            "El nombre no puede estar vacío."
        }

        require(cleanName.length <= 60) {
            "El nombre no puede superar los 60 caracteres."
        }

        val data = hashMapOf<String, Any>(
            "name" to cleanName,
            "createdAt" to FieldValue.serverTimestamp()
        )

        suspendCancellableCoroutine<Unit> { continuation ->

            workspaceCollection
                .add(data)
                .addOnSuccessListener {

                    if (continuation.isActive) {
                        continuation.resume(Unit)
                    }
                }
                .addOnFailureListener { exception ->

                    if (continuation.isActive) {
                        continuation.resumeWithException(exception)
                    }
                }
        }
    }

    // ==============================
    // ELIMINAR ESPACIO
    // ==============================

    suspend fun deleteWorkspace(
        workspaceId: String
    ) {

        require(workspaceId.isNotBlank()) {
            "Identificador de espacio inválido."
        }

        suspendCancellableCoroutine<Unit> { continuation ->

            workspaceCollection
                .document(workspaceId)
                .delete()
                .addOnSuccessListener {

                    if (continuation.isActive) {
                        continuation.resume(Unit)
                    }
                }
                .addOnFailureListener { exception ->

                    if (continuation.isActive) {
                        continuation.resumeWithException(exception)
                    }
                }
        }
    }
}
