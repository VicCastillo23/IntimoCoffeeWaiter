package com.intimocoffee.waiter.feature.products.domain.repository

import com.intimocoffee.waiter.feature.products.domain.model.Product
import kotlinx.coroutines.flow.Flow

interface ProductRepository {
    fun getAllActiveProducts(): Flow<List<Product>>
    fun getProductsByCategory(categoryId: Long): Flow<List<Product>>
    suspend fun getProductById(id: Long): Product?
    fun searchProducts(query: String): Flow<List<Product>>
    suspend fun getProductByBarcode(barcode: String): Product?
    suspend fun createProduct(product: Product): Boolean
    suspend fun updateProduct(product: Product): Boolean
    suspend fun deactivateProduct(productId: Long): Boolean
}
