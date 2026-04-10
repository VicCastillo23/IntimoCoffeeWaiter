package com.intimocoffee.waiter.core.network

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

/**
 * Retrofit interface para el servidor de Loyalty en AWS (https://api.cafeintimo.mx).
 * Endpoints espejo de AwsLoyaltyClient en IntimoCoffeeApp.
 */
interface AwsLoyaltyApiService {

    @GET("loyalty/customer/{id}")
    suspend fun getCustomerById(
        @Path("id") id: Long
    ): Response<AwsApiResponse<AwsLoyaltyCustomerData>>

    /** Busca un cliente por número de teléfono. Retorna 404 si no existe. */
    @GET("loyalty/customer/phone/{phone}")
    suspend fun getCustomerByPhone(
        @Path("phone") phone: String
    ): Response<AwsApiResponse<AwsLoyaltyCustomerData>>

    /** Vincula una orden a un cliente, registrando los puntos ganados. */
    @POST("loyalty/link-order")
    suspend fun linkOrder(
        @Body request: AwsLinkOrderRequest
    ): Response<AwsApiResponse<String>>
}

// ─── DTOs (espejo de IntimoCoffeeApp/AwsLoyaltyClient) ───────────────────────

@Serializable
data class AwsApiResponse<T>(
    @SerialName("success") val success: Boolean,
    @SerialName("data") val data: T? = null,
    @SerialName("message") val message: String? = null,
    @SerialName("timestamp") val timestamp: String? = null
)

@Serializable
data class AwsLoyaltyCustomerData(
    @SerialName("id") val id: Long,
    @SerialName("name") val name: String,
    @SerialName("lastName") val lastName: String? = null,
    @SerialName("phone") val phone: String,
    @SerialName("email") val email: String? = null,
    @SerialName("totalPoints") val totalPoints: Int = 0,
    @SerialName("lifetimePoints") val lifetimePoints: Int = 0,
    @SerialName("tier") val tier: String = "BRONZE",
    @SerialName("totalVisits") val totalVisits: Int = 0,
    @SerialName("totalSpent") val totalSpent: Double = 0.0,
    @SerialName("createdAt") val createdAt: String = ""
)

@Serializable
data class AwsLinkOrderRequest(
    @SerialName("orderId") val orderId: Long,
    @SerialName("customerId") val customerId: Long,
    @SerialName("orderTotal") val orderTotal: Double? = null
)
