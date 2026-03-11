package com.intimocoffee.waiter.feature.fidelity.data.repository

import com.intimocoffee.waiter.core.database.dao.FidelityCustomerDao
import com.intimocoffee.waiter.core.database.entity.FidelityCustomerEntity
import com.intimocoffee.waiter.feature.fidelity.domain.model.FidelityCustomer
import com.intimocoffee.waiter.feature.fidelity.domain.repository.FidelityRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.math.BigDecimal
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FidelityRepositoryImpl @Inject constructor(
    private val dao: FidelityCustomerDao
) : FidelityRepository {

    override suspend fun getByPhone(phone: String): FidelityCustomer? =
        dao.getByPhone(phone)?.toDomain()

    override suspend fun addPoints(phone: String, orderTotal: BigDecimal): FidelityCustomer {
        val pointsToAdd = FidelityRepository.calculatePoints(orderTotal)
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
