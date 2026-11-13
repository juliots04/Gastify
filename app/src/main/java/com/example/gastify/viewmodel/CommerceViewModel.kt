package com.example.gastify.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

import com.example.gastify.data.model.CommerceOperation
import com.example.gastify.data.model.CommerceOperationType
import com.example.gastify.data.model.CommerceRequest

import com.example.gastify.data.repository.CommerceRepository

import com.example.gastify.util.FirebaseErrorFormatter

import com.google.firebase.firestore.ListenerRegistration

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

import java.util.UUID

// ==========================================
// ESTADO DE COMPRAS Y VENTAS
// ==========================================

data class CommerceUiState(

    val operations: List<CommerceOperation> = emptyList(),

    val isLoading: Boolean = true,

    val isSaving: Boolean = false,

    val errorMessage: String? = null,

    val successMessage: String? = null
)

// ==========================================
// VIEWMODEL
// ==========================================

class CommerceViewModel(
    private val repository: CommerceRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        CommerceUiState()
    )

    val uiState: StateFlow<CommerceUiState> =
        _uiState

    private var listener: ListenerRegistration? = null

    private var listenerGeneration = 0L

    private var isActive = true

    // ======================================
    // OPERACIÓN PENDIENTE
    // ======================================

    private var pendingRequest: CommerceRequest? = null

    private var pendingOperationId: String? = null

    // ======================================
    // ESCUCHAR HISTORIAL
    // ======================================

    fun startListening() {

        if (
            listener != null ||
            !isActive
        ) {
            return
        }

        listenerGeneration++

        val currentGeneration =
            listenerGeneration

        _uiState.update {

            it.copy(
                isLoading = true,
                errorMessage = null
            )
        }

        listener = repository.listen(

            onSuccess = { operations ->

                if (
                    isActive &&
                    currentGeneration == listenerGeneration
                ) {

                    // MutableStateFlow evita propagar
                    // estados iguales.

                    _uiState.update { current ->

                        if (
                            current.operations == operations &&
                            !current.isLoading
                        ) {

                            current

                        } else {

                            current.copy(

                                operations = operations,

                                isLoading = false,

                                errorMessage = null
                            )
                        }
                    }
                }
            },

            onError = { exception ->

                if (
                    isActive &&
                    currentGeneration == listenerGeneration
                ) {

                    _uiState.update {

                        it.copy(

                            isLoading = false,

                            errorMessage =
                                FirebaseErrorFormatter.message(
                                    exception,
                                    "consultar compras y ventas"
                                )
                        )
                    }
                }
            }
        )
    }

    // ======================================
    // DETENER HISTORIAL
    // ======================================

    fun stopListening() {

        listenerGeneration++

        listener?.remove()

        listener = null
    }

    // ======================================
    // REGISTRAR COMPRA O VENTA
    // ======================================

    fun registerOperation(
        request: CommerceRequest,
        onSuccess: () -> Unit = {}
    ) {

        if (
            !isActive ||
            _uiState.value.isSaving
        ) {
            return
        }

        // Si el usuario reintenta exactamente
        // la misma operación, reutilizamos
        // el identificador anterior.

        val operationId = if (
            pendingRequest == request &&
            pendingOperationId != null
        ) {

            pendingOperationId!!

        } else {

            UUID.randomUUID().toString()
        }

        pendingRequest = request

        pendingOperationId = operationId

        _uiState.update {

            it.copy(

                isSaving = true,

                errorMessage = null,

                successMessage = null
            )
        }

        // ======================================
        // ENVIAR TRANSACCIÓN
        // ======================================

        try {

            repository.registerOperation(

                operationId = operationId,

                request = request,

                onSuccess = {

                    if (isActive) {

                        // La operación se confirmó
                        // o ya existía con el mismo
                        // identificador.

                        pendingRequest = null

                        pendingOperationId = null

                        val message = when (
                            request.type
                        ) {

                            CommerceOperationType.PURCHASE -> {

                                "Compra registrada correctamente."
                            }

                            CommerceOperationType.SALE -> {

                                "Venta registrada correctamente."
                            }
                        }

                        _uiState.update {

                            it.copy(

                                isSaving = false,

                                errorMessage = null,

                                successMessage = message
                            )
                        }

                        onSuccess()
                    }
                },

                onError = { exception ->

                    if (isActive) {

                        // Conservamos operationId para
                        // permitir un reintento seguro.

                        _uiState.update {

                            it.copy(

                                isSaving = false,

                                errorMessage =
                                    FirebaseErrorFormatter.message(
                                        exception,
                                        "registrar la operación"
                                    )
                            )
                        }
                    }
                }
            )

        } catch (exception: Exception) {

            if (isActive) {

                _uiState.update {

                    it.copy(

                        isSaving = false,

                        errorMessage =
                            FirebaseErrorFormatter.message(
                                exception,
                                "registrar la operación"
                            )
                    )
                }
            }
        }
    }

    // ======================================
    // LIMPIAR MENSAJES
    // ======================================

    fun clearMessages() {

        _uiState.update {

            it.copy(

                errorMessage = null,

                successMessage = null
            )
        }
    }

    // ======================================
    // LIBERAR RECURSOS
    // ======================================

    override fun onCleared() {

        isActive = false

        stopListening()

        super.onCleared()
    }

    // ======================================
    // FACTORY
    // ======================================

    companion object {

        fun factory(
            userId: String,
            workspaceId: String
        ): ViewModelProvider.Factory {

            return object : ViewModelProvider.Factory {

                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(
                    modelClass: Class<T>
                ): T {

                    require(
                        modelClass.isAssignableFrom(
                            CommerceViewModel::class.java
                        )
                    )

                    return CommerceViewModel(

                        CommerceRepository(
                            userId,
                            workspaceId
                        )

                    ) as T
                }
            }
        }
    }
}
