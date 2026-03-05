package com.intimocoffee.waiter.feature.dashboard.data.repository

import com.intimocoffee.waiter.core.database.dao.OrderDao
import com.intimocoffee.waiter.core.database.dao.ProductDao
import com.intimocoffee.waiter.core.database.dao.TableDao
import com.intimocoffee.waiter.feature.dashboard.domain.model.DashboardStats
import com.intimocoffee.waiter.feature.dashboard.domain.model.OrderSummary
import com.intimocoffee.waiter.feature.dashboard.domain.model.ProductSummary
import com.intimocoffee.waiter.feature.dashboard.domain.model.TableSummary
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DashboardRepository @Inject constructor(
    private val tableDao: TableDao,
    private val orderDao: OrderDao,
    private val productDao: ProductDao
) {
    
    fun getDashboardStats(): Flow<DashboardStats> {
        return combine(
            tableDao.getAllTables(),
            orderDao.getAllOrders(),
            productDao.getAllProducts()
        ) { tables, orders, products ->
            
            // Resumen de mesas
            val occupiedTables = tables.filter { it.status == "OCCUPIED" }
            val freeTables = tables.filter { it.status == "FREE" }
            val reservedTables = tables.filter { it.status == "RESERVED" }
            
            val tableSummary = TableSummary(
                total = tables.size,
                occupied = occupiedTables.size,
                free = freeTables.size,
                reserved = reservedTables.size,
                outOfService = tables.filter { it.status == "OUT_OF_SERVICE" }.size
            )
            
            // Resumen de pedidos
            val today = System.currentTimeMillis() - (24 * 60 * 60 * 1000) // Últimas 24 horas
            val todaysOrders = orders.filter { it.createdAt >= today }
            
            val pendingOrders = todaysOrders.filter { it.status == "PENDING" }
            val preparingOrders = todaysOrders.filter { it.status == "PREPARING" }
            val readyOrders = todaysOrders.filter { it.status == "READY" }
            val completedOrders = todaysOrders.filter { 
                it.status == "DELIVERED" || it.status == "PAID" 
            }
            
            val orderSummary = OrderSummary(
                total = todaysOrders.size,
                pending = pendingOrders.size,
                preparing = preparingOrders.size,
                ready = readyOrders.size,
                completed = completedOrders.size,
                todayRevenue = completedOrders.sumOf { it.getTotalAsBigDecimal() }.toDouble()
            )
            
            // Resumen de productos
            val activeProducts = products.filter { it.isActive }
            val lowStockProducts = activeProducts.filter { product ->
                product.stockQuantity != null && 
                product.minStockLevel != null && 
                product.stockQuantity <= product.minStockLevel
            }
            val outOfStockProducts = activeProducts.filter { product ->
                product.stockQuantity != null && product.stockQuantity <= 0
            }
            
            val productSummary = ProductSummary(
                total = activeProducts.size,
                lowStock = lowStockProducts.size,
                outOfStock = outOfStockProducts.size,
                active = activeProducts.size
            )
            
            DashboardStats(
                tableSummary = tableSummary,
                orderSummary = orderSummary,
                productSummary = productSummary
            )
        }
    }
}