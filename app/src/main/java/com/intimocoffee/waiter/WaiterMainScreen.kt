package com.intimocoffee.waiter

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.intimocoffee.waiter.feature.orders.domain.model.Order
import com.intimocoffee.waiter.feature.orders.domain.model.OrderItem
import com.intimocoffee.waiter.feature.orders.domain.model.OrderStatus
import com.intimocoffee.waiter.feature.orders.presentation.OrdersViewModel
import java.math.BigDecimal
import java.text.NumberFormat
import java.util.*

private enum class WaiterTab { MIS_ORDENES, LISTAS }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WaiterMainScreen(
    viewModel: OrdersViewModel = hiltViewModel(),
    onNavigateToCreateOrder: () -> Unit = {},
    onLogout: () -> Unit = {},
) {
    val uiState by viewModel.uiState.collectAsState()
    var selectedTab by remember { mutableStateOf(WaiterTab.MIS_ORDENES) }

    LaunchedEffect(Unit) {
        viewModel.loadOrders()
    }

    // Ordenes del mesero actual
    val currentUserId = uiState.currentUserId
    val waiterOrders = uiState.orders
        .filter { order ->
            (currentUserId == null || order.createdBy == currentUserId) &&
            !OrderStatus.isCompleted(order.status)
        }
        .sortedByDescending { it.createdAt }

    // Ordenes listas para entregar
    val readyOrders = uiState.orders
        .filter { it.status == OrderStatus.READY }
        .sortedBy { it.createdAt }

    // Auto-cambiar a "Listas" si hay nuevas
    val readyCount = readyOrders.size

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onNavigateToCreateOrder,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("Nueva Orden", fontWeight = FontWeight.SemiBold) }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(12.dp))

            // ── Header ────────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = uiState.currentUserName?.let { "👨\u200d🍽️ $it" } ?: "👨\u200d🍽️ Mesero",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    IconButton(
                        onClick = { viewModel.refreshOrders() },
                        modifier = Modifier.background(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                            RoundedCornerShape(12.dp)
                        )
                    ) {
                        Icon(
                            Icons.Default.Refresh,
                            contentDescription = "Actualizar",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(
                        onClick = onLogout,
                        modifier = Modifier.background(
                            MaterialTheme.colorScheme.error.copy(alpha = 0.1f),
                            RoundedCornerShape(12.dp)
                        )
                    ) {
                        Icon(
                            Icons.Default.Logout,
                            contentDescription = "Cerrar sesión",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ── Chips de navegación ────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                FilterChip(
                    selected = selectedTab == WaiterTab.MIS_ORDENES,
                    onClick = { selectedTab = WaiterTab.MIS_ORDENES },
                    label = {
                        Text(
                            "📋 Mis Órdenes (${waiterOrders.size})",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = if (selectedTab == WaiterTab.MIS_ORDENES) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    modifier = Modifier.height(44.dp)
                )

                BadgedBox(
                    badge = {
                        if (readyCount > 0) {
                            Badge(containerColor = MaterialTheme.colorScheme.error) {
                                Text(
                                    readyCount.toString(),
                                    color = MaterialTheme.colorScheme.onError
                                )
                            }
                        }
                    }
                ) {
                    FilterChip(
                        selected = selectedTab == WaiterTab.LISTAS,
                        onClick = { selectedTab = WaiterTab.LISTAS },
                        label = {
                            Text(
                                "🔔 Listas para servir",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = if (selectedTab == WaiterTab.LISTAS) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        modifier = Modifier.height(44.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // ── Contenido del tab seleccionado ─────────────────────
            if (uiState.isLoading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            } else {
                when (selectedTab) {
                    WaiterTab.MIS_ORDENES -> {
                        if (waiterOrders.isEmpty()) {
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Receipt,
                                        contentDescription = null,
                                        modifier = Modifier.size(56.dp),
                                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.25f)
                                    )
                                    Text(
                                        "No tienes órdenes activas.",
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        "Usa el botón + para crear una nueva.",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                    )
                                }
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                                contentPadding = PaddingValues(bottom = 80.dp)
                            ) {
                                items(waiterOrders) { order ->
                                    WaiterOrderCard(
                                        order = order,
                                        onDeliverOrder = { orderId ->
                                            viewModel.updateOrderStatus(orderId, OrderStatus.DELIVERED)
                                        }
                                    )
                                }
                            }
                        }
                    }

                    WaiterTab.LISTAS -> {
                        if (readyOrders.isEmpty()) {
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Restaurant,
                                        contentDescription = null,
                                        modifier = Modifier.size(56.dp),
                                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.25f)
                                    )
                                    Text(
                                        "No hay órdenes listas para servir.",
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                                contentPadding = PaddingValues(bottom = 80.dp)
                            ) {
                                items(readyOrders) { order ->
                                    WaiterOrderCard(
                                        order = order,
                                        onDeliverOrder = { orderId ->
                                            viewModel.updateOrderStatus(orderId, OrderStatus.DELIVERED)
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun WaiterOrderCard(
    order: Order,
    onDeliverOrder: (Long) -> Unit
) {
    val isReady = order.status == OrderStatus.READY
    val cardColor = if (isReady) MaterialTheme.colorScheme.primary.copy(alpha = 0.05f) else MaterialTheme.colorScheme.surface
    val borderColor = if (isReady) MaterialTheme.colorScheme.primary else Color.Transparent
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .background(borderColor.copy(alpha = if (isReady) 0.15f else 0f), RoundedCornerShape(12.dp)),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isReady) 6.dp else 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = cardColor
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Header with priority indicator
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (isReady) {
                            Icon(
                                Icons.Default.Notifications,
                                contentDescription = "Lista",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Text(
                            text = "Orden #${order.orderNumber}",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = if (isReady) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    Text(
                        text = "Mesa: ${order.tableName ?: order.tableId}",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    
                    order.customerName?.let { name ->
                        Text(
                            text = "Cliente: $name",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                WaiterOrderStatusChip(order.status)
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Order summary
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
            ) {
                Column(
                    modifier = Modifier.padding(12.dp)
                ) {
                    Text(
                        text = "Resumen de la orden:",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    order.items.forEach { item ->
                        WaiterOrderItemRow(item)
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Divider(color = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Total:",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = formatCurrency(order.total),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            if (isReady) {
                Spacer(modifier = Modifier.height(16.dp))
                
                // Deliver button
                Button(
                    onClick = { onDeliverOrder(order.id) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(Icons.Default.Done, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Marcar como Entregada",
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // Timestamp
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = if (isReady) "Lista desde: ${formatDateTime(order.updatedAt.toString())}" 
                      else "Entregada: ${formatDateTime(order.updatedAt.toString())}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun WaiterOrderItemRow(item: OrderItem) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "${item.quantity}x ${item.productName}",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
            if (item.notes?.isNotBlank() == true) {
                Text(
                    text = "  • ${item.notes}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium
                )
            }
        }
        
        Text(
            text = formatCurrency(item.subtotal),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun WaiterOrderStatusChip(status: OrderStatus) {
    // Conservamos los textos especiales para READY/DELIVERED pero usamos la paleta
    val label = when (status) {
        OrderStatus.READY -> "🔔 LISTA"
        OrderStatus.DELIVERED -> "Entregada"
        else -> status.displayName
    }

    val backgroundColor = when (status) {
        OrderStatus.READY -> MaterialTheme.colorScheme.primary
        OrderStatus.DELIVERED -> MaterialTheme.colorScheme.secondary
        else -> MaterialTheme.colorScheme.surfaceVariant
    }

    val contentColor = when (status) {
        OrderStatus.READY,
        OrderStatus.DELIVERED -> MaterialTheme.colorScheme.onPrimary
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = backgroundColor
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            style = MaterialTheme.typography.labelMedium,
            color = contentColor,
            fontWeight = FontWeight.Bold
        )
    }
}

private fun formatCurrency(amount: BigDecimal): String {
    val currencyFormat = NumberFormat.getCurrencyInstance(Locale("es", "CO"))
    return currencyFormat.format(amount)
}

private fun formatDateTime(dateTime: String): String {
    return try {
        dateTime.take(16).replace("T", " ")
    } catch (e: Exception) {
        dateTime
    }
}