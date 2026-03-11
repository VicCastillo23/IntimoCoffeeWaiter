package com.intimocoffee.waiter.feature.fidelity.data.repository

import android.util.Log
import com.intimocoffee.waiter.core.database.dao.FidelityCustomerDao
import com.intimocoffee.waiter.core.database.entity.FidelityCustomerEntity
import com.intimocoffee.waiter.core.network.DynamicRetrofitProvider
import com.intimocoffee.waiter.core.network.LoyaltyLinkOrderRequest
import com.intimocoffee.waiter.core.network.LoyaltyLoginRequest
import com.intimocoffee.waiter.feature.fidelity.domain.model.FidelityCustomer
import com.intimocoffee.waiter.feature.fidelity.domain.repository.FidelityRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.math.BigDecimal
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FidelityRepositoryImpl @Inject constructor(
    private val dao: FidelityCustomerDao,
    private val retrofitProvider: DynamicRetrofitProvider
) : FidelityRepository {

    companion object {
        private const val TAG = "FidelityRepository"
    }

    /**
     * Busca primero en el servidor de loyalty; si falla o no responde, usa la caché local.
     * Cuando el servidor responde, actualiza/crea el registro local.
     */
    override suspend fun getByPhone(phone: String): FidelityCustomer? {
        // 1. Intento en el servidor
        try {
            val response = retrofitProvider.getApiService()
                .loyaltyLoginByPhone(LoyaltyLoginRequest(phone))
            if (response.isSuccessful) {
                val body = response.body()
                if (body?.success == true && body.data != null) {
                    val serverData = body.data
                    Log.d(TAG, "Cliente encontrado en servidor: ${serverData.name} (id=${serverData.id})")
                    // Actualizar caché local
                    val existing = dao.getByPhone(phone)
                    if (existing != null) {
                        dao.update(existing.copy(
                            name = serverData.name,
                            totalPoints = serverData.totalPoints,
                            updatedAt = System.currentTimeMillis()
                        ))
                    } else {
                        dao.insert(FidelityCustomerEntity(
                            phone = serverData.phone,
                            name = serverData.name,
                            totalPoints = serverData.totalPoints
                        ))
                    }
                    return FidelityCustomer(
                        id = serverData.id,
                        phone = serverData.phone,
                        name = serverData.name,
                        totalPoints = serverData.totalPoints
                    )
                } else {
                    Log.d(TAG, "Cliente no encontrado en servidor (phone=$phone)")
                    return null // No está registrado
                }
            } else if (response.code() == 401 || response.code() == 404) {
                // El servidor confirmó que no existe
                Log.d(TAG, "Servidor: cliente no registrado (${response.code()})")
                return null
            }
            // Otro error HTTP → caer a local
        } catch (e: Exception) {
            Log.w(TAG, "Servidor no disponible, usando caché local: ${e.message}")
        }
        // 2. Fallback: caché local
        return dao.getByPhone(phone)?.toDomain()
    }

    /**
     * Suma puntos localmente y, si se provee [orderId], vincula la orden en el servidor.
     */
    override suspend fun addPoints(phone: String, orderTotal: BigDecimal, orderId: Long): FidelityCustomer {
        val pointsToAdd = FidelityRepository.calculatePoints(orderTotal)

        // Vincular orden en servidor si corresponde
        if (orderId > 0L) {
            try {
                val loginResp = retrofitProvider.getApiService()
                    .loyaltyLoginByPhone(LoyaltyLoginRequest(phone))
                val serverCustomerId = loginResp.body()?.data?.id
                if (serverCustomerId != null) {
                    retrofitProvider.getApiService().loyaltyLinkOrder(
                        LoyaltyLinkOrderRequest(
                            orderId = orderId,
                            customerId = serverCustomerId,
                            orderTotal = orderTotal.toDouble()
                        )
                    )
                    Log.d(TAG, "Orden $orderId vinculada al cliente $serverCustomerId en servidor")
                }
            } catch (e: Exception) {
                Log.w(TAG, "No se pudo vincular orden en servidor: ${e.message}")
            }
        }

        // Actualizar puntos localmente
        val existing = dao.getByPhone(phone)
        return if (existing != null) {
            val updated = existing.copy(
                totalPoints = existing.totalPoints + pointsToAdd,
                updatedAt = System.currentTimeMillis()
            )
            dao.update(updated)
            updated.toDomain()
        } else {
            val newEntity = FidelityCustomerEntity(
                phone = phone,
                totalPoints = pointsToAdd
            )
            val newId = dao.insert(newEntity)
            newEntity.copy(id = newId).toDomain()
        }
    }

    override fun getAllCustomers(): Flow<List<FidelityCustomer>> =
        dao.getAllCustomers().map { list -> list.map { it.toDomain() } }

    private fun FidelityCustomerEntity.toDomain() = FidelityCustomer(
        id = id,
        phone = phone,
        name = name,
        totalPoints = totalPoints
    )
}
