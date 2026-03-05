package com.intimocoffee.waiter.core.network

import android.util.Log
import com.intimocoffee.waiter.feature.orders.domain.model.CartItem
import com.intimocoffee.waiter.feature.orders.domain.model.Order
import com.intimocoffee.waiter.feature.orders.domain.model.OrderItem
import com.intimocoffee.waiter.feature.orders.domain.model.OrderStatus
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RemoteOrderService @Inject constructor(
    private val retrofitProvider: DynamicRetrofitProvider
) {
    
    /**
     * Creates an order on the main IntimoCoffeeApp server.
     *
     * Por ahora seguimos usando la API-First de creación de órdenes (POST /api/orders)
     * para no romper la integración existente con IntimoCoffeeApp, que espera un
     * CreateOrderRequest en su backend REST. El HttpServer embebido (Ktor) sigue
     * recibiendo órdenes desde MAIN/KITCHEN/BAR vía sus propios modelos NetworkOrder.
     */
    suspend fun createOrderOnServer(
        tableId: Long?,
        tableName: String?,
        customerName: String?,
        cartItems: List<CartItem>,
        notes: String?,
        createdBy: Long
    ): Result<Long> {
        return try {
            Log.d("RemoteOrderService", "Creating order on server - Table: $tableId, Items: ${cartItems.size}")
            
            val createOrderItems = cartItems.map { cartItem ->
                CreateOrderItemRequest(
                    productId = cartItem.product.id,
                    productName = cartItem.product.name,
                    quantity = cartItem.quantity,
                    unitPrice = cartItem.unitPrice.toString(),
                    subtotal = cartItem.subtotal.toString(),
                    notes = cartItem.notes,
                    categoryId = cartItem.product.categoryId
                )
            }
            
            val request = CreateOrderRequest(
                tableId = tableId,
                tableName = tableName,
                customerName = customerName,
                items = createOrderItems,
                notes = notes,
                createdBy = createdBy
            )
            
            Log.d("RemoteOrderService", "Sending request to server (API-First): $request")
            
            val response = retrofitProvider.getApiService().createOrder(request)
            
            if (response.isSuccessful && response.body()?.success == true) {
                val orderId = response.body()?.data?.orderId ?: 0L
                Log.d("RemoteOrderService", "Order created successfully on server with ID: $orderId")
                Result.success(orderId)
            } else {
                val errorMessage = response.body()?.message ?: "Unknown error"
                Log.e("RemoteOrderService", "Failed to create order on server: $errorMessage")
                Result.failure(Exception("Server error: $errorMessage"))
            }
            
        } catch (e: Exception) {
            Log.e("RemoteOrderService", "Exception creating order on server: ${e.message}", e)
            // If connection fails, try to rediscover server
            if (e is java.net.ConnectException || e is java.net.UnknownHostException) {
                Log.i("RemoteOrderService", "Connection failed, triggering server rediscovery...")
                try {
                    retrofitProvider.rediscoverServer()
                    // Retry the call with new server
                    val retryCreateOrderItems = cartItems.map { cartItem ->
                        CreateOrderItemRequest(
                            productId = cartItem.product.id,
                            productName = cartItem.product.name,
                            quantity = cartItem.quantity,
                            unitPrice = cartItem.unitPrice.toString(),
                            subtotal = cartItem.subtotal.toString(),
                            notes = cartItem.notes,
                            categoryId = cartItem.product.categoryId
                        )
                    }
                    val retryRequest = CreateOrderRequest(
                        tableId = tableId,
                        tableName = tableName,
                        customerName = customerName,
                        items = retryCreateOrderItems,
                        notes = notes,
                        createdBy = createdBy
                    )
                    val retryResponse = retrofitProvider.getApiService().createOrder(retryRequest)
                    if (retryResponse.isSuccessful && retryResponse.body()?.success == true) {
                        val orderId = retryResponse.body()?.data?.orderId ?: 0L
                        Log.d("RemoteOrderService", "Retry successful! Order created with ID: $orderId")
                        return Result.success(orderId)
                    }
                } catch (e2: Exception) {
                    Log.e("RemoteOrderService", "Retry after rediscovery also failed: ${e2.message}")
                }
            }
            Result.failure(e)
        }
    }
    
    /**
     * Updates order status on the main server
     *
     * Usa el endpoint del HttpServer embebido de IntimoCoffeeApp (PUT /orders/status),
     * que recibe un cuerpo equivalente a OrderStatusUpdate.
     */
    suspend fun updateOrderStatusOnServer(
        orderId: Long,
        newStatus: OrderStatus,
        updatedBy: String = "WAITER"
    ): Result<Boolean> {
        return try {
            Log.d("RemoteOrderService", "Updating order $orderId status to $newStatus on server (HttpServer /orders/status)")
            
            val timestamp = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).toString()
            
            val request = UpdateOrderStatusRequest(
                orderId = orderId,
                itemId = null, // Full order update
                newStatus = newStatus.name,
                updatedBy = updatedBy,
                timestamp = timestamp
            )
            
            val response = retrofitProvider.getApiService().updateOrderStatus(request)
            
            if (response.isSuccessful && response.body()?.success == true) {
                Log.d("RemoteOrderService", "Order status updated successfully on server")
                Result.success(true)
            } else {
                val errorMessage = response.body()?.message ?: "Unknown error"
                Log.e("RemoteOrderService", "Failed to update order status on server: $errorMessage")
                Result.failure(Exception("Server error: $errorMessage"))
            }
            
        } catch (e: Exception) {
            Log.e("RemoteOrderService", "Exception updating order status on server: ${e.message}", e)
            // If connection fails, try to rediscover server
            if (e is java.net.ConnectException || e is java.net.UnknownHostException) {
                Log.i("RemoteOrderService", "Connection failed, triggering server rediscovery...")
                try {
                    retrofitProvider.rediscoverServer()
                    // Retry the call with new server
                    val timestamp = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).toString()
                    val retryRequest = UpdateOrderStatusRequest(
                        orderId = orderId,
                        itemId = null,
                        newStatus = newStatus.name,
                        updatedBy = updatedBy,
                        timestamp = timestamp
                    )
                    val retryResponse = retrofitProvider.getApiService().updateOrderStatus(retryRequest)
                    if (retryResponse.isSuccessful && retryResponse.body()?.success == true) {
                        Log.d("RemoteOrderService", "Retry successful! Status updated.")
                        return Result.success(true)
                    }
                } catch (e2: Exception) {
                    Log.e("RemoteOrderService", "Retry after rediscovery also failed: ${e2.message}")
                }
            }
            Result.failure(e)
        }
    }
    
    /**
     * Updates individual order item status on the main server (granular update)
     */
    suspend fun updateOrderItemStatusOnServer(
        orderId: Long,
        itemId: Long,
        newStatus: OrderStatus,
        updatedBy: String = "WAITER"
    ): Result<Boolean> {
        return try {
            Log.d("RemoteOrderService", "Updating order $orderId item $itemId status to $newStatus on server")
            
            val timestamp = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).toString()
            
            val request = UpdateItemStatusRequest(
                newStatus = newStatus.name,
                updatedBy = updatedBy,
                timestamp = timestamp
            )
            
            val response = retrofitProvider.getApiService().updateOrderItemStatus(orderId, itemId, request)
            
            if (response.isSuccessful && response.body()?.success == true) {
                Log.d("RemoteOrderService", "Order item status updated successfully on server")
                Result.success(true)
            } else {
                val errorMessage = response.body()?.message ?: "Unknown error"
                Log.e("RemoteOrderService", "Failed to update order item status on server: $errorMessage")
                Result.failure(Exception("Server error: $errorMessage"))
            }
            
        } catch (e: Exception) {
            Log.e("RemoteOrderService", "Exception updating order item status on server: ${e.message}", e)
            // If connection fails, try to rediscover server
            if (e is java.net.ConnectException || e is java.net.UnknownHostException) {
                Log.i("RemoteOrderService", "Connection failed, triggering server rediscovery...")
                try {
                    retrofitProvider.rediscoverServer()
                    // Retry the call with new server
                    val timestamp = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).toString()
                    val retryRequest = UpdateItemStatusRequest(
                        newStatus = newStatus.name,
                        updatedBy = updatedBy,
                        timestamp = timestamp
                    )
                    val retryResponse = retrofitProvider.getApiService().updateOrderItemStatus(orderId, itemId, retryRequest)
                    if (retryResponse.isSuccessful && retryResponse.body()?.success == true) {
                        Log.d("RemoteOrderService", "Retry successful! Item status updated.")
                        return Result.success(true)
                    }
                } catch (e2: Exception) {
                    Log.e("RemoteOrderService", "Retry after rediscovery also failed: ${e2.message}")
                }
            }
            Result.failure(e)
        }
    }
    
    /**
     * Gets active orders from the main server
     */
    suspend fun getActiveOrdersFromServer(): Result<List<OrderResponse>> {
        return try {
            Log.d("RemoteOrderService", "Fetching active orders from server")
            
            val response = retrofitProvider.getApiService().getActiveOrders()
            
            if (response.isSuccessful) {
                val orders = response.body() ?: emptyList()
                Log.d("RemoteOrderService", "Retrieved ${orders.size} active orders from server")
                Result.success(orders)
            } else {
                Log.e("RemoteOrderService", "Failed to fetch active orders from server")
                Result.failure(Exception("Failed to fetch orders"))
            }
            
        } catch (e: Exception) {
            Log.e("RemoteOrderService", "Exception fetching active orders from server: ${e.message}", e)
            // If connection fails, try to rediscover server
            if (e is java.net.ConnectException || e is java.net.UnknownHostException) {
                Log.i("RemoteOrderService", "Connection failed, triggering server rediscovery...")
                try {
                    retrofitProvider.rediscoverServer()
                    // Retry the call with new server
                    val retryResponse = retrofitProvider.getApiService().getActiveOrders()
                    if (retryResponse.isSuccessful) {
                        val orders = retryResponse.body() ?: emptyList()
                        Log.d("RemoteOrderService", "Retry successful! Retrieved ${orders.size} orders from new server")
                        return Result.success(orders)
                    }
                } catch (e2: Exception) {
                    Log.e("RemoteOrderService", "Retry after rediscovery also failed: ${e2.message}")
                }
            }
            Result.failure(e)
        }
    }
    
    /**
     * Gets products from the main server
     */
    suspend fun getProductsFromServer(): Result<List<ProductResponse>> {
        return try {
            Log.d("RemoteOrderService", "Fetching products from server")
            
            val response = retrofitProvider.getApiService().getAllProducts()
            
            if (response.isSuccessful) {
                val products = response.body() ?: emptyList()
                Log.d("RemoteOrderService", "Retrieved ${products.size} products from server")
                Result.success(products)
            } else {
                Log.e("RemoteOrderService", "Failed to fetch products from server")
                Result.failure(Exception("Failed to fetch products"))
            }
            
        } catch (e: Exception) {
            Log.e("RemoteOrderService", "Exception fetching products from server: ${e.message}", e)
            // If connection fails, try to rediscover server
            if (e is java.net.ConnectException || e is java.net.UnknownHostException) {
                Log.i("RemoteOrderService", "Connection failed, triggering server rediscovery...")
                try {
                    retrofitProvider.rediscoverServer()
                    // Retry the call with new server
                    val retryResponse = retrofitProvider.getApiService().getAllProducts()
                    if (retryResponse.isSuccessful) {
                        val products = retryResponse.body() ?: emptyList()
                        Log.d("RemoteOrderService", "Retry successful! Retrieved ${products.size} products from new server")
                        return Result.success(products)
                    }
                } catch (e2: Exception) {
                    Log.e("RemoteOrderService", "Retry after rediscovery also failed: ${e2.message}")
                }
            }
            Result.failure(e)
        }
    }
    
    /**
     * Gets tables from the main server
     */
    suspend fun getTablesFromServer(): Result<List<TableResponse>> {
        return try {
            Log.d("RemoteOrderService", "Fetching tables from server")
            
            val response = retrofitProvider.getApiService().getAllTables()
            
            if (response.isSuccessful) {
                val tables = response.body() ?: emptyList()
                Log.d("RemoteOrderService", "Retrieved ${tables.size} tables from server")
                Result.success(tables)
            } else {
                Log.e("RemoteOrderService", "Failed to fetch tables from server")
                Result.failure(Exception("Failed to fetch tables"))
            }
            
        } catch (e: Exception) {
            Log.e("RemoteOrderService", "Exception fetching tables from server: ${e.message}", e)
            // If connection fails, try to rediscover server
            if (e is java.net.ConnectException || e is java.net.UnknownHostException) {
                Log.i("RemoteOrderService", "Connection failed, triggering server rediscovery...")
                try {
                    retrofitProvider.rediscoverServer()
                    // Retry the call with new server
                    val retryResponse = retrofitProvider.getApiService().getAllTables()
                    if (retryResponse.isSuccessful) {
                        val tables = retryResponse.body() ?: emptyList()
                        Log.d("RemoteOrderService", "Retry successful! Retrieved ${tables.size} tables from new server")
                        return Result.success(tables)
                    }
                } catch (e2: Exception) {
                    Log.e("RemoteOrderService", "Retry after rediscovery also failed: ${e2.message}")
                }
            }
            Result.failure(e)
        }
    }
}