package com.intimocoffee.waiter.feature.inventory.presentation.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import java.text.NumberFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StockAdjustmentDialog(
    products: List<ProductStockDetails>,
    onDismiss: () -> Unit,
    onAdjustStock: (productId: Long, newQuantity: Int, adjustmentType: StockAdjustmentType, reason: String, notes: String?) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedProductId by remember { mutableLongStateOf(products.firstOrNull()?.productId ?: 0L) }
    var adjustmentType by remember { mutableStateOf(StockAdjustmentType.MANUAL_ADJUSTMENT) }
    var newQuantity by remember { mutableStateOf("") }
    var reason by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var expandedProduct by remember { mutableStateOf(false) }
    var expandedType by remember { mutableStateOf(false) }
    
    val selectedProduct = products.find { it.productId == selectedProductId }
    val currentStock = selectedProduct?.currentStock ?: 0
    
    // Update reason based on adjustment type
    LaunchedEffect(adjustmentType) {
        reason = when (adjustmentType) {
            StockAdjustmentType.RESTOCK -> "Reabastecimiento de inventario"
            StockAdjustmentType.CORRECTION -> "Corrección de inventario"
            StockAdjustmentType.DAMAGED -> "Productos dañados/vencidos"
            StockAdjustmentType.MANUAL_ADJUSTMENT -> "Ajuste manual"
            StockAdjustmentType.INVENTORY_COUNT -> "Conteo de inventario"
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
                .wrapContentHeight(),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Ajustar Stock",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = "Cerrar")
                        }
                    }
                }
                
                // Product Selection
                item {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Producto",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Medium
                        )
                        
                        ExposedDropdownMenuBox(
                            expanded = expandedProduct,
                            onExpandedChange = { expandedProduct = !expandedProduct }
                        ) {
                            OutlinedTextField(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .menuAnchor(),
                                readOnly = true,
                                value = selectedProduct?.productName ?: "Seleccionar producto",
                                onValueChange = {},
                                trailingIcon = {
                                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedProduct)
                                },
                                colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
                            )
                            
                            ExposedDropdownMenu(
                                expanded = expandedProduct,
                                onDismissRequest = { expandedProduct = false }
                            ) {
                                products.forEach { product ->
                                    DropdownMenuItem(
                                        text = {
                                            Column {
                                                Text(
                                                    text = product.productName,
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    fontWeight = FontWeight.Medium
                                                )
                                                Text(
                                                    text = "Stock actual: ${product.currentStock}",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        },
                                        onClick = {
                                            selectedProductId = product.productId
                                            expandedProduct = false
                                            newQuantity = product.currentStock.toString()
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
                
                // Current Stock Info
                item {
                    selectedProduct?.let { product ->
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)
                            )
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = "Estado Actual",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "Stock: ${product.currentStock}",
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    Text(
                                        text = "Mínimo: ${product.minStockLevel}",
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                                
                                // Show alerts
                                product.alerts.forEach { alert ->
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
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Text(
                                            text = alert.alertType.displayName,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = getAlertColor(alert.alertType)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                
                // Adjustment Type Selection
                item {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Tipo de Ajuste",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Medium
                        )
                        
                        ExposedDropdownMenuBox(
                            expanded = expandedType,
                            onExpandedChange = { expandedType = !expandedType }
                        ) {
                            OutlinedTextField(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .menuAnchor(),
                                readOnly = true,
                                value = adjustmentType.displayName,
                                onValueChange = {},
                                trailingIcon = {
                                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedType)
                                },
                                colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
                            )
                            
                            ExposedDropdownMenu(
                                expanded = expandedType,
                                onDismissRequest = { expandedType = false }
                            ) {
                                StockAdjustmentType.values().forEach { type ->
                                    DropdownMenuItem(
                                        text = {
                                            Column {
                                                Text(
                                                    text = type.displayName,
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    fontWeight = FontWeight.Medium
                                                )
                                                Text(
                                                    text = type.description,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        },
                                        onClick = {
                                            adjustmentType = type
                                            expandedType = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
                
                // Quick Restock Options (only show for RESTOCK type)
                item {
                    if (adjustmentType == StockAdjustmentType.RESTOCK) {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                            )
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "Restock Rápido",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                
                                LazyColumn(
                                    verticalArrangement = Arrangement.spacedBy(4.dp),
                                    modifier = Modifier.height(120.dp) // Limit height
                                ) {
                                    selectedProduct?.let { product ->
                                        val suggestions = getRestockSuggestions(product)
                                        items(suggestions) { suggestion ->
                                            OutlinedButton(
                                                onClick = {
                                                    newQuantity = (currentStock + suggestion.quantity).toString()
                                                },
                                                modifier = Modifier.fillMaxWidth(),
                                                colors = ButtonDefaults.outlinedButtonColors(
                                                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                                                )
                                            ) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Text(suggestion.label)
                                                    Text(
                                                        text = "+${suggestion.quantity}",
                                                        fontWeight = FontWeight.Bold
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
                
                // New Quantity Input
                item {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = if (adjustmentType == StockAdjustmentType.RESTOCK) "Nueva Cantidad Total" else "Cantidad",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Medium
                        )
                        
                        OutlinedTextField(
                            value = newQuantity,
                            onValueChange = { newQuantity = it },
                            label = { Text("Cantidad") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth(),
                            supportingText = {
                                val quantity = newQuantity.toIntOrNull() ?: 0
                                val difference = quantity - currentStock
                                val text = when {
                                    difference > 0 -> "↑ Aumentar en $difference unidades"
                                    difference < 0 -> "↓ Reducir en ${-difference} unidades"
                                    else -> "Sin cambios"
                                }
                                Text(
                                    text = text,
                                    color = when {
                                        difference > 0 -> SuccessColor
                                        difference < 0 -> MaterialTheme.colorScheme.error
                                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                                    }
                                )
                            }
                        )
                    }
                }
                
                // Reason Input
                item {
                    OutlinedTextField(
                        value = reason,
                        onValueChange = { reason = it },
                        label = { Text("Motivo") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 2
                    )
                }
                
                // Notes Input
                item {
                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text("Notas adicionales (opcional)") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 3
                    )
                }
                
                // Action Buttons
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = onDismiss,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Cancelar")
                        }
                        
                        Button(
                            onClick = {
                                val quantity = newQuantity.toIntOrNull()
                                if (quantity != null && quantity >= 0 && reason.isNotBlank()) {
                                    onAdjustStock(
                                        selectedProductId,
                                        quantity,
                                        adjustmentType,
                                        reason,
                                        notes.takeIf { it.isNotBlank() }
                                    )
                                }
                            },
                            enabled = newQuantity.toIntOrNull() != null && 
                                     (newQuantity.toIntOrNull() ?: -1) >= 0 && 
                                     reason.isNotBlank(),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = when (adjustmentType) {
                                    StockAdjustmentType.RESTOCK -> Icons.Default.Add
                                    else -> Icons.Default.Edit
                                },
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text("Aplicar")
                        }
                    }
                }
            }
        }
    }
}

enum class StockAdjustmentType(val displayName: String, val description: String) {
    RESTOCK("Reabastecimiento", "Agregar nuevos productos al inventario"),
    CORRECTION("Corrección", "Corregir errores en el conteo"),
    DAMAGED("Productos Dañados", "Remover productos dañados o vencidos"),
    MANUAL_ADJUSTMENT("Ajuste Manual", "Ajuste manual del stock"),
    INVENTORY_COUNT("Conteo Físico", "Ajuste basado en conteo físico")
}

data class RestockSuggestion(
    val label: String,
    val quantity: Int
)

private fun getRestockSuggestions(product: ProductStockDetails): List<RestockSuggestion> {
    val minStock = product.minStockLevel
    val currentStock = product.currentStock
    
    return listOf(
        RestockSuggestion("Llevar al mínimo", maxOf(0, minStock - currentStock)),
        RestockSuggestion("Stock para 1 semana", minStock * 2),
        RestockSuggestion("Stock para 2 semanas", minStock * 3),
        RestockSuggestion("Stock para 1 mes", minStock * 4),
        RestockSuggestion("Restock completo", minStock * 5)
    ).filter { it.quantity > 0 }
}

private fun getAlertColor(alertType: StockAlertType): Color {
    return when (alertType) {
        StockAlertType.OUT_OF_STOCK -> ErrorLight
        StockAlertType.CRITICAL -> ErrorLight
        StockAlertType.LOW_STOCK -> WarningColor
        StockAlertType.OVERSTOCKED -> InfoColor
    }
}
