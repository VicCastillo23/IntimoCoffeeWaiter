package com.intimocoffee.waiter.feature.orders.presentation.create

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.runtime.collectAsState
import com.intimocoffee.waiter.feature.fidelity.domain.model.FidelityCustomer
import com.intimocoffee.waiter.feature.orders.domain.model.CartItem
import com.intimocoffee.waiter.feature.orders.presentation.components.StockWarningsDialog
import com.intimocoffee.waiter.feature.products.domain.model.Category
import com.intimocoffee.waiter.feature.products.domain.model.Product
import com.intimocoffee.waiter.feature.tables.domain.model.Table
import com.intimocoffee.waiter.feature.tables.domain.model.TableStatus
import java.math.BigDecimal
import java.text.NumberFormat
import java.util.*

// ─── Colores por categoría ───────────────────────────────────────────────────
private val categoryColorMap = mapOf(
    1L to Color(0xFF8D4925),
    2L to Color(0xFF1E88E5),
    3L to Color(0xFFFF6F00),
    4L to Color(0xFF43A047),
    5L to Color(0xFFF9A825),
)
private val defaultCatColor = Color(0xFF757575)

private fun catColor(id: Long): Color = categoryColorMap[id] ?: defaultCatColor

private fun parseHexColor(hex: String): Color = try {
    Color(android.graphics.Color.parseColor(hex))
} catch (_: Exception) { defaultCatColor }

// ─── Pantalla principal ──────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateOrderScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: CreateOrderViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(uiState.orderCreated) {
        if (uiState.orderCreated) onNavigateBack()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // TopAppBar
        TopAppBar(
            title = {
                Column {
                    Text("Nueva Orden", fontWeight = FontWeight.Bold)
                    uiState.selectedTable?.let { t ->
                        Text(
                            "Mesa ${t.displayName} seleccionada",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f)
                        )
                    }
                }
            },
            navigationIcon = {
                IconButton(onClick = onNavigateBack) {
                    Icon(Icons.Default.ArrowBack, "Volver")
                }
            },
            actions = {
                if (uiState.isLoading || uiState.isValidatingStock) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                    Spacer(Modifier.width(12.dp))
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.primary,
                titleContentColor = MaterialTheme.colorScheme.onPrimary,
                navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
            )
        )

        // Error banner
        uiState.error?.let { msg ->
            Surface(color = MaterialTheme.colorScheme.errorContainer, modifier = Modifier.fillMaxWidth()) {
                Text(
                    msg,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        }

        // Fila de mesas
        TableChipsRow(
            tables = uiState.availableTables,
            selectedTable = uiState.selectedTable,
            onSelect = viewModel::selectTable
        )

        // Contenido principal
        if (uiState.isLoading && uiState.products.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            Row(modifier = Modifier.fillMaxSize()) {
                // Izquierda: Productos
                ProductPanel(
                    categories = uiState.categories,
                    selectedCategoryId = uiState.selectedCategoryId,
                    products = uiState.filteredProducts,
                    onCategorySelect = viewModel::selectCategory,
                    onProductClick = viewModel::addProductToCart,
                    modifier = Modifier.weight(0.58f).fillMaxHeight()
                )

                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .fillMaxHeight()
                        .background(MaterialTheme.colorScheme.outlineVariant)
                )

                // Derecha: Carrito
                CartPanel(
                    cartItems = uiState.cartItems,
                    total = uiState.calculatedTotal,
                    customerPhone = uiState.customerPhone,
                    fidelityCustomer = uiState.fidelityCustomer,
                    fidelityPointsToEarn = uiState.fidelityPointsToEarn,
                    isFidelityLoading = uiState.isFidelityLoading,
                    onPhoneChange = viewModel::updatePhone,
                    onUpdateQuantity = viewModel::updateCartItemQuantity,
                    onRemoveItem = viewModel::removeCartItem,
                    onCreateOrder = viewModel::createOrder,
                    isCreateOrderEnabled = uiState.selectedTable != null && uiState.cartItems.isNotEmpty(),
                    modifier = Modifier.weight(0.42f).fillMaxHeight()
                )
            }
        }
    }

    // Diálogo de stock insuficiente
    if (uiState.showStockWarnings && uiState.stockAvailabilityReport != null) {
        StockWarningsDialog(
            stockReport = uiState.stockAvailabilityReport!!,
            onProceedAnyway = viewModel::proceedWithWarnings,
            onDismiss = viewModel::hideStockWarnings
        )
    }
}

