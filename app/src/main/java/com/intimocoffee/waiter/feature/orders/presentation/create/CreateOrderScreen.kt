package com.intimocoffee.waiter.feature.orders.presentation.create

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
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
@OptIn(ExperimentalMaterial3Api::class, ExperimentalComposeUiApi::class)
@Composable
fun CreateOrderScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: CreateOrderViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val keyboardController = LocalSoftwareKeyboardController.current

    LaunchedEffect(uiState.orderCreated) {
        if (uiState.orderCreated) onNavigateBack()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // 1. TopAppBar
        TopAppBar(
            title = {
                Column {
                    Text("Nueva Orden", fontWeight = FontWeight.Bold)
                    uiState.selectedTable?.let { t ->
                        Text(
                            "Mesa ${t.displayName} seleccionada",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.80f)
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
                        modifier = Modifier
                            .size(22.dp)
                            .padding(end = 2.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                    Spacer(Modifier.width(10.dp))
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.primary,
                titleContentColor = MaterialTheme.colorScheme.onPrimary,
                navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
            )
        )

        // 2. Error banner
        uiState.error?.let { msg ->
            Surface(
                color = MaterialTheme.colorScheme.errorContainer,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    msg,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        }

        // 3. Selector de mesa
        TableChipsRow(
            tables = uiState.availableTables,
            selectedTable = uiState.selectedTable,
            onSelect = viewModel::selectTable
        )

        // 4. Número de cliente + Buscar
        PhoneSearchRow(
            phone = uiState.customerPhone,
            isLoading = uiState.isFidelityLoading,
            onPhoneChange = viewModel::updatePhone,
            onSearch = {
                keyboardController?.hide()
                viewModel.triggerFidelitySearch()
            }
        )

        // 5. Card de fidelidad (animada)
        AnimatedVisibility(
            visible = uiState.customerPhone.length >= 7 && !uiState.isFidelityLoading,
            enter = expandVertically(animationSpec = tween(220)) + fadeIn(tween(220)),
            exit = shrinkVertically(animationSpec = tween(180)) + fadeOut(tween(180))
        ) {
            FidelityInfoCard(
                fidelityCustomer = uiState.fidelityCustomer,
                pointsToEarn = uiState.fidelityPointsToEarn,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 4.dp)
            )
        }

        // 6. Chips de categorías
        CategoryChipsRow(
            categories = uiState.categories,
            selectedCategoryId = uiState.selectedCategoryId,
            onSelect = viewModel::selectCategory
        )

        Divider(color = MaterialTheme.colorScheme.outlineVariant)

        // 7. Grid de productos (toma el espacio restante)
        if (uiState.isLoading && uiState.products.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else if (uiState.filteredProducts.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Default.SearchOff, null,
                        Modifier.size(40.dp),
                        tint = MaterialTheme.colorScheme.onSurface.copy(0.20f)
                    )
                    Text(
                        "Sin productos en esta categoría",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(0.35f)
                    )
                }
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 110.dp),
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(uiState.filteredProducts) { product ->
                    ProductCard(
                        product = product,
                        onClick = { viewModel.addProductToCart(product) }
                    )
                }
            }
        }

        // 8. Panel "Resumen de Orden" (parte inferior fija)
        CartSummaryPanel(
            cartItems = uiState.cartItems,
            total = uiState.calculatedTotal,
            isCreateEnabled = uiState.selectedTable != null && uiState.cartItems.isNotEmpty(),
            onUpdateQuantity = viewModel::updateCartItemQuantity,
            onRemoveItem = viewModel::removeCartItem,
            onCreateOrder = viewModel::createOrder
        )
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

// ─── Fila de mesas ───────────────────────────────────────────────────────────
@Composable
private fun TableChipsRow(
    tables: List<Table>,
    selectedTable: Table?,
    onSelect: (Table) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                Icons.Default.TableRestaurant, null,
                modifier = Modifier.size(14.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Text(
                text = if (selectedTable != null) "Mesa ${selectedTable.displayName} ✓"
                       else "Seleccionar mesa:",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary
            )
            Icon(
                Icons.Default.ChevronRight, null,
                Modifier.size(14.dp),
                tint = MaterialTheme.colorScheme.primary.copy(0.45f)
            )
        }
        Spacer(Modifier.height(5.dp))
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
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
                    modifier = Modifier.size(40.dp),
                    shape = RoundedCornerShape(8.dp),
                    color = if (isSelected) tColor.copy(alpha = 0.18f)
                            else MaterialTheme.colorScheme.surfaceVariant,
                    border = BorderStroke(
                        if (isSelected) 2.dp else 1.dp,
                        tColor.copy(if (isSelected) 1f else 0.28f)
                    )
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            table.number.toString(),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium,
                            color = if (isSelected) tColor
                                    else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

// ─── Fila teléfono + Buscar ──────────────────────────────────────────────────
@Composable
private fun PhoneSearchRow(
    phone: String,
    isLoading: Boolean,
    onPhoneChange: (String) -> Unit,
    onSearch: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        OutlinedTextField(
            value = phone,
            onValueChange = onPhoneChange,
            label = { Text("Número Cliente") },
            leadingIcon = {
                Icon(
                    Icons.Default.Phone, null,
                    Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            },
            trailingIcon = {
                if (isLoading) {
                    CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                }
            },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Phone,
                imeAction = ImeAction.Search
            ),
            keyboardActions = KeyboardActions(onSearch = { onSearch() }),
            singleLine = true,
            modifier = Modifier.weight(1f),
            textStyle = MaterialTheme.typography.bodyMedium,
            shape = RoundedCornerShape(10.dp)
        )
        Button(
            onClick = onSearch,
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.height(52.dp),
            contentPadding = PaddingValues(horizontal = 14.dp)
        ) {
            Icon(Icons.Default.Search, null, Modifier.size(18.dp))
            Spacer(Modifier.width(4.dp))
            Text("Buscar", style = MaterialTheme.typography.labelLarge)
        }
    }
}

// ─── Card de fidelidad ────────────────────────────────────────────────────────
@Composable
private fun FidelityInfoCard(
    fidelityCustomer: FidelityCustomer?,
    pointsToEarn: Int,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (fidelityCustomer != null)
                MaterialTheme.colorScheme.primary.copy(0.10f)
            else
                MaterialTheme.colorScheme.secondaryContainer.copy(0.50f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    if (fidelityCustomer != null) Icons.Default.Star else Icons.Default.PersonAdd,
                    null,
                    Modifier.size(18.dp),
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
            if (pointsToEarn > 0) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primary
                ) {
                    Text(
                        "+$pointsToEarn pts",
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onPrimary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

// ─── Chips de categorías ──────────────────────────────────────────────────────
@Composable
private fun CategoryChipsRow(
    categories: List<Category>,
    selectedCategoryId: Long?,
    onSelect: (Long?) -> Unit
) {
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
                onClick = { onSelect(null) }
            )
        }
        items(categories) { cat ->
            CategoryPill(
                label = "${cat.icon ?: ""}  ${cat.name}",
                color = parseHexColor(cat.color),
                selected = selectedCategoryId == cat.id,
                onClick = { onSelect(cat.id) }
            )
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
            .aspectRatio(0.80f)
            .clickable(enabled = !isOut) { onClick() },
        shape = RoundedCornerShape(10.dp),
        elevation = CardDefaults.cardElevation(if (isOut) 0.dp else 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isOut) MaterialTheme.colorScheme.surfaceVariant.copy(0.6f)
                            else MaterialTheme.colorScheme.surface
        )
    ) {
        Column(Modifier.fillMaxSize()) {
            // Barra de color en top
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(5.dp)
                    .background(
                        if (isOut) MaterialTheme.colorScheme.outline.copy(0.18f) else color
                    )
            )
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 8.dp, vertical = 7.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = product.name,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    color = if (isOut) MaterialTheme.colorScheme.onSurface.copy(0.35f)
                            else MaterialTheme.colorScheme.onSurface
                )
                if (isOut) {
                    Text(
                        "Agotado",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error.copy(0.65f),
                        fontWeight = FontWeight.Medium
                    )
                } else {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            fmt.format(product.price),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = color
                        )
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = color.copy(0.12f)
                        ) {
                            Icon(
                                Icons.Default.Add, null,
                                modifier = Modifier
                                    .size(22.dp)
                                    .padding(4.dp),
                                tint = color
                            )
                        }
                    }
                }
            }
        }
    }
}

