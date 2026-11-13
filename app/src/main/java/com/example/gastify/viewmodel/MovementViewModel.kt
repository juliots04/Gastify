package com.example.gastify.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope

import com.example.gastify.data.model.Movement
import com.example.gastify.data.repository.MovementRepository

import com.google.firebase.firestore.ListenerRegistration

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

import kotlinx.coroutines.launch

data class MovementUiState(

    val movements: List<Movement> = emptyList(),

    val isLoading: Boolean = true,

    val isSaving: Boolean = false,

    val errorMessage: String? = null,

    val successMessage: String? = null
)

class MovementViewModel(
    private val repository: MovementRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        MovementUiState()
    )

    val uiState: StateFlow<MovementUiState> = _uiState

    private var listener: ListenerRegistration? = null

    // ========================================
    // ESCUCHAR MOVIMIENTOS
    // ========================================

    fun startListening() {

        if (listener != null) return

        _uiState.update {
            it.copy(isLoading = true)
        }

        listener = repository.listenToMovements(

            onSuccess = { movements ->

                _uiState.update {

                    it.copy(
                        movements = movements,
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
                                ?: "No se pudieron cargar los movimientos."
                    )
                }
            }
        )
    }

    // ========================================
    // DETENER SINCRONIZACIÓN
    // ========================================

    fun stopListening() {

        listener?.remove()

        listener = null
    }

    // ========================================
    // REGISTRAR INGRESO O GASTO
    // ========================================

    fun createMovement(
        accountId: String,
        categoryId: String,
        type: String,
        amountCents: Long,
        note: String
    ) {

        if (_uiState.value.isSaving) return

        viewModelScope.launch {

            _uiState.update {

                it.copy(
                    isSaving = true,
                    errorMessage = null,
                    successMessage = null
                )
            }

            try {

                repository.createMovement(
                    accountId = accountId,
                    categoryId = categoryId,
                    type = type,
                    amountCents = amountCents,
                    note = note
                )

                _uiState.update {

                    it.copy(
                        successMessage =
                            "Movimiento registrado correctamente."
                    )
                }

            } catch (exception: Exception) {

                _uiState.update {

                    it.copy(
                        errorMessage =
                            exception.localizedMessage
                                ?: "No se pudo registrar el movimiento."
                    )
                }

            } finally {

                _uiState.update {
                    it.copy(isSaving = false)
                }
            }
        }
    }

    // ========================================
    // LIMPIAR MENSAJES
    // ========================================

    fun clearMessages() {

        _uiState.update {

            it.copy(
                errorMessage = null,
                successMessage = null
            )
        }
    }

    // ========================================
    // LIBERAR RECURSOS
    // ========================================

    override fun onCleared() {

        stopListening()

        super.onCleared()
    }

    // ========================================
    // FACTORY
    // ========================================

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

                    if (
                        !modelClass.isAssignableFrom(
                            MovementViewModel::class.java
                        )
                    ) {

                        throw IllegalArgumentException(
                            "ViewModel no reconocido."
                        )
                    }

                    return MovementViewModel(
                        MovementRepository(
                            userId = userId,
                            workspaceId = workspaceId
                        )
                    ) as T
                }
            }
        }
    }
}
