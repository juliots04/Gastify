package com.example.gastify.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope

import com.example.gastify.data.model.Account
import com.example.gastify.data.repository.AccountRepository

import com.google.firebase.firestore.ListenerRegistration

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AccountUiState(
    val accounts: List<Account> = emptyList(),
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val errorMessage: String? = null
)

class AccountViewModel(
    private val repository: AccountRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        AccountUiState()
    )

    val uiState: StateFlow<AccountUiState> = _uiState

    private var listener: ListenerRegistration? = null

    // ========================================
    // INICIAR SINCRONIZACIÓN
    // ========================================

    fun startListening() {

        if (listener != null) return

        _uiState.update {
            it.copy(isLoading = true)
        }

        listener = repository.listenToAccounts(

            onSuccess = { accounts ->

                _uiState.update {

                    it.copy(
                        accounts = accounts,
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
                                ?: "No se pudieron cargar las cuentas."
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
    // CREAR CUENTA
    // ========================================

    fun createAccount(
        name: String,
        type: String,
        openingBalanceCents: Long
    ) {

        if (_uiState.value.isSaving) return

        val cleanName = name.trim()

        val duplicated = _uiState.value.accounts.any {
            it.name.equals(
                cleanName,
                ignoreCase = true
            )
        }

        if (duplicated) {

            _uiState.update {

                it.copy(
                    errorMessage =
                        "Ya existe una cuenta con ese nombre."
                )
            }

            return
        }

        viewModelScope.launch {

            _uiState.update {
                it.copy(isSaving = true)
            }

            try {

                repository.createAccount(
                    name = cleanName,
                    type = type,
                    openingBalanceCents = openingBalanceCents
                )

            } catch (exception: Exception) {

                _uiState.update {

                    it.copy(
                        errorMessage =
                            exception.localizedMessage
                                ?: "No se pudo crear la cuenta."
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
    // RENOMBRAR CUENTA
    // ========================================

    fun renameAccount(
        accountId: String,
        newName: String
    ) {

        if (_uiState.value.isSaving) return

        val duplicated = _uiState.value.accounts.any {

            it.id != accountId &&
                it.name.equals(
                    newName.trim(),
                    ignoreCase = true
                )
        }

        if (duplicated) {

            _uiState.update {

                it.copy(
                    errorMessage =
                        "Ya existe una cuenta con ese nombre."
                )
            }

            return
        }

        viewModelScope.launch {

            _uiState.update {
                it.copy(isSaving = true)
            }

            try {

                repository.renameAccount(
                    accountId = accountId,
                    newName = newName
                )

            } catch (exception: Exception) {

                _uiState.update {

                    it.copy(
                        errorMessage =
                            exception.localizedMessage
                                ?: "No se pudo actualizar la cuenta."
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
    // ELIMINAR CUENTA
    // ========================================

    fun deleteAccount(
        accountId: String
    ) {

        if (_uiState.value.isSaving) return

        viewModelScope.launch {

            _uiState.update {
                it.copy(isSaving = true)
            }

            try {

                repository.deleteAccount(
                    accountId
                )

            } catch (exception: Exception) {

                _uiState.update {

                    it.copy(
                        errorMessage =
                            exception.localizedMessage
                                ?: "No se pudo eliminar la cuenta."
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
    // LIMPIAR ERRORES
    // ========================================

    fun clearError() {

        _uiState.update {
            it.copy(errorMessage = null)
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
                            AccountViewModel::class.java
                        )
                    ) {
                        throw IllegalArgumentException(
                            "ViewModel no reconocido."
                        )
                    }

                    return AccountViewModel(
                        AccountRepository(
                            userId = userId,
                            workspaceId = workspaceId
                        )
                    ) as T
                }
            }
        }
    }
}
