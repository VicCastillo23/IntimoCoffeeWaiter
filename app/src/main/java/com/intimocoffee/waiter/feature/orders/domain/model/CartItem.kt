package com.intimocoffee.waiter.feature.orders.domain.model

import com.intimocoffee.waiter.feature.products.domain.model.Product
import java.math.BigDecimal

data class CartItem(
    val product: Product,
    val quantity: Int,
    val notes: String? = null,
    val unitPrice: BigDecimal = product.price
) {
    val subtotal: BigDecimal
        get() = unitPrice.multiply(BigDecimal(quantity))
    
    fun withQuantity(newQuantity: Int): CartItem {
        return copy(quantity = newQuantity)
    }
    
    fun withNotes(newNotes: String?): CartItem {
        return copy(notes = newNotes)
    }
}