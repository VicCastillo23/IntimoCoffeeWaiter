package com.intimocoffee.waiter.feature.orders.presentation.create

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.runtime.collectAsState
import com.intimocoffee.waiter.R
import com.intimocoffee.waiter.feature.fidelity.domain.model.FidelityCustomer
import com.intimocoffee.waiter.feature.orders.domain.model.CartItem
import com.intimocoffee.waiter.feature.products.domain.model.Category
import com.intimocoffee.waiter.feature.products.domain.model.Product
import com.intimocoffee.waiter.feature.tables.domain.model.Table
import com.intimocoffee.waiter.feature.tables.domain.model.TableStatus
import com.intimocoffee.waiter.ui.theme.IntimoCoffeeAppTheme
import java.math.BigDecimal
import java.text.NumberFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateOrderScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: CreateOrderViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    
    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            // Show error snackbar or handle error
        }
    }
    
    // Navigate back when order is successfully created
    LaunchedEffect(uiState.orderCreated) {
        if (uiState.orderCreated) {
            onNavigateBack()
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
    ) {
        // Top App Bar
        TopAppBar(
            title = { Text("Nueva Orden") },
            navigationIcon = {
                IconButton(onClick = onNavigateBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Volver")
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.primary,
                titleContentColor = MaterialTheme.colorScheme.onPrimary,
                navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
            )
        )

        if (uiState.isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Left Panel - Table Selection & Product Selection
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Table Selection Section
                        TableSelectionSection(
                            selectedTable = uiState.selectedTable,
                            availableTables = uiState.availableTables,
                            onSelectTable = viewModel::selectTable,
                            onShowTableSelector = {} // Not needed anymore
                        )

                        Divider()

                        // Product Selection Section
                        ProductSelectionSection(
                            products = uiState.filteredProducts,
                            categories = emptyList(), // Temporarily empty
                            selectedCategoryId = uiState.selectedCategoryId,
                            onCategorySelect = viewModel::selectCategory,
                            onProductClick = viewModel::addProductToCart
                        )
                    }
                }

                // Right Panel - Cart & Checkout
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                    )
                ) {
                    CartSection(
                        cartItems = uiState.cartItems,
                        subtotal = uiState.calculatedSubtotal,
                        tax = uiState.calculatedTax,
                        total = uiState.calculatedTotal,
                        customerPhone = uiState.customerPhone,
                        customerName = uiState.customerName,
                        fidelityCustomer = uiState.fidelityCustomer,
                        fidelityPointsToEarn = uiState.fidelityPointsToEarn,
                        isFidelityLoading = uiState.isFidelityLoading,
                        onPhoneChange = viewModel::updatePhone,
                        onCustomerNameChange = viewModel::updateCustomerName,
                        onUpdateQuantity = viewModel::updateCartItemQuantity,
                        onUpdateNotes = viewModel::updateCartItemNotes,
                        onRemoveItem = viewModel::removeCartItem,
                        onCreateOrder = viewModel::createOrder,
                        isCreateOrderEnabled = uiState.selectedTable != null && uiState.cartItems.isNotEmpty()
                    )
                }
            }
        }
    }

    // Dialogs removed temporarily - table selection is done inline

}

@Composable
private fun TableSelectionSection(
    selectedTable: Table?,
    availableTables: List<Table>,
    onSelectTable: (Table) -> Unit,
    onShowTableSelector: () -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Mesa",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )

        // Show selected table info if any
        selectedTable?.let { table ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                ),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    MaterialTheme.colorScheme.primary
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp)
                ) {
                    Text(
                        text = "Mesa seleccionada: ${table.displayName}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    
                    if (table.status == TableStatus.OCCUPIED) {
                        Text(
                            text = "⚠️ Mesa ocupada - Creando orden adicional",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }

        // Show all available tables in a grid
        if (availableTables.isNotEmpty()) {
            Text(
                text = "Mesas disponibles: ${availableTables.size}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )

            LazyVerticalGrid(
                columns = GridCells.Fixed(4), // 4 columns to give more space per card
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.height(160.dp) // Increased height for more info
            ) {
                items(availableTables) { table ->
                    EnhancedTableCard(
                        table = table,
                        isSelected = selectedTable?.id == table.id,
                        onClick = { onSelectTable(table) },
                        modifier = Modifier
                            .aspectRatio(1f)
                            .fillMaxWidth() // Use available width
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProductSelectionSection(
    products: List<Product>,
    categories: List<Category>,
    selectedCategoryId: Long?,
    onCategorySelect: (Long?) -> Unit,
    onProductClick: (Product) -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Productos",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )

        // Category chips bar — tamaño grande para uso táctil
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(horizontal = 4.dp)
        ) {
            item {
                FilterChip(
                    onClick = { onCategorySelect(null) },
                    label = {
                        Text(
                            "🍽️ Todos",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = if (selectedCategoryId == null) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    selected = selectedCategoryId == null,
                    modifier = Modifier
                        .height(44.dp)
                        .padding(horizontal = 2.dp)
                )
            }

            items(categories) { category ->
                FilterChip(
                    onClick = { onCategorySelect(category.id) },
                    label = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            category.icon?.let {
                                Text(text = it, style = MaterialTheme.typography.bodyLarge)
                            }
                            Text(
                                text = category.name,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = if (selectedCategoryId == category.id) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    },
                    selected = selectedCategoryId == category.id,
                    modifier = Modifier
                        .height(44.dp)
                        .padding(horizontal = 2.dp)
                )
            }
        }

        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 120.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(products) { product ->
                ProductCard(
                    product = product,
                    onClick = { onProductClick(product) }
                )
            }
        }
    }
}

@Composable
private fun CartSection(
    cartItems: List<CartItem>,
    subtotal: BigDecimal,
    tax: BigDecimal,
    total: BigDecimal,
    customerPhone: String,
    customerName: String,
    fidelityCustomer: FidelityCustomer?,
    fidelityPointsToEarn: Int,
    isFidelityLoading: Boolean,
    onPhoneChange: (String) -> Unit,
    onCustomerNameChange: (String) -> Unit,
    onUpdateQuantity: (Long, Int) -> Unit,
    onUpdateNotes: (Long, String) -> Unit,
    onRemoveItem: (Long) -> Unit,
    onCreateOrder: () -> Unit,
    isCreateOrderEnabled: Boolean
) {
    val currencyFormat = NumberFormat.getCurrencyInstance(Locale("es", "CO"))

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Resumen de Orden",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )

        Spacer(modifier = Modifier.height(12.dp))

        // ── Fidelidad ────────────────────────────────────────────
        OutlinedTextField(
            value = customerPhone,
            onValueChange = onPhoneChange,
            label = { Text("Tel. cliente (puntos fidelidad)") },
            leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null) },
            trailingIcon = {
                if (isFidelityLoading) CircularProgressIndicator(modifier = Modifier.size(20.dp))
            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        if (customerPhone.length >= 7) {
            Spacer(modifier = Modifier.height(6.dp))
            if (fidelityCustomer != null) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
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
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                Icons.Default.Star,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Column {
                                Text(
                                    text = fidelityCustomer.displayName,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "${fidelityCustomer.totalPoints} pts actuales",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                )
                            }
                        }
                        if (fidelityPointsToEarn > 0) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.primary
                            ) {
                                Text(
                                    text = "+$fidelityPointsToEarn pts",
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            } else if (!isFidelityLoading) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
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
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                Icons.Default.PersonAdd,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "Cliente nuevo",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                        if (fidelityPointsToEarn > 0) {
                            Text(
                                text = "+$fidelityPointsToEarn pts a ganar",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.secondary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
        // ────────────────────────────────────────────────────────

        Spacer(modifier = Modifier.height(12.dp))

        if (cartItems.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ShoppingCart,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                    )
                    Text(
                        text = "No hay productos en la orden",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(cartItems) { cartItem ->
                    CartItemCard(
                        cartItem = cartItem,
                        onUpdateQuantity = onUpdateQuantity,
                        onUpdateNotes = onUpdateNotes,
                        onRemove = onRemoveItem
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Order Summary
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Subtotal:")
                        Text(currencyFormat.format(subtotal))
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("IVA (10%):")
                        Text(currencyFormat.format(tax))
                    }
                    Divider()
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
                            text = currencyFormat.format(total),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = onCreateOrder,
                enabled = isCreateOrderEnabled,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Crear Orden")
            }
        }
    }
}

@Composable
private fun TableCard(
    table: Table,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        border = if (isSelected) {
            androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
        } else null
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = table.number.toString(),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun EnhancedTableCard(
    table: Table,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
            } else when (table.status) {
                TableStatus.OCCUPIED -> MaterialTheme.colorScheme.error.copy(alpha = 0.1f)
                TableStatus.RESERVED -> MaterialTheme.colorScheme.tertiary.copy(alpha = 0.1f)
                else -> MaterialTheme.colorScheme.surface
            }
        ),
        border = if (isSelected) {
            BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
        } else {
            null
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Table number - main focus
            Text(
                text = table.number.toString(),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                color = if (isSelected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurface
                }
            )
            
            // Zone/description if available
            if (table.zone.isNotEmpty()) {
                Text(
                    text = table.zone,
                    style = MaterialTheme.typography.labelMedium,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }
            
            // Status indicator
            Text(
                text = when (table.status) {
                    TableStatus.OCCUPIED -> "Ocupada"
                    TableStatus.RESERVED -> "Reservada"
                    else -> "Disponible"
                },
                style = MaterialTheme.typography.labelSmall,
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Medium,
                color = when (table.status) {
                    TableStatus.OCCUPIED -> MaterialTheme.colorScheme.error
                    TableStatus.RESERVED -> MaterialTheme.colorScheme.tertiary
                    else -> MaterialTheme.colorScheme.primary
                }
            )
        }
    }
}

@Composable
private fun ProductCard(
    product: Product,
    onClick: () -> Unit
) {
    val currencyFormat = NumberFormat.getCurrencyInstance(Locale("es", "CO"))
    val stockQuantity = product.stockQuantity ?: 0
    val isLowStock = stockQuantity <= (product.minStockLevel ?: 5)
    val isOutOfStock = stockQuantity == 0
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(100.dp) // Increased height to accommodate stock info
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = when {
                isOutOfStock -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
                isLowStock -> MaterialTheme.colorScheme.tertiary.copy(alpha = 0.2f)
                else -> MaterialTheme.colorScheme.surface
            }
        ),
        border = when {
            isOutOfStock -> BorderStroke(1.dp, MaterialTheme.colorScheme.error)
            isLowStock -> BorderStroke(1.dp, MaterialTheme.colorScheme.tertiary)
            else -> null
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = product.name,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Text(
                    text = currencyFormat.format(product.price),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                
                Text(
                    text = when {
                        isOutOfStock -> "Sin stock"
                        isLowStock -> "Stock: $stockQuantity ⚠️"
                        else -> "Stock: $stockQuantity"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = when {
                        isOutOfStock -> MaterialTheme.colorScheme.error
                        isLowStock -> MaterialTheme.colorScheme.tertiary
                        else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    },
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CartItemCard(
    cartItem: CartItem,
    onUpdateQuantity: (Long, Int) -> Unit,
    onUpdateNotes: (Long, String) -> Unit,
    onRemove: (Long) -> Unit
) {
    val currencyFormat = NumberFormat.getCurrencyInstance(Locale("es", "CO"))
    var showNotesDialog by remember { mutableStateOf(false) }
    var notes by remember { mutableStateOf(cartItem.notes ?: "") }
    
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = cartItem.product.name,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = currencyFormat.format(cartItem.unitPrice),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                    if (cartItem.notes?.isNotBlank() == true) {
                        Text(
                            text = "Nota: ${cartItem.notes}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
                
                IconButton(
                    onClick = { onRemove(cartItem.product.id) },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Eliminar",
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Quantity controls
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    IconButton(
                        onClick = { onUpdateQuantity(cartItem.product.id, cartItem.quantity - 1) },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Remove,
                            contentDescription = "Reducir cantidad",
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    
                    Text(
                        text = cartItem.quantity.toString(),
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )
                    
                    IconButton(
                        onClick = { onUpdateQuantity(cartItem.product.id, cartItem.quantity + 1) },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Aumentar cantidad",
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TextButton(
                        onClick = { showNotesDialog = true }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Nota", style = MaterialTheme.typography.bodySmall)
                    }

                    Text(
                        text = currencyFormat.format(cartItem.subtotal),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
    
    if (showNotesDialog) {
        AlertDialog(
            onDismissRequest = { showNotesDialog = false },
            title = { Text("Agregar Nota") },
            text = {
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Nota especial") },
                    placeholder = { Text("Ej: Sin cebolla, punto medio...") },
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onUpdateNotes(cartItem.product.id, notes)
                        showNotesDialog = false
                    }
                ) {
                    Text("Guardar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showNotesDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TableSelectorDialog(
    tables: List<Table>,
    onTableSelected: (Table) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Seleccionar Mesa") },
        text = {
            LazyVerticalGrid(
                columns = GridCells.Fixed(4),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.height(200.dp)
            ) {
                items(tables) { table ->
                    TableCard(
                        table = table,
                        isSelected = false,
                        onClick = { onTableSelected(table) },
                        modifier = Modifier.aspectRatio(1f)
                    )
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProductSearchDialog(
    products: List<Product>,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onProductSelected: (Product) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Seleccionar Producto") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = onSearchQueryChange,
                    label = { Text("Buscar") },
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = null)
                    },
                    modifier = Modifier.fillMaxWidth()
                )
                
                LazyColumn(
                    modifier = Modifier.height(300.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(products) { product ->
                        ProductCard(
                            product = product,
                            onClick = { onProductSelected(product) }
                        )
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}

@Preview(showBackground = true, widthDp = 1200, heightDp = 800)
@Composable
private fun CreateOrderScreenPreview() {
    IntimoCoffeeAppTheme {
        CreateOrderScreen(
            onNavigateBack = { }
        )
    }
}