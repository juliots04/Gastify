package com.example.gastify.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope

import com.example.gastify.data.model.Workspace
import com.example.gastify.data.repository.WorkspaceRepository

import com.google.firebase.firestore.ListenerRegistration

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class WorkspaceUiState(
    val workspaces: List<Workspace> = emptyList(),
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val errorMessage: String? = null
)

class WorkspaceViewModel(
    private val repository: WorkspaceRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        WorkspaceUiState()
    )

    val uiState: StateFlow<WorkspaceUiState> = _uiState

    private var listenerRegistration: ListenerRegistration? = null

    // ==============================
    // INICIAR SINCRONIZACIÓN
    // ==============================

    fun startListening() {

        if (listenerRegistration != null) {
            return
        }

        _uiState.update {
            it.copy(isLoading = true)
        }

        listenerRegistration = repository.listenToWorkspaces(

            onSuccess = { workspaces ->

                _uiState.update {
                    it.copy(
                        workspaces = workspaces,
                        isLoading = false,
                        errorMessage = null
                    )
                }
            },

            onError = { exception ->

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = exception.localizedMessage
                            ?: "No se pudieron cargar los espacios."
                    )
                }
            }
        )
    }

    // ==============================
    // DETENER SINCRONIZACIÓN
    // ==============================

    fun stopListening() {

        listenerRegistration?.remove()

        listenerRegistration = null
    }

    // ==============================
    // CREAR ESPACIO
    // ==============================

    fun createWorkspace(
        name: String
    ) {

        val cleanName = name.trim()

        if (cleanName.isEmpty()) {
            return
        }

        if (_uiState.value.isSaving) {
            return
        }

        val exists = _uiState.value.workspaces.any {
            it.name.equals(
                cleanName,
                ignoreCase = true
            )
        }

        if (exists) {

            _uiState.update {
                it.copy(
                    errorMessage = "Ya existe un espacio con ese nombre."
                )
            }

            return
        }

        viewModelScope.launch {

            _uiState.update {
                it.copy(isSaving = true)
            }

            try {

                repository.createWorkspace(cleanName)

            } catch (exception: Exception) {

                _uiState.update {
                    it.copy(
                        errorMessage = exception.localizedMessage
                            ?: "No se pudo crear el espacio."
                    )
                }

            } finally {

                _uiState.update {
                    it.copy(isSaving = false)
                }
            }
        }
    }

    // ==============================
    // ELIMINAR ESPACIO
    // ==============================

    fun deleteWorkspace(
        name: String
    ) {

        val workspace = _uiState.value.workspaces
            .firstOrNull {
                it.name == name
            }

        if (workspace == null) {
            return
        }

        viewModelScope.launch {

            try {

                repository.deleteWorkspace(
                    workspace.id
                )

            } catch (exception: Exception) {

                _uiState.update {
                    it.copy(
                        errorMessage = exception.localizedMessage
                            ?: "No se pudo eliminar el espacio."
                    )
                }
            }
        }
    }

    // ==============================
    // LIMPIAR ERRORES
    // ==============================

    fun clearError() {

        _uiState.update {
            it.copy(errorMessage = null)
        }
    }

    // ==============================
    // LIBERAR RECURSOS
    // ==============================

    override fun onCleared() {

        stopListening()

        super.onCleared()
    }

    companion object {

        fun factory(
            userId: String
        ): ViewModelProvider.Factory {

            return object : ViewModelProvider.Factory {

                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(
                    modelClass: Class<T>
                ): T {

                    if (
                        !modelClass.isAssignableFrom(
                            WorkspaceViewModel::class.java
                        )
                    ) {
                        throw IllegalArgumentException(
                            "ViewModel no reconocido."
                        )
                    }

                    return WorkspaceViewModel(
                        WorkspaceRepository(userId)
                    ) as T
                }
            }
        }
    }
}
