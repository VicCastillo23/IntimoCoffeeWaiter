package com.intimocoffee.waiter.feature.orders.presentation.create

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.intimocoffee.waiter.core.network.ModifierOptionResponse
import com.intimocoffee.waiter.core.network.RemoteOrderService
import com.intimocoffee.waiter.feature.auth.domain.repository.AuthRepository
import com.intimocoffee.waiter.feature.fidelity.domain.model.FidelityCustomer
import com.intimocoffee.waiter.feature.fidelity.domain.repository.FidelityRepository
import com.intimocoffee.waiter.feature.inventory.domain.service.InventoryService
import com.intimocoffee.waiter.feature.inventory.domain.service.StockAvailabilityReport
import com.intimocoffee.waiter.feature.orders.domain.model.CartItem
import com.intimocoffee.waiter.feature.orders.domain.model.Order
import com.intimocoffee.waiter.feature.orders.domain.model.OrderItem
import com.intimocoffee.waiter.feature.orders.domain.model.OrderStatus
import com.intimocoffee.waiter.feature.orders.domain.repository.OrderRepository
import com.intimocoffee.waiter.feature.products.domain.model.Category
import com.intimocoffee.waiter.feature.products.domain.model.ParentCategory
import com.intimocoffee.waiter.feature.products.domain.model.Product
import com.intimocoffee.waiter.feature.products.domain.repository.ProductRepository
import com.intimocoffee.waiter.feature.tables.domain.model.Table
import com.intimocoffee.waiter.feature.tables.domain.model.TableStatus
import com.intimocoffee.waiter.feature.tables.domain.repository.TableRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import java.math.BigDecimal
import javax.inject.Inject

data class CreateOrderUiState(
    val selectedTable: Table? = null,
    val availableTables: List<Table> = emptyList(),
    val products: List<Product> = emptyList(),
    // Categorías padre (nivel superior, p1-p7)
    val parentCategories: List<ParentCategory> = emptyList(),
    // Categorías hoja (subcategorías, IDs 1-18)
    val categories: List<Category> = emptyList(),
    // Selección en dos niveles
    val selectedParentCategoryId: String? = null,
    val selectedCategoryId: Long? = null,
    val cartItems: List<CartItem> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val subtotal: BigDecimal = BigDecimal.ZERO,
    val tax: BigDecimal = BigDecimal.ZERO,
    val total: BigDecimal = BigDecimal.ZERO,
    val showTableSelector: Boolean = false,
    val showProductSearch: Boolean = false,
    val orderCreated: Boolean = false,
    val stockAvailabilityReport: StockAvailabilityReport? = null,
    val showStockWarnings: Boolean = false,
    val isValidatingStock: Boolean = false,
    // Fidelidad
    val customerPhone: String = "",
    val customerName: String = "",
    val fidelityCustomer: FidelityCustomer? = null,
    val isFidelityLoading: Boolean = false,
    // Modificadores
    val productForModifiers: Product? = null,
    val modifierOptionsByCategory: Map<Long, List<ModifierOptionResponse>> = emptyMap(),
    // Sugerencia de cliente de mesa activa
    val suggestedCustomerName: String? = null,
    val showCustomerSuggestion: Boolean = false,
) {
    val taxRate = BigDecimal("0.10") // 10% tax
    
    val calculatedSubtotal: BigDecimal
        get() = cartItems.fold(BigDecimal.ZERO) { acc, item -> acc.add(item.subtotal) }
    
    val calculatedTax: BigDecimal
        get() = calculatedSubtotal.multiply(taxRate)
    
    val calculatedTotal: BigDecimal
        get() = calculatedSubtotal.add(calculatedTax)

    val fidelityPointsToEarn: Int
        get() = FidelityRepository.calculatePoints(calculatedTotal)

    /** Subcategorías visibles para el padre seleccionado */
    val subCategoriesForParent: List<Category>
        get() = if (selectedParentCategoryId == null) emptyList()
                else categories.filter { it.parentCategoryId == selectedParentCategoryId }

    /** Productos filtrados según la selección de dos niveles */
    val filteredProducts: List<Product>
        get() = when {
            selectedParentCategoryId == null -> products
            selectedCategoryId != null -> products.filter { it.categoryId == selectedCategoryId }
            else -> {
                val subIds = categories
                    .filter { it.parentCategoryId == selectedParentCategoryId }
                    .map { it.id }
                products.filter { it.categoryId in subIds }
            }
        }
}

