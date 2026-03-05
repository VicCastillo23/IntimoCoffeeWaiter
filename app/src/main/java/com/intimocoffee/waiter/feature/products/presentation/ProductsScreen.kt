package com.intimocoffee.waiter.feature.products.presentation

import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.runtime.collectAsState
import com.intimocoffee.waiter.feature.products.domain.model.Category
import com.intimocoffee.waiter.feature.products.domain.model.Product
import com.intimocoffee.waiter.ui.theme.IntimoCoffeeAppTheme
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.text.NumberFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductsScreen(
    viewModel: ProductsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        viewModel.loadProducts()
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
            Text(
                text = "Gestión de Productos",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold
            )
            
            FilledTonalButton(
                onClick = { viewModel.showAddProductDialog() }
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("Nuevo Producto")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Category Filter Chips
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                FilterChip(
                    onClick = { viewModel.filterByCategory(null) },
                    label = { Text("Todas") },
                    selected = uiState.selectedCategoryId == null
                )
            }
            
            items(uiState.categories) { category ->
                FilterChip(
                    onClick = { viewModel.filterByCategory(category.id) },
                    label = { 
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            category.icon?.let { 
                                Text(it, style = MaterialTheme.typography.labelSmall) 
                            }
                            Text(category.name)
                        }
                    },
                    selected = uiState.selectedCategoryId == category.id,
                    leadingIcon = if (uiState.selectedCategoryId == category.id) {
                        { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                    } else null
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Products Grid
        when {
            uiState.isLoading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            
            uiState.filteredProducts.isEmpty() -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Default.Inventory,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = if (uiState.selectedCategoryId != null) {
                                "No se encontraron productos en esta categoría"
                            } else {
                                "No hay productos registrados"
                            },
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            
            else -> {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 220.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(uiState.filteredProducts) { product ->
                        ProductCard(
                            product = product,
                            category = viewModel.getCategoryById(product.categoryId),
                            onEditClick = { viewModel.showEditProductDialog(product) },
                            onDeactivateClick = {
                                scope.launch {
                                    viewModel.deactivateProduct(product.id)
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    // Add/Edit Product Dialog
    if (uiState.showAddProductDialog) {
        AddProductDialog(
            editingProduct = uiState.editingProduct,
            categories = uiState.categories,
            onDismiss = { viewModel.hideAddProductDialog() },
            onSave = { name, description, price, categoryId, stockQuantity, minStockLevel, barcode ->
                scope.launch {
                    val editingProduct = uiState.editingProduct
                    if (editingProduct != null) {
                        val updatedProduct = editingProduct.copy(
                            name = name,
                            description = description.takeIf { it.isNotBlank() },
                            price = price,
                            categoryId = categoryId,
                            stockQuantity = stockQuantity,
                            minStockLevel = minStockLevel,
                            barcode = barcode?.takeIf { it.isNotBlank() }
                        )
                        viewModel.updateProduct(updatedProduct)
                    } else {
                        viewModel.createProduct(name, description, price, categoryId, stockQuantity, minStockLevel, barcode)
                    }
                }
            }
        )
    }

    // Error handling
    uiState.error?.let { error ->
        LaunchedEffect(error) {
            // Handle error (e.g., show snackbar)
            viewModel.clearError()
        }
    }
}

@Composable
private fun ProductCard(
    product: Product,
    category: Category?,
    onEditClick: () -> Unit,
    onDeactivateClick: () -> Unit
) {
    val currencyFormat = NumberFormat.getCurrencyInstance(Locale("es", "CO"))
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(130.dp), // Perfect height to fit content without excess spacing
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp) // Minimal padding for perfect fit
        ) {
            // Top section: Header with actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = product.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1, // Reduced to 1 line
                        overflow = TextOverflow.Ellipsis
                    )
                    product.description?.let { description ->
                        Spacer(modifier = Modifier.height(2.dp)) // Reduced spacing
                        Text(
                            text = description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1, // Reduced to 1 line
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Row {
                    IconButton(
                        onClick = onEditClick,
                        modifier = Modifier.size(28.dp) // Smaller buttons
                    ) {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = "Editar",
                            modifier = Modifier.size(14.dp)
                        )
                    }
                    IconButton(
                        onClick = onDeactivateClick,
                        modifier = Modifier.size(28.dp) // Smaller buttons
                    ) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Eliminar",
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp)) // Minimal spacing

            // Middle section: Category chip
            category?.let { cat ->
                Surface(
                    modifier = Modifier.wrapContentWidth(),
                    color = Color(android.graphics.Color.parseColor(cat.color)).copy(alpha = 0.1f),
                    shape = RoundedCornerShape(6.dp) // Smaller radius
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp), // Minimal padding
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        cat.icon?.let {
                            Text(
                                text = it,
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                        Text(
                            text = cat.name,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Medium,
                            color = Color(android.graphics.Color.parseColor(cat.color))
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp)) // Minimal fixed spacing

            // Bottom section: Price, stock, and barcode
            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp) // Minimal spacing
            ) {
                // Price and stock info
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    Text(
                        text = currencyFormat.format(product.price),
                        style = MaterialTheme.typography.titleMedium, // Smaller price text
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    product.stockQuantity?.let { stock ->
                        Column(
                            horizontalAlignment = Alignment.End
                        ) {
                            Text(
                                text = "Stock: $stock",
                                style = MaterialTheme.typography.labelMedium, // Slightly larger than labelSmall
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            
                            if (product.isLowStock) {
                                Text(
                                    text = "Stock bajo",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.error,
                                    fontWeight = FontWeight.Medium
                                )
                            } else if (!product.isInStock) {
                                Text(
                                    text = "Agotado",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.error,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }

                // Barcode if exists (always at bottom)
                product.barcode?.let { barcode ->
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), // More subtle
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp), // Reduced padding
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                Icons.Default.QrCode,
                                contentDescription = null,
                                modifier = Modifier.size(12.dp), // Smaller icon
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = barcode,
                                style = MaterialTheme.typography.labelSmall, // Smaller text
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddProductDialog(
    editingProduct: Product?,
    categories: List<Category>,
    onDismiss: () -> Unit,
    onSave: (name: String, description: String, price: BigDecimal, categoryId: Long, stockQuantity: Int?, minStockLevel: Int?, barcode: String?) -> Unit
) {
    var name by remember { mutableStateOf(editingProduct?.name ?: "") }
    var description by remember { mutableStateOf(editingProduct?.description ?: "") }
    var price by remember { mutableStateOf(editingProduct?.price?.toString() ?: "") }
    var selectedCategoryId by remember { mutableStateOf(editingProduct?.categoryId ?: categories.firstOrNull()?.id ?: 1L) }
    var stockQuantity by remember { mutableStateOf(editingProduct?.stockQuantity?.toString() ?: "") }
    var minStockLevel by remember { mutableStateOf(editingProduct?.minStockLevel?.toString() ?: "") }
    var barcode by remember { mutableStateOf(editingProduct?.barcode ?: "") }
    var expandedCategory by remember { mutableStateOf(false) }

    val isEditing = editingProduct != null
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { 
            Text(if (isEditing) "Editar Producto" else "Nuevo Producto") 
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nombre *") },
                    placeholder = { Text("ej: Café Americano") },
                    modifier = Modifier.fillMaxWidth(),
                    isError = name.isBlank()
                )

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Descripción") },
                    placeholder = { Text("Descripción del producto...") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = price,
                        onValueChange = { price = it },
                        label = { Text("Precio *") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f),
                        isError = price.toBigDecimalOrNull()?.let { it <= BigDecimal.ZERO } ?: true,
                        leadingIcon = {
                            Text(
                                text = "$",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    )

                    ExposedDropdownMenuBox(
                        expanded = expandedCategory,
                        onExpandedChange = { expandedCategory = !expandedCategory },
                        modifier = Modifier.weight(1f)
                    ) {
                        OutlinedTextField(
                            value = categories.find { it.id == selectedCategoryId }?.name ?: "",
                            onValueChange = { },
                            readOnly = true,
                            label = { Text("Categoría *") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedCategory) },
                            modifier = Modifier.menuAnchor()
                        )
                        
                        ExposedDropdownMenu(
                            expanded = expandedCategory,
                            onDismissRequest = { expandedCategory = false }
                        ) {
                            categories.forEach { category ->
                                DropdownMenuItem(
                                    text = { 
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            category.icon?.let { 
                                                Text(it, style = MaterialTheme.typography.bodyMedium) 
                                            }
                                            Text(category.name)
                                        }
                                    },
                                    onClick = {
                                        selectedCategoryId = category.id
                                        expandedCategory = false
                                    }
                                )
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = stockQuantity,
                        onValueChange = { stockQuantity = it },
                        label = { Text("Stock inicial") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        isError = stockQuantity.isNotBlank() && (stockQuantity.toIntOrNull()?.let { it < 0 } ?: true)
                    )

                    OutlinedTextField(
                        value = minStockLevel,
                        onValueChange = { minStockLevel = it },
                        label = { Text("Stock mínimo") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        isError = minStockLevel.isNotBlank() && (minStockLevel.toIntOrNull()?.let { it < 0 } ?: true)
                    )
                }

                OutlinedTextField(
                    value = barcode,
                    onValueChange = { barcode = it },
                    label = { Text("Código de barras") },
                    placeholder = { Text("Opcional") },
                    modifier = Modifier.fillMaxWidth(),
                    leadingIcon = {
                        Icon(
                            Icons.Default.QrCode,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val productPrice = price.toBigDecimalOrNull()
                    val stock = if (stockQuantity.isBlank()) null else stockQuantity.toIntOrNull()
                    val minStock = if (minStockLevel.isBlank()) null else minStockLevel.toIntOrNull()
                    
                    if (name.isNotBlank() && productPrice != null && productPrice > BigDecimal.ZERO) {
                        onSave(name, description, productPrice, selectedCategoryId, stock, minStock, barcode)
                    }
                },
                enabled = name.isNotBlank() && 
                         price.toBigDecimalOrNull()?.let { it > BigDecimal.ZERO } == true &&
                         (stockQuantity.isBlank() || stockQuantity.toIntOrNull()?.let { it >= 0 } == true) &&
                         (minStockLevel.isBlank() || minStockLevel.toIntOrNull()?.let { it >= 0 } == true)
            ) {
                Text("Guardar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}

@Preview(showBackground = true)
@Composable
private fun ProductsScreenPreview() {
    IntimoCoffeeAppTheme {
        ProductsScreen()
    }
}