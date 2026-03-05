package com.intimocoffee.waiter.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.math.BigDecimal

@Entity(tableName = "products")
data class ProductEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val description: String? = null,
    val price: String, // Stored as String to avoid precision issues with BigDecimal in Room
    val categoryId: String,
    val imageUrl: String? = null,
    val isActive: Boolean = true,
    val stockQuantity: Int? = null,
    val minStockLevel: Int? = null,
    val barcode: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    fun getPriceAsBigDecimal(): BigDecimal = BigDecimal(price)
    
    companion object {
        fun fromPrice(price: BigDecimal): String = price.toString()
    }
}