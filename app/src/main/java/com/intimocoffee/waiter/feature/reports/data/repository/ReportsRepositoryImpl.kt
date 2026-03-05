package com.intimocoffee.waiter.feature.reports.data.repository

import com.intimocoffee.waiter.feature.reports.domain.repository.ReportsRepository
import com.intimocoffee.waiter.feature.reports.domain.model.*
import com.intimocoffee.waiter.feature.orders.domain.repository.OrderRepository
import com.intimocoffee.waiter.feature.orders.domain.model.OrderStatus
import com.intimocoffee.waiter.feature.reservations.domain.repository.ReservationRepository
import com.intimocoffee.waiter.feature.reservations.domain.model.ReservationStatus
import com.intimocoffee.waiter.feature.products.domain.repository.ProductRepository
import com.intimocoffee.waiter.feature.tables.domain.repository.TableRepository
import com.intimocoffee.waiter.feature.tables.presentation.PaymentMethod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import java.math.BigDecimal
import java.math.RoundingMode
import javax.inject.Inject

class ReportsRepositoryImpl @Inject constructor(
    private val orderRepository: OrderRepository,
    private val reservationRepository: ReservationRepository,
    private val productRepository: ProductRepository,
    private val tableRepository: TableRepository
) : ReportsRepository {

    override suspend fun generateDailyCutReport(date: LocalDate, userId: Long): DailyCutReport {
        // Get all paid orders for the date (not yet archived)
        val allOrders = orderRepository.getAllOrders().first()
        val dailyOrders = allOrders.filter { order ->
            (order.status == OrderStatus.PAID || order.status == OrderStatus.ARCHIVED) &&
            order.createdAt.date == date
        }

        // Get reservations for the date
        val allReservations = reservationRepository.getAllReservations().first()
        val dailyReservations = allReservations.filter { reservation ->
            reservation.reservationDate.date == date
        }

        // Get products data
        val allProducts = productRepository.getAllActiveProducts().first()

        // Get tables data
        val allTables = tableRepository.getAllActiveTables().first()

        // Calculate sales summary
        val salesSummary = calculateSalesSummary(dailyOrders)

        // Calculate orders summary
        val ordersSummary = calculateOrdersSummary(dailyOrders)

        // Calculate customers summary
        val customersSummary = calculateCustomersSummary(dailyOrders)

        // Calculate reservations summary
        val reservationsSummary = calculateReservationsSummary(dailyReservations, allTables.size)

        // Calculate inventory summary
        val inventorySummary = calculateInventorySummary(dailyOrders, allProducts)

        // Calculate payment methods breakdown
        val paymentMethodsBreakdown = calculatePaymentMethodsBreakdown(dailyOrders)

        return DailyCutReport(
            date = date,
            generatedAt = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()),
            generatedBy = "Usuario $userId", // TODO: Get actual user name
            salesSummary = salesSummary,
            ordersSummary = ordersSummary,
            customersSummary = customersSummary,
            reservationsSummary = reservationsSummary,
            inventorySummary = inventorySummary,
            paymentMethodsBreakdown = paymentMethodsBreakdown
        )
    }

    private fun calculateSalesSummary(orders: List<com.intimocoffee.waiter.feature.orders.domain.model.Order>): SalesSummary {
        val totalRevenue = orders.fold(BigDecimal.ZERO) { acc, order -> acc.add(order.total) }
        val totalSubtotal = orders.fold(BigDecimal.ZERO) { acc, order -> acc.add(order.subtotal) }
        val totalTax = orders.fold(BigDecimal.ZERO) { acc, order -> acc.add(order.tax) }
        val totalDiscount = orders.fold(BigDecimal.ZERO) { acc, order -> acc.add(order.discount) }
        val averageOrderValue = if (orders.isNotEmpty()) {
            totalRevenue.divide(BigDecimal(orders.size), 2, RoundingMode.HALF_UP)
        } else BigDecimal.ZERO

        // Group orders by hour for revenue by hour
        val revenueByHour = orders.groupBy { it.createdAt.hour }
            .map { (hour, hourOrders) ->
                HourlyRevenue(
                    hour = hour,
                    revenue = hourOrders.fold(BigDecimal.ZERO) { acc, order -> acc.add(order.total) },
                    orderCount = hourOrders.size
                )
            }
            .sortedBy { it.hour }

        return SalesSummary(
            totalRevenue = totalRevenue,
            totalSubtotal = totalSubtotal,
            totalTax = totalTax,
            totalDiscount = totalDiscount,
            averageOrderValue = averageOrderValue,
            revenueByHour = revenueByHour
        )
    }

    private fun calculateOrdersSummary(orders: List<com.intimocoffee.waiter.feature.orders.domain.model.Order>): OrdersSummary {
        val totalOrders = orders.size
        val completedOrders = orders.count { it.status == OrderStatus.PAID || it.status == OrderStatus.ARCHIVED }
        val cancelledOrders = orders.count { it.status == OrderStatus.CANCELLED }
        
        val totalItems = orders.sumOf { order -> order.items.sumOf { it.quantity } }
        val averageOrderSize = if (orders.isNotEmpty()) {
            BigDecimal(totalItems).divide(BigDecimal(orders.size), 2, RoundingMode.HALF_UP)
        } else BigDecimal.ZERO

        // Find peak hours (hours with most orders)
        val ordersByHour = orders.groupBy { it.createdAt.hour }
        val maxOrdersInHour = ordersByHour.values.maxOfOrNull { it.size } ?: 0
        val peakHours = ordersByHour.filter { it.value.size == maxOrdersInHour }
            .keys.sorted().map { "${it}:00" }

        val ordersByStatus = orders.groupBy { it.status.displayName }
            .mapValues { it.value.size }

        return OrdersSummary(
            totalOrders = totalOrders,
            completedOrders = completedOrders,
            cancelledOrders = cancelledOrders,
            averageOrderSize = averageOrderSize,
            peakHours = peakHours,
            ordersByStatus = ordersByStatus
        )
    }

    private fun calculateCustomersSummary(orders: List<com.intimocoffee.waiter.feature.orders.domain.model.Order>): CustomersSummary {
        // For now, we'll count each order as a customer since we don't have customer tracking
        val totalCustomers = orders.size
        val newCustomers = totalCustomers // All are considered new for now
        val returningCustomers = 0
        
        val totalRevenue = orders.fold(BigDecimal.ZERO) { acc, order -> acc.add(order.total) }
        val averageSpendPerCustomer = if (totalCustomers > 0) {
            totalRevenue.divide(BigDecimal(totalCustomers), 2, RoundingMode.HALF_UP)
        } else BigDecimal.ZERO

        return CustomersSummary(
            totalCustomers = totalCustomers,
            newCustomers = newCustomers,
            returningCustomers = returningCustomers,
            averageSpendPerCustomer = averageSpendPerCustomer
        )
    }

    private fun calculateReservationsSummary(
        reservations: List<com.intimocoffee.waiter.feature.reservations.domain.model.Reservation>,
        totalTables: Int
    ): ReservationsSummary {
        val totalReservations = reservations.size
        val confirmedReservations = reservations.count { it.status == ReservationStatus.CONFIRMED }
        val cancelledReservations = reservations.count { it.status == ReservationStatus.CANCELLED }
        val noShowReservations = reservations.count { it.status == ReservationStatus.NO_SHOW }
        
        val averagePartySize = if (reservations.isNotEmpty()) {
            reservations.map { it.partySize }.average()
        } else 0.0

        // Calculate occupancy rate based on seated reservations
        val seatedReservations = reservations.count { it.status == ReservationStatus.SEATED }
        val occupancyRate = if (totalTables > 0) {
            (seatedReservations.toDouble() / totalTables) * 100
        } else 0.0

        return ReservationsSummary(
            totalReservations = totalReservations,
            confirmedReservations = confirmedReservations,
            cancelledReservations = cancelledReservations,
            noShowReservations = noShowReservations,
            averagePartySize = averagePartySize,
            occupancyRate = occupancyRate
        )
    }

    private fun calculateInventorySummary(
        orders: List<com.intimocoffee.waiter.feature.orders.domain.model.Order>,
        products: List<com.intimocoffee.waiter.feature.products.domain.model.Product>
    ): InventorySummary {
        // Calculate top selling products
        val productSales = mutableMapOf<Long, Pair<Int, BigDecimal>>() // quantity to revenue
        
        orders.forEach { order ->
            order.items.forEach { item ->
                val current = productSales[item.productId] ?: (0 to BigDecimal.ZERO)
                productSales[item.productId] = (current.first + item.quantity) to (current.second.add(item.subtotal))
            }
        }

        val topSellingProducts = productSales.entries
            .sortedByDescending { it.value.first }
            .take(10)
            .map { (productId, salesData) ->
                val product = products.find { it.id == productId }
                ProductSalesInfo(
                    productId = productId,
                    productName = product?.name ?: "Producto Desconocido",
                    quantitySold = salesData.first,
                    revenue = salesData.second
                )
            }

        // Calculate stock issues
        val lowStockProducts = products.filter { (it.stockQuantity ?: 0) <= 10 && (it.stockQuantity ?: 0) > 0 }
            .map { product ->
                ProductStockInfo(
                    productId = product.id,
                    productName = product.name,
                    currentStock = product.stockQuantity ?: 0,
                    minimumStock = product.minStockLevel ?: 5
                )
            }

        val outOfStockProducts = products.filter { (it.stockQuantity ?: 0) <= 0 }
            .map { product ->
                ProductStockInfo(
                    productId = product.id,
                    productName = product.name,
                    currentStock = product.stockQuantity ?: 0,
                    minimumStock = product.minStockLevel ?: 5
                )
            }

        return InventorySummary(
            topSellingProducts = topSellingProducts,
            lowStockProducts = lowStockProducts,
            outOfStockProducts = outOfStockProducts
        )
    }

    private fun calculatePaymentMethodsBreakdown(orders: List<com.intimocoffee.waiter.feature.orders.domain.model.Order>): List<PaymentMethodSummary> {
        // Since we don't have payment method in the Order model, we'll simulate this
        // In a real app, you'd have payment method data
        val totalRevenue = orders.fold(BigDecimal.ZERO) { acc, order -> acc.add(order.total) }
        
        return listOf(
            PaymentMethodSummary(
                method = "Efectivo",
                amount = totalRevenue.multiply(BigDecimal("0.4")), // 40% cash
                count = (orders.size * 0.4).toInt(),
                percentage = 40.0
            ),
            PaymentMethodSummary(
                method = "Tarjeta",
                amount = totalRevenue.multiply(BigDecimal("0.6")), // 60% card
                count = (orders.size * 0.6).toInt(),
                percentage = 60.0
            )
        )
    }

    override suspend fun getDailyCutReports(startDate: LocalDate, endDate: LocalDate): Flow<List<DailyCutReport>> {
        // TODO: Implement database storage/retrieval
        return flowOf(emptyList())
    }

    override suspend fun saveDailyCutReport(report: DailyCutReport): Boolean {
        // TODO: Implement database storage
        return true
    }
    
    override suspend fun archiveAllPaidOrders(date: LocalDate): Boolean {
        return try {
            val allOrders = orderRepository.getAllOrders().first()
            val paidOrdersToArchive = allOrders.filter { order ->
                order.status == OrderStatus.PAID && order.createdAt.date == date
            }
            
            paidOrdersToArchive.forEach { order ->
                orderRepository.updateOrderStatus(order.id, OrderStatus.ARCHIVED)
            }
            
            true
        } catch (e: Exception) {
            false
        }
    }
}