package com.example.gastify.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

import com.example.gastify.data.model.Movement
import com.example.gastify.data.repository.StatisticsRepository

import com.google.firebase.firestore.ListenerRegistration

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

data class StatisticsUiState(

    val movements: List<Movement> = emptyList(),

    val isLoading: Boolean = true,

    val errorMessage: String? = null
)

class StatisticsViewModel(
    private val repository: StatisticsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        StatisticsUiState()
    )

    val uiState: StateFlow<StatisticsUiState> = _uiState

    private var listener: ListenerRegistration? = null

    private var currentStart: Long? = null

    private var currentEnd: Long? = null

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
                movements = emptyList(),
                isLoading = true,
                errorMessage = null
            )
        }

        listener = repository.listenToMonth(

            startMillis = startMillis,

            endMillis = endMillis,

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
                        movements = emptyList(),
                        isLoading = false,

                        errorMessage =
                            exception.localizedMessage
                                ?: "No se pudieron cargar las estadísticas."
                    )
                }
            }
        )
    }

    fun stopListening() {

        listener?.remove()

        listener = null

        currentStart = null
        currentEnd = null
    }

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

                    if (
                        !modelClass.isAssignableFrom(
                            StatisticsViewModel::class.java
                        )
                    ) {

                        throw IllegalArgumentException(
                            "ViewModel no reconocido."
                        )
                    }

                    return StatisticsViewModel(
                        StatisticsRepository(
                            userId = userId,
                            workspaceId = workspaceId
                        )
                    ) as T
                }
            }
        }
    }
}
