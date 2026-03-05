package com.intimocoffee.waiter.feature.tables.presentation

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.intimocoffee.waiter.feature.orders.domain.model.Order
import com.intimocoffee.waiter.feature.orders.domain.model.OrderItem
import com.intimocoffee.waiter.feature.orders.domain.model.OrderStatus
import com.intimocoffee.waiter.feature.orders.domain.repository.OrderRepository
import com.intimocoffee.waiter.feature.products.domain.model.Product
import com.intimocoffee.waiter.feature.products.domain.repository.ProductRepository
import com.intimocoffee.waiter.feature.tables.domain.model.Table
import com.intimocoffee.waiter.feature.tables.domain.model.TableStatus
import com.intimocoffee.waiter.feature.tables.domain.model.BillSplit
import com.intimocoffee.waiter.feature.tables.domain.repository.TableRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

@HiltViewModel
class SimpleTableDetailsViewModel @Inject constructor(
    private val tableRepository: TableRepository,
    private val orderRepository: OrderRepository,
    private val productRepository: ProductRepository
) : ViewModel() {
    
    companion object {
        private const val TAG = "SimpleTableDetailsViewModel"
    }
    
    private val _uiState = MutableStateFlow(SimpleTableDetailsUiState())
    val uiState: StateFlow<SimpleTableDetailsUiState> = _uiState.asStateFlow()
    
    fun loadTableDetails(tableId: Long) {
        Log.d(TAG, "Loading table details for table: $tableId")
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            
            try {
                val table = tableRepository.getTableById(tableId)
                Log.d(TAG, "Loaded table: $table")
                
                // Get the first emission from the flow (current state)
                val allOrdersForTable = orderRepository.getOrdersByTable(tableId).first()
                Log.d(TAG, "Found ${allOrdersForTable.size} total orders for table $tableId")
                allOrdersForTable.forEachIndexed { index, order ->
                    Log.d(TAG, "Order $index: ID=${order.id}, Number=${order.orderNumber}, Status=${order.status}, Items=${order.items.size}")
                }
                
                // Filter to get only active orders (excluding paid, archived and cancelled orders)
                val activeOrders = allOrdersForTable.filter { order ->
                    order.status in listOf(
                        OrderStatus.PENDING,
                        OrderStatus.PREPARING, 
                        OrderStatus.READY,
                        OrderStatus.DELIVERED
                    )
                }
                Log.d(TAG, "Found ${activeOrders.size} active orders after filtering")
                
                // Use the most recent active order as primary, but store all active orders
                val currentOrder = activeOrders
                    .sortedByDescending { it.createdAt }
                    .firstOrNull()
                
                // Load available products
                val availableProducts = productRepository.getAllActiveProducts().first()
                
                _uiState.value = _uiState.value.copy(
                    table = table,
                    currentOrder = currentOrder,
                    allActiveOrders = activeOrders,
                    availableProducts = availableProducts,
                    isLoading = false,
                    error = null
                )
            } catch (e: Exception) {
                Log.e(TAG, "Error loading table details: ${e.message}", e)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Error desconocido"
                )
            }
        }
    }
    
    fun showCloseTableDialog() {
        _uiState.value = _uiState.value.copy(showCloseDialog = true)
    }
    
    fun hideCloseTableDialog() {
        _uiState.value = _uiState.value.copy(showCloseDialog = false)
    }
    
    fun showMultipleOrdersCloseDialog() {
        _uiState.value = _uiState.value.copy(showMultipleOrdersCloseDialog = true)
    }
    
    fun hideMultipleOrdersCloseDialog() {
        _uiState.value = _uiState.value.copy(showMultipleOrdersCloseDialog = false)
    }
    
    fun showTicket() {
        _uiState.value = _uiState.value.copy(showTicket = true)
    }
    
    fun hideTicket() {
        _uiState.value = _uiState.value.copy(showTicket = false)
    }
    
    fun showAddProductDialog() {
        _uiState.value = _uiState.value.copy(showAddProductDialog = true)
    }
    
    fun hideAddProductDialog() {
        _uiState.value = _uiState.value.copy(showAddProductDialog = false)
    }
    
    fun showCreateSubOrderDialog() {
        _uiState.value = _uiState.value.copy(showCreateSubOrderDialog = true)
    }
    
    fun hideCreateSubOrderDialog() {
        _uiState.value = _uiState.value.copy(showCreateSubOrderDialog = false)
    }
    
    fun showBillSplitDialog() {
        _uiState.value = _uiState.value.copy(showBillSplitDialog = true)
    }
    
    fun hideBillSplitDialog() {
        _uiState.value = _uiState.value.copy(showBillSplitDialog = false)
    }
    
    suspend fun closeTable(paymentMethod: PaymentMethod) {
        val table = _uiState.value.table ?: return
        val activeOrders = _uiState.value.allActiveOrders
        
        if (activeOrders.isEmpty()) {
            Log.w(TAG, "No active orders to close")
            return
        }
        
        Log.d(TAG, "Closing table with ${activeOrders.size} active orders")
        
        try {
            // Close all active orders by updating their status to PAID
            activeOrders.forEach { order ->
                Log.d(TAG, "Updating order ${order.id} status to PAID")
                orderRepository.updateOrderStatus(order.id, OrderStatus.PAID)
            }
            
            // Update table status to free
            val updatedTable = table.copy(
                status = TableStatus.FREE,
                currentOrderId = null
            )
            tableRepository.updateTable(updatedTable)
            Log.d(TAG, "Updated table status to FREE")
            
            // Generate appropriate ticket
            val ticket = if (activeOrders.size == 1) {
                generateTicket(activeOrders.first(), table, paymentMethod)
            } else {
                createMultipleOrdersTicket(activeOrders, table, paymentMethod)
            }
            
            _uiState.value = _uiState.value.copy(
                showCloseDialog = false,
                showTicket = true,
                lastTicket = ticket,
                table = updatedTable,
                currentOrder = null,
                allActiveOrders = emptyList(),
                error = null
            )
            
            Log.d(TAG, "Successfully closed table with ${activeOrders.size} orders")
        } catch (e: Exception) {
            Log.e(TAG, "Error closing table: ${e.message}", e)
            _uiState.value = _uiState.value.copy(
                error = e.message ?: "Error al cerrar mesa",
                showCloseDialog = false
            )
        }
    }
    
    private fun generateTicket(order: Order, table: Table, paymentMethod: PaymentMethod): SimpleTicket {
        val currencyFormat = NumberFormat.getCurrencyInstance(Locale.US)
        val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        
        val ticketItems = order.items.map { item ->
            SimpleTicket.Item(
                name = item.productName,
                quantity = item.quantity,
                unitPrice = currencyFormat.format(item.productPrice),
                total = currencyFormat.format(item.subtotal),
                notes = item.notes
            )
        }
        
        return SimpleTicket(
            orderNumber = order.orderNumber,
            tableName = table.displayName,
            items = ticketItems,
            subtotal = currencyFormat.format(order.subtotal),
            tax = currencyFormat.format(order.tax),
            total = currencyFormat.format(order.total),
            paymentMethod = paymentMethod,
            timestamp = dateFormat.format(Date())
        )
    }
    
    fun addProductsToOrder(orderId: Long, orderItems: List<OrderItem>) {
        Log.d(TAG, "Adding products to order: $orderId, items: ${orderItems.size}")
        orderItems.forEach { item ->
            Log.d(TAG, "Item to add: ${item.productName} - Qty: ${item.quantity} - Price: ${item.productPrice}")
        }
        
        viewModelScope.launch {
            try {
                var successCount = 0
                var failureCount = 0
                
                // Add order items using OrderRepository and track success/failure
                orderItems.forEach { item ->
                    Log.d(TAG, "Attempting to add item: ${item.productName} to order $orderId")
                    val success = orderRepository.addItemToOrder(orderId, item)
                    if (success) {
                        successCount++
                        Log.d(TAG, "Successfully added item: ${item.productName}")
                    } else {
                        failureCount++
                        Log.e(TAG, "Failed to add item: ${item.productName}")
                    }
                }
                
                Log.d(TAG, "Add items result: $successCount success, $failureCount failures")
                
                if (failureCount > 0) {
                    _uiState.value = _uiState.value.copy(
                        error = "No se pudieron agregar $failureCount de ${orderItems.size} productos"
                    )
                }
                
                // Always reload orders to update totals (even if some failed)
                val tableId = _uiState.value.table?.id
                if (tableId != null) {
                    Log.d(TAG, "Reloading table details for table: $tableId")
                    // Add a small delay to ensure database has been updated
                    kotlinx.coroutines.delay(500)
                    Log.d(TAG, "Starting forced reload after database update")
                    loadTableDetails(tableId)
                } else {
                    Log.e(TAG, "Cannot reload table details: table ID is null")
                }
                
                // Show success message if all items were added successfully
                if (failureCount == 0) {
                    _uiState.value = _uiState.value.copy(
                        error = null,
                        showAddProductDialog = false
                    )
                    Log.d(TAG, "All items added successfully, closing dialog")
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "Error adding products to order: ${e.message}", e)
                _uiState.value = _uiState.value.copy(
                    error = "Error al agregar productos: ${e.message}"
                )
            }
        }
    }

    fun closeMultipleOrders(paymentMethod: PaymentMethod) {
        Log.d(TAG, "Closing multiple orders: ${_uiState.value.allActiveOrders.map { it.id }}")
        viewModelScope.launch {
            try {
                val table = _uiState.value.table ?: return@launch
                val orders = _uiState.value.allActiveOrders
                
                // Close each order by marking as PAID
                orders.forEach { order ->
                    orderRepository.updateOrderStatus(order.id, OrderStatus.PAID)
                }
                
                // Update table status to free
                val updatedTable = table.copy(
                    status = TableStatus.FREE,
                    currentOrderId = null
                )
                tableRepository.updateTable(updatedTable)
                
                // Prepare ticket data
                val ticket = createMultipleOrdersTicket(orders, table, paymentMethod)
                
                // Update UI state
                _uiState.value = _uiState.value.copy(
                    showMultipleOrdersCloseDialog = false,
                    showTicket = true,
                    lastTicket = ticket,
                    table = updatedTable,
                    allActiveOrders = emptyList(),
                    currentOrder = null,
                    error = null
                )
            } catch (e: Exception) {
                Log.e(TAG, "Error closing multiple orders: ${e.message}", e)
                _uiState.value = _uiState.value.copy(
                    error = "Error al cerrar las órdenes: ${e.message}"
                )
            }
        }
    }

    private fun createMultipleOrdersTicket(
        orders: List<Order>, 
        table: Table, 
        paymentMethod: PaymentMethod
    ): SimpleTicket {
        val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        val currencyFormat = NumberFormat.getCurrencyInstance(Locale.US)
        
        // Combine all order items from all orders
        val allOrderItems = mutableListOf<SimpleTicket.Item>()
        
        orders.forEach { order ->
            // Add a separator for each order
            if (allOrderItems.isNotEmpty()) {
                allOrderItems.add(
                    SimpleTicket.Item(
                        name = "--- Orden #${order.orderNumber} ---",
                        quantity = 0,
                        unitPrice = "",
                        total = "",
                        notes = null
                    )
                )
            }
            
            // Add items from current order
            order.items.forEach { item ->
                allOrderItems.add(
                    SimpleTicket.Item(
                        name = item.productName,
                        quantity = item.quantity,
                        unitPrice = currencyFormat.format(item.productPrice),
                        total = currencyFormat.format(item.subtotal),
                        notes = item.notes
                    )
                )
            }
        }
        
        // Calculate combined totals
        val totalSubtotal = orders.fold(BigDecimal.ZERO) { acc, order -> acc.add(order.subtotal) }
        val totalTax = orders.fold(BigDecimal.ZERO) { acc, order -> acc.add(order.tax) }
        val grandTotal = orders.fold(BigDecimal.ZERO) { acc, order -> acc.add(order.total) }
        
        return SimpleTicket(
            orderNumber = orders.joinToString(", ") { "#${it.orderNumber}" },
            tableName = table.displayName,
            items = allOrderItems,
            subtotal = currencyFormat.format(totalSubtotal),
            tax = currencyFormat.format(totalTax),
            total = currencyFormat.format(grandTotal),
            paymentMethod = paymentMethod,
            timestamp = dateFormat.format(Date())
        )
    }
    
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
    
    suspend fun processBillSplit(billSplit: BillSplit, paymentMethod: PaymentMethod) {
        val table = _uiState.value.table ?: return
        val activeOrders = _uiState.value.allActiveOrders
        
        if (activeOrders.isEmpty()) {
            Log.w(TAG, "No active orders to process split")
            return
        }
        
        Log.d(TAG, "Processing bill split with ${billSplit.persons.size} persons")
        
        try {
            // Close all active orders by updating their status to PAID
            activeOrders.forEach { order ->
                Log.d(TAG, "Updating order ${order.id} status to PAID")
                orderRepository.updateOrderStatus(order.id, OrderStatus.PAID)
            }
            
            // Update table status to free
            val updatedTable = table.copy(
                status = TableStatus.FREE,
                currentOrderId = null
            )
            tableRepository.updateTable(updatedTable)
            Log.d(TAG, "Updated table status to FREE")
            
            // Generate split tickets for each person
            val splitTickets = billSplit.persons.map { person ->
                generateSplitTicket(person, billSplit, table, paymentMethod)
            }
            
            _uiState.value = _uiState.value.copy(
                showBillSplitDialog = false,
                showTicket = true,
                lastTicket = splitTickets.firstOrNull(), // Show first ticket for now
                splitTickets = splitTickets,
                table = updatedTable,
                currentOrder = null,
                allActiveOrders = emptyList(),
                error = null
            )
            
            Log.d(TAG, "Successfully processed bill split with ${billSplit.persons.size} persons")
        } catch (e: Exception) {
            Log.e(TAG, "Error processing bill split: ${e.message}", e)
            _uiState.value = _uiState.value.copy(
                error = e.message ?: "Error al procesar división de cuenta",
                showBillSplitDialog = false
            )
        }
    }
    
    private fun generateSplitTicket(
        person: com.intimocoffee.waiter.feature.tables.domain.model.PersonSplit, 
        billSplit: BillSplit,
        table: Table, 
        paymentMethod: PaymentMethod
    ): SimpleTicket {
        val currencyFormat = NumberFormat.getCurrencyInstance(Locale.US)
        val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        
        val ticketItems = person.items.map { item ->
            SimpleTicket.Item(
                name = item.productName,
                quantity = item.quantity,
                unitPrice = currencyFormat.format(item.productPrice),
                total = currencyFormat.format(item.subtotal),
                notes = item.notes
            )
        }
        
        return SimpleTicket(
            orderNumber = "${person.displayName} - División",
            tableName = table.displayName,
            items = ticketItems,
            subtotal = currencyFormat.format(person.subtotal),
            tax = currencyFormat.format(person.tax),
            total = currencyFormat.format(person.total),
            paymentMethod = paymentMethod,
            timestamp = dateFormat.format(Date())
        )
    }
}

data class SimpleTableDetailsUiState(
    val table: Table? = null,
    val currentOrder: Order? = null,
    val allActiveOrders: List<Order> = emptyList(),
    val availableProducts: List<Product> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val showCloseDialog: Boolean = false,
    val showMultipleOrdersCloseDialog: Boolean = false,
    val showTicket: Boolean = false,
    val lastTicket: SimpleTicket? = null,
    val showAddProductDialog: Boolean = false,
    val showCreateSubOrderDialog: Boolean = false,
    val showBillSplitDialog: Boolean = false,
    val splitTickets: List<SimpleTicket> = emptyList()
) {
    // Calculate totals from all active orders
    val totalSubtotal: BigDecimal
        get() = allActiveOrders.fold(BigDecimal.ZERO) { acc, order -> acc.add(order.subtotal) }
    
    val totalTax: BigDecimal
        get() = allActiveOrders.fold(BigDecimal.ZERO) { acc, order -> acc.add(order.tax) }
    
    val totalAmount: BigDecimal
        get() = allActiveOrders.fold(BigDecimal.ZERO) { acc, order -> acc.add(order.total) }
    
    val totalItems: Int
        get() = allActiveOrders.sumOf { it.items.size }
}
