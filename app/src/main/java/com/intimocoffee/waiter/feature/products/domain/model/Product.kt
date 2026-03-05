package com.intimocoffee.waiter.feature.products.domain.model

import java.math.BigDecimal

data class Product(
    val id: Long,
    val name: String,
    val description: String?,
    val price: BigDecimal,
    val categoryId: Long,
    val categoryName: String? = null,
    val imageUrl: String?,
    val isActive: Boolean,
    val stockQuantity: Int?,
    val minStockLevel: Int?,
    val barcode: String?
) {
    val isInStock: Boolean
        get() = stockQuantity?.let { it > 0 } ?: true
    
    val isLowStock: Boolean
        get() = stockQuantity?.let { stock ->
            minStockLevel?.let { min -> stock <= min } ?: false
        } ?: false
}