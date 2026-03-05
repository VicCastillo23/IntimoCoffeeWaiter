package com.intimocoffee.waiter.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.math.BigDecimal

@Entity(tableName = "order_items")
data class OrderItemEntity(
    @PrimaryKey
    val id: String,
    val orderId: String,
    val productId: String,
    val productName: String? = null, // Nuevo campo opcional
    val categoryId: Long? = null, // Para dividir cocina/barra
    val quantity: Int,
    val unitPrice: String, // Stored as String to avoid precision issues
    val totalPrice: String,
    val notes: String? = null,
    
    // Nuevos campos para tracking individual
    val itemStatus: String? = "PENDING", // OrderItemStatus as string
    val sentToKitchenAt: Long? = null,
    val preparationStartedAt: Long? = null,
    val readyAt: Long? = null,
    val deliveredAt: Long? = null,
    val preparationTimeMinutes: Int? = null,
    
    val createdAt: Long = System.currentTimeMillis()
) {
    fun getUnitPriceAsBigDecimal(): BigDecimal = BigDecimal(unitPrice)
    fun getTotalPriceAsBigDecimal(): BigDecimal = BigDecimal(totalPrice)
}
