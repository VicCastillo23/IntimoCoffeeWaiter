package com.intimocoffee.waiter.core.database.dao

import androidx.room.*
import com.intimocoffee.waiter.core.database.entity.FidelityCustomerEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FidelityCustomerDao {

    @Query("SELECT * FROM fidelity_customers WHERE phone = :phone LIMIT 1")
    suspend fun getByPhone(phone: String): FidelityCustomerEntity?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(customer: FidelityCustomerEntity): Long

    @Update
    suspend fun update(customer: FidelityCustomerEntity)

    @Query("SELECT * FROM fidelity_customers ORDER BY totalPoints DESC")
    fun getAllCustomers(): Flow<List<FidelityCustomerEntity>>

    @Query("SELECT COUNT(*) FROM fidelity_customers")
    suspend fun getCustomerCount(): Int
}
