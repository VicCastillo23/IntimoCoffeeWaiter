package com.intimocoffee.waiter.core.database.dao

import androidx.room.*
import com.intimocoffee.waiter.core.database.entity.TableEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TableDao {
    
    @Query("SELECT * FROM tables ORDER BY number ASC")
    fun getAllTables(): Flow<List<TableEntity>>
    
    @Query("SELECT * FROM tables WHERE isActive = 1 ORDER BY number ASC")
    fun getAllActiveTables(): Flow<List<TableEntity>>
    
    @Query("SELECT * FROM tables WHERE zone = :zone AND isActive = 1 ORDER BY number ASC")
    fun getTablesByZone(zone: String): Flow<List<TableEntity>>
    
    @Query("SELECT * FROM tables WHERE id = :tableId")
    suspend fun getTableById(tableId: String): TableEntity?
    
    @Query("SELECT * FROM tables WHERE number = :number")
    suspend fun getTableByNumber(number: Int): TableEntity?
    
    @Query("SELECT * FROM tables WHERE status = 'FREE' AND isActive = 1 ORDER BY number ASC")
    fun getAvailableTables(): Flow<List<TableEntity>>
    
    @Query("SELECT * FROM tables WHERE status != 'OUT_OF_SERVICE' AND isActive = 1 ORDER BY number ASC")
    fun getTablesForNewOrders(): Flow<List<TableEntity>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTable(table: TableEntity)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTables(tables: List<TableEntity>)
    
    @Update
    suspend fun updateTable(table: TableEntity)
    
    @Query("UPDATE tables SET status = :status, currentOrderId = :orderId WHERE id = :tableId")
    suspend fun updateTableStatus(tableId: String, status: String, orderId: String? = null)
    
    @Query("UPDATE tables SET isActive = 0 WHERE id = :tableId")
    suspend fun deactivateTable(tableId: String)
    
    @Delete
    suspend fun deleteTable(table: TableEntity)
    
    @Query("DELETE FROM tables")
    suspend fun deleteAllTables()
}