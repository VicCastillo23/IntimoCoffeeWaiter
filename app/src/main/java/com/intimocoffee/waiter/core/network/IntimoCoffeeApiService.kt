package com.intimocoffee.waiter.core.network

import com.intimocoffee.waiter.feature.orders.domain.model.Order
import com.intimocoffee.waiter.feature.orders.domain.model.OrderItem
import com.intimocoffee.waiter.feature.orders.domain.model.OrderStatus
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import retrofit2.Response
import retrofit2.http.*
import java.math.BigDecimal

/**
 * API Service to communicate with IntimoCoffeeApp main server.
 *
 * Hay dos capas coexistiendo:
 * - HttpServer embebido en IntimoCoffeeApp (Ktor) con rutas como /health, /orders, /orders/status
 * - API REST "API-First" con prefijo /api (por ejemplo /api/orders, /api/products)
 *
 * Para órdenes usamos ambas según el caso:
 * - Crear orden: POST /api/orders (API-First)
 * - Actualizar estado: PUT /orders/status (HttpServer)
 */
interface IntimoCoffeeApiService {
    
    // --- Endpoints usados por Waiter contra el HttpServer de IntimoCoffeeApp ---
    // NOTA: el HttpServer actual expone POST /orders y PUT /orders/status con modelos
    // NetworkOrder y OrderStatusUpdate. Para crear órdenes desde Waiter usamos
    // la API-First anterior (/api/orders) mientras migramos por completo.

    // Login de usuarios (autenticación online contra el servidor principal)
    @POST("api/login")
    suspend fun login(@Body request: LoginRequest): Response<LoginResponse>

    // API-First de creación de órdenes (servidor REST principal)
    @POST("api/orders")
    suspend fun createOrder(@Body request: CreateOrderRequest): Response<CreateOrderResponse>
    
    // Actualización de estado de orden usando el HttpServer (PUT /orders/status)
    // Nota: reusamos UpdateOrderStatusRequest como wrapper compatible con OrderStatusUpdate.
    @PUT("orders/status")
    suspend fun updateOrderStatus(@Body request: UpdateOrderStatusRequest): Response<ApiResponse>
    
    // Endpoints legacy/granulares (si se habilitan en el servidor REST principal)
    @PUT("api/orders/{orderId}/items/{itemId}/status") 
    suspend fun updateOrderItemStatus(
        @Path("orderId") orderId: Long,
        @Path("itemId") itemId: Long,
        @Body request: UpdateItemStatusRequest
    ): Response<ApiResponse>
    
    // Lectura de órdenes activas desde API-First (siempre que el servidor REST esté disponible)
    @GET("api/orders/active")
    suspend fun getActiveOrders(): Response<List<OrderResponse>>
    
    // TODO: estos endpoints apuntan al servidor REST principal; el HttpServer aún no expone
    // productos/mesas. Los mantenemos por compatibilidad con la configuración API-First.
    @GET("api/products")
    suspend fun getAllProducts(): Response<List<ProductResponse>>

    @GET("api/categories")
    suspend fun getAllCategories(): Response<List<CategoryResponse>>
    
    @GET("api/tables")
    suspend fun getAllTables(): Response<List<TableResponse>>

    // --- Loyalty endpoints (corresponden al servidor Ktor de IntimoCoffeeApp) ---

    /** Busca un cliente por teléfono. Retorna 200+data si existe, 401 si no. */
    @POST("loyalty/customer/login")
    suspend fun loyaltyLoginByPhone(@Body request: LoyaltyLoginRequest): Response<LoyaltyLoginApiResponse>

    /** Vincula una orden a un cliente del programa de lealtad (suma puntos en el servidor). */
    @POST("loyalty/link-order")
    suspend fun loyaltyLinkOrder(@Body request: LoyaltyLinkOrderRequest): Response<ApiResponse>
}

// Request/Response DTOs
@Serializable
data class CreateOrderRequest(
    @SerialName("tableId") val tableId: Long?,
    @SerialName("tableName") val tableName: String?,
    @SerialName("customerName") val customerName: String?,
    @SerialName("items") val items: List<CreateOrderItemRequest>,
    @SerialName("notes") val notes: String?,
    @SerialName("createdBy") val createdBy: Long
)

@Serializable
data class CreateOrderItemRequest(
    @SerialName("productId") val productId: Long,
    @SerialName("productName") val productName: String,
    @SerialName("quantity") val quantity: Int,
    @SerialName("unitPrice") val unitPrice: String, // BigDecimal as String
    @SerialName("subtotal") val subtotal: String,   // BigDecimal as String
    @SerialName("notes") val notes: String?,
    @SerialName("categoryId") val categoryId: Long
)

@Serializable
data class CreateOrderResponse(
    @SerialName("success") val success: Boolean,
    @SerialName("data") val data: OrderCreatedData?,
    @SerialName("message") val message: String?,
    @SerialName("timestamp") val timestamp: String
)

@Serializable
data class OrderCreatedData(
    @SerialName("orderId") val orderId: Long,
    @SerialName("orderNumber") val orderNumber: String
)

@Serializable
data class UpdateOrderStatusRequest(
    @SerialName("orderId") val orderId: Long,
    @SerialName("itemId") val itemId: Long? = null, // For granular updates
    @SerialName("newStatus") val newStatus: String,
    @SerialName("updatedBy") val updatedBy: String,
    @SerialName("timestamp") val timestamp: String
)

@Serializable
data class UpdateItemStatusRequest(
    @SerialName("newStatus") val newStatus: String,
    @SerialName("updatedBy") val updatedBy: String,
    @SerialName("timestamp") val timestamp: String
)

@Serializable
data class ApiResponse(
    @SerialName("success") val success: Boolean,
    @SerialName("data") val data: String? = null,
    @SerialName("message") val message: String? = null,
    @SerialName("timestamp") val timestamp: String
)

@Serializable
data class OrderResponse(
    @SerialName("id") val id: Long,
    @SerialName("orderNumber") val orderNumber: String,
    @SerialName("tableId") val tableId: Long?,
    @SerialName("tableName") val tableName: String?,
    @SerialName("customerName") val customerName: String?,
    @SerialName("status") val status: String,
    @SerialName("items") val items: List<OrderItemResponse>,
    @SerialName("subtotal") val subtotal: String,
    @SerialName("tax") val tax: String,
    @SerialName("total") val total: String,
    @SerialName("notes") val notes: String?,
    @SerialName("createdAt") val createdAt: String,
    @SerialName("updatedAt") val updatedAt: String,
    @SerialName("createdBy") val createdBy: Long
)

@Serializable
data class OrderItemResponse(
    @SerialName("id") val id: Long,
    @SerialName("productId") val productId: Long,
    @SerialName("productName") val productName: String,
    @SerialName("quantity") val quantity: Int,
    @SerialName("unitPrice") val unitPrice: String,
    @SerialName("subtotal") val subtotal: String,
    @SerialName("notes") val notes: String?,
    @SerialName("itemStatus") val itemStatus: String,
    @SerialName("categoryId") val categoryId: Long
)

// --- Auth DTOs ---
@Serializable
data class LoginRequest(
    @SerialName("username") val username: String,
    @SerialName("password") val password: String,
)

@Serializable
data class UserLoginResponse(
    @SerialName("id") val id: Long,
    @SerialName("username") val username: String,
    @SerialName("fullName") val fullName: String,
    @SerialName("role") val role: String,
    @SerialName("isActive") val isActive: Boolean,
)

@Serializable
data class LoginResponse(
    @SerialName("success") val success: Boolean,
    @SerialName("data") val data: UserLoginResponse?,
    @SerialName("message") val message: String?,
    @SerialName("timestamp") val timestamp: String,
)

@Serializable
data class ProductResponse(
    @SerialName("id") val id: Long,
    @SerialName("name") val name: String,
    @SerialName("description") val description: String?,
    @SerialName("price") val price: String,
    @SerialName("categoryId") val categoryId: Long,
    @SerialName("categoryName") val categoryName: String?,
    @SerialName("imageUrl") val imageUrl: String?,
    @SerialName("isActive") val isActive: Boolean,
    @SerialName("stockQuantity") val stockQuantity: Int?,
    @SerialName("minStockLevel") val minStockLevel: Int?
)

@Serializable
data class CategoryResponse(
    @SerialName("id") val id: String,
    @SerialName("name") val name: String,
    @SerialName("color") val color: String,
    @SerialName("sortOrder") val sortOrder: Int,
    @SerialName("parentCategoryId") val parentCategoryId: String? = null
)

// --- Loyalty DTOs ---

@Serializable
data class LoyaltyLoginRequest(
    @SerialName("phone") val phone: String
)

/** Datos del cliente devueltos por el servidor de loyalty */
@Serializable
data class LoyaltyCustomerData(
    @SerialName("id") val id: Long,
    @SerialName("name") val name: String,
    @SerialName("lastName") val lastName: String? = null,
    @SerialName("phone") val phone: String,
    @SerialName("totalPoints") val totalPoints: Int = 0,
    @SerialName("lifetimePoints") val lifetimePoints: Int = 0,
    @SerialName("tier") val tier: String = "BRONZE",
    @SerialName("totalVisits") val totalVisits: Int = 0,
    @SerialName("totalSpent") val totalSpent: Double = 0.0,
    @SerialName("createdAt") val createdAt: String = ""
)

@Serializable
data class LoyaltyLoginApiResponse(
    @SerialName("success") val success: Boolean,
    @SerialName("data") val data: LoyaltyCustomerData? = null,
    @SerialName("message") val message: String? = null,
    @SerialName("timestamp") val timestamp: String = ""
)

@Serializable
data class LoyaltyLinkOrderRequest(
    @SerialName("orderId") val orderId: Long,
    @SerialName("customerId") val customerId: Long,
    @SerialName("orderTotal") val orderTotal: Double? = null
)

@Serializable
data class TableResponse(
    @SerialName("id") val id: Long,
    @SerialName("number") val number: Int,
    @SerialName("name") val name: String?,
    @SerialName("capacity") val capacity: Int,
    @SerialName("zone") val zone: String,
    @SerialName("status") val status: String,
    @SerialName("isActive") val isActive: Boolean
)