package com.intimocoffee.waiter.feature.fidelity.domain.repository

import com.intimocoffee.waiter.feature.fidelity.domain.model.FidelityCustomer
import kotlinx.coroutines.flow.Flow
import java.math.BigDecimal

interface FidelityRepository {
    suspend fun getByPhone(phone: String): FidelityCustomer?

    /**
     * Adds points to a customer's account based on the order total.
     * Rule: 1 point per $1,000 COP.
     * Creates the customer if they don't exist yet.
     * Optionally pass [orderId] to link the order on the remote server.
     */
    suspend fun addPoints(phone: String, orderTotal: BigDecimal, orderId: Long = 0L): FidelityCustomer

    fun getAllCustomers(): Flow<List<FidelityCustomer>>

    companion object {
        /** Points earned per 1,000 COP */
        const val POINTS_PER_THOUSAND = 1

        fun calculatePoints(total: BigDecimal): Int =
            total.divide(BigDecimal("1000"), 0, java.math.RoundingMode.FLOOR).toInt()
                .coerceAtLeast(0)
    }
}
