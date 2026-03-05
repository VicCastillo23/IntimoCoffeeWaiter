package com.intimocoffee.waiter.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.math.BigDecimal

@Entity(tableName = "orders")
data class OrderEntity(
    @PrimaryKey
    val id: String,
    val orderNumber: Int,
    val tableId: String? = null,
    val tableName: String? = null, // Nuevo campo opcional
    val customerName: String? = null, // Renombrado de customerId
    val customerId: String? = null, // Mantener para compatibilidad
    val userId: String, // Employee who created the order
    val status: String, // PENDING, SENT_TO_KITCHEN, SENT_TO_BAR, IN_PREPARATION, READY, DELIVERED, PAID, CANCELLED
    val subtotal: String, // Stored as String to avoid precision issues
    val tax: String = "0.00",
    val discount: String = "0.00",
    val total: String,
    val paymentMethod: String? = null,
    val paymentStatus: String = "PENDING", // PENDING, PAID, PARTIAL, REFUNDED
    val notes: String? = null,
    val originalOrderId: String? = null, // Reference to original order if this is an additional order
    val isAdditionalOrder: Boolean = false, // Flag to identify additional orders
    
    // Nuevos campos para tracking de tiempo
    val sentToKitchenAt: Long? = null,
    val sentToBarAt: Long? = null,
    val preparationStartedAt: Long? = null,
    val readyAt: Long? = null,
    val deliveredAt: Long? = null,
    val completedAt: Long? = null,
    
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    fun getSubtotalAsBigDecimal(): BigDecimal = BigDecimal(subtotal)
    fun getTaxAsBigDecimal(): BigDecimal = BigDecimal(tax)
    fun getDiscountAsBigDecimal(): BigDecimal = BigDecimal(discount)
    fun getTotalAsBigDecimal(): BigDecimal = BigDecimal(total)
}
