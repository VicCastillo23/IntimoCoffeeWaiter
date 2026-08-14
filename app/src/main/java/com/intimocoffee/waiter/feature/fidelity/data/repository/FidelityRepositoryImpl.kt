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

class FidelityRepositoryImpl @Inject constructor(
    private val dao: FidelityCustomerDao,
    private val awsApi: AwsLoyaltyApiService
) : FidelityRepository {

    companion object {
        private const val TAG = "FidelityRepository"
    }

    /**
     * Busca primero en el servidor de loyalty; si falla o no responde, usa la caché local.
     */
    override suspend fun getByPhone(phone: String): FidelityCustomer? {
        // 1. Consulta directa a AWS
        try {
            val response = awsApi.getCustomerByPhone(phone)
            when {
                response.isSuccessful && response.body()?.data != null -> {
                    val serverData = response.body()!!.data!!
                    Log.d(TAG, "AWS: cliente encontrado → ${serverData.name} (id=${serverData.id}, pts=${serverData.totalPoints})")
                    // Actualizar/crear caché local
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
                }
                response.isSuccessful -> {
                    Log.d(TAG, "AWS: cliente no registrado (phone=$phone)")
                    return null
                }
                else -> {
                    Log.w(TAG, "AWS: error ${response.code()}, fallback a caché local")
                    return dao.getByPhone(phone)?.toDomain()
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "AWS no disponible, usando caché local: ${e.message}")
        }
        return dao.getByPhone(phone)?.toDomain()
    }

    override suspend fun getByCustomerId(id: Long): FidelityCustomer? {
        try {
            val response = awsApi.getCustomerById(id)
            if (response.isSuccessful && response.body()?.data != null) {
                val serverData = response.body()!!.data!!
                val phone = serverData.phone
                val existing = dao.getByPhone(phone)
                if (existing != null) {
                    dao.update(
                        existing.copy(
                            name = serverData.name,
                            totalPoints = serverData.totalPoints,
                            updatedAt = System.currentTimeMillis()
                        )
                    )
                } else {
                    dao.insert(
                        FidelityCustomerEntity(
                            phone = phone,
                            name = serverData.name,
                            totalPoints = serverData.totalPoints
                        )
                    )
                }
                return FidelityCustomer(
                    id = serverData.id,
                    phone = serverData.phone,
                    name = serverData.name,
                    totalPoints = serverData.totalPoints
                )
            }
        } catch (e: Exception) {
            Log.w(TAG, "getByCustomerId AWS failed: ${e.message}")
        }
        return null
    }

    /**
     * Awards points only after a successful AWS link-order for a known customer.
     * Does not inflate local points for unknown phones or failed links.
     */
    override suspend fun addPoints(phone: String, orderTotal: BigDecimal, orderId: Long): FidelityCustomer? {
        val pointsToAdd = FidelityRepository.calculatePoints(orderTotal)
        if (orderId <= 0L || pointsToAdd <= 0) {
            Log.d(TAG, "addPoints skipped: orderId=$orderId points=$pointsToAdd")
            return dao.getByPhone(phone)?.toDomain()
        }

        return try {
            val resp = awsApi.getCustomerByPhone(phone)
            val serverCustomer = resp.body()?.data
            if (serverCustomer == null) {
                Log.d(TAG, "AWS: unknown phone=$phone — not awarding local points")
                return null
            }
            val linkResp = awsApi.linkOrder(
                AwsLinkOrderRequest(
                    orderId = orderId,
                    customerId = serverCustomer.id,
                    orderTotal = orderTotal.toDouble()
                )
            )
            if (!linkResp.isSuccessful || linkResp.body()?.success != true) {
                Log.w(
                    TAG,
                    "AWS: link-order failed for order $orderId → HTTP ${linkResp.code()}: ${linkResp.body()?.message}"
                )
                return dao.getByPhone(phone)?.toDomain()
            }

            Log.d(TAG, "AWS: orden $orderId vinculada al cliente ${serverCustomer.id}; updating local cache")
            val existing = dao.getByPhone(phone)
            if (existing != null) {
                val updated = existing.copy(
                    name = serverCustomer.name ?: existing.name,
                    totalPoints = existing.totalPoints + pointsToAdd,
                    updatedAt = System.currentTimeMillis()
                )
                dao.update(updated)
                updated.toDomain()
            } else {
                val newEntity = FidelityCustomerEntity(
                    phone = serverCustomer.phone,
                    name = serverCustomer.name,
                    totalPoints = serverCustomer.totalPoints + pointsToAdd
                )
                val newId = dao.insert(newEntity)
                newEntity.copy(id = newId).toDomain()
            }
        } catch (e: Exception) {
            Log.w(TAG, "AWS: addPoints aborted (no local inflate): ${e.message}")
            null
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
