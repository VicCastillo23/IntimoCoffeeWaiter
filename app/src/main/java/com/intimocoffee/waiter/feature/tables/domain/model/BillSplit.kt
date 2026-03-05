package com.intimocoffee.waiter.feature.tables.domain.model

import com.intimocoffee.waiter.feature.orders.domain.model.OrderItem
import com.intimocoffee.waiter.feature.tables.presentation.PaymentMethod
import java.math.BigDecimal

enum class SplitType {
    BY_PRODUCTS,    // Por selección de productos
    EQUAL_PARTS     // Por partes iguales
}

data class BillSplit(
    val type: SplitType,
    val personCount: Int,
    val persons: List<PersonSplit>,
    val originalTotal: BigDecimal,
    val originalSubtotal: BigDecimal,
    val originalTax: BigDecimal
)

data class PersonSplit(
    val personNumber: Int,
    val name: String,
    val items: List<OrderItem> = emptyList(), // Solo para división por productos
    val subtotal: BigDecimal,
    val tax: BigDecimal,
    val total: BigDecimal,
    val paymentMethod: PaymentMethod? = null,
    val isPaid: Boolean = false
) {
    val displayName: String
        get() = if (name.isBlank()) "Persona $personNumber" else name
}

data class ProductAssignment(
    val orderItem: OrderItem,
    val assignedPersons: Set<Int>, // Números de persona a los que está asignado
    val splitQuantity: Map<Int, Int> // Persona -> cantidad asignada
) {
    val isFullyAssigned: Boolean
        get() = splitQuantity.values.sum() == orderItem.quantity
        
    val unassignedQuantity: Int
        get() = orderItem.quantity - splitQuantity.values.sum()
}