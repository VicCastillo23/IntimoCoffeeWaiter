package com.intimocoffee.waiter.feature.tables.domain.repository

import com.intimocoffee.waiter.feature.tables.domain.model.Table
import com.intimocoffee.waiter.feature.tables.domain.model.TableStatus
import kotlinx.coroutines.flow.Flow

interface TableRepository {
    fun getAllActiveTables(): Flow<List<Table>>
    fun getTablesByZone(zone: String): Flow<List<Table>>
    suspend fun getTableById(id: Long): Table?
    suspend fun getTableByNumber(number: Int): Table?
    fun getAvailableTables(): Flow<List<Table>>
    fun getTablesForNewOrders(): Flow<List<Table>>
    suspend fun updateTableStatus(tableId: Long, status: TableStatus, orderId: Long? = null): Boolean
    suspend fun createTable(table: Table): Boolean
    suspend fun updateTable(table: Table): Boolean
    suspend fun deactivateTable(tableId: Long): Boolean
}
