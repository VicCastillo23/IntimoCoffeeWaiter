package com.intimocoffee.waiter.feature.products.data.mapper

import com.intimocoffee.waiter.core.database.entity.ProductEntity
import com.intimocoffee.waiter.feature.products.domain.model.Product
import java.math.BigDecimal
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProductMapper @Inject constructor() {
    
    fun toDomain(entity: ProductEntity): Product {
        return Product(
            id = entity.id.toLongOrNull() ?: 0L,
            rawId = entity.id,
            name = entity.name,
            description = entity.description,
            price = BigDecimal(entity.price),
            categoryId = entity.categoryId.toLongOrNull() ?: 0L,
            categoryName = null, // Will be populated separately if needed
            imageUrl = entity.imageUrl,
            isActive = entity.isActive,
            stockQuantity = entity.stockQuantity,
            minStockLevel = entity.minStockLevel,
            barcode = entity.barcode,
            taxRatePercent = entity.taxRatePercent?.takeIf { it.isNotBlank() }?.let {
                runCatching { BigDecimal(it) }.getOrNull()
            }
        )
    }
    
    fun toEntity(product: Product): ProductEntity {
        return ProductEntity(
            id = when {
                product.rawId.isNotBlank() -> product.rawId
                product.id == 0L -> generateId()
                else -> product.id.toString()
            },
            name = product.name,
            description = product.description,
            price = product.price.toString(),
            categoryId = product.categoryId.toString(),
            imageUrl = product.imageUrl,
            isActive = product.isActive,
            stockQuantity = product.stockQuantity,
            minStockLevel = product.minStockLevel,
            barcode = product.barcode,
            taxRatePercent = product.taxRatePercent?.stripTrailingZeros()?.toPlainString()
        )
    }
    
    private fun generateId(): String = java.util.UUID.randomUUID().toString()
}