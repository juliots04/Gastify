package com.example.gastify.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

import com.example.gastify.data.model.CommerceOperation

import com.example.gastify.data.repository.ProfitabilityRepository

import com.google.firebase.firestore.ListenerRegistration

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

data class ProfitabilityUiState(

    val operations: List<CommerceOperation> = emptyList(),

    val isLoading: Boolean = true,

    val isFromCache: Boolean = false,

    val errorMessage: String? = null
)

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

    // ==========================================
    // CONSULTAR PERÍODO
    // ==========================================

    fun startListening(
        startMillis: Long,
        endMillis: Long
    ) {

        if (
            listener != null &&
            currentStart == startMillis &&
            currentEnd == endMillis
        ) {
            return
        }

        stopListening()

        currentStart = startMillis

        currentEnd = endMillis

        _uiState.update {

            it.copy(
                operations = emptyList(),
                isLoading = true,
                isFromCache = false,
                errorMessage = null
            )
        }

        listener = repository.listenMonth(

            startMillis = startMillis,

            endMillis = endMillis,

            onSuccess = { operations, fromCache ->

                _uiState.update {

                    it.copy(
                        operations = operations,
                        isLoading = false,
                        isFromCache = fromCache,
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
                                ?: "No se pudo consultar la rentabilidad."
                    )
                }
            }
        )
    }

    // ==========================================
    // DETENER CONSULTA
    // ==========================================

    fun stopListening() {

        listener?.remove()

        listener = null

        currentStart = null

        currentEnd = null
    }

    // ==========================================
    // LIMPIAR ERROR
    // ==========================================

    fun clearError() {

        _uiState.update {

            it.copy(
                errorMessage = null
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
