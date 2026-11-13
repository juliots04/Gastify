package com.example.gastify.data.repository

import com.example.gastify.data.model.InventoryProduct
import com.example.gastify.data.model.StockMovement
import com.example.gastify.data.model.StockMovementType

import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query

import java.util.Locale

class InventoryRepository(
    userId: String,
    workspaceId: String
) {

    companion object {

        const val MAX_QUANTITY = 1_000_000

        const val MAX_AMOUNT_CENTS =
            999_999_999_999L
    }

    private val database = FirebaseFirestore.getInstance()

    private val workspaceReference = database
        .collection("users")
        .document(userId)
        .collection("workspaces")
        .document(workspaceId)

    private val products = workspaceReference
        .collection("products")

    private val stockMovements = workspaceReference
        .collection("stockMovements")

    // ==========================================
    // ESCUCHAR PRODUCTOS
    // ==========================================

    fun listen(
        onSuccess: (List<InventoryProduct>) -> Unit,
        onError: (Exception) -> Unit
    ): ListenerRegistration {

        return products.addSnapshotListener { snapshot, error ->

            if (error != null) {

                onError(error)

                return@addSnapshotListener
            }

            val result = snapshot?.documents
                ?.mapNotNull { document ->

                    val name = document.getString("name")
                        ?: return@mapNotNull null

                    val quantity = document.getLong("quantity")
                        ?: return@mapNotNull null

                    val minimum = document.getLong("minQuantity")
                        ?: return@mapNotNull null

                    val cost = document.getLong("costCents")
                        ?: return@mapNotNull null

                    val price = document.getLong("priceCents")
                        ?: return@mapNotNull null

                    if (
                        quantity !in 0L..MAX_QUANTITY.toLong() ||
                        minimum !in 0L..MAX_QUANTITY.toLong() ||
                        cost !in 0L..MAX_AMOUNT_CENTS ||
                        price !in 0L..MAX_AMOUNT_CENTS
                    ) {

                        return@mapNotNull null
                    }

                    InventoryProduct(
                        id = document.id,

                        name = name,

                        sku = document.getString("sku")
                            ?: "",

                        description = document.getString(
                            "description"
                        ) ?: "",

                        quantity = quantity.toInt(),

                        minQuantity = minimum.toInt(),

                        costCents = cost,

                        priceCents = price,

                        createdAt = document.getTimestamp(
                            "createdAt"
                        ),

                        updatedAt = document.getTimestamp(
                            "updatedAt"
                        )
                    )
                }
                ?.sortedBy {
                    it.name.lowercase(Locale.ROOT)
                }
                ?: emptyList()

            onSuccess(result)
        }
    }

    // ==========================================
    // ESCUCHAR HISTORIAL
    // ==========================================

    fun listenStockMovements(
        onSuccess: (List<StockMovement>) -> Unit,
        onError: (Exception) -> Unit
    ): ListenerRegistration {

        return stockMovements
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

                val result = snapshot?.documents
                    ?.mapNotNull { document ->

                        val type = document.getString("type")
                            ?: return@mapNotNull null

                        if (
                            StockMovementType.entries.none {
                                it.name == type
                            }
                        ) {

                            return@mapNotNull null
                        }

                        val delta = document.getLong(
                            "quantityDelta"
                        ) ?: return@mapNotNull null

                        val previous = document.getLong(
                            "previousQuantity"
                        ) ?: return@mapNotNull null

                        val resulting = document.getLong(
                            "resultingQuantity"
                        ) ?: return@mapNotNull null

                        if (
                            delta !in
                                -MAX_QUANTITY.toLong()..
                                    MAX_QUANTITY.toLong() ||
                            previous !in
                                0L..MAX_QUANTITY.toLong() ||
                            resulting !in
                                0L..MAX_QUANTITY.toLong()
                        ) {

                            return@mapNotNull null
                        }

                        StockMovement(
                            id = document.id,

                            productId = document.getString(
                                "productId"
                            ) ?: "",

                            productName = document.getString(
                                "productName"
                            ) ?: "",

                            type = type,

                            quantityDelta = delta.toInt(),

                            previousQuantity = previous.toInt(),

                            resultingQuantity = resulting.toInt(),

                            note = document.getString("note")
                                ?: "",

                            createdAt = document.getTimestamp(
                                "createdAt"
                            )
                        )
                    }
                    ?: emptyList()

                onSuccess(result)
            }
    }

    // ==========================================
    // VALIDAR PRODUCTO
    // ==========================================

    private fun validateProduct(
        product: InventoryProduct
    ) {

        require(product.name.trim().length in 1..80) {
            "El nombre debe tener entre 1 y 80 caracteres."
        }

        require(product.sku.length <= 40) {
            "El SKU no puede superar 40 caracteres."
        }

        require(product.description.length <= 200) {
            "La descripción es demasiado larga."
        }

        require(product.quantity in 0..MAX_QUANTITY) {
            "La cantidad no es válida."
        }

        require(product.minQuantity in 0..MAX_QUANTITY) {
            "El stock mínimo no es válido."
        }

        require(
            product.costCents in 0L..MAX_AMOUNT_CENTS
        ) {
            "El costo no es válido."
        }

        require(
            product.priceCents in 0L..MAX_AMOUNT_CENTS
        ) {
            "El precio no es válido."
        }
    }

    // ==========================================
    // CREAR O EDITAR PRODUCTO
    // ==========================================

    fun save(
        product: InventoryProduct,
        onSuccess: () -> Unit,
        onError: (Exception) -> Unit
    ) {

        validateProduct(product)

        val metadata = hashMapOf<String, Any>(
            "name" to product.name.trim(),

            "sku" to product.sku.trim(),

            "description" to product.description.trim(),

            "minQuantity" to product.minQuantity,

            "costCents" to product.costCents,

            "priceCents" to product.priceCents,

            "updatedAt" to FieldValue.serverTimestamp()
        )

        if (product.id.isBlank()) {

            val reference = products.document()

            metadata["quantity"] = product.quantity

            metadata["createdAt"] =
                FieldValue.serverTimestamp()

            reference.set(metadata)
                .addOnSuccessListener {

                    onSuccess()
                }
                .addOnFailureListener {

                    onError(it)
                }

        } else {

            // Al editar un producto ya existente,
            // NO modificamos la cantidad.
            //
            // Las cantidades solamente cambian
            // mediante movimientos de stock.

            products.document(product.id)
                .update(metadata)
                .addOnSuccessListener {

                    onSuccess()
                }
                .addOnFailureListener {

                    onError(it)
                }
        }
    }

    // ==========================================
    // REGISTRAR MOVIMIENTO DE STOCK
    // ==========================================

    fun registerStockMovement(
        operationId: String,
        productId: String,
        type: StockMovementType,
        quantityValue: Int,
        note: String,
        onSuccess: () -> Unit,
        onError: (Exception) -> Unit
    ) {

        require(operationId.isNotBlank()) {
            "Identificador de operación inválido."
        }

        require(productId.isNotBlank()) {
            "Selecciona un producto."
        }

        require(note.length <= 200) {
            "La descripción es demasiado larga."
        }

        require(quantityValue in 0..MAX_QUANTITY) {
            "La cantidad no es válida."
        }

        if (type != StockMovementType.ADJUSTMENT) {

            require(quantityValue > 0) {
                "La cantidad debe ser mayor que cero."
            }
        }

        val productReference = products.document(
            productId
        )

        val movementReference = stockMovements.document(
            operationId
        )

        database.runTransaction { transaction ->

            // Primero leer todos los documentos.
            // Firestore puede repetir la transacción
            // si detecta cambios simultáneos.

            val existingMovement = transaction.get(
                movementReference
            )

            if (existingMovement.exists()) {

                // El identificador ya fue utilizado.
                // No volvemos a alterar el stock.

                val existingProductId =
                    existingMovement.getString("productId")

                if (existingProductId != productId) {

                    throw IllegalStateException(
                        "La operación pertenece a otro producto."
                    )
                }

                return@runTransaction Unit
            }

            val snapshot = transaction.get(
                productReference
            )

            if (!snapshot.exists()) {

                throw IllegalStateException(
                    "El producto ya no existe."
                )
            }

            val previousLong = snapshot.getLong(
                "quantity"
            ) ?: throw IllegalStateException(
                "El producto no tiene un stock válido."
            )

            require(
                previousLong in
                    0L..MAX_QUANTITY.toLong()
            ) {
                "El stock actual no es válido."
            }

            val previous = previousLong.toInt()

            val productName = snapshot.getString(
                "name"
            ) ?: throw IllegalStateException(
                "El producto no tiene nombre."
            )

            val resulting = when (type) {

                StockMovementType.ENTRY -> {

                    previous.toLong() +
                        quantityValue.toLong()
                }

                StockMovementType.EXIT -> {

                    previous.toLong() -
                        quantityValue.toLong()
                }

                StockMovementType.ADJUSTMENT -> {

                    quantityValue.toLong()
                }
            }

            require(
                resulting in
                    0L..MAX_QUANTITY.toLong()
            ) {

                if (resulting < 0L) {

                    "Stock insuficiente para registrar la salida."

                } else {

                    "Se supera el máximo permitido de unidades."
                }
            }

            val newQuantity = resulting.toInt()

            require(newQuantity != previous) {
                "La cantidad no cambió."
            }

            val delta = newQuantity - previous

            val movementData = hashMapOf<String, Any>(

                "productId" to productId,

                "productName" to productName,

                "type" to type.name,

                "quantityDelta" to delta,

                "previousQuantity" to previous,

                "resultingQuantity" to newQuantity,

                "note" to note.trim(),

                "createdAt" to FieldValue.serverTimestamp()
            )

            // Ambas escrituras forman una sola
            // transacción atómica.

            transaction.update(
                productReference,

                mapOf(
                    "quantity" to newQuantity,

                    "lastStockMovementId" to operationId,

                    "updatedAt" to
                        FieldValue.serverTimestamp()
                )
            )

            transaction.set(
                movementReference,
                movementData
            )

            Unit

        }.addOnSuccessListener {

            onSuccess()

        }.addOnFailureListener {

            onError(it)
        }
    }

    // ==========================================
    // ELIMINAR PRODUCTO
    // ==========================================

    fun delete(
        productId: String,
        onSuccess: () -> Unit,
        onError: (Exception) -> Unit
    ) {

        require(productId.isNotBlank()) {
            "Producto no válido."
        }

        products.document(productId)
            .delete()
            .addOnSuccessListener {

                onSuccess()
            }
            .addOnFailureListener {

                onError(it)
            }
    }
}
