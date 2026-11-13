package com.example.gastify.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope

import com.example.gastify.data.model.Category
import com.example.gastify.data.repository.CategoryRepository

import com.google.firebase.firestore.ListenerRegistration

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CategoryUiState(
    val categories: List<Category> = emptyList(),
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val errorMessage: String? = null
)

class CategoryViewModel(
    private val repository: CategoryRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        CategoryUiState()
    )

    val uiState: StateFlow<CategoryUiState> = _uiState

    private var listener: ListenerRegistration? = null

    // INICIAR SINCRONIZACIÓN

    fun startListening() {

        if (listener != null) return

        _uiState.update {
            it.copy(isLoading = true)
        }

        listener = repository.listenToCategories(

            onSuccess = { categories ->

                _uiState.update {
                    it.copy(
                        categories = categories,
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
                            ?: "Error al cargar categorías."
                    )
                }
            }
        )
    }

    // DETENER SINCRONIZACIÓN

    fun stopListening() {

        listener?.remove()
        listener = null
    }

    // CREAR O ACTUALIZAR

    fun saveCategory(
        categoryId: String?,
        name: String,
        type: String,
        icon: String,
        color: String
    ) {

        if (_uiState.value.isSaving) return

        val cleanName = name.trim()

        if (cleanName.isEmpty()) {

            _uiState.update {
                it.copy(
                    errorMessage = "Introduce un nombre."
                )
            }

            return
        }

        val duplicated = _uiState.value.categories.any {
            it.id != categoryId &&
            it.name.equals(cleanName, ignoreCase = true) &&
            it.type == type
        }

        if (duplicated) {

            _uiState.update {
                it.copy(
                    errorMessage = "Ya existe una categoría con ese nombre."
                )
            }

            return
        }

        viewModelScope.launch {

            _uiState.update {
                it.copy(isSaving = true)
            }

            try {

                if (categoryId.isNullOrBlank()) {

                    repository.createCategory(
                        name = cleanName,
                        type = type,
                        icon = icon,
                        color = color
                    )

                } else {

                    repository.updateCategory(
                        categoryId = categoryId,
                        name = cleanName,
                        type = type,
                        icon = icon,
                        color = color
                    )
                }

            } catch (exception: Exception) {

                _uiState.update {
                    it.copy(
                        errorMessage = exception.localizedMessage
                            ?: "No se pudo guardar la categoría."
                    )
                }

            } finally {

                _uiState.update {
                    it.copy(isSaving = false)
                }
            }
        }
    }

    // ELIMINAR

    fun deleteCategory(
        categoryId: String
    ) {

        if (_uiState.value.isSaving) return

        viewModelScope.launch {

            _uiState.update {
                it.copy(isSaving = true)
            }

            try {

                repository.deleteCategory(categoryId)

            } catch (exception: Exception) {

                _uiState.update {
                    it.copy(
                        errorMessage = exception.localizedMessage
                            ?: "No se pudo eliminar la categoría."
                    )
                }

            } finally {

                _uiState.update {
                    it.copy(isSaving = false)
                }
            }
        }
    }

    // LIMPIAR ERROR

    fun clearError() {

        _uiState.update {
            it.copy(errorMessage = null)
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

                    if (!modelClass.isAssignableFrom(
                            CategoryViewModel::class.java
                        )
                    ) {
                        throw IllegalArgumentException(
                            "ViewModel no reconocido."
                        )
                    }

                    return CategoryViewModel(
                        CategoryRepository(
                            userId = userId,
                            workspaceId = workspaceId
                        )
                    ) as T
                }
            }
        }
    }
}
