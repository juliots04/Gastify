package com.example.gastify.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

import com.example.gastify.data.model.CommerceOperation

import com.example.gastify.data.repository.ProfitabilityRepository

import com.example.gastify.util.FirebaseErrorFormatter

import com.google.firebase.firestore.ListenerRegistration

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

// ==========================================
// ESTADO DE RENTABILIDAD
// ==========================================

data class ProfitabilityUiState(

    val operations: List<CommerceOperation> = emptyList(),

    val isLoading: Boolean = true,

    val isFromCache: Boolean = false,

    val errorMessage: String? = null
)

// ==========================================
// VIEWMODEL
// ==========================================

class ProfitabilityViewModel(
    private val repository: ProfitabilityRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        ProfitabilityUiState()
    )

    val uiState: StateFlow<ProfitabilityUiState> =
        _uiState

    private var listener: ListenerRegistration? = null

    private var currentStart: Long? = null

    private var currentEnd: Long? = null

    private var listenerGeneration = 0L

    private var isActive = true

    // ======================================
    // CONSULTAR PERÍODO
    // ======================================

    fun startListening(
        startMillis: Long,
        endMillis: Long
    ) {

        if (!isActive) {
            return
        }

        // Evitar crear otra escucha cuando
        // ya estamos consultando el mismo mes.

        if (
            listener != null &&
            currentStart == startMillis &&
            currentEnd == endMillis
        ) {
            return
        }

        // Desconectar el período anterior.

        stopListening()

        currentStart = startMillis

        currentEnd = endMillis

        val currentGeneration =
            listenerGeneration

        // Limpiar datos del mes anterior
        // para evitar mezclarlos.

        _uiState.update {

            it.copy(

                operations = emptyList(),

                isLoading = true,

                isFromCache = false,

                errorMessage = null
            )
        }

        // ======================================
        // CONSULTAR FIREBASE
        // ======================================

        try {

            listener = repository.listenMonth(

                startMillis = startMillis,

                endMillis = endMillis,

                onSuccess = { operations, fromCache ->

                    if (
                        isActive &&
                        currentGeneration == listenerGeneration
                    ) {

                        _uiState.update { current ->

                            // No generar cambios si
                            // la información es igual.

                            if (
                                current.operations == operations &&
                                current.isFromCache == fromCache &&
                                !current.isLoading &&
                                current.errorMessage == null
                            ) {

                                current

                            } else {

                                current.copy(

                                    operations = operations,

                                    isLoading = false,

                                    isFromCache = fromCache,

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

                            // Mantener los datos que ya
                            // se hayan recibido.

                            it.copy(

                                isLoading = false,

                                errorMessage =
                                    FirebaseErrorFormatter.message(
                                        exception,
                                        "consultar la rentabilidad"
                                    )
                            )
                        }
                    }
                }
            )

        } catch (exception: Exception) {

            _uiState.update {

                it.copy(

                    isLoading = false,

                    errorMessage =
                        FirebaseErrorFormatter.message(
                            exception,
                            "consultar la rentabilidad"
                        )
                )
            }
        }
    }

    // ======================================
    // DETENER CONSULTA
    // ======================================

    fun stopListening() {

        listenerGeneration++

        listener?.remove()

        listener = null

        currentStart = null

        currentEnd = null
    }

    // ======================================
    // LIMPIAR ERROR
    // ======================================

    fun clearError() {

        _uiState.update {

            it.copy(
                errorMessage = null
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
                            ProfitabilityViewModel::class.java
                        )
                    )

                    return ProfitabilityViewModel(

                        ProfitabilityRepository(
                            userId,
                            workspaceId
                        )

                    ) as T
                }
            }
        }
    }
}