@HiltViewModel
class CreateOrderViewModel @Inject constructor(
    private val orderRepository: OrderRepository,
    private val productRepository: ProductRepository,
    private val tableRepository: TableRepository,
    private val inventoryService: InventoryService,
    private val remoteOrderService: RemoteOrderService,
    private val authRepository: AuthRepository,
    private val fidelityRepository: FidelityRepository,
) : ViewModel() {

    private var fidelityLookupJob: Job? = null

    private val _uiState = MutableStateFlow(CreateOrderUiState())
    val uiState: StateFlow<CreateOrderUiState> = _uiState.asStateFlow()

    init {
        loadInitialData()
    }

    private fun loadInitialData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            
            // Load tables available for new orders (including occupied tables)
            tableRepository.getTablesForNewOrders().collect { tables ->
                _uiState.value = _uiState.value.copy(
                    availableTables = tables,
                    isLoading = false
                )
            }
        }
        
        viewModelScope.launch {
            productRepository.getAllActiveProducts().collect { products ->
                _uiState.value = _uiState.value.copy(products = products)
            }
        }

        viewModelScope.launch {
            try {
                val result = remoteOrderService.getCategoriesFromServer()
                if (result.isSuccess) {
                    val all = result.getOrNull() ?: emptyList()
                    val parents = all
                        .filter { it.parentCategoryId == null }
                        .sortedBy { it.sortOrder }
                        .map { ParentCategory(id = it.id, name = it.name, color = it.color, sortOrder = it.sortOrder) }
                    val leaves = all
                        .filter { it.parentCategoryId != null }
                        .sortedBy { it.sortOrder }
                        .mapNotNull { resp ->
                            val leafId = resp.id.toLongOrNull() ?: return@mapNotNull null
                            Category(
                                id = leafId,
                                name = resp.name,
                                description = null,
                                color = resp.color,
                                icon = null,
                                isActive = true,
                                sortOrder = resp.sortOrder,
                                parentCategoryId = resp.parentCategoryId
                            )
                        }
                    _uiState.value = _uiState.value.copy(
                        parentCategories = parents,
                        categories = leaves
                    )
                    Log.d("CreateOrderViewModel", "Loaded ${parents.size} parent categories, ${leaves.size} leaf categories")
                }
            } catch (e: Exception) {
                Log.w("CreateOrderViewModel", "Failed to load categories from server: ${e.message}")
            }
        }

        viewModelScope.launch {
            try {
                val result = remoteOrderService.getModifierOptionsFromServer()
                if (result.isSuccess) {
                    val grouped = (result.getOrNull() ?: emptyList())
                        .groupBy { it.categoryId.toLongOrNull() ?: 0L }
                    _uiState.value = _uiState.value.copy(modifierOptionsByCategory = grouped)
                    Log.d("CreateOrderViewModel", "Loaded modifier options: ${grouped.size} categories")
                }
            } catch (e: Exception) {
                Log.w("CreateOrderViewModel", "Failed to load modifier options: ${e.message}")
            }
        }
    }

    fun selectTable(table: Table) {
        Log.d("CreateOrderViewModel", "Selecting table: $table")
        _uiState.value = _uiState.value.copy(
            selectedTable = table,
            showTableSelector = false,
            showCustomerSuggestion = false,
            suggestedCustomerName = null
        )
        viewModelScope.launch {
            try {
                val result = remoteOrderService.getActiveOrdersFromServer()
                if (result.isSuccess) {
                    val suggestion = result.getOrNull()
                        ?.filter { it.tableId == table.id && !it.customerName.isNullOrBlank() }
                        ?.firstOrNull()
                        ?.customerName
                    if (!suggestion.isNullOrBlank()) {
                        _uiState.value = _uiState.value.copy(
                            suggestedCustomerName = suggestion,
                            showCustomerSuggestion = true
                        )
                    }
                }
            } catch (e: Exception) {
                Log.w("CreateOrderViewModel", "Could not fetch active orders for suggestion: ${e.message}")
            }
        }
    }

    fun showTableSelector() {
        _uiState.value = _uiState.value.copy(showTableSelector = true)
    }

    fun hideTableSelector() {
        _uiState.value = _uiState.value.copy(showTableSelector = false)
    }

    fun showProductSearch() {
        _uiState.value = _uiState.value.copy(showProductSearch = true)
    }

    fun hideProductSearch() {
        _uiState.value = _uiState.value.copy(showProductSearch = false)
    }
    
    /** Selecciona (o deselecciona) una categoría padre. Resetea la subcategoría.
     *  Si el padre tiene una única subcategoría, la auto-selecciona. */
    fun selectParentCategory(parentId: String?) {
        val subCats = _uiState.value.categories.filter { it.parentCategoryId == parentId }
        val autoLeaf = if (subCats.size == 1) subCats[0].id else null
        _uiState.value = _uiState.value.copy(
            selectedParentCategoryId = parentId,
            selectedCategoryId = autoLeaf
        )
    }

    fun selectCategory(categoryId: Long?) {
        _uiState.value = _uiState.value.copy(selectedCategoryId = categoryId)
    }

    fun updatePhone(phone: String) {
        _uiState.value = _uiState.value.copy(customerPhone = phone, fidelityCustomer = null)
        if (phone.length >= 7) {
            fidelityLookupJob?.cancel()
            fidelityLookupJob = viewModelScope.launch {
                delay(400) // debounce
                _uiState.value = _uiState.value.copy(isFidelityLoading = true)
                val customer = fidelityRepository.getByPhone(phone)
                _uiState.value = _uiState.value.copy(
                    fidelityCustomer = customer,
                    isFidelityLoading = false
                )
            }
        }
    }

    fun triggerFidelitySearch() {
        val phone = _uiState.value.customerPhone
        if (phone.length >= 7) {
            fidelityLookupJob?.cancel()
            fidelityLookupJob = viewModelScope.launch {
                _uiState.value = _uiState.value.copy(isFidelityLoading = true)
                val customer = fidelityRepository.getByPhone(phone)
                _uiState.value = _uiState.value.copy(
                    fidelityCustomer = customer,
                    isFidelityLoading = false
                )
            }
        }
    }

    fun updateCustomerName(name: String) {
        _uiState.value = _uiState.value.copy(customerName = name)
    }

    /** Abre el sheet de modificadores para el producto seleccionado. */
    fun openModifiers(product: Product) {
        _uiState.value = _uiState.value.copy(productForModifiers = product)
    }

    /** Cierra el sheet de modificadores sin agregar nada. */
    fun closeModifiers() {
        _uiState.value = _uiState.value.copy(productForModifiers = null)
    }

    /** Acepta la sugerencia de cliente de la mesa y rellena el nombre. */
    fun acceptCustomerSuggestion() {
        val name = _uiState.value.suggestedCustomerName ?: return
        _uiState.value = _uiState.value.copy(
            customerName = name,
            showCustomerSuggestion = false
        )
    }

    /** Descarta la sugerencia de cliente. */
    fun dismissCustomerSuggestion() {
        _uiState.value = _uiState.value.copy(showCustomerSuggestion = false)
    }

    /** Agrega el producto con los modificadores seleccionados al carrito.
     *  Si ya existe un CartItem con el mismo producto Y las mismas notas, incrementa cantidad.
     *  Si las notas son distintas, agrega como línea separada.
     *  priceExtra se suma al precio base (p.ej. leche de avena/coco). */
    fun addProductWithModifiers(
        product: Product,
        selectedModifiers: List<String>,
        customNote: String,
        priceExtra: BigDecimal = BigDecimal.ZERO
    ) {
        val notes = buildString {
            if (selectedModifiers.isNotEmpty()) append(selectedModifiers.joinToString(", "))
            if (customNote.isNotBlank()) {
                if (selectedModifiers.isNotEmpty()) append(" — ")
                append(customNote.trim())
            }
        }.takeIf { it.isNotBlank() }

        val unitPrice = product.price.add(priceExtra)
        val currentState = _uiState.value
        val existingItem = currentState.cartItems.find {
            it.product.id == product.id && it.notes == notes
        }
        val updatedCart = if (existingItem != null) {
            currentState.cartItems.map { cartItem ->
                if (cartItem.product.id == product.id && cartItem.notes == notes)
                    cartItem.withQuantity(cartItem.quantity + 1)
                else cartItem
            }
        } else {
            currentState.cartItems + CartItem(product = product, quantity = 1, notes = notes, unitPrice = unitPrice)
        }
        _uiState.value = currentState.copy(cartItems = updatedCart, productForModifiers = null)
    }

    /** Agrega directamente sin modificadores (botón + rápido). */
    fun addProductToCart(product: Product) {
        val currentState = _uiState.value
        val existingItem = currentState.cartItems.find { it.product.id == product.id && it.notes == null }
        
        val updatedCart = if (existingItem != null) {
            currentState.cartItems.map { cartItem ->
                if (cartItem.product.id == product.id && cartItem.notes == null) {
                    cartItem.withQuantity(cartItem.quantity + 1)
                } else {
                    cartItem
                }
            }
        } else {
            currentState.cartItems + CartItem(product = product, quantity = 1)
        }
        
        _uiState.value = currentState.copy(
            cartItems = updatedCart,
            showProductSearch = false
        )
    }

    fun updateCartItemQuantity(productId: Long, quantity: Int) {
        val currentState = _uiState.value
        val updatedCart = if (quantity <= 0) {
            currentState.cartItems.filter { it.product.id != productId }
        } else {
            currentState.cartItems.map { cartItem ->
                if (cartItem.product.id == productId) {
                    cartItem.withQuantity(quantity)
                } else {
                    cartItem
                }
            }
        }
        
        _uiState.value = currentState.copy(cartItems = updatedCart)
    }

    fun updateCartItemNotes(productId: Long, notes: String) {
        val currentState = _uiState.value
        val updatedCart = currentState.cartItems.map { cartItem ->
            if (cartItem.product.id == productId) {
                cartItem.withNotes(notes.takeIf { it.isNotBlank() })
            } else {
                cartItem
            }
        }
        
        _uiState.value = currentState.copy(cartItems = updatedCart)
    }

    fun removeCartItem(productId: Long) {
        val currentState = _uiState.value
        val updatedCart = currentState.cartItems.filter { it.product.id != productId }
        _uiState.value = currentState.copy(cartItems = updatedCart)
    }


    fun createOrder() {
        val currentState = _uiState.value
        
        Log.d("CreateOrderViewModel", "Creating order with selectedTable: ${currentState.selectedTable}")
        Log.d("CreateOrderViewModel", "Cart items: ${currentState.cartItems}")
        
        if (currentState.selectedTable == null) {
            _uiState.value = currentState.copy(error = "Debe seleccionar una mesa")
            return
        }
        
        if (currentState.cartItems.isEmpty()) {
            _uiState.value = currentState.copy(error = "Debe agregar productos a la orden")
            return
        }
        
        // First validate stock availability
        validateStockAvailability()
    }
    
    private fun validateStockAvailability() {
        viewModelScope.launch {
            val currentState = _uiState.value
            
            try {
                _uiState.value = currentState.copy(isValidatingStock = true, error = null)
                
                val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
                val orderItems = currentState.cartItems.map { cartItem ->
                    OrderItem(
                        id = 0L,
                        orderId = 0L,
                        productId = cartItem.product.id,
                        productName = cartItem.product.name,
                        productPrice = cartItem.unitPrice,
                        quantity = cartItem.quantity,
                        subtotal = cartItem.subtotal,
                        notes = cartItem.notes,
                        createdAt = now
                    )
                }
                
                val stockReport = inventoryService.getStockAvailabilityReport(orderItems)
                
                _uiState.value = currentState.copy(
                    isValidatingStock = false,
                    stockAvailabilityReport = stockReport
                )
                
                // If stock is insufficient, show warnings
                if (!stockReport.isAvailable || stockReport.warnings.isNotEmpty()) {
                    _uiState.value = _uiState.value.copy(showStockWarnings = true)
                } else {
                    // Stock is sufficient, proceed with order creation
                    proceedWithOrderCreation()
                }
                
            } catch (e: Exception) {
                _uiState.value = currentState.copy(
                    isValidatingStock = false,
                    error = "Error validating stock: ${e.message}"
                )
            }
        }
    }
    
    private fun proceedWithOrderCreation() {
        val currentState = _uiState.value
        
        viewModelScope.launch {
            try {
                _uiState.value = currentState.copy(isLoading = true, error = null)
                
                val selectedTable = currentState.selectedTable!!

                // Obtener el usuario actual para marcar quién creó la orden
                val currentUser = try {
                    authRepository.getCurrentUser()
                } catch (e: Exception) {
                    null
                }
                val createdByUserId = currentUser?.id ?: 1L

                Log.d(
                    "CreateOrderViewModel",
                    "Creating order on remote server as userId=$createdByUserId (user=${currentUser?.username})"
                )
                
                // Create order directly on the main server using API
                val customerLabel = currentState.customerName.ifBlank {
                    currentState.customerPhone.ifBlank { null }
                }
                val result = remoteOrderService.createOrderOnServer(
                    tableId = selectedTable.id,
                    tableName = selectedTable.displayName,
                    customerName = customerLabel,
                    cartItems = currentState.cartItems,
                    notes = null,
                    createdBy = createdByUserId
                )
                
                val orderId = result.getOrNull() ?: 0L
                
                if (result.isSuccess && orderId > 0) {
                    Log.d("CreateOrderViewModel", "Order created successfully on remote server with ID: $orderId")

                    // Save fidelity points if phone was provided
                    val phone = currentState.customerPhone
                    if (phone.isNotBlank()) {
                        try {
                            val updatedCustomer = fidelityRepository.addPoints(phone, currentState.calculatedTotal, orderId)
                            Log.d("CreateOrderViewModel", "Fidelity points saved for $phone (orderId=$orderId, pts=${updatedCustomer.totalPoints})")
                            _uiState.value = _uiState.value.copy(fidelityCustomer = updatedCustomer)
                        } catch (e: Exception) {
                            Log.w("CreateOrderViewModel", "Failed to save fidelity points: ${e.message}")
                        }
                    }
                    
                    // Update local table status (if needed)
                    if (selectedTable.status == TableStatus.FREE) {
                        try {
                            tableRepository.updateTableStatus(
                                tableId = selectedTable.id,
                                status = TableStatus.OCCUPIED,
                                orderId = orderId
                            )
                        } catch (e: Exception) {
                            Log.w("CreateOrderViewModel", "Failed to update local table status: ${e.message}")
                        }
                    }
                    
                    // Set order created flag to trigger navigation
                    _uiState.value = currentState.copy(
                        isLoading = false,
                        error = null,
                        orderCreated = true
                    )
                } else {
                    val errorMessage = result.exceptionOrNull()?.message ?: "Error desconocido"
                    Log.e("CreateOrderViewModel", "Failed to create order on remote server: $errorMessage")
                    _uiState.value = currentState.copy(
                        isLoading = false,
                        error = "Error al crear la orden: $errorMessage"
                    )
                }
                
            } catch (e: Exception) {
                _uiState.value = currentState.copy(
                    isLoading = false,
                    error = "Error: ${e.message}"
                )
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
    
    fun hideStockWarnings() {
        _uiState.value = _uiState.value.copy(
            showStockWarnings = false,
            stockAvailabilityReport = null
        )
    }
    
    fun proceedWithWarnings() {
        _uiState.value = _uiState.value.copy(showStockWarnings = false)
        proceedWithOrderCreation()
    }
}
