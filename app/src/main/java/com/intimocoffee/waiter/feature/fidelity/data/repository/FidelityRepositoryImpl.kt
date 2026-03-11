package com.intimocoffee.waiter.feature.fidelity.data.repository

import android.util.Log
import com.intimocoffee.waiter.core.database.dao.FidelityCustomerDao
import com.intimocoffee.waiter.core.database.entity.FidelityCustomerEntity
import com.intimocoffee.waiter.core.network.AwsLinkOrderRequest
import com.intimocoffee.waiter.core.network.AwsLoyaltyApiService
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
    private val awsApi: AwsLoyaltyApiService
) : FidelityRepository {

    companion object {
        private const val TAG = "FidelityRepository"
    }

    /**
     * Busca primero en el servidor de loyalty; si falla o no responde, usa la caché local.
     * Cuando el servidor responde, actualiza/crea el registro local.
     */
    override suspend fun getByPhone(phone: String): FidelityCustomer? {
        // 1. Consulta directa a AWS
        try {
            val response = awsApi.getCustomerByPhone(phone)
            return when {
                response.isSuccessful && response.body()?.data != null -> {
                    val serverData = response.body()!!.data!!
                    Log.d(TAG, "AWS: cliente encontrado → ${serverData.name} (id=${serverData.id}, pts=${serverData.totalPoints})")
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
                    FidelityCustomer(
                        id = serverData.id,
                        phone = serverData.phone,
                        name = serverData.name,
                        totalPoints = serverData.totalPoints
                    )
                }
                response.code() == 404 -> {
                    Log.d(TAG, "AWS: cliente no registrado (phone=$phone)")
                    null
                }
                else -> {
                    Log.w(TAG, "AWS: error ${response.code()}, fallback a caché local")
                    dao.getByPhone(phone)?.toDomain()
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "AWS no disponible, usando caché local: ${e.message}")
        }
        // 2. Fallback: caché local
        return dao.getByPhone(phone)?.toDomain()
    }

    /**
     * Suma puntos localmente y, si se provee [orderId], vincula la orden en el servidor.
     */
    override suspend fun addPoints(phone: String, orderTotal: BigDecimal, orderId: Long): FidelityCustomer {
        val pointsToAdd = FidelityRepository.calculatePoints(orderTotal)

        // Vincular orden en AWS si corresponde
        if (orderId > 0L) {
            try {
                val resp = awsApi.getCustomerByPhone(phone)
                val serverCustomerId = resp.body()?.data?.id
                if (serverCustomerId != null) {
                    awsApi.linkOrder(
                        AwsLinkOrderRequest(
                            orderId = orderId,
                            customerId = serverCustomerId,
                            orderTotal = orderTotal.toDouble()
                        )
                    )
                    Log.d(TAG, "AWS: orden $orderId vinculada al cliente $serverCustomerId")
                }
            } catch (e: Exception) {
                Log.w(TAG, "AWS: no se pudo vincular orden: ${e.message}")
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
