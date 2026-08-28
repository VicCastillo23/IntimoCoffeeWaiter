package com.intimocoffee.waiter

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.intimocoffee.waiter.R
import com.intimocoffee.waiter.feature.orders.domain.model.Order
import com.intimocoffee.waiter.feature.orders.domain.model.OrderItem
import com.intimocoffee.waiter.feature.orders.domain.model.OrderStatus
import com.intimocoffee.waiter.feature.orders.presentation.OrderEditScreen
import com.intimocoffee.waiter.core.alert.WorkAlertSound
import com.intimocoffee.waiter.feature.orders.presentation.OrdersViewModel
import com.intimocoffee.waiter.ota.OtaUpdateDialog
import java.math.BigDecimal
import java.text.NumberFormat
import java.util.*

private enum class WaiterTab { MIS_ORDENES, LISTAS, ENTREGADAS }

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun WaiterMainScreen(
    viewModel: OrdersViewModel = hiltViewModel(),
    onNavigateToCreateOrder: () -> Unit = {},
    onLogout: () -> Unit = {},
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    var showOtaDialog by remember { mutableStateOf(false) }
    uiState.orderToEdit?.let { orderToEdit ->
        OrderEditScreen(
            order = orderToEdit,
            products = uiState.productsForEdit,
            modifierOptionsByCategory = uiState.modifierOptionsByCategory,
            pricedModifierSectionsByCategory = uiState.pricedModifierSectionsByCategory,
            temperaturaOptionsByCategory = uiState.temperaturaOptionsByCategory,
            onBack = { viewModel.dismissEditOrder() },
            onSave = { removed, updated, added ->
                viewModel.applyOrderEditsRemote(orderToEdit, removed, updated, added)
            }
        )
        return
    }

    var selectedTab by remember { mutableStateOf(WaiterTab.MIS_ORDENES) }

    LaunchedEffect(Unit) {
        viewModel.loadOrders()
    }

    LaunchedEffect(Unit) {
        viewModel.workAlertEvent.collect {
            WorkAlertSound.play(context)
        }
    }

    // Ordenes del mesero actual
    val currentUserId = uiState.currentUserId
    val waiterOrders = uiState.orders
        .filter { order ->
            (currentUserId == null || order.createdBy == currentUserId) &&
            order.status != OrderStatus.DELIVERED &&
            order.status != OrderStatus.PAID &&
            !OrderStatus.isCompleted(order.status)
        }
        .sortedByDescending { it.createdAt }

    // Ordenes listas para entregar
    val readyOrders = uiState.orders
        .filter { it.status == OrderStatus.READY }
        .sortedBy { it.createdAt }

    // Órdenes ya entregadas (para validar con la mesa)
    val deliveredOrders = uiState.orders
        .filter { order ->
            order.status == OrderStatus.DELIVERED &&
                (currentUserId == null || order.createdBy == currentUserId)
        }
        .sortedByDescending { it.updatedAt }

    // Auto-cambiar a "Listas" si hay nuevas
    val readyCount = readyOrders.size
    val deliveredCount = deliveredOrders.size

    // Sonido de campana cuando hay nuevas órdenes listas
    var prevReadyCount by remember { mutableStateOf(-1) }
    LaunchedEffect(readyCount) {
        if (prevReadyCount >= 0 && readyCount > prevReadyCount) {
            try {
                WorkAlertSound.play(context)
            } catch (_: Exception) {}
        }
        prevReadyCount = readyCount
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onNavigateToCreateOrder,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                icon = { Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(20.dp)) },
                text = { Text("Nueva", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.labelLarge) }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 12.dp)
        ) {
            Spacer(modifier = Modifier.height(12.dp))

            // ── Header ────────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Image(
                        painter = painterResource(R.drawable.intimo_logo),
                        contentDescription = "Íntimo",
                        modifier = Modifier.size(22.dp),
                    )
                    Text(
                        text = uiState.currentUserName?.takeIf { it.isNotBlank() } ?: "Mesero",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

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
                            contentDescription = "Actualizar órdenes",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(
                        onClick = { showOtaDialog = true },
                        modifier = Modifier.background(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                            RoundedCornerShape(12.dp)
                        )
                    ) {
                        Icon(
                            Icons.Default.SystemUpdate,
                            contentDescription = "Actualizar app",
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

            // ── Chip de conexión ────────────────────────────────────
            if (uiState.serverUrl.isNotBlank()) {
                val displayUrl = uiState.serverUrl
                    .removePrefix("http://")
                    .trimEnd('/')
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            Icons.Default.Wifi, null,
                            Modifier.size(12.dp),
                            tint = MaterialTheme.colorScheme.secondary
                        )
                        Text(
                            "Conectado: $displayUrl",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ── Chips de navegación ────────────────────────────────────
            androidx.compose.foundation.lazy.LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                item {
                    FilterChip(
                        selected = selectedTab == WaiterTab.MIS_ORDENES,
                        onClick = { selectedTab = WaiterTab.MIS_ORDENES },
                        label = {
                            Text(
                                "Mis Órdenes (${waiterOrders.size})",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = if (selectedTab == WaiterTab.MIS_ORDENES) FontWeight.Bold else FontWeight.Normal,
                            )
                        },
                        modifier = Modifier.height(36.dp),
                    )
                }
                item {
                    BadgedBox(
                        badge = {
                            if (readyCount > 0) {
                                Badge(containerColor = MaterialTheme.colorScheme.error) {
                                    Text(
                                        readyCount.toString(),
                                        color = MaterialTheme.colorScheme.onError,
                                    )
                                }
                            }
                        },
                    ) {
                        FilterChip(
                            selected = selectedTab == WaiterTab.LISTAS,
                            onClick = { selectedTab = WaiterTab.LISTAS },
                            label = {
                                Text(
                                    "Listas (${readyCount})",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = if (selectedTab == WaiterTab.LISTAS) FontWeight.Bold else FontWeight.Normal,
                                )
                            },
                            modifier = Modifier.height(36.dp),
                        )
                    }
                }
                item {
                    FilterChip(
                        selected = selectedTab == WaiterTab.ENTREGADAS,
                        onClick = { selectedTab = WaiterTab.ENTREGADAS },
                        label = {
                            Text(
                                "Entregadas (${deliveredCount})",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = if (selectedTab == WaiterTab.ENTREGADAS) FontWeight.Bold else FontWeight.Normal,
                            )
                        },
                        modifier = Modifier.height(36.dp),
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            uiState.error?.let { msg ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                ) {
                    Text(
                        text = msg,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

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
                                        },
                                        onLongClickEdit = { viewModel.openEditOrder(order) },
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
                                        },
                                        onLongClickEdit = { viewModel.openEditOrder(order) },
                                    )
                                }
                            }
                        }
                    }

                    WaiterTab.ENTREGADAS -> {
                        if (deliveredOrders.isEmpty()) {
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    Icon(
                                        Icons.Default.DoneAll,
                                        contentDescription = null,
                                        modifier = Modifier.size(56.dp),
                                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.25f),
                                    )
                                    Text(
                                        "Aún no hay órdenes entregadas.",
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                    Text(
                                        "Aquí verás las que marques como entregadas para validar con la mesa.",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                    )
                                }
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                                contentPadding = PaddingValues(bottom = 80.dp),
                            ) {
                                items(deliveredOrders) { order ->
                                    WaiterOrderCard(
                                        order = order,
                                        onDeliverOrder = {},
                                        showDeliverAction = false,
                                        onLongClickEdit = { viewModel.openEditOrder(order) },
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showOtaDialog) {
        OtaUpdateDialog(
            onDismiss = { showOtaDialog = false },
            lanServerUrl = uiState.serverUrl.takeIf { it.isNotBlank() },
            autoCheck = true,
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun WaiterOrderCard(
    order: Order,
    onDeliverOrder: (Long) -> Unit,
    onLongClickEdit: () -> Unit = {},
    showDeliverAction: Boolean = true,
) {
    val isReady = order.status == OrderStatus.READY
    val isDelivered = order.status == OrderStatus.DELIVERED
    val cardColor = when {
        isReady -> MaterialTheme.colorScheme.primary.copy(alpha = 0.05f)
        isDelivered -> MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.45f)
        else -> MaterialTheme.colorScheme.surface
    }
    val borderColor = if (isReady) MaterialTheme.colorScheme.primary else Color.Transparent
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = {},
                onLongClick = onLongClickEdit
            )
            .background(borderColor.copy(alpha = if (isReady) 0.15f else 0f), RoundedCornerShape(12.dp)),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isReady) 3.dp else 1.dp),
        colors = CardDefaults.cardColors(
            containerColor = cardColor
        )
    ) {
        Column(
            modifier = Modifier.padding(10.dp)
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
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        if (isReady) {
                            Icon(
                                Icons.Default.Notifications,
                                contentDescription = "Lista",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Text(
                            text = "Orden #${order.orderNumber}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (isReady) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(2.dp))
                    
                    Text(
                        text = "Mesa: ${order.tableName ?: order.tableId}",
                        style = MaterialTheme.typography.bodyMedium,
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

            if (showDeliverAction && isReady) {
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

            if (!isDelivered) {
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(
                    onClick = onLongClickEdit,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Editar orden")
                }
            }

            // Timestamp
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = when {
                    isReady -> "Lista desde: ${formatDateTime(order.updatedAt.toString())}"
                    isDelivered -> "Entregada: ${formatDateTime(order.updatedAt.toString())}"
                    else -> "Actualizada: ${formatDateTime(order.updatedAt.toString())}"
                },
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