package com.intimocoffee.waiter.feature.orders.presentation

import android.graphics.Color.parseColor
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Modifier
import com.intimocoffee.waiter.feature.orders.domain.model.OrderItemStatus
import com.intimocoffee.waiter.feature.orders.domain.model.OrderStatus
import com.intimocoffee.waiter.ui.components.IntimoStatusChip

/**
 * Helpers de UI para mapear estados de órdenes/ítems a colores y chips reutilizables.
 */

// --- Extensiones de color ---

fun OrderStatus.toColor(): Color = Color(parseColor(this.color))

fun OrderStatus.contentColor(): Color =
    when (this) {
        OrderStatus.PENDING,
        OrderStatus.READY,
        OrderStatus.PAID -> Color.Black
        else -> Color.White
    }

fun OrderItemStatus.toColor(): Color = Color(parseColor(this.color))

fun OrderItemStatus.contentColor(): Color =
    when (this) {
        OrderItemStatus.PENDING,
        OrderItemStatus.SENT_TO_STATION,
        OrderItemStatus.READY -> Color.Black
        else -> Color.White
    }

// --- Chips específicos de dominio ---

@Composable
fun OrderStatusChip(
    status: OrderStatus,
    modifier: Modifier = Modifier
) {
    IntimoStatusChip(
        text = status.displayName,
        modifier = modifier,
        containerColor = status.toColor(),
        contentColor = status.contentColor()
    )
}

@Composable
fun OrderItemStatusChip(
    status: OrderItemStatus,
    modifier: Modifier = Modifier
) {
    IntimoStatusChip(
        text = status.displayName,
        modifier = modifier,
        containerColor = status.toColor(),
        contentColor = status.contentColor()
    )
}
