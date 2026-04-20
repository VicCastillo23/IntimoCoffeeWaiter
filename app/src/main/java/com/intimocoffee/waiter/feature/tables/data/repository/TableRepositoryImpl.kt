package com.intimocoffee.waiter.feature.tables.data.repository

import android.util.Log
import com.intimocoffee.waiter.core.database.dao.TableDao
import com.intimocoffee.waiter.core.network.ApiMappers.toTableDomainModels
import com.intimocoffee.waiter.core.network.RemoteOrderService
import com.intimocoffee.waiter.feature.tables.data.mapper.TableMapper
import com.intimocoffee.waiter.feature.tables.domain.model.Table
import com.intimocoffee.waiter.feature.tables.domain.model.TableStatus
import com.intimocoffee.waiter.feature.tables.domain.repository.TableRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TableRepositoryImpl @Inject constructor(
    private val remoteOrderService: RemoteOrderService,
    private val tableDao: TableDao,
    private val tableMapper: TableMapper
) : TableRepository {
    
    override fun getAllActiveTables(): Flow<List<Table>> = flow {
        try {
            Log.d("TableRepository", "Fetching tables from remote server (online-only)...")
            val result = remoteOrderService.getTablesFromServer()

            if (result.isSuccess) {
                val tables = result.getOrNull()
                    ?.toTableDomainModels()
                    ?.filter { it.isActive }
                    ?: emptyList()
                Log.d(
                    "TableRepository",
                    "Successfully fetched ${tables.size} active tables from server"
                )
                emit(tables)
            } else {
                Log.e(
                    "TableRepository",
                    "Failed to fetch tables from server (no local fallback)",
                    result.exceptionOrNull()
                )
                emit(emptyList())
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(
                "TableRepository",
                "Exception fetching tables from server (no local fallback)",
                e
            )
            emit(emptyList())
        }
    }
    
    override fun getTablesByZone(zone: String): Flow<List<Table>> = flow {
        try {
            Log.d(
                "TableRepository",
                "Fetching tables by zone '$zone' from remote server (online-only)..."
            )
            val result = remoteOrderService.getTablesFromServer()

            if (result.isSuccess) {
                val tables = result.getOrNull()
                    ?.toTableDomainModels()
                    ?.filter { it.isActive && it.zone.equals(zone, ignoreCase = true) }
                    ?: emptyList()
                Log.d(
                    "TableRepository",
                    "Successfully fetched ${tables.size} tables for zone '$zone'"
                )
                emit(tables)
            } else {
                Log.e(
                    "TableRepository",
                    "Failed to fetch tables by zone from server (no local fallback)",
                    result.exceptionOrNull()
                )
                emit(emptyList())
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(
                "TableRepository",
                "Exception fetching tables by zone from server (no local fallback)",
                e
            )
            emit(emptyList())
        }
    }
    
    override suspend fun getTableById(id: Long): Table? {
        return try {
            Log.d("TableRepository", "Fetching table $id from remote server (online-only)...")
            val result = remoteOrderService.getTablesFromServer()

            if (result.isSuccess) {
                val table = result.getOrNull()
                    ?.toTableDomainModels()
                    ?.find { it.id == id }
                Log.d(
                    "TableRepository",
                    "Table $id found in server response: ${table?.displayName ?: "Not found"}"
                )
                table
            } else {
                Log.e(
                    "TableRepository",
                    "Failed to fetch table by ID from server (no local fallback)",
                    result.exceptionOrNull()
                )
                null
            }
        } catch (e: Exception) {
            Log.e(
                "TableRepository",
                "Exception fetching table by ID from server (no local fallback)",
                e
            )
            null
        }
    }
    
    override suspend fun getTableByNumber(number: Int): Table? {
        return try {
            Log.d(
                "TableRepository",
                "Fetching table by number $number from remote server (online-only)..."
            )
            val result = remoteOrderService.getTablesFromServer()

            if (result.isSuccess) {
                val table = result.getOrNull()
                    ?.toTableDomainModels()
                    ?.find { it.number == number }
                Log.d(
                    "TableRepository",
                    "Table #$number found in server response: ${table?.displayName ?: "Not found"}"
                )
                table
            } else {
                Log.e(
                    "TableRepository",
                    "Failed to fetch table by number from server (no local fallback)",
                    result.exceptionOrNull()
                )
                null
            }
        } catch (e: Exception) {
            Log.e(
                "TableRepository",
                "Exception fetching table by number from server (no local fallback)",
                e
            )
            null
        }
    }
    
    override fun getAvailableTables(): Flow<List<Table>> = flow {
        try {
            Log.d("TableRepository", "Fetching available tables from remote server (online-only)...")
            val result = remoteOrderService.getTablesFromServer()

            if (result.isSuccess) {
                val tables = result.getOrNull()
                    ?.toTableDomainModels()
                    ?.filter { it.isActive && it.status == TableStatus.FREE }
                    ?: emptyList()
                Log.d(
                    "TableRepository",
                    "Successfully fetched ${tables.size} available tables from server"
                )
                emit(tables)
            } else {
                Log.e(
                    "TableRepository",
                    "Failed to fetch available tables from server (no local fallback)",
                    result.exceptionOrNull()
                )
                emit(emptyList())
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(
                "TableRepository",
                "Exception fetching available tables from server (no local fallback)",
                e
            )
            emit(emptyList())
        }
    }
    
    override fun getTablesForNewOrders(): Flow<List<Table>> = flow {
        try {
            Log.d(
                "TableRepository",
                "Fetching tables for new orders from remote server (online-only)..."
            )
            val result = remoteOrderService.getTablesFromServer()

            if (result.isSuccess) {
                val tables = result.getOrNull()
                    ?.toTableDomainModels()
                    ?.filter { it.isActive } // All active tables can accept new orders
                    ?: emptyList()
                Log.d(
                    "TableRepository",
                    "Successfully fetched ${tables.size} tables for new orders from server"
                )
                emit(tables)
            } else {
                Log.e(
                    "TableRepository",
                    "Failed to fetch tables for new orders from server (no local fallback)",
                    result.exceptionOrNull()
                )
                emit(emptyList())
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(
                "TableRepository",
                "Exception fetching tables for new orders from server (no local fallback)",
                e
            )
            emit(emptyList())
        }
    }
    
    override suspend fun updateTableStatus(
        tableId: Long, 
        status: TableStatus, 
        orderId: Long?
    ): Boolean {
        return try {
            tableDao.updateTableStatus(
                tableId = tableId.toString(),
                status = status.name,
                orderId = orderId?.toString()
            )
            true
        } catch (e: Exception) {
            false
        }
    }
    
    override suspend fun createTable(table: Table): Boolean {
        return try {
            tableDao.insertTable(tableMapper.toEntity(table))
            true
        } catch (e: Exception) {
            false
        }
    }
    
    override suspend fun updateTable(table: Table): Boolean {
        return try {
            tableDao.updateTable(tableMapper.toEntity(table))
            true
        } catch (e: Exception) {
            false
        }
    }
    
    override suspend fun deactivateTable(tableId: Long): Boolean {
        return try {
            tableDao.deactivateTable(tableId.toString())
            true
        } catch (e: Exception) {
            false
        }
    }
}
