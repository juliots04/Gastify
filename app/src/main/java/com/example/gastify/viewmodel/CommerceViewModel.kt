package com.example.gastify.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

import com.example.gastify.data.model.CommerceOperation
import com.example.gastify.data.model.CommerceRequest

import com.example.gastify.data.repository.CommerceRepository

import com.google.firebase.firestore.ListenerRegistration

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

import java.util.UUID

data class CommerceUiState(

    val operations: List<CommerceOperation> = emptyList(),

    val isLoading: Boolean = true,

    val isSaving: Boolean = false,

    val errorMessage: String? = null,

    val successMessage: String? = null
)

class CommerceViewModel(
    private val repository: CommerceRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        CommerceUiState()
    )

    val uiState: StateFlow<CommerceUiState> =
        _uiState

    private var listener: ListenerRegistration? = null

    // Permiten reintentar una misma operación
    // sin generar otro identificador.

    private var pendingRequest: CommerceRequest? = null

    private var pendingOperationId: String? = null

    // ==========================================
    // ESCUCHAR HISTORIAL
    // ==========================================

    fun startListening() {

        if (listener != null) {
            return
        }

        _uiState.update {

            it.copy(
                isLoading = true
            )
        }

        listener = repository.listen(

            onSuccess = { operations ->

                _uiState.update {

                    it.copy(
                        operations = operations,
                        isLoading = false,
                        errorMessage = null
                    )
                }
            },

            onError = { exception ->

                _uiState.update {

                    it.copy(
                        isLoading = false,

                        errorMessage =
                            exception.localizedMessage
                                ?: "No se pudo cargar el historial."
                    )
                }
            }
        )
    }

    // ==========================================
    // DETENER ESCUCHA
    // ==========================================

    fun stopListening() {

        listener?.remove()

        listener = null
    }

    // ==========================================
    // REGISTRAR COMPRA O VENTA
    // ==========================================

    fun registerOperation(
        request: CommerceRequest,
        onSuccess: () -> Unit = {}
    ) {

        if (_uiState.value.isSaving) {
            return
        }

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

        try {

            repository.registerOperation(

                operationId = operationId,

                request = request,

                onSuccess = {

                    pendingRequest = null

                    pendingOperationId = null

                    _uiState.update {

                        it.copy(
                            isSaving = false,

                            successMessage = when (
                                request.type
                            ) {

                                com.example.gastify.data.model
                                    .CommerceOperationType.PURCHASE ->
                                    "Compra registrada correctamente."

                                com.example.gastify.data.model
                                    .CommerceOperationType.SALE ->
                                    "Venta registrada correctamente."
                            }
                        )
                    }

                    onSuccess()
                },

                onError = { exception ->

                    _uiState.update {

                        it.copy(
                            isSaving = false,

                            errorMessage =
                                exception.localizedMessage
                                    ?: "No se pudo registrar la operación."
                        )
                    }
                }
            )

        } catch (exception: Exception) {

            _uiState.update {

                it.copy(
                    isSaving = false,

                    errorMessage =
                        exception.localizedMessage
                            ?: "Los datos de la operación no son válidos."
                )
            }
        }
    }

    // ==========================================
    // LIMPIAR MENSAJES
    // ==========================================

    fun clearMessages() {

        _uiState.update {

            it.copy(
                errorMessage = null,

                successMessage = null
            )
        }
    }

    override fun onCleared() {

        stopListening()

        super.onCleared()
    }

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
