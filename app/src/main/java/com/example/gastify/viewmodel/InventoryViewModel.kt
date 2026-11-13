package com.example.gastify.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

import com.example.gastify.data.model.InventoryProduct
import com.example.gastify.data.model.StockMovement
import com.example.gastify.data.model.StockMovementType

import com.example.gastify.data.repository.InventoryRepository

import com.google.firebase.firestore.ListenerRegistration

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

import java.util.UUID

data class InventoryUiState(

    val products: List<InventoryProduct> = emptyList(),

    val stockMovements: List<StockMovement> = emptyList(),

    val isLoading: Boolean = true,

    val isHistoryLoading: Boolean = true,

    val isSaving: Boolean = false,

    val errorMessage: String? = null,

    val successMessage: String? = null
)

class InventoryViewModel(
    private val repository: InventoryRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        InventoryUiState()
    )

    val uiState: StateFlow<InventoryUiState> =
        _uiState

    private var productsListener:
        ListenerRegistration? = null

    private var movementsListener:
        ListenerRegistration? = null

    // ==========================================
    // ESCUCHAR FIREBASE
    // ==========================================

    fun startListening() {

        if (productsListener == null) {

            _uiState.update {

                it.copy(isLoading = true)
            }

            productsListener = repository.listen(

                onSuccess = { products ->

                    _uiState.update {

                        it.copy(
                            products = products,
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
                                    ?: "No se pudo cargar el inventario."
                        )
                    }
                }
            )
        }

        if (movementsListener == null) {

            _uiState.update {

                it.copy(
                    isHistoryLoading = true
                )
            }

            movementsListener =
                repository.listenStockMovements(

                    onSuccess = { movements ->

                        _uiState.update {

                            it.copy(
                                stockMovements = movements,
                                isHistoryLoading = false
                            )
                        }
                    },

                    onError = { exception ->

                        _uiState.update {

                            it.copy(
                                isHistoryLoading = false,

                                errorMessage =
                                    exception.localizedMessage
                                        ?: "No se pudo cargar el historial."
                            )
                        }
                    }
                )
        }
    }

    // ==========================================
    // DETENER ESCUCHAS
    // ==========================================

    fun stopListening() {

        productsListener?.remove()

        movementsListener?.remove()

        productsListener = null

        movementsListener = null
    }

    // ==========================================
    // GUARDAR PRODUCTO
    // ==========================================

    fun saveProduct(
        product: InventoryProduct,
        onSuccess: () -> Unit = {}
    ) {

        if (_uiState.value.isSaving) {
            return
        }

        _uiState.update {

            it.copy(
                isSaving = true,
                errorMessage = null,
                successMessage = null
            )
        }

        try {

            repository.save(

                product = product,

                onSuccess = {

                    _uiState.update {

                        it.copy(
                            isSaving = false,

                            successMessage =
                                "Producto guardado correctamente."
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
                                    ?: "No se pudo guardar el producto."
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
                            ?: "Datos del producto inválidos."
                )
            }
        }
    }

    // ==========================================
    // REGISTRAR STOCK
    // ==========================================

    fun registerStockMovement(
        productId: String,
        type: StockMovementType,
        quantityValue: Int,
        note: String,
        onSuccess: () -> Unit = {}
    ) {

        if (_uiState.value.isSaving) {
            return
        }

        _uiState.update {

            it.copy(
                isSaving = true,
                errorMessage = null,
                successMessage = null
            )
        }

        // Un identificador nuevo por confirmación.
        // Se conserva durante los reintentos
        // internos de la transacción.

        val operationId = UUID.randomUUID()
            .toString()

        try {

            repository.registerStockMovement(

                operationId = operationId,

                productId = productId,

                type = type,

                quantityValue = quantityValue,

                note = note,

                onSuccess = {

                    _uiState.update {

                        it.copy(
                            isSaving = false,

                            successMessage =
                                "Movimiento de stock registrado."
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
                                    ?: "No se pudo registrar el movimiento."
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
                            ?: "Movimiento inválido."
                )
            }
        }
    }

    // ==========================================
    // ELIMINAR PRODUCTO
    // ==========================================

    fun deleteProduct(
        productId: String
    ) {

        if (_uiState.value.isSaving) {
            return
        }

        _uiState.update {

            it.copy(
                isSaving = true,
                errorMessage = null,
                successMessage = null
            )
        }

        try {

            repository.delete(

                productId = productId,

                onSuccess = {

                    _uiState.update {

                        it.copy(
                            isSaving = false,

                            successMessage =
                                "Producto eliminado."
                        )
                    }
                },

                onError = { exception ->

                    _uiState.update {

                        it.copy(
                            isSaving = false,

                            errorMessage =
                                exception.localizedMessage
                                    ?: "No se pudo eliminar el producto."
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
                            ?: "No se pudo eliminar."
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
                            InventoryViewModel::class.java
                        )
                    ) {
                        "ViewModel no reconocido."
                    }

                    return InventoryViewModel(
                        InventoryRepository(
                            userId,
                            workspaceId
                        )
                    ) as T
                }
            }
        }
    }
}
