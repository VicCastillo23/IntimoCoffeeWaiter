package com.intimocoffee.waiter.feature.fidelity.domain.repository

import com.intimocoffee.waiter.feature.fidelity.domain.model.FidelityCustomer
import kotlinx.coroutines.flow.Flow
import java.math.BigDecimal

interface FidelityRepository {
    suspend fun getByPhone(phone: String): FidelityCustomer?

    /** Cliente por ID (útil para QR antiguo con solo ID). */
    suspend fun getByCustomerId(id: Long): FidelityCustomer?

    /**
     * Adds points only after a successful AWS link-order when the customer exists remotely.
     * Returns null when the phone is unknown or the link fails (does not inflate local points).
     */
    suspend fun addPoints(phone: String, orderTotal: BigDecimal, orderId: Long = 0L): FidelityCustomer?

    fun getAllCustomers(): Flow<List<FidelityCustomer>>

    companion object {
        /** Points earned per 1,000 COP */
        const val POINTS_PER_THOUSAND = 1

        fun calculatePoints(total: BigDecimal): Int =
            total.divide(BigDecimal("1000"), 0, java.math.RoundingMode.FLOOR).toInt()
                .coerceAtLeast(0)
    }
}
