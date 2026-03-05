package com.intimocoffee.waiter.feature.inventory.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.intimocoffee.waiter.feature.inventory.domain.model.*
import com.intimocoffee.waiter.feature.inventory.presentation.components.RestockDialog
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InventoryScreen(
    viewModel: InventoryViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    
    LaunchedEffect(Unit) {
        viewModel.loadInventoryData()
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Gestión de Inventario",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold
            )
            
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                IconButton(onClick = { viewModel.refreshData() }) {
                    Icon(Icons.Default.Refresh, contentDescription = "Actualizar")
                }
                
                FilledTonalButton(
                    onClick = { viewModel.showStockAdjustmentDialog() }
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Restock")
                }
            }
        }
        
        if (uiState.isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            // Stock Summary Cards
            uiState.stockSummary?.let { summary ->
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(horizontal = 4.dp)
                ) {
                    item {
                        StockSummaryCard(
                            title = "Total Productos",
                            value = summary.totalProducts.toString(),
                            subtitle = "En catálogo",
                            icon = Icons.Default.Inventory,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    item {
                        StockSummaryCard(
                            title = "En Stock",
                            value = summary.productsInStock.toString(),
                            subtitle = "Disponibles",
                            icon = Icons.Default.CheckCircle,
                            color = Color(0xFF4CAF50)
                        )
                    }
                    item {
                        StockSummaryCard(
                            title = "Stock Bajo",
                            value = summary.productsLowStock.toString(),
                            subtitle = "Necesitan restock",
                            icon = Icons.Default.Warning,
                            color = Color(0xFFFF9800)
                        )
                    }
                    item {
                        StockSummaryCard(
                            title = "Sin Stock",
                            value = summary.productsOutOfStock.toString(),
                            subtitle = "Agotados",
                            icon = Icons.Default.Error,
                            color = Color(0xFFF44336)
                        )
                    }
                    item {
                        StockSummaryCard(
                            title = "Valor Total",
                            value = NumberFormat.getCurrencyInstance(Locale.US).format(summary.totalStockValue),
                            subtitle = "Inventario",
                            icon = Icons.Default.AttachMoney,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                }
            }
            
            // Critical Alerts Section
            if (uiState.alerts.isNotEmpty()) {
                val criticalAlerts = uiState.alerts.filter { 
                    it.alertType in listOf(StockAlertType.OUT_OF_STOCK, StockAlertType.CRITICAL) 
                }
                
                if (criticalAlerts.isNotEmpty()) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFFFFF3E0)
                        ),
                        border = CardDefaults.outlinedCardBorder().copy(
                            brush = androidx.compose.ui.graphics.SolidColor(Color(0xFFFF9800))
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = Color(0xFFFF9800),
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = "Alertas Críticas de Stock",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFFF9800)
                                )
                            }
                            
                            criticalAlerts.take(3).forEach { alert ->
                                AlertItem(
                                    alert = alert,
                                    onClick = { viewModel.showProductDetails(alert.productId) }
                                )
                            }
                            
                            if (criticalAlerts.size > 3) {
                                TextButton(
                                    onClick = { viewModel.showAllAlerts() }
                                ) {
                                    Text("Ver todas las alertas (${criticalAlerts.size})")
                                }
                            }
                        }
                    }
                }
            }
            
            // Products Stock List
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Stock por Producto",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        
                        // Filter buttons
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(
                                onClick = { viewModel.filterProducts("all") },
                                label = { Text("Todos") },
                                selected = uiState.selectedFilter == "all"
                            )
                            FilterChip(
                                onClick = { viewModel.filterProducts("low") },
                                label = { Text("Stock Bajo") },
                                selected = uiState.selectedFilter == "low"
                            )
                            FilterChip(
                                onClick = { viewModel.filterProducts("out") },
                                label = { Text("Sin Stock") },
                                selected = uiState.selectedFilter == "out"
                            )
                        }
                    }
                }
                
                items(uiState.filteredProducts) { productStock ->
                    ProductStockCard(
                        productStock = productStock,
                        onClick = { viewModel.showProductDetails(productStock.productId) }
                    )
                }
                
                if (uiState.filteredProducts.isEmpty() && !uiState.isLoading) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No hay productos que mostrar",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
        
        // Restock Dialog
        if (uiState.showStockAdjustmentDialog) {
            RestockDialog(
                products = uiState.filteredProducts,
                onDismiss = { viewModel.hideStockAdjustmentDialog() },
                onRestock = { productId, addQuantity, notes ->
                    viewModel.restockProduct(
                        productId = productId,
                        addQuantity = addQuantity,
                        notes = notes
                    )
                }
            )
        }
    }
}

@Composable
private fun StockSummaryCard(
    title: String,
    value: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color
) {
    Card(
        modifier = Modifier.width(140.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(24.dp)
            )
            
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center
            )
            
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun AlertItem(
    alert: StockAlert,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = alert.productName,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = when (alert.alertType) {
                    StockAlertType.OUT_OF_STOCK -> "Sin stock disponible"
                    StockAlertType.CRITICAL -> "Stock crítico: ${alert.currentStock} unidades"
                    StockAlertType.LOW_STOCK -> "Stock bajo: ${alert.currentStock} unidades"
                    StockAlertType.OVERSTOCKED -> "Sobrestock: ${alert.currentStock} unidades"
                },
                style = MaterialTheme.typography.bodySmall,
                color = Color.fromHex(alert.alertType.color)
            )
        }
        
        alert.suggestedOrder?.let { suggested ->
            Text(
                text = "Sugerido: $suggested",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ProductStockCard(
    productStock: ProductStockDetails,
    onClick: () -> Unit
) {
    val currencyFormat = NumberFormat.getCurrencyInstance(Locale.US)
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = productStock.productName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Stock: ${productStock.currentStock}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                    
                    Text(
                        text = "Mín: ${productStock.minStockLevel}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    productStock.unitCost?.let { cost ->
                        Text(
                            text = "Valor: ${currencyFormat.format(productStock.totalValue)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                // Show alerts if any
                productStock.alerts.forEach { alert ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(Color.fromHex(alert.alertType.color))
                        )
                        Text(
                            text = alert.alertType.displayName,
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.fromHex(alert.alertType.color),
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
            
            // Stock status indicator
            val stockStatusColor = when {
                productStock.currentStock == 0 -> Color(0xFFF44336)
                productStock.currentStock <= productStock.minStockLevel -> Color(0xFFFF9800)
                else -> Color(0xFF4CAF50)
            }
            
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(stockStatusColor.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = productStock.currentStock.toString(),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = stockStatusColor
                )
            }
        }
    }
}

// Helper extension function
private fun Color.Companion.fromHex(hex: String): Color {
    return Color(android.graphics.Color.parseColor(hex))
}