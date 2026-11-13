package com.example.gastify.data.repository

import com.example.gastify.data.model.CommerceOperation
import com.example.gastify.data.model.CommerceOperationType
import com.example.gastify.data.model.CommerceRequest
import com.example.gastify.data.model.StockMovementType

import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query

class CommerceRepository(
    private val userId: String,
    private val workspaceId: String
) {

    companion object {

        const val MAX_QUANTITY = 1_000_000

        const val MAX_AMOUNT_CENTS =
            999_999_999_999L
    }

    private val database =
        FirebaseFirestore.getInstance()

    private val workspace = database
        .collection("users")
        .document(userId)
        .collection("workspaces")
        .document(workspaceId)

    private val products =
        workspace.collection("products")

    private val accounts =
        workspace.collection("accounts")

    private val categories =
        workspace.collection("categories")

    private val stockMovements =
        workspace.collection("stockMovements")

    private val financialMovements =
        workspace.collection("movements")

    private val commerceOperations =
        workspace.collection("commerceOperations")

    // ==========================================
    // VALIDACIONES
    // ==========================================

    private fun validateRequest(
        operationId: String,
        request: CommerceRequest
    ) {

        require(
            operationId.isNotBlank() &&
                operationId.length <= 120 &&
                '/' !in operationId
        ) {
            "Identificador de operación inválido."
        }

        require(
            request.productId.isNotBlank() &&
                '/' !in request.productId
        ) {
            "Selecciona un producto válido."
        }

        require(
            request.accountId.isNotBlank() &&
                '/' !in request.accountId
        ) {
            "Selecciona una cuenta válida."
        }

        require(
            request.categoryId.isNotBlank() &&
                '/' !in request.categoryId
        ) {
            "Selecciona una categoría válida."
        }

        require(
            request.quantity in 1..MAX_QUANTITY
        ) {
            "La cantidad no es válida."
        }

        require(
            request.unitPriceCents in
                1L..MAX_AMOUNT_CENTS
        ) {
            "El precio unitario no es válido."
        }

        require(
            request.note.length <= 200
        ) {
            "La descripción es demasiado larga."
        }

        val total = Math.multiplyExact(
            request.quantity.toLong(),
            request.unitPriceCents
        )

        require(
            total in 1L..MAX_AMOUNT_CENTS
        ) {
            "El importe total supera el máximo permitido."
        }
    }

    // ==========================================
    // CONSULTAR HISTORIAL
    // ==========================================

    fun listen(
        onSuccess: (List<CommerceOperation>) -> Unit,
        onError: (Exception) -> Unit
    ): ListenerRegistration {

        return commerceOperations
            .orderBy(
                "createdAt",
                Query.Direction.DESCENDING
            )
            .limit(50)
            .addSnapshotListener { snapshot, error ->

                if (error != null) {

                    onError(error)

                    return@addSnapshotListener
                }

                val operations = snapshot
                    ?.documents
                    ?.mapNotNull { document ->

                        val type = document.getString("type")
                            ?: return@mapNotNull null

                        if (
                            CommerceOperationType.entries.none {
                                it.name == type
                            }
                        ) {
                            return@mapNotNull null
                        }

                        val quantity = document.getLong(
                            "quantity"
                        ) ?: return@mapNotNull null

                        val unitPrice = document.getLong(
                            "unitPriceCents"
                        ) ?: return@mapNotNull null

                        val total = document.getLong(
                            "totalCents"
                        ) ?: return@mapNotNull null

                        val previousQuantity = document.getLong(
                            "previousQuantity"
                        ) ?: return@mapNotNull null

                        val resultingQuantity = document.getLong(
                            "resultingQuantity"
                        ) ?: return@mapNotNull null

                        val previousBalance = document.getLong(
                            "previousBalanceCents"
                        ) ?: return@mapNotNull null

                        val resultingBalance = document.getLong(
                            "balanceAfterCents"
                        ) ?: return@mapNotNull null

                        if (
                            quantity !in
                                1L..MAX_QUANTITY.toLong() ||
                            unitPrice !in
                                1L..MAX_AMOUNT_CENTS ||
                            total !in
                                1L..MAX_AMOUNT_CENTS ||
                            previousQuantity !in
                                0L..MAX_QUANTITY.toLong() ||
                            resultingQuantity !in
                                0L..MAX_QUANTITY.toLong() ||
                            previousBalance !in
                                0L..MAX_AMOUNT_CENTS ||
                            resultingBalance !in
                                0L..MAX_AMOUNT_CENTS
                        ) {
                            return@mapNotNull null
                        }

                        val historicalUnitCost =
                            document.getLong(
                                "historicalUnitCostCents"
                            )

                        val historicalTotalCost =
                            document.getLong(
                                "historicalTotalCostCents"
                            )

                        val validHistoricalCost =
                            historicalUnitCost != null &&
                                historicalTotalCost != null &&
                                historicalUnitCost in
                                    0L..MAX_AMOUNT_CENTS &&
                                historicalTotalCost ==
                                    historicalUnitCost *
                                        quantity

                        CommerceOperation(

                            id = document.id,

                            type = type,

                            productId = document.getString(
                                "productId"
                            ) ?: "",

                            productName = document.getString(
                                "productName"
                            ) ?: "",

                            productSku = document.getString(
                                "productSku"
                            ) ?: "",

                            accountId = document.getString(
                                "accountId"
                            ) ?: "",

                            categoryId = document.getString(
                                "categoryId"
                            ) ?: "",

                            quantity = quantity.toInt(),

                            unitPriceCents = unitPrice,

                            totalCents = total,

                            previousQuantity =
                                previousQuantity.toInt(),

                            resultingQuantity =
                                resultingQuantity.toInt(),

                            previousBalanceCents =
                                previousBalance,

                            balanceAfterCents =
                                resultingBalance,

                            note = document.getString("note")
                                ?: "",

                            createdAt = document.getTimestamp(
                                "createdAt"
                            ),

                            historicalUnitCostCents =
                                if (validHistoricalCost) {
                                    historicalUnitCost
                                } else {
                                    null
                                },

                            historicalTotalCostCents =
                                if (validHistoricalCost) {
                                    historicalTotalCost
                                } else {
                                    null
                                }
                        )
                    }
                    ?: emptyList()

                onSuccess(operations)
            }
    }

    // ==========================================
    // REGISTRAR COMPRA O VENTA
    // ==========================================

    fun registerOperation(
        operationId: String,
        request: CommerceRequest,
        onSuccess: () -> Unit,
        onError: (Exception) -> Unit
    ) {

        validateRequest(
            operationId,
            request
        )

        val productReference = products.document(
            request.productId
        )

        val accountReference = accounts.document(
            request.accountId
        )

        val categoryReference = categories.document(
            request.categoryId
        )

        val commerceReference =
            commerceOperations.document(
                operationId
            )

        val stockMovementReference =
            stockMovements.document(
                operationId
            )

        val financialMovementReference =
            financialMovements.document(
                operationId
            )

        val totalCents = Math.multiplyExact(
            request.quantity.toLong(),
            request.unitPriceCents
        )

        database.runTransaction { transaction ->

            // ==================================
            // 1. EVITAR DUPLICADOS
            // ==================================

            val previousOperation = transaction.get(
                commerceReference
            )

            if (previousOperation.exists()) {

                val sameOperation =
                    previousOperation.getString("type") ==
                        request.type.name &&

                        previousOperation.getString("productId") ==
                        request.productId &&

                        previousOperation.getString("accountId") ==
                        request.accountId &&

                        previousOperation.getString("categoryId") ==
                        request.categoryId &&

                        previousOperation.getLong("quantity") ==
                        request.quantity.toLong() &&

                        previousOperation.getLong("unitPriceCents") ==
                        request.unitPriceCents &&

                        previousOperation.getString("note") ==
                        request.note.trim()

                require(sameOperation) {
                    "El identificador pertenece a otra operación."
                }

                return@runTransaction Unit
            }

            // ==================================
            // 2. LEER DOCUMENTOS
            // ==================================

            val productSnapshot = transaction.get(
                productReference
            )

            val accountSnapshot = transaction.get(
                accountReference
            )

            val categorySnapshot = transaction.get(
                categoryReference
            )

            require(productSnapshot.exists()) {
                "El producto no existe."
            }

            require(accountSnapshot.exists()) {
                "La cuenta no existe."
            }

            require(categorySnapshot.exists()) {
                "La categoría no existe."
            }

            // ==================================
            // 3. DATOS DEL PRODUCTO
            // ==================================

            val productName = productSnapshot.getString(
                "name"
            ) ?: throw IllegalStateException(
                "Producto sin nombre."
            )

            val productSku = productSnapshot.getString(
                "sku"
            ) ?: ""

            val previousStockLong = productSnapshot.getLong(
                "quantity"
            ) ?: throw IllegalStateException(
                "Stock inválido."
            )

            require(
                previousStockLong in
                    0L..MAX_QUANTITY.toLong()
            ) {
                "Stock fuera del límite permitido."
            }

            val previousStock =
                previousStockLong.toInt()

            // ==================================
            // 4. COSTO HISTÓRICO
            // ==================================

            val productCost = productSnapshot.getLong(
                "costCents"
            ) ?: throw IllegalStateException(
                "El producto no tiene un costo válido."
            )

            require(
                productCost in 0L..MAX_AMOUNT_CENTS
            ) {
                "Costo del producto inválido."
            }

            if (
                request.type ==
                CommerceOperationType.PURCHASE
            ) {

                require(
                    productCost == request.unitPriceCents
                ) {
                    "El costo de compra debe coincidir con el costo del producto."
                }
            }

            // IMPORTANTE:
            // Capturamos el costo actual ANTES
            // de registrar la operación.

            val historicalUnitCostCents =
                productCost

            val historicalTotalCostCents =
                Math.multiplyExact(
                    historicalUnitCostCents,
                    request.quantity.toLong()
                )

            // ==================================
            // 5. SALDO DE LA CUENTA
            // ==================================

            val previousBalance = accountSnapshot
                .getLong("balanceCents")
                ?: throw IllegalStateException(
                    "La cuenta no tiene un saldo válido."
                )

            require(
                previousBalance in
                    0L..MAX_AMOUNT_CENTS
            ) {
                "Saldo fuera del límite permitido."
            }

            // ==================================
            // 6. CATEGORÍA FINANCIERA
            // ==================================

            val categoryType = categorySnapshot.getString(
                "type"
            ) ?: ""

            val expectedCategoryType = when (
                request.type
            ) {

                CommerceOperationType.PURCHASE ->
                    "EXPENSE"

                CommerceOperationType.SALE ->
                    "INCOME"
            }

            require(
                categoryType == expectedCategoryType
            ) {

                if (
                    request.type ==
                    CommerceOperationType.PURCHASE
                ) {

                    "Selecciona una categoría de gastos."

                } else {

                    "Selecciona una categoría de ingresos."
                }
            }

            // ==================================
            // 7. NUEVO STOCK
            // ==================================

            val quantityDelta = when (
                request.type
            ) {

                CommerceOperationType.PURCHASE ->
                    request.quantity

                CommerceOperationType.SALE ->
                    -request.quantity
            }

            val resultingStock =
                previousStock.toLong() +
                    quantityDelta.toLong()

            require(
                resultingStock in
                    0L..MAX_QUANTITY.toLong()
            ) {

                if (resultingStock < 0L) {

                    "Stock insuficiente para realizar la venta."

                } else {

                    "Se supera el máximo permitido de unidades."
                }
            }

            // ==================================
            // 8. NUEVO SALDO FINANCIERO
            // ==================================

            val resultingBalance = when (
                request.type
            ) {

                CommerceOperationType.PURCHASE ->
                    previousBalance - totalCents

                CommerceOperationType.SALE ->
                    previousBalance + totalCents
            }

            require(
                resultingBalance in
                    0L..MAX_AMOUNT_CENTS
            ) {

                if (resultingBalance < 0L) {

                    "Saldo insuficiente para realizar la compra."

                } else {

                    "El saldo supera el máximo permitido."
                }
            }

            // ==================================
            // 9. PREPARAR MOVIMIENTOS
            // ==================================

            val operationNote = request.note.trim()

            val stockType = when (
                request.type
            ) {

                CommerceOperationType.PURCHASE ->
                    StockMovementType.ENTRY.name

                CommerceOperationType.SALE ->
                    StockMovementType.EXIT.name
            }

            val financialType = when (
                request.type
            ) {

                CommerceOperationType.PURCHASE ->
                    "EXPENSE"

                CommerceOperationType.SALE ->
                    "INCOME"
            }

            val financialNote = (
                if (
                    request.type ==
                    CommerceOperationType.PURCHASE
                ) {
                    "Compra"
                } else {
                    "Venta"
                }
            ) + ": $productName x${request.quantity}"

            // ==================================
            // 10. HISTORIAL DE STOCK
            // ==================================

            val stockMovementData = hashMapOf<String, Any>(

                "productId" to request.productId,

                "productName" to productName,

                "type" to stockType,

                "quantityDelta" to quantityDelta,

                "previousQuantity" to previousStock,

                "resultingQuantity" to
                    resultingStock.toInt(),

                "note" to operationNote,

                "createdAt" to
                    FieldValue.serverTimestamp()
            )

            // ==================================
            // 11. MOVIMIENTO FINANCIERO
            // ==================================

            val financialMovementData =
                hashMapOf<String, Any>(

                    "accountId" to request.accountId,

                    "categoryId" to request.categoryId,

                    "type" to financialType,

                    "amountCents" to totalCents,

                    "note" to financialNote.take(200),

                    "previousBalanceCents" to
                        previousBalance,

                    "balanceAfterCents" to
                        resultingBalance,

                    "createdAt" to
                        FieldValue.serverTimestamp()
                )

            // ==================================
            // 12. COMPROBANTE COMERCIAL
            // ==================================

            val commerceData = hashMapOf<String, Any>(

                "type" to request.type.name,

                "productId" to request.productId,

                "productName" to productName,

                "productSku" to productSku,

                "accountId" to request.accountId,

                "categoryId" to request.categoryId,

                "quantity" to request.quantity,

                "unitPriceCents" to
                    request.unitPriceCents,

                "totalCents" to totalCents,

                "previousQuantity" to previousStock,

                "resultingQuantity" to
                    resultingStock.toInt(),

                "previousBalanceCents" to
                    previousBalance,

                "balanceAfterCents" to
                    resultingBalance,

                "note" to operationNote,

                // NUEVOS CAMPOS

                "historicalUnitCostCents" to
                    historicalUnitCostCents,

                "historicalTotalCostCents" to
                    historicalTotalCostCents,

                "createdAt" to
                    FieldValue.serverTimestamp()
            )

            // ==================================
            // 13. ACTUALIZAR PRODUCTO
            // ==================================

            transaction.update(
                productReference,

                mapOf(
                    "quantity" to resultingStock.toInt(),

                    "lastStockMovementId" to
                        operationId,

                    "updatedAt" to
                        FieldValue.serverTimestamp()
                )
            )

            // ==================================
            // 14. GUARDAR HISTORIAL DE STOCK
            // ==================================

            transaction.set(
                stockMovementReference,
                stockMovementData
            )

            // ==================================
            // 15. ACTUALIZAR CUENTA
            // ==================================

            transaction.update(
                accountReference,

                mapOf(
                    "balanceCents" to resultingBalance,

                    "lastMovementId" to operationId
                )
            )

            // ==================================
            // 16. GUARDAR MOVIMIENTO FINANCIERO
            // ==================================

            transaction.set(
                financialMovementReference,
                financialMovementData
            )

            // ==================================
            // 17. GUARDAR COMPROBANTE
            // ==================================

            transaction.set(
                commerceReference,
                commerceData
            )

            Unit

        }.addOnSuccessListener {

            onSuccess()

        }.addOnFailureListener { exception ->

            onError(exception)
        }
    }
}
