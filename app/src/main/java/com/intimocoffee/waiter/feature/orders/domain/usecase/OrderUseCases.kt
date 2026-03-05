package com.intimocoffee.waiter.feature.orders.domain.usecase

import android.util.Log
import com.intimocoffee.waiter.core.network.RemoteOrderService
import com.intimocoffee.waiter.feature.orders.domain.model.Order
import com.intimocoffee.waiter.feature.orders.domain.model.OrderItem
import com.intimocoffee.waiter.feature.orders.domain.model.OrderStatus
import com.intimocoffee.waiter.feature.orders.domain.repository.OrderRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import java.math.BigDecimal
import javax.inject.Inject

class GetOrdersUseCase @Inject constructor(
    private val orderRepository: OrderRepository
) {
    operator fun invoke(): Flow<List<Order>> = orderRepository.getAllOrders()
}

class GetActiveOrdersUseCase @Inject constructor(
    private val orderRepository: OrderRepository
) {
    operator fun invoke(): Flow<List<Order>> = orderRepository.getActiveOrders()
}

class GetOrdersByStatusUseCase @Inject constructor(
    private val orderRepository: OrderRepository
) {
    operator fun invoke(status: OrderStatus): Flow<List<Order>> = 
        orderRepository.getOrdersByStatus(status)
}

class CreateOrderUseCase @Inject constructor(
    private val orderRepository: OrderRepository
) {
    suspend operator fun invoke(
        tableId: Long? = null,
        tableName: String? = null,
        customerName: String? = null,
        items: List<OrderItem>,
        notes: String? = null,
        createdBy: Long
    ): Result<Long> {
        return try {
            if (items.isEmpty()) {
                return Result.failure(Exception("La orden debe tener al menos un producto"))
            }

            val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
            val orderNumber = orderRepository.generateOrderNumber()
            
            val subtotal = items.sumOf { it.subtotal }
            val tax = subtotal * BigDecimal("0.16") // 16% IVA
            val total = subtotal + tax

            val order = Order(
                orderNumber = orderNumber,
                tableId = tableId,
                tableName = tableName,
                customerName = customerName,
                status = OrderStatus.PENDING,
                items = items,
                subtotal = subtotal,
                tax = tax,
                total = total,
                notes = notes,
                createdAt = now,
                updatedAt = now,
                createdBy = createdBy
            )

            val orderId = orderRepository.createOrder(order, items)
            Result.success(orderId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

class UpdateOrderStatusUseCase @Inject constructor(
    private val orderRepository: OrderRepository,
    private val remoteOrderService: RemoteOrderService,
) {
    suspend operator fun invoke(orderId: Long, newStatus: OrderStatus): Result<Boolean> {
        return try {
            Log.d("UpdateOrderStatusUseCase", "Starting remote update for orderId=$orderId, newStatus=$newStatus")

            // 1) Actualizar estado en el servidor principal (fuente de la verdad)
            val remoteResult = remoteOrderService.updateOrderStatusOnServer(orderId, newStatus)
            if (remoteResult.isFailure || remoteResult.getOrNull() != true) {
                val error = remoteResult.exceptionOrNull()
                Log.e(
                    "UpdateOrderStatusUseCase",
                    "Remote status update failed for orderId=$orderId, status=$newStatus",
                    error
                )
                return Result.failure(error ?: Exception("Error al actualizar estado en servidor"))
            }

            Log.d("UpdateOrderStatusUseCase", "Remote status update succeeded, attempting local sync (best-effort)")

            // 2) Opcional: intentar sincronizar la copia local, pero sin fallar la operación si algo sale mal
            try {
                orderRepository.updateOrderStatus(orderId, newStatus)
            } catch (e: Exception) {
                Log.w("UpdateOrderStatusUseCase", "Local status update failed, but remote succeeded: ${e.message}")
            }

            Result.success(true)
        } catch (e: Exception) {
            Log.e("UpdateOrderStatusUseCase", "Exception in updateOrderStatus", e)
            Result.failure(e)
        }
    }
}

class CancelOrderUseCase @Inject constructor(
    private val orderRepository: OrderRepository
) {
    suspend operator fun invoke(orderId: Long): Result<Boolean> {
        return try {
            val order = orderRepository.getOrderById(orderId)
                ?: return Result.failure(Exception("Orden no encontrada"))

            if (order.status == OrderStatus.DELIVERED || order.status == OrderStatus.CANCELLED) {
                return Result.failure(Exception("No se puede cancelar una orden ${order.status.displayName}"))
            }

            val success = orderRepository.updateOrderStatus(orderId, OrderStatus.CANCELLED)
            Result.success(success)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

class GetOrderDetailsUseCase @Inject constructor(
    private val orderRepository: OrderRepository
) {
    suspend operator fun invoke(orderId: Long): Order? {
        return orderRepository.getOrderById(orderId)
    }
}

class AddItemToOrderUseCase @Inject constructor(
    private val orderRepository: OrderRepository
) {
    suspend operator fun invoke(orderId: Long, item: OrderItem): Result<Boolean> {
        return try {
            val order = orderRepository.getOrderById(orderId)
                ?: return Result.failure(Exception("Orden no encontrada"))

            if (order.status != OrderStatus.PENDING) {
                return Result.failure(Exception("Solo se pueden agregar productos a órdenes pendientes"))
            }

            val success = orderRepository.addItemToOrder(orderId, item)
            Result.success(success)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}