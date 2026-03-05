package com.intimocoffee.waiter.feature.dashboard.domain.model

data class DashboardStats(
    val tableSummary: TableSummary,
    val orderSummary: OrderSummary,
    val productSummary: ProductSummary
)

data class TableSummary(
    val total: Int,
    val occupied: Int,
    val free: Int,
    val reserved: Int,
    val outOfService: Int
) {
    val occupancyPercentage: Int = if (total > 0) (occupied * 100) / total else 0
}

data class OrderSummary(
    val total: Int,
    val pending: Int,
    val preparing: Int,
    val ready: Int,
    val completed: Int,
    val todayRevenue: Double
) {
    val activeOrders: Int = pending + preparing + ready
}

data class ProductSummary(
    val total: Int,
    val lowStock: Int,
    val outOfStock: Int,
    val active: Int
) {
    val stockStatus: String = when {
        outOfStock > 0 -> "CRITICAL"
        lowStock > 0 -> "WARNING"
        else -> "GOOD"
    }
}