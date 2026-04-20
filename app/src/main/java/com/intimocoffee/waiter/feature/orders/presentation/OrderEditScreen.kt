package com.intimocoffee.waiter.feature.orders.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.intimocoffee.waiter.core.network.ModifierOptionResponse
import com.intimocoffee.waiter.feature.orders.domain.model.Order
import com.intimocoffee.waiter.feature.orders.domain.model.OrderItem
import com.intimocoffee.waiter.feature.orders.domain.model.OrderStatus
import com.intimocoffee.waiter.feature.orders.presentation.components.ProductModifierSheet
import com.intimocoffee.waiter.feature.products.domain.model.Product
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import java.math.BigDecimal
import java.text.NumberFormat
import java.util.Locale

private data class EditScreenLine(
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
fun OrderEditScreen(
    order: Order,
    products: List<Product>,
    modifierOptionsByCategory: Map<Long, List<ModifierOptionResponse>> = emptyMap(),
    pricedModifierSectionsByCategory: Map<Long, List<Pair<String, List<ModifierOptionResponse>>>> = emptyMap(),
    temperaturaOptionsByCategory: Map<Long, List<ModifierOptionResponse>> = emptyMap(),
    onBack: () -> Unit,
    onSave: (removedIds: List<Long>, updated: List<OrderItem>, added: List<OrderItem>) -> Unit,
) {
    val fmt = NumberFormat.getCurrencyInstance(Locale("es", "CO"))
    val canEdit = !OrderStatus.isCompleted(order.status)

    val lines = remember(order.id, order.items) {
        mutableStateListOf<EditScreenLine>().apply {
            order.items.forEachIndexed { index, it ->
                add(
                    EditScreenLine(
                        stableKey = (it.id * 1000L) + index,
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
    var productForModifiers by remember(order.id) { mutableStateOf<Product?>(null) }

    fun replaceLine(key: Long, block: (EditScreenLine) -> EditScreenLine) {
        val idx = lines.indexOfFirst { it.stableKey == key }
        if (idx >= 0) lines[idx] = block(lines[idx])
    }

    fun addProduct(product: Product) {
        val idx = lines.indexOfFirst { it.itemId == 0L && it.productId == product.id }
        if (idx >= 0) {
            val current = lines[idx]
            lines[idx] = current.copy(quantity = current.quantity + 1)
            return
        }
        lines.add(
            EditScreenLine(
                stableKey = -System.nanoTime(),
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

    fun addProductWithModifiers(
        product: Product,
        selectedModifiers: List<String>,
        customNote: String,
        priceExtra: BigDecimal
    ) {
        val notes = buildString {
            if (selectedModifiers.isNotEmpty()) append(selectedModifiers.joinToString(", "))
            if (customNote.isNotBlank()) {
                if (selectedModifiers.isNotEmpty()) append(" — ")
                append(customNote.trim())
            }
        }.takeIf { it.isNotBlank() }
        val unitPrice = product.price.add(priceExtra)
        val idx = lines.indexOfFirst {
            it.itemId == 0L && it.productId == product.id && it.notes == notes && it.unitPrice.compareTo(unitPrice) == 0
        }
        if (idx >= 0) {
            val current = lines[idx]
            lines[idx] = current.copy(quantity = current.quantity + 1)
        } else {
            lines.add(
                EditScreenLine(
                    stableKey = -System.nanoTime(),
                    itemId = 0L,
                    productId = product.id,
                    name = product.name,
                    unitPrice = unitPrice,
                    quantity = 1,
                    notes = notes,
                    categoryId = product.categoryId
                )
            )
        }
    }

    fun saveChanges() {
        val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
        val originalById = order.items.associateBy { it.id }
        val removedIds = originalById.keys.filter { id -> lines.none { it.itemId == id } }
        val updated = mutableListOf<OrderItem>()
        lines.forEach { line ->
            if (line.itemId == 0L) return@forEach
            val orig = originalById[line.itemId] ?: return@forEach
            val newSub = line.unitPrice.multiply(BigDecimal(line.quantity))
            if (line.quantity != orig.quantity || newSub.compareTo(orig.subtotal) != 0) {
                updated.add(orig.copy(quantity = line.quantity, subtotal = newSub))
            }
        }
        val added = mutableListOf<OrderItem>()
        lines.forEach { line ->
            if (line.itemId != 0L || line.quantity <= 0) return@forEach
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
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Editar orden #${order.orderNumber}", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Volver")
                    }
                },
                actions = {
                    Button(onClick = { saveChanges() }, enabled = canEdit) {
                        Text("Guardar")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background)
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (!canEdit) {
                Text(
                    "Esta orden ya está pagada o cancelada y no se puede modificar.",
                    color = MaterialTheme.colorScheme.error
                )
            }

            Text("Resumen de Orden", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            LazyColumn(
                modifier = Modifier.heightIn(min = 130.dp, max = 230.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                itemsIndexed(lines, key = { index, line -> "${line.stableKey}_$index" }) { _, line ->
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(line.name, maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.Medium)
                                Text(fmt.format(line.unitPrice), style = MaterialTheme.typography.labelSmall)
                                if (!line.notes.isNullOrBlank()) {
                                    Text(
                                        text = line.notes,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(
                                    onClick = {
                                        if (line.quantity > 1) replaceLine(line.stableKey) { it.copy(quantity = it.quantity - 1) }
                                        else lines.remove(line)
                                    },
                                    enabled = canEdit
                                ) { Icon(Icons.Default.Remove, contentDescription = "Quitar uno") }
                                Text("${line.quantity}", fontWeight = FontWeight.Bold)
                                IconButton(
                                    onClick = { replaceLine(line.stableKey) { it.copy(quantity = it.quantity + 1) } },
                                    enabled = canEdit
                                ) { Icon(Icons.Default.Add, contentDescription = "Agregar uno") }
                                IconButton(
                                    onClick = { lines.remove(line) },
                                    enabled = canEdit
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = "Eliminar", tint = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }
                }
            }

            Divider()
            Text("Agregar desde menu", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)

            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 110.dp),
                modifier = Modifier.fillMaxWidth().weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                itemsIndexed(products.take(180), key = { index, p -> "prod_${p.id}_$index" }) { _, p ->
                    val pendingQty = lines.filter { it.itemId == 0L && it.productId == p.id }.sumOf { it.quantity }
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(enabled = canEdit) { productForModifiers = p },
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = p.name,
                                style = MaterialTheme.typography.bodySmall,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(fmt.format(p.price), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Tocar para personalizar",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                if (pendingQty > 0) {
                                    Text("+$pendingQty", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(2.dp))
        }
    }

    productForModifiers?.let { p ->
        ProductModifierSheet(
            product = p,
            modifierOptions = modifierOptionsByCategory,
            pricedSections = pricedModifierSectionsByCategory[p.categoryId] ?: emptyList(),
            temperaturaOptions = temperaturaOptionsByCategory[p.categoryId] ?: emptyList(),
            onAdd = { modifiers, note, priceExtra ->
                addProductWithModifiers(p, modifiers, note, priceExtra)
                productForModifiers = null
            },
            onDismiss = { productForModifiers = null }
        )
    }
}
