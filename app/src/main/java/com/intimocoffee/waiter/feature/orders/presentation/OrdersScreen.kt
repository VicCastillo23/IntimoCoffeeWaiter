package com.intimocoffee.waiter.feature.orders.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.intimocoffee.waiter.feature.orders.domain.model.Order
import com.intimocoffee.waiter.feature.orders.domain.model.OrderItemStatus
import com.intimocoffee.waiter.feature.orders.domain.model.OrderStatus
import java.math.BigDecimal
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrdersScreen(
    onNavigateToCreateOrder: () -> Unit = {},
    viewModel: OrdersViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.loadOrders()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Órdenes",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold
            )
            
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                IconButton(
                    onClick = { viewModel.refreshOrders() }
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = "Actualizar")
                }
                
                FilledTonalButton(
                    onClick = onNavigateToCreateOrder
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Nueva Orden")
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Status Filter Chips
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                FilterChip(
                    onClick = { viewModel.filterByStatus(null) },
                    label = { Text("Todas") },
                    selected = uiState.selectedStatus == null
                )
            }
            
            // Exclude ARCHIVED status from filters to maintain clean day impression
            items(OrderStatus.values().filter { it != OrderStatus.ARCHIVED }) { status ->
                FilterChip(
                    onClick = { viewModel.filterByStatus(status) },
                    label = { Text(status.displayName) },
                    selected = uiState.selectedStatus == status,
                    leadingIcon = if (uiState.selectedStatus == status) {
                        { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                    } else null
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Orders List
        when {
            uiState.isLoading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            
            uiState.filteredOrders.isEmpty() -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Default.Receipt,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = if (uiState.selectedStatus != null) {
                                "No se encontraron órdenes con este estado"
                            } else {
                                "No hay órdenes registradas"
                            },
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            
            else -> {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(uiState.filteredOrders) { order ->
                        OrderCard(
                            order = order,
                            onOrderClick = { viewModel.showOrderDetails(order) },
                            onStatusChange = { newStatus -> 
                                viewModel.updateOrderStatus(order.id, newStatus) 
                            },
                            onCancelOrder = { viewModel.cancelOrder(order.id) }
                        )
                    }
                }
            }
        }
    }

    // Show error if exists
    uiState.error?.let { error ->
        LaunchedEffect(error) {
            // Here you could show a Snackbar or Toast
            // For now, we'll just clear it after showing
            viewModel.clearError()
        }
    }

}

@Composable
fun OrderCard(
    order: Order,
    onOrderClick: () -> Unit,
    onStatusChange: (OrderStatus) -> Unit,
    onCancelOrder: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onOrderClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Header row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = order.orderNumber,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        
                        // Show additional order indicator
                        if (order.isAdditionalOrder) {
                            Surface(
                                color = MaterialTheme.colorScheme.tertiary,
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = "ADICIONAL",
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onTertiary
                                )
                            }
                        }
                    }
                    
                    order.customerName?.let { name ->
                        Text(
                            text = "Cliente: $name",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    
                    order.tableName?.let { table ->
                        Text(
                            text = "Mesa: $table",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                // Usa el chip reutilizable basado en OrderStatus definido en OrderStatusUi.kt
                OrderStatusChip(
                    status = order.status,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Items summary
            Text(
                text = "${order.items.size} producto(s) • Total: ${formatCurrency(order.total)}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Show all items
            if (order.items.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Column(
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    order.items.forEach { item ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(
                                modifier = Modifier.weight(1f)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    // Item status chip (por ahora seguimos usando la lógica existente)
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = getItemStatusColor(item, order.status).copy(alpha = 0.15f)
                                    ) {
                                        Text(
                                            text = getItemStatusText(item, order.status),
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = getItemStatusColor(item, order.status),
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                    
                                    Text(
                                        text = "${item.quantity}x ${item.productName}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                
                                if (item.notes?.isNotBlank() == true) {
                                    Text(
                                        text = "  • ${item.notes}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(start = 16.dp, top = 2.dp)
                                    )
                                }
                            }
                            
                            Text(
                                text = formatCurrency(item.subtotal),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(4.dp))
                    }
                }
                
                // Add a subtle divider
                Spacer(modifier = Modifier.height(8.dp))
                Divider(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Date and actions row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = formatDateTime(order.createdAt.toString()),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Action buttons based on status
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    val validTransitions = OrderStatus.getValidTransitions(order.status)
                    
                    validTransitions.forEach { nextStatus ->
                        if (nextStatus != OrderStatus.CANCELLED) {
                            TextButton(
                                onClick = { onStatusChange(nextStatus) }
                            ) {
                                Text(
                                    text = nextStatus.displayName,
                                    style = MaterialTheme.typography.labelMedium
                                )
                            }
                        }
                    }
                    
                    if (OrderStatus.CANCELLED in validTransitions) {
                        TextButton(
                            onClick = onCancelOrder,
                            colors = ButtonDefaults.textButtonColors(
                                contentColor = MaterialTheme.colorScheme.error
                            )
                        ) {
                            Text(
                                text = "Cancelar",
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun formatCurrency(amount: BigDecimal): String {
    val currencyFormat = NumberFormat.getCurrencyInstance(Locale("es", "CO"))
    return currencyFormat.format(amount)
}

private fun formatDateTime(dateTime: String): String {
    return try {
        // Try to parse the string and format it properly
        // For now, return a simple formatted version
        dateTime.take(16).replace("T", " ")
    } catch (e: Exception) {
        dateTime
    }
}

private fun getItemStatusText(item: com.intimocoffee.waiter.feature.orders.domain.model.OrderItem, orderStatus: OrderStatus): String {
    if (orderStatus == OrderStatus.CANCELLED) return OrderItemStatus.CANCELLED.displayName
    return item.itemStatus.displayName
}

private fun getItemStatusColor(item: com.intimocoffee.waiter.feature.orders.domain.model.OrderItem, orderStatus: OrderStatus): Color {
    if (orderStatus == OrderStatus.CANCELLED) {
        return Color(android.graphics.Color.parseColor(OrderItemStatus.CANCELLED.color))
    }
    return Color(android.graphics.Color.parseColor(item.itemStatus.color))
}

