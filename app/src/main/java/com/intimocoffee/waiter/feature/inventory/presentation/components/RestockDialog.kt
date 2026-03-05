package com.intimocoffee.waiter.feature.inventory.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.intimocoffee.waiter.feature.inventory.domain.model.ProductStockDetails
import com.intimocoffee.waiter.feature.inventory.domain.model.StockAlertType
import com.intimocoffee.waiter.ui.theme.InfoColor
import com.intimocoffee.waiter.ui.theme.SuccessColor
import com.intimocoffee.waiter.ui.theme.WarningColor
import com.intimocoffee.waiter.ui.theme.ErrorLight

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RestockDialog(
    products: List<ProductStockDetails>,
    onDismiss: () -> Unit,
    onRestock: (productId: Long, addQuantity: Int, notes: String?) -> Unit,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    var recentlyRestockedProducts by remember { mutableStateOf(setOf<Long>()) }
    
    // Remover productos de la lista de feedback después de 3 segundos
    LaunchedEffect(recentlyRestockedProducts) {
        if (recentlyRestockedProducts.isNotEmpty()) {
            kotlinx.coroutines.delay(3000)
            recentlyRestockedProducts = emptySet()
        }
    }
    
    val filteredProducts = remember(products, searchQuery) {
        // Los productos ya vienen filtrados desde el repositorio (solo alimentos)
        if (searchQuery.isBlank()) {
            products.sortedBy { 
                // Priorizar productos con alertas críticas
                when {
                    it.alerts.any { alert -> alert.alertType == StockAlertType.OUT_OF_STOCK } -> 0
                    it.alerts.any { alert -> alert.alertType == StockAlertType.CRITICAL } -> 1  
                    it.alerts.any { alert -> alert.alertType == StockAlertType.LOW_STOCK } -> 2
                    else -> 3
                }
            }
        } else {
            products.filter { 
                it.productName.contains(searchQuery, ignoreCase = true) 
            }.sortedBy { it.productName }
        }
    }
    
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true,
            usePlatformDefaultWidth = false
        )
    ) {
        Card(
            modifier = modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.85f),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Restock de Alimentos",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Solo productos de alimentación",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Cerrar")
                    }
                }
                
                // Search Bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    label = { Text("Buscar producto") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                
                // Product List with Inline Restock
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filteredProducts) { product ->
                        ProductRestockCard(
                            product = product,
                            isRecentlyRestocked = product.productId in recentlyRestockedProducts,
                            onRestock = { quantity, notes ->
                                onRestock(product.productId, quantity, notes)
                                // Agregar a la lista de productos recientemente reabastecidos
                                recentlyRestockedProducts = recentlyRestockedProducts + product.productId
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ProductRestockCard(
    product: ProductStockDetails,
    isRecentlyRestocked: Boolean = false,
    onRestock: (quantity: Int, notes: String?) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    var customQuantity by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = when {
                isRecentlyRestocked -> MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                product.alerts.any { it.alertType == StockAlertType.OUT_OF_STOCK } -> 
                    MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.1f)
                product.alerts.any { it.alertType == StockAlertType.CRITICAL } -> 
                    MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.2f)
                product.alerts.any { it.alertType == StockAlertType.LOW_STOCK } -> 
                    MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.1f)
                else -> MaterialTheme.colorScheme.surface
            }
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Product Info Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = product.productName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Actual: ${product.currentStock}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Mín: ${product.minStockLevel}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    
                    // Show success message or main alert
                    if (isRecentlyRestocked) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = "Restock aplicado",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    } else {
                        product.alerts.firstOrNull()?.let { alert ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = when (alert.alertType) {
                                        StockAlertType.OUT_OF_STOCK -> Icons.Default.Error
                                        StockAlertType.CRITICAL -> Icons.Default.Warning  
                                        StockAlertType.LOW_STOCK -> Icons.Default.Info
                                        StockAlertType.OVERSTOCKED -> Icons.Default.TrendingUp
                                    },
                                    contentDescription = null,
                                    tint = getAlertColor(alert.alertType),
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = alert.alertType.displayName,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = getAlertColor(alert.alertType),
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
                
                // Stock Status Circle
                val stockColor = when {
                    product.currentStock == 0 -> MaterialTheme.colorScheme.error
                    product.currentStock <= product.minStockLevel -> WarningColor
                    else -> SuccessColor
                }
                
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(stockColor.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = product.currentStock.toString(),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = stockColor
                        )
                        if (product.currentStock <= product.minStockLevel) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Reabastecer",
                                tint = stockColor,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
            
            // Quick Restock Buttons
            val suggestions = listOf(
                "Al mín" to maxOf(0, product.minStockLevel - product.currentStock),
                "1 sem" to product.minStockLevel * 2,
                "1 mes" to product.minStockLevel * 4
            ).filter { it.second > 0 }
            
            if (suggestions.isNotEmpty()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    suggestions.forEach { (label, quantity) ->
                        OutlinedButton(
                            onClick = { 
                                onRestock(quantity, "Restock rápido: $label")
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.primary
                            )
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "+$quantity",
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.labelLarge
                                )
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                        }
                    }
                    
                    // More options button
                    OutlinedButton(
                        onClick = { expanded = !expanded },
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.secondary
                        )
                    ) {
                        Icon(
                            imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = if (expanded) "Menos opciones" else "Más opciones",
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
            
            // Expanded custom input
            if (expanded) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = customQuantity,
                            onValueChange = { customQuantity = it },
                            label = { Text("Cantidad") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        
                        Button(
                            onClick = {
                                val quantity = customQuantity.toIntOrNull()
                                if (quantity != null && quantity > 0) {
                                    onRestock(quantity, notes.takeIf { it.isNotBlank() })
                                    customQuantity = ""
                                    notes = ""
                                    expanded = false
                                }
                            },
                            enabled = (customQuantity.toIntOrNull() ?: 0) > 0
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        }
                    }
                    
                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text("Notas (opcional)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        placeholder = { Text("Ej: Lote nuevo") }
                    )
                }
            }
        }
    }
}

private fun getAlertColor(alertType: StockAlertType): Color {
    return when (alertType) {
        StockAlertType.OUT_OF_STOCK -> ErrorLight
        StockAlertType.CRITICAL -> ErrorLight
        StockAlertType.LOW_STOCK -> WarningColor
        StockAlertType.OVERSTOCKED -> InfoColor
    }
}
