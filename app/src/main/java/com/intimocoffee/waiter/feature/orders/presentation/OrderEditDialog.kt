package com.intimocoffee.waiter.feature.orders.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.intimocoffee.waiter.feature.orders.domain.model.Order
import com.intimocoffee.waiter.feature.orders.domain.model.OrderItem
import com.intimocoffee.waiter.feature.orders.domain.model.OrderStatus
import com.intimocoffee.waiter.feature.products.domain.model.Product
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import java.math.BigDecimal
import java.text.NumberFormat
import java.util.Locale

private data class EditableLine(
    val stableKey: Long,
    val itemId: Long,
    val productId: Long,
    val name: String,
    val unitPrice: BigDecimal,
    val quantity: Int,
    val notes: String?,
    val categoryId: Long,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrderEditDialog(
    order: Order,
    products: List<Product>,
    onDismiss: () -> Unit,
    onSave: (removedIds: List<Long>, updated: List<OrderItem>, added: List<OrderItem>) -> Unit,
) {
    val fmt = NumberFormat.getCurrencyInstance(Locale("es", "CO"))
    val canEdit = !OrderStatus.isCompleted(order.status)

    val lines = remember(order.id, order.items) {
        mutableStateListOf<EditableLine>().apply {
            order.items.forEachIndexed { index, it ->
                val uniqueKey = if (it.id != 0L) (it.id * 1000L) + index else -(index + 1L)
                add(
                    EditableLine(
                        stableKey = uniqueKey,
                        itemId = it.id,
                        productId = it.productId,
                        name = it.productName,
                        unitPrice = it.productPrice,
                        quantity = it.quantity,
                        notes = it.notes,
                        categoryId = it.categoryId,
                    )
                )
            }
        }
    }

    fun replaceLine(key: Long, block: (EditableLine) -> EditableLine) {
        val idx = lines.indexOfFirst { it.stableKey == key }
        if (idx >= 0) lines[idx] = block(lines[idx])
    }

    fun addProductToEditableLines(product: Product) {
        val idx = lines.indexOfFirst { it.itemId == 0L && it.productId == product.id }
        if (idx >= 0) {
            val current = lines[idx]
            lines[idx] = current.copy(quantity = current.quantity + 1)
            return
        }
        val newKey = -(System.currentTimeMillis() + product.id)
        lines.add(
            EditableLine(
                stableKey = newKey,
                itemId = 0L,
                productId = product.id,
                name = product.name,
                unitPrice = product.price,
                quantity = 1,
                notes = null,
                categoryId = product.categoryId
            )
        )
    }

    fun adjustNewProductQty(product: Product, delta: Int) {
        val idx = lines.indexOfFirst { it.itemId == 0L && it.productId == product.id }
        if (idx < 0) {
            if (delta > 0) addProductToEditableLines(product)
            return
        }
        val current = lines[idx]
        val nextQty = current.quantity + delta
        if (nextQty <= 0) {
            lines.removeAt(idx)
        } else {
            lines[idx] = current.copy(quantity = nextQty)
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                TopAppBar(
                    title = { Text("Editar orden #${order.orderNumber}") },
                    navigationIcon = {
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = "Cerrar")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )
                if (!canEdit) {
                    Text(
                        "Esta orden ya está pagada o cancelada y no se puede modificar.",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.heightIn(min = 180.dp, max = 360.dp)
                ) {
                    items(lines, key = { it.stableKey }) { line ->
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(line.name, fontWeight = FontWeight.SemiBold)
                                    Text(fmt.format(line.unitPrice), style = MaterialTheme.typography.labelSmall)
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    IconButton(
                                        onClick = {
                                            if (line.quantity > 1) {
                                                replaceLine(line.stableKey) { it.copy(quantity = it.quantity - 1) }
                                            } else {
                                                lines.remove(line)
                                            }
                                        },
                                        enabled = canEdit
                                    ) {
                                        Icon(Icons.Default.Remove, contentDescription = "Menos")
                                    }
                                    Text("${line.quantity}", fontWeight = FontWeight.Bold)
                                    IconButton(
                                        onClick = {
                                            replaceLine(line.stableKey) { it.copy(quantity = it.quantity + 1) }
                                        },
                                        enabled = canEdit
                                    ) {
                                        Icon(Icons.Default.Add, contentDescription = "Más")
                                    }
                                    IconButton(
                                        onClick = { lines.remove(line) },
                                        enabled = canEdit
                                    ) {
                                        Icon(Icons.Default.Delete, contentDescription = "Quitar", tint = MaterialTheme.colorScheme.error)
                                    }
                                }
                            }
                        }
                    }
                }

                Divider()
                Text("Agregar producto", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleSmall)
                LazyColumn(
                    modifier = Modifier.heightIn(max = 220.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(products.take(80), key = { it.id }) { p ->
                        val pendingQty = lines
                            .filter { it.itemId == 0L && it.productId == p.id }
                            .sumOf { it.quantity }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(p.name, style = MaterialTheme.typography.bodySmall)
                                Text(fmt.format(p.price), style = MaterialTheme.typography.labelSmall)
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(
                                    onClick = { adjustNewProductQty(p, -1) },
                                    enabled = canEdit && pendingQty > 0
                                ) {
                                    Icon(Icons.Default.Remove, contentDescription = "Quitar")
                                }
                                Text(
                                    text = pendingQty.toString(),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                IconButton(
                                    onClick = { addProductToEditableLines(p) },
                                    enabled = canEdit
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = "Agregar")
                                }
                                FilledTonalButton(
                                    onClick = { addProductToEditableLines(p) },
                                    enabled = canEdit
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = null)
                                    Text(" Agregar")
                                }
                                if (pendingQty > 0) {
                                    Surface(
                                        tonalElevation = 2.dp,
                                        shape = MaterialTheme.shapes.small,
                                        color = MaterialTheme.colorScheme.primaryContainer,
                                        modifier = Modifier.padding(start = 6.dp)
                                    ) {
                                        Text(
                                            text = "+$pendingQty",
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) { Text("Cancelar") }
                    Button(
                        onClick = {
                            val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
                            val originalById = order.items.associateBy { it.id }
                            val removedIds = originalById.keys.filter { id -> lines.none { it.itemId == id } }
                            val updated = mutableListOf<OrderItem>()
                            for (line in lines) {
                                if (line.itemId == 0L) continue
                                val orig = originalById[line.itemId] ?: continue
                                val newSub = line.unitPrice.multiply(BigDecimal(line.quantity))
                                if (line.quantity != orig.quantity || newSub.compareTo(orig.subtotal) != 0) {
                                    updated.add(
                                        orig.copy(
                                            quantity = line.quantity,
                                            subtotal = newSub
                                        )
                                    )
                                }
                            }
                            val added = mutableListOf<OrderItem>()
                            for (line in lines) {
                                if (line.itemId != 0L || line.quantity <= 0) continue
                                added.add(
                                    OrderItem(
                                        id = 0L,
                                        orderId = order.id,
                                        productId = line.productId,
                                        productName = line.name,
                                        productPrice = line.unitPrice,
                                        quantity = line.quantity,
                                        subtotal = line.unitPrice.multiply(BigDecimal(line.quantity)),
                                        notes = line.notes,
                                        categoryId = line.categoryId,
                                        createdAt = now
                                    )
                                )
                            }
                            onSave(removedIds, updated, added)
                        },
                        enabled = canEdit
                    ) { Text("Guardar cambios") }
                }
            }
        }
    }
}
