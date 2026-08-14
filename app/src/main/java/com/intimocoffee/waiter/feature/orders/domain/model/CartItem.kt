package com.intimocoffee.waiter.feature.orders.domain.model

import com.intimocoffee.waiter.feature.products.domain.model.Product
import java.math.BigDecimal
import java.math.RoundingMode

data class CartItem(
    val product: Product,
    val quantity: Int,
    val notes: String? = null,
    val unitPrice: BigDecimal = product.price
) {
    /** Identifica una sola fila del carrito (producto + nota + precio unitario). */
    fun rowKey(): String {
        val priceKey = unitPrice.setScale(2, RoundingMode.HALF_UP).toPlainString()
        return "${product.cartLineKey()}\u0001${notes ?: ""}\u0001$priceKey"
    }

    val subtotal: BigDecimal
        get() = unitPrice.multiply(BigDecimal(quantity))
    
    fun withQuantity(newQuantity: Int): CartItem {
        return copy(quantity = newQuantity)
    }
    
    fun withNotes(newNotes: String?): CartItem {
        return copy(notes = newNotes)
    }
}
