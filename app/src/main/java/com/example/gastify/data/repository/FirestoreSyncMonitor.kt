package com.example.gastify.data.repository

import android.os.Handler
import android.os.Looper

import com.example.gastify.util.FirebaseErrorFormatter

import com.google.firebase.auth.FirebaseAuth

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.MetadataChanges

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

enum class FirestoreConnectionStatus {
    CHECKING,
    SERVER,
    CACHE,
    ERROR
}

data class FirestoreSyncState(
    val connection: FirestoreConnectionStatus =
        FirestoreConnectionStatus.CHECKING,

    val hasPendingWorkspaceWrites: Boolean = false,

    val isVerifyingWrites: Boolean = false,

    val errorMessage: String? = null
)

class FirestoreSyncMonitor(
    private val userId: String
) {

    companion object {
        private const val VERIFICATION_TIMEOUT_MS = 12_000L
    }

    private val database = FirebaseFirestore.getInstance()

    private val auth = FirebaseAuth.getInstance()

    private val handler = Handler(
        Looper.getMainLooper()
    )

    private val _state = MutableStateFlow(
        FirestoreSyncState()
    )

    val state: StateFlow<FirestoreSyncState> = _state

    private var listener: ListenerRegistration? = null

    private var generation = 0L

    private var verificationGeneration = 0L

    private var verificationInProgress = false

    private var receivedCachedData = false

    private var verificationTimeout: Runnable? = null

    // ==========================================
    // VALIDAR SESIÓN ACTIVA
    // ==========================================

    private fun isActive(
        expectedGeneration: Long
    ): Boolean {

        return generation == expectedGeneration &&
            auth.currentUser?.uid == userId
    }

    // ==========================================
    // ACTUALIZAR ESTADO
    // ==========================================

    private fun publish(
        newState: FirestoreSyncState
    ) {

        if (_state.value == newState) {
            return
        }

        _state.value = newState
    }

    // ==========================================
    // INICIAR ESCUCHA
    // ==========================================

    fun start() {

        if (listener != null) {
            return
        }

        require(userId.isNotBlank()) {
            "Usuario no válido."
        }

        generation++

        val currentGeneration = generation

        receivedCachedData = false

        verificationInProgress = false

        publish(
            FirestoreSyncState()
        )

        listener = database
            .collection("users")
            .document(userId)
            .collection("workspaces")
            .limit(1)
            .addSnapshotListener(
                MetadataChanges.INCLUDE
            ) { snapshot, error ->

                if (!isActive(currentGeneration)) {
                    return@addSnapshotListener
                }

                // ==================================
                // ERROR DE FIRESTORE
                // ==================================

                if (error != null) {

                    publish(
                        _state.value.copy(
                            connection =
                                FirestoreConnectionStatus.ERROR,

                            errorMessage =
                                FirebaseErrorFormatter.message(
                                    error,
                                    "consultar Firebase"
                                )
                        )
                    )

                    return@addSnapshotListener
                }

                if (snapshot == null) {
                    return@addSnapshotListener
                }

                // ==================================
                // METADATOS DE FIRESTORE
                // ==================================

                val fromCache =
                    snapshot.metadata.isFromCache

                // CORRECCIÓN:
                // hasPendingWrites es un método,
                // no una propiedad pública.

                val hasPendingWrites =
                    snapshot.metadata.hasPendingWrites()

                // ==================================
                // RESPUESTA DE CACHÉ
                // ==================================

                if (fromCache) {

                    receivedCachedData = true

                    publish(
                        _state.value.copy(
                            connection =
                                FirestoreConnectionStatus.CACHE,

                            hasPendingWorkspaceWrites =
                                hasPendingWrites,

                            errorMessage = null
                        )
                    )

                    return@addSnapshotListener
                }

                // ==================================
                // RESPUESTA DEL SERVIDOR
                // ==================================

                val shouldVerify =
                    receivedCachedData &&
                        !verificationInProgress

                receivedCachedData = false

                publish(
                    _state.value.copy(
                        connection =
                            FirestoreConnectionStatus.SERVER,

                        hasPendingWorkspaceWrites =
                            hasPendingWrites,

                        errorMessage = null
                    )
                )

                if (shouldVerify) {

                    verifyPendingWrites(
                        currentGeneration
                    )
                }
            }
    }

    // ==========================================
    // VERIFICAR ESCRITURAS PENDIENTES
    // ==========================================

    private fun verifyPendingWrites(
        currentGeneration: Long
    ) {

        if (
            verificationInProgress ||
            !isActive(currentGeneration)
        ) {
            return
        }

        verificationInProgress = true

        verificationGeneration++

        val currentVerification =
            verificationGeneration

        publish(
            _state.value.copy(
                isVerifyingWrites = true,
                errorMessage = null
            )
        )

        // ======================================
        // TIEMPO MÁXIMO DE ESPERA VISUAL
        // ======================================

        val timeout = Runnable {

            if (
                !isActive(currentGeneration) ||
                currentVerification != verificationGeneration
            ) {
                return@Runnable
            }

            verificationInProgress = false

            publish(
                _state.value.copy(
                    isVerifyingWrites = false,

                    errorMessage =
                        "La confirmación de escrituras está tardando. " +
                            "Firebase continuará intentando sincronizar."
                )
            )
        }

        verificationTimeout = timeout

        handler.postDelayed(
            timeout,
            VERIFICATION_TIMEOUT_MS
        )

        // ======================================
        // ESPERAR CONFIRMACIÓN DE FIREBASE
        // ======================================

        database.waitForPendingWrites()
            .addOnCompleteListener { task ->

                if (
                    !isActive(currentGeneration) ||
                    currentVerification != verificationGeneration
                ) {
                    return@addOnCompleteListener
                }

                handler.removeCallbacks(
                    timeout
                )

                if (verificationTimeout === timeout) {
                    verificationTimeout = null
                }

                verificationInProgress = false

                publish(
                    _state.value.copy(
                        isVerifyingWrites = false,

                        errorMessage = if (
                            task.isSuccessful
                        ) {
                            null
                        } else {
                            FirebaseErrorFormatter.message(
                                task.exception,
                                "confirmar las escrituras pendientes"
                            )
                        }
                    )
                )
            }
    }

    // ==========================================
    // DETENER ESCUCHA
    // ==========================================

    fun stop() {

        generation++

        verificationGeneration++

        listener?.remove()

        listener = null

        verificationTimeout?.let {
            handler.removeCallbacks(it)
        }

        verificationTimeout = null

        verificationInProgress = false

        receivedCachedData = false
    }
}