// ─── Fila compacta de mesas ──────────────────────────────────────────────────
@Composable
private fun TableChipsRow(
    tables: List<Table>,
    selectedTable: Table?,
    onSelect: (Table) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.06f))
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                Icons.Default.TableRestaurant, null,
                modifier = Modifier.size(15.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Text(
                text = if (selectedTable != null) "Mesa ${selectedTable.displayName} ✓"
                       else "Seleccionar mesa",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary
            )
        }
        Spacer(Modifier.height(6.dp))
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(horizontal = 2.dp)
        ) {
            items(tables) { table ->
                val isSelected = selectedTable?.id == table.id
                val tColor = when (table.status) {
                    TableStatus.OCCUPIED -> MaterialTheme.colorScheme.error
                    TableStatus.RESERVED -> MaterialTheme.colorScheme.tertiary
                    else -> MaterialTheme.colorScheme.primary
                }
                Surface(
                    onClick = { onSelect(table) },
                    modifier = Modifier.size(44.dp),
                    shape = RoundedCornerShape(10.dp),
                    color = if (isSelected) tColor.copy(alpha = 0.18f) else MaterialTheme.colorScheme.surface,
                    border = BorderStroke(
                        if (isSelected) 2.dp else 1.dp,
                        tColor.copy(if (isSelected) 1f else 0.35f)
                    )
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            table.number.toString(),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Normal,
                            color = tColor
                        )
                    }
                }
            }
        }
    }
}

// ─── Panel izquierdo: categorías + productos ─────────────────────────────────
@Composable
private fun ProductPanel(
    categories: List<Category>,
    selectedCategoryId: Long?,
    products: List<Product>,
    onCategorySelect: (Long?) -> Unit,
    onProductClick: (Product) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        // Barra de categorías
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
                .padding(horizontal = 10.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                CategoryPill(
                    label = "🍽️  Todo",
                    color = MaterialTheme.colorScheme.primary,
                    selected = selectedCategoryId == null,
                    onClick = { onCategorySelect(null) }
                )
            }
            items(categories) { cat ->
                CategoryPill(
                    label = "${cat.icon ?: ""}  ${cat.name}",
                    color = parseHexColor(cat.color),
                    selected = selectedCategoryId == cat.id,
                    onClick = { onCategorySelect(cat.id) }
                )
            }
        }

        Divider(color = MaterialTheme.colorScheme.outlineVariant)

        // Grid de productos
        if (products.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    "Sin productos",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f)
                )
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 118.dp),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(products) { product ->
                    ProductCard(product = product, onClick = { onProductClick(product) })
                }
            }
        }
    }
}

// ─── Chip / pill de categoría ─────────────────────────────────────────────────
@Composable
private fun CategoryPill(label: String, color: Color, selected: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        color = if (selected) color else color.copy(alpha = 0.10f),
        border = BorderStroke(1.dp, color.copy(alpha = if (selected) 1f else 0.25f))
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            color = if (selected) Color.White else color
        )
    }
}

// ─── Tarjeta de producto ──────────────────────────────────────────────────────
@Composable
private fun ProductCard(product: Product, onClick: () -> Unit) {
    val fmt = NumberFormat.getCurrencyInstance(Locale("es", "CO"))
    val isOut = product.stockQuantity?.let { it == 0 } ?: false
    val color = catColor(product.categoryId)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(0.82f)
            .clickable(enabled = !isOut) { onClick() },
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(if (isOut) 0.dp else 3.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isOut) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                            else color.copy(alpha = 0.07f)
        )
    ) {
        Column(Modifier.fillMaxSize()) {
            // Barra de color
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(5.dp)
                    .background(if (isOut) MaterialTheme.colorScheme.outline.copy(0.2f) else color.copy(0.75f))
            )
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = product.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    color = if (isOut) MaterialTheme.colorScheme.onSurface.copy(0.35f)
                            else MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.weight(1f))
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    if (isOut) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.error.copy(0.12f)
                        ) {
                            Text(
                                "Agotado",
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.error,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    } else {
                        Text(
                            fmt.format(product.price),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = color
                        )
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = color.copy(alpha = 0.15f)
                        ) {
                            Icon(
                                Icons.Default.Add,
                                contentDescription = "Agregar",
                                modifier = Modifier.size(26.dp).padding(4.dp),
                                tint = color
                            )
                        }
                    }
                }
            }
        }
    }
}

// ─── Panel derecho: carrito ───────────────────────────────────────────────────
@Composable
private fun CartPanel(
    cartItems: List<CartItem>,
    total: BigDecimal,
    customerPhone: String,
    fidelityCustomer: FidelityCustomer?,
    fidelityPointsToEarn: Int,
    isFidelityLoading: Boolean,
    onPhoneChange: (String) -> Unit,
    onUpdateQuantity: (Long, Int) -> Unit,
    onRemoveItem: (Long) -> Unit,
    onCreateOrder: () -> Unit,
    isCreateOrderEnabled: Boolean,
    modifier: Modifier = Modifier
) {
    val fmt = NumberFormat.getCurrencyInstance(Locale("es", "CO"))

    Column(
        modifier = modifier
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        // Campo teléfono
        OutlinedTextField(
            value = customerPhone,
            onValueChange = onPhoneChange,
            label = { Text("Teléfono") },
            leadingIcon = {
                Icon(Icons.Default.Phone, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
            },
            trailingIcon = {
                if (isFidelityLoading) CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            textStyle = MaterialTheme.typography.bodyMedium,
            shape = RoundedCornerShape(10.dp)
        )

        // Card de fidelidad
        AnimatedVisibility(
            visible = customerPhone.length >= 7 && !isFidelityLoading,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Card(
                modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                shape = RoundedCornerShape(10.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (fidelityCustomer != null)
                        MaterialTheme.colorScheme.primary.copy(0.10f)
                    else
                        MaterialTheme.colorScheme.secondaryContainer.copy(0.45f)
                )
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            if (fidelityCustomer != null) Icons.Default.Star else Icons.Default.PersonAdd,
                            null,
                            Modifier.size(16.dp),
                            tint = if (fidelityCustomer != null) MaterialTheme.colorScheme.primary
                                   else MaterialTheme.colorScheme.secondary
                        )
                        Column {
                            Text(
                                fidelityCustomer?.displayName ?: "Cliente nuevo",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                                color = if (fidelityCustomer != null) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.secondary
                            )
                            if (fidelityCustomer != null) {
                                Text(
                                    "${fidelityCustomer.totalPoints} pts acumulados",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(0.55f)
                                )
                            }
                        }
                    }
                    if (fidelityPointsToEarn > 0) {
                        Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.primary) {
                            Text(
                                "+$fidelityPointsToEarn pts",
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onPrimary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        Divider(modifier = Modifier.padding(vertical = 8.dp))

        // Items
        if (cartItems.isEmpty()) {
            Box(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Default.ShoppingCart, null,
                        Modifier.size(44.dp),
                        tint = MaterialTheme.colorScheme.onSurface.copy(0.18f)
                    )
                    Text(
                        "Toca un producto\npara agregar",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(0.38f),
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(1.dp)
            ) {
                items(cartItems) { item ->
                    CartItemRow(item = item, onUpdateQuantity = onUpdateQuantity, onRemove = onRemoveItem)
                    Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(0.4f))
                }
            }
        }

        // Total + botón
        Divider(modifier = Modifier.padding(top = 8.dp))
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("Total", style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(0.55f))
                Text(
                    fmt.format(total),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            if (cartItems.isNotEmpty()) {
                Text(
                    "${cartItems.sumOf { it.quantity }} items",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(0.5f)
                )
            }
        }

        Button(
            onClick = onCreateOrder,
            enabled = isCreateOrderEnabled,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.Default.CheckCircle, null, Modifier.size(22.dp))
            Spacer(Modifier.width(8.dp))
            Text("Crear Orden", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
    }
}

// ─── Fila de item en el carrito ───────────────────────────────────────────────
@Composable
private fun CartItemRow(
    item: CartItem,
    onUpdateQuantity: (Long, Int) -> Unit,
    onRemove: (Long) -> Unit
) {
    val fmt = NumberFormat.getCurrencyInstance(Locale("es", "CO"))
    val color = catColor(item.product.categoryId)

    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // Barra lateral de color
        Box(
            modifier = Modifier
                .width(3.dp)
                .height(34.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(color.copy(alpha = 0.7f))
        )

        Column(modifier = Modifier.weight(1f)) {
            Text(
                item.product.name,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                fmt.format(item.unitPrice),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(0.5f)
            )
        }

        // Controles +/-
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            IconButton(
                onClick = { if (item.quantity == 1) onRemove(item.product.id) else onUpdateQuantity(item.product.id, item.quantity - 1) },
                modifier = Modifier.size(28.dp)
            ) {
                Icon(
                    if (item.quantity == 1) Icons.Default.Delete else Icons.Default.Remove, null,
                    Modifier.size(15.dp),
                    tint = if (item.quantity == 1) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                )
            }
            Text(
                item.quantity.toString(),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.widthIn(min = 18.dp),
                textAlign = TextAlign.Center
            )
            IconButton(
                onClick = { onUpdateQuantity(item.product.id, item.quantity + 1) },
                modifier = Modifier.size(28.dp)
            ) {
                Icon(Icons.Default.Add, null, Modifier.size(15.dp))
            }
        }

        // Subtotal
        Text(
            fmt.format(item.subtotal),
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Bold,
            color = color,
            modifier = Modifier.widthIn(min = 52.dp),
            textAlign = TextAlign.End
        )
    }
}