// ─── Panel "Resumen de Orden" (parte inferior) ────────────────────────────────
@Composable
private fun CartSummaryPanel(
    cartItems: List<CartItem>,
    total: BigDecimal,
    isCreateEnabled: Boolean,
    onUpdateQuantity: (Long, Int) -> Unit,
    onRemoveItem: (Long) -> Unit,
    onCreateOrder: () -> Unit
) {
    val fmt = NumberFormat.getCurrencyInstance(Locale("es", "CO"))
    val hasItems = cartItems.isNotEmpty()
    val totalQty = cartItems.sumOf { it.quantity }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 10.dp,
        tonalElevation = 2.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .animateContentSize(animationSpec = tween(250))
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Default.Receipt, null,
                        Modifier.size(18.dp),
                        tint = if (hasItems) MaterialTheme.colorScheme.primary
                               else MaterialTheme.colorScheme.onSurface.copy(0.35f)
                    )
                    Text(
                        "Resumen de Orden",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (hasItems) MaterialTheme.colorScheme.onSurface
                                else MaterialTheme.colorScheme.onSurface.copy(0.45f)
                    )
                    if (hasItems) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.primary
                        ) {
                            Text(
                                "$totalQty",
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
                if (hasItems) {
                    Text(
                        fmt.format(total),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // Lista de items (solo cuando hay productos)
            if (hasItems) {
                Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(0.4f))
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 180.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    items(cartItems) { item ->
                        CartItemRow(
                            item = item,
                            onUpdateQuantity = onUpdateQuantity,
                            onRemove = onRemoveItem
                        )
                    }
                }
                Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(0.4f))
            }

            // Botón Generar Orden
            Button(
                onClick = onCreateOrder,
                enabled = isCreateEnabled,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp)
                    .height(52.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Icon(Icons.Default.CheckCircle, null, Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    "Generar Orden",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
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
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // Barra lateral de color
        Box(
            modifier = Modifier
                .width(3.dp)
                .height(26.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(color)
        )
        Text(
            item.product.name,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        // Controles +/-
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(
                onClick = {
                    if (item.quantity == 1) onRemove(item.product.id)
                    else onUpdateQuantity(item.product.id, item.quantity - 1)
                },
                modifier = Modifier.size(28.dp)
            ) {
                Icon(
                    if (item.quantity == 1) Icons.Default.DeleteOutline else Icons.Default.Remove,
                    null,
                    Modifier.size(14.dp),
                    tint = if (item.quantity == 1) MaterialTheme.colorScheme.error
                           else MaterialTheme.colorScheme.onSurface.copy(0.65f)
                )
            }
            Text(
                "${item.quantity}",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.widthIn(min = 18.dp),
                textAlign = TextAlign.Center
            )
            IconButton(
                onClick = { onUpdateQuantity(item.product.id, item.quantity + 1) },
                modifier = Modifier.size(28.dp)
            ) {
                Icon(Icons.Default.Add, null, Modifier.size(14.dp), tint = color)
            }
        }
        Text(
            fmt.format(item.subtotal),
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.SemiBold,
            color = color
        )
    }
}
