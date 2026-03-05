package com.intimocoffee.waiter.feature.tables.presentation

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.intimocoffee.waiter.feature.orders.domain.model.Order
import com.intimocoffee.waiter.feature.orders.domain.model.OrderStatus
import com.intimocoffee.waiter.feature.tables.domain.model.Table
import com.intimocoffee.waiter.feature.tables.domain.model.TableStatus
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.text.NumberFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SimpleTableDetailsScreen(
    tableId: Long,
    onNavigateBack: () -> Unit,
    onTableClosed: () -> Unit,
    viewModel: SimpleTableDetailsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val scope = rememberCoroutineScope()

    LaunchedEffect(tableId) {
        viewModel.loadTableDetails(tableId)
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
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                IconButton(onClick = onNavigateBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Volver")
                }
                
                uiState.table?.let { table ->
                    Column {
                        Text(
                            text = table.displayName,
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Estado: ${table.status.getDisplayName()}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = when (table.status) {
                                TableStatus.OCCUPIED -> Color(0xFFF44336)
                                TableStatus.RESERVED -> Color(0xFFFF9800)
                                else -> MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                    }
                }
            }

            // Action buttons for occupied tables
            if (uiState.table?.status == TableStatus.OCCUPIED && uiState.allActiveOrders.isNotEmpty()) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Add products to existing order
                    OutlinedButton(
                        onClick = { viewModel.showAddProductDialog() }
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Agregar")
                    }
                    
                    // Create new sub-order
                    OutlinedButton(
                        onClick = { viewModel.showCreateSubOrderDialog() }
                    ) {
                        Icon(Icons.Default.PostAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Nueva Orden")
                    }
                    
                    // Close table button
                    Button(
                        onClick = { 
                            // Show appropriate dialog based on number of active orders
                            if (uiState.allActiveOrders.size == 1) {
                                viewModel.showCloseTableDialog()
                            } else {
                                viewModel.showMultipleOrdersCloseDialog()
                            }
                        }
                    ) {
                        Icon(Icons.Default.Receipt, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Cerrar Mesa")
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        when {
            uiState.isLoading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            
            uiState.table == null -> {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Default.Error,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Mesa no encontrada",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
            
            uiState.table?.status != TableStatus.OCCUPIED || uiState.allActiveOrders.isEmpty() -> {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Default.TableRestaurant,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        val displayText = if (uiState.table?.status == TableStatus.OCCUPIED && uiState.allActiveOrders.isEmpty()) {
                            "Mesa ocupada pero sin órdenes activas"
                        } else {
                            "Mesa ${uiState.table?.status?.getDisplayName()?.lowercase() ?: "desconocida"}"
                        }
                        
                        Text(
                            text = displayText,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "No hay órdenes activas para esta mesa",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
            
            else -> {
                MultipleOrdersDetailsContent(
                    orders = uiState.allActiveOrders,
                    table = uiState.table!!,
                    totalSubtotal = uiState.totalSubtotal,
                    totalTax = uiState.totalTax,
                    totalAmount = uiState.totalAmount
                )
            }
        }
    }

    // Close table dialogs
    if (uiState.showCloseDialog && uiState.allActiveOrders.size == 1 && uiState.table != null) {
        CloseTableDialog(
            order = uiState.allActiveOrders.first(),
            table = uiState.table!!,
            onDismiss = { viewModel.hideCloseTableDialog() },
            onConfirm = { paymentMethod ->
                scope.launch {
                    viewModel.closeTable(paymentMethod)
                }
            },
            onDivideAccount = {
                viewModel.hideCloseTableDialog()
                viewModel.showBillSplitDialog()
            }
        )
    }
    
    if (uiState.showMultipleOrdersCloseDialog && uiState.allActiveOrders.size > 1 && uiState.table != null) {
        MultipleOrdersCloseTableDialog(
            orders = uiState.allActiveOrders,
            table = uiState.table!!,
            onDismiss = { viewModel.hideMultipleOrdersCloseDialog() },
            onConfirm = { paymentMethod ->
                scope.launch {
                    viewModel.closeMultipleOrders(paymentMethod)
                }
            },
            onDivideAccount = {
                viewModel.hideMultipleOrdersCloseDialog()
                viewModel.showBillSplitDialog()
            }
        )
    }

    // Add products dialog
    if (uiState.showAddProductDialog && uiState.allActiveOrders.isNotEmpty()) {
        AddProductToOrderDialog(
            orders = uiState.allActiveOrders,
            availableProducts = uiState.availableProducts,
            onDismiss = { viewModel.hideAddProductDialog() },
            onConfirm = { orderId, orderItems ->
                viewModel.addProductsToOrder(orderId, orderItems)
            }
        )
    }

    // Show tickets - individual o múltiples
    if (uiState.showTicket) {
        if (uiState.splitTickets.isNotEmpty()) {
            // Mostrar múltiples tickets de división
            MultipleTicketsDialog(
                tickets = uiState.splitTickets,
                onDismiss = {
                    viewModel.hideTicket()
                    onTableClosed() // Navigate back to tables list after closing tickets
                }
            )
        } else if (uiState.lastTicket != null) {
            // Mostrar ticket único
            TicketDialog(
                ticket = uiState.lastTicket!!,
                onDismiss = { 
                    viewModel.hideTicket()
                    onTableClosed() // Navigate back to tables list after closing ticket
                }
            )
        }
    }

    // Bill split dialog
    if (uiState.showBillSplitDialog && uiState.allActiveOrders.isNotEmpty() && uiState.table != null) {
        BillSplitMainDialog(
            orders = uiState.allActiveOrders,
            tableName = uiState.table!!.displayName,
            onDismiss = { viewModel.hideBillSplitDialog() },
            onSplitComplete = { billSplit, paymentMethod ->
                scope.launch {
                    viewModel.processBillSplit(billSplit, paymentMethod)
                }
            }
        )
    }
    
    // Show error
    uiState.error?.let { error ->
        LaunchedEffect(error) {
            // Here you could show a Snackbar with the error
            viewModel.clearError()
        }
    }
}

@Composable
fun OrderDetailsContent(
    order: Order,
    table: Table
) {
    val currencyFormat = NumberFormat.getCurrencyInstance(Locale.US)
    
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Order header
        item {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Orden ${order.orderNumber}",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = order.status.displayName,
                            style = MaterialTheme.typography.labelLarge,
                            color = Color(android.graphics.Color.parseColor(order.status.color)),
                            fontWeight = FontWeight.Medium
                        )
                    }
                    
                    order.notes?.let { notes ->
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Notas: $notes",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        )
                    }
                }
            }
        }

        // Items
        item {
            Text(
                text = "Productos (${order.items.size})",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
        }

        items(order.items) { item ->
            Card {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "${item.quantity}x ${item.productName}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        
                        Text(
                            text = currencyFormat.format(item.productPrice),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        
                        item.notes?.let { notes ->
                            Text(
                                text = "Notas: $notes",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                            )
                        }
                    }
                    
                    Text(
                        text = currencyFormat.format(item.subtotal),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Totals
        item {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Resumen",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Subtotal:")
                        Text(currencyFormat.format(order.subtotal))
                    }
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Impuestos:")
                        Text(currencyFormat.format(order.tax))
                    }
                    
                    if (order.discount > BigDecimal.ZERO) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Descuento:")
                            Text(
                                "-${currencyFormat.format(order.discount)}",
                                color = Color(0xFF4CAF50)
                            )
                        }
                    }
                    
                    Divider()
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            "Total:",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            currencyFormat.format(order.total),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MultipleOrdersDetailsContent(
    orders: List<Order>,
    table: Table,
    totalSubtotal: BigDecimal,
    totalTax: BigDecimal,
    totalAmount: BigDecimal
) {
    val currencyFormat = NumberFormat.getCurrencyInstance(Locale.US)
    
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Summary card with totals
        item {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Resumen Total",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${orders.size} orden${if (orders.size > 1) "es" else ""}",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Subtotal:")
                        Text(currencyFormat.format(totalSubtotal))
                    }
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Impuestos:")
                        Text(currencyFormat.format(totalTax))
                    }
                    
                    Divider(modifier = Modifier.padding(vertical = 8.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Total:",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = currencyFormat.format(totalAmount),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
        
        // Individual orders
        items(orders.sortedByDescending { it.createdAt }) { order ->
            OrderCard(order = order)
        }
    }
}

@Composable
private fun OrderCard(order: Order) {
    val currencyFormat = NumberFormat.getCurrencyInstance(Locale.US)
    
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Orden #${order.orderNumber}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = order.status.displayName,
                    style = MaterialTheme.typography.labelLarge,
                    color = Color(android.graphics.Color.parseColor(order.status.color)),
                    fontWeight = FontWeight.Medium
                )
            }
            
            if (order.notes?.isNotEmpty() == true) {
                Text(
                    text = "Notas: ${order.notes}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Items
            Text(
                text = "Productos (${order.items.size})",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Medium
            )
            
            order.items.forEach { item ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "${item.quantity}x ${item.productName}",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = currencyFormat.format(item.subtotal),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                }
                
                if (item.notes?.isNotEmpty() == true) {
                    Text(
                        text = "  • ${item.notes}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Total orden:",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = currencyFormat.format(order.total),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}
