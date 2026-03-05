package com.intimocoffee.waiter.feature.reports.domain.model

import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import java.math.BigDecimal

/**
 * Represents a daily cut report with all business metrics
 */
data class DailyCutReport(
    val date: LocalDate,
    val generatedAt: LocalDateTime,
    val generatedBy: String,
    
    // Sales Summary
    val salesSummary: SalesSummary,
    
    // Orders Summary
    val ordersSummary: OrdersSummary,
    
    // Customer Summary
    val customersSummary: CustomersSummary,
    
    // Reservations Summary
    val reservationsSummary: ReservationsSummary,
    
    // Products/Inventory Summary
    val inventorySummary: InventorySummary,
    
    // Payment Methods Breakdown
    val paymentMethodsBreakdown: List<PaymentMethodSummary>
)

data class SalesSummary(
    val totalRevenue: BigDecimal,
    val totalSubtotal: BigDecimal,
    val totalTax: BigDecimal,
    val totalDiscount: BigDecimal,
    val averageOrderValue: BigDecimal,
    val revenueByHour: List<HourlyRevenue>
)

data class OrdersSummary(
    val totalOrders: Int,
    val completedOrders: Int,
    val cancelledOrders: Int,
    val averageOrderSize: BigDecimal,
    val peakHours: List<String>,
    val ordersByStatus: Map<String, Int>
)

data class CustomersSummary(
    val totalCustomers: Int,
    val newCustomers: Int,
    val returningCustomers: Int,
    val averageSpendPerCustomer: BigDecimal
)

data class ReservationsSummary(
    val totalReservations: Int,
    val confirmedReservations: Int,
    val cancelledReservations: Int,
    val noShowReservations: Int,
    val averagePartySize: Double,
    val occupancyRate: Double
)

data class InventorySummary(
    val topSellingProducts: List<ProductSalesInfo>,
    val lowStockProducts: List<ProductStockInfo>,
    val outOfStockProducts: List<ProductStockInfo>
)

data class PaymentMethodSummary(
    val method: String,
    val amount: BigDecimal,
    val count: Int,
    val percentage: Double
)

data class HourlyRevenue(
    val hour: Int,
    val revenue: BigDecimal,
    val orderCount: Int
)

data class ProductSalesInfo(
    val productId: Long,
    val productName: String,
    val quantitySold: Int,
    val revenue: BigDecimal
)

data class ProductStockInfo(
    val productId: Long,
    val productName: String,
    val currentStock: Int,
    val minimumStock: Int
)