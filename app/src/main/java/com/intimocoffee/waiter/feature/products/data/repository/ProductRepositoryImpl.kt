package com.intimocoffee.waiter.feature.products.data.repository

import android.util.Log
import com.intimocoffee.waiter.core.database.dao.ProductDao
import com.intimocoffee.waiter.core.network.ApiMappers.toProductDomainModels
import com.intimocoffee.waiter.core.network.RemoteOrderService
import com.intimocoffee.waiter.feature.products.data.mapper.ProductMapper
import com.intimocoffee.waiter.feature.products.domain.model.Product
import com.intimocoffee.waiter.feature.products.domain.repository.ProductRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProductRepositoryImpl @Inject constructor(
    private val remoteOrderService: RemoteOrderService,
    private val productDao: ProductDao,
    private val productMapper: ProductMapper
) : ProductRepository {
    
    override fun getAllActiveProducts(): Flow<List<Product>> = flow {
        try {
            Log.d("ProductRepository", "Fetching products from remote server (online-only)...")
            val result = remoteOrderService.getProductsFromServer()

            if (result.isSuccess) {
                val products = result.getOrNull()
                    ?.toProductDomainModels()
                    ?.filter { it.isActive }
                    ?: emptyList()
                Log.d(
                    "ProductRepository",
                    "Successfully fetched ${products.size} active products from server"
                )
                emit(products)
            } else {
                Log.e(
                    "ProductRepository",
                    "Failed to fetch products from server (no local fallback)",
                    result.exceptionOrNull()
                )
                emit(emptyList())
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(
                "ProductRepository",
                "Exception fetching products from server (no local fallback)",
                e
            )
            emit(emptyList())
        }
    }
    
    override fun getProductsByCategory(categoryId: Long): Flow<List<Product>> = flow {
        try {
            Log.d("ProductRepository", "Fetching products by category $categoryId from remote server (online-only)...")
            val result = remoteOrderService.getProductsFromServer()

            if (result.isSuccess) {
                val products = result.getOrNull()
                    ?.toProductDomainModels()
                    ?.filter { it.isActive && it.categoryId == categoryId }
                    ?: emptyList()
                Log.d(
                    "ProductRepository",
                    "Successfully fetched ${products.size} products for category $categoryId"
                )
                emit(products)
            } else {
                Log.e(
                    "ProductRepository",
                    "Failed to fetch products by category from server (no local fallback)",
                    result.exceptionOrNull()
                )
                emit(emptyList())
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(
                "ProductRepository",
                "Exception fetching products by category (no local fallback)",
                e
            )
            emit(emptyList())
        }
    }
    
    override suspend fun getProductById(id: Long): Product? {
        return try {
            Log.d("ProductRepository", "Fetching product $id from remote server (online-only)...")
            val result = remoteOrderService.getProductsFromServer()

            if (result.isSuccess) {
                val product = result.getOrNull()
                    ?.toProductDomainModels()
                    ?.find { it.id == id }
                Log.d(
                    "ProductRepository",
                    "Product $id found in server response: ${product?.name ?: "Not found"}"
                )
                product
            } else {
                Log.e(
                    "ProductRepository",
                    "Failed to fetch product from server (no local fallback)",
                    result.exceptionOrNull()
                )
                null
            }
        } catch (e: Exception) {
            Log.e(
                "ProductRepository",
                "Exception fetching product from server (no local fallback)",
                e
            )
            null
        }
    }
    
    override fun searchProducts(query: String): Flow<List<Product>> = flow {
        try {
            Log.d("ProductRepository", "Searching products with query '$query' from remote server (online-only)...")
            val result = remoteOrderService.getProductsFromServer()

            if (result.isSuccess) {
                val products = result.getOrNull()
                    ?.toProductDomainModels()
                    ?.filter {
                        it.isActive && (
                            it.name.contains(query, ignoreCase = true) ||
                                it.description?.contains(query, ignoreCase = true) == true
                        )
                    }
                    ?: emptyList()
                Log.d(
                    "ProductRepository",
                    "Found ${products.size} products matching '$query' in server response"
                )
                emit(products)
            } else {
                Log.e(
                    "ProductRepository",
                    "Failed to search products from server (no local fallback)",
                    result.exceptionOrNull()
                )
                emit(emptyList())
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(
                "ProductRepository",
                "Exception searching products from server (no local fallback)",
                e
            )
            emit(emptyList())
        }
    }
    
    override suspend fun getProductByBarcode(barcode: String): Product? {
        return try {
            Log.d(
                "ProductRepository",
                "Searching product by barcode '$barcode' from remote server (online-only)..."
            )
            val result = remoteOrderService.getProductsFromServer()

            if (result.isSuccess) {
                val product = result.getOrNull()
                    ?.toProductDomainModels()
                    ?.find { it.barcode == barcode && it.isActive }
                Log.d(
                    "ProductRepository",
                    "Product with barcode '$barcode' found in server response: ${product?.name ?: "Not found"}"
                )
                product
            } else {
                Log.e(
                    "ProductRepository",
                    "Failed to fetch product by barcode from server (no local fallback)",
                    result.exceptionOrNull()
                )
                null
            }
        } catch (e: Exception) {
            Log.e(
                "ProductRepository",
                "Exception fetching product by barcode from server (no local fallback)",
                e
            )
            null
        }
    }
    
    override suspend fun createProduct(product: Product): Boolean {
        return try {
            productDao.insertProduct(productMapper.toEntity(product))
            true
        } catch (e: Exception) {
            false
        }
    }
    
    override suspend fun updateProduct(product: Product): Boolean {
        return try {
            productDao.updateProduct(productMapper.toEntity(product))
            true
        } catch (e: Exception) {
            false
        }
    }
    
    override suspend fun deactivateProduct(productId: Long): Boolean {
        return try {
            productDao.deactivateProduct(productId.toString())
            true
        } catch (e: Exception) {
            false
        }
    }
}
