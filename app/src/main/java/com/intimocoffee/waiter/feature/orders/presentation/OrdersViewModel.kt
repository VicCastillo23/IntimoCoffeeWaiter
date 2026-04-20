package com.intimocoffee.waiter.feature.orders.presentation

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.intimocoffee.waiter.core.network.RemoteOrderService
import com.intimocoffee.waiter.feature.auth.domain.repository.AuthRepository
import com.intimocoffee.waiter.feature.orders.domain.model.Order
import com.intimocoffee.waiter.feature.orders.domain.model.OrderItem
import com.intimocoffee.waiter.feature.orders.domain.model.OrderStatus
import com.intimocoffee.waiter.feature.orders.domain.usecase.*
import com.intimocoffee.waiter.feature.orders.presentation.modifiers.splitModifierOptionsFromApi
import com.intimocoffee.waiter.feature.products.domain.model.Product
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.*
import com.intimocoffee.waiter.core.network.DynamicRetrofitProvider
import com.intimocoffee.waiter.feature.products.domain.repository.ProductRepository
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

data class OrdersUiState(
    val orders: List<Order> = emptyList(),
    val filteredOrders: List<Order> = emptyList(),
    val selectedStatus: OrderStatus? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val selectedOrder: Order? = null,
    val showOrderDetails: Boolean = false,
    val searchQuery: String = "",
    val showCreateOrder: Boolean = false,
    val currentUserId: Long? = null,
    val currentUserName: String? = null,
    val serverUrl: String = "",
    val orderToEdit: Order? = null,
    val productsForEdit: List<Product> = emptyList(),
    val modifierOptionsByCategory: Map<Long, List<com.intimocoffee.waiter.core.network.ModifierOptionResponse>> = emptyMap(),
    val pricedModifierSectionsByCategory: Map<Long, List<Pair<String, List<com.intimocoffee.waiter.core.network.ModifierOptionResponse>>>> = emptyMap(),
    val temperaturaOptionsByCategory: Map<Long, List<com.intimocoffee.waiter.core.network.ModifierOptionResponse>> = emptyMap(),
)

@HiltViewModel
class OrdersViewModel @Inject constructor(
    private val getOrdersUseCase: GetOrdersUseCase,
    private val getActiveOrdersUseCase: GetActiveOrdersUseCase,
    private val getOrdersByStatusUseCase: GetOrdersByStatusUseCase,
    private val updateOrderStatusUseCase: UpdateOrderStatusUseCase,
    private val cancelOrderUseCase: CancelOrderUseCase,
    private val getOrderDetailsUseCase: GetOrderDetailsUseCase,
    private val authRepository: AuthRepository,
    private val retrofitProvider: DynamicRetrofitProvider,
    private val remoteOrderService: RemoteOrderService,
    private val productRepository: ProductRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(OrdersUiState())
    val uiState = _uiState.asStateFlow()

    private val _orders = MutableStateFlow<List<Order>>(emptyList())

    /** IDs de órdenes ya vistas; si aparece una nueva en sync, alerta sonora. */
    private val knownOrderIds = mutableSetOf<Long>()
    private var ordersAlertBaselineReady = false
    private val _workAlertEvent = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val workAlertEvent = _workAlertEvent.asSharedFlow()

    private var autoSyncJob: Job? = null
    private val autoSyncIntervalMs = 10_000L // 10 segundos

    init {
        loadCurrentUser()
        loadOrders()
        startAutoSync()
        _uiState.update { it.copy(serverUrl = retrofitProvider.getCurrentServerUrl()) }
    }

    private fun loadCurrentUser() {
        viewModelScope.launch {
            try {
                val user = authRepository.getCurrentUser()
                _uiState.update {
                    it.copy(
                        currentUserId = user?.id,
                        currentUserName = user?.fullName ?: user?.username
                    )
                }
            } catch (e: Exception) {
                // Ignore auth errors here; screen still works without user filter
            }
        }
    }

    fun loadOrders() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            try {
                val allOrders = getOrdersUseCase().first()
                // Exclude ARCHIVED orders to give impression of clean day
                val orders = allOrders.filter { it.status != OrderStatus.ARCHIVED }
                _orders.value = orders
                knownOrderIds.clear()
                knownOrderIds.addAll(orders.map { it.id })
                ordersAlertBaselineReady = true
                _uiState.update {
                    it.copy(
                        orders = orders,
                        filteredOrders = filterOrders(orders, it.selectedStatus, it.searchQuery),
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = "Error al cargar las órdenes: ${e.message}"
                    )
                }
            }
        }
    }

    fun filterByStatus(status: OrderStatus?) {
        _uiState.update { currentState ->
            currentState.copy(
                selectedStatus = status,
                filteredOrders = filterOrders(_orders.value, status, currentState.searchQuery)
            )
        }
    }

    fun searchOrders(query: String) {
        _uiState.update { currentState ->
            currentState.copy(
                searchQuery = query,
                filteredOrders = filterOrders(_orders.value, currentState.selectedStatus, query)
            )
        }
    }

    private fun filterOrders(orders: List<Order>, status: OrderStatus?, query: String): List<Order> {
        var filtered = orders

        // Filtrar por estado (never show ARCHIVED orders)
        if (status != null && status != OrderStatus.ARCHIVED) {
            filtered = filtered.filter { it.status == status }
        }

        // Filtrar por búsqueda
        if (query.isNotBlank()) {
            filtered = filtered.filter { order ->
                order.orderNumber.contains(query, ignoreCase = true) ||
                order.customerName?.contains(query, ignoreCase = true) == true ||
                order.tableName?.contains(query, ignoreCase = true) == true ||
                order.items.any { it.productName.contains(query, ignoreCase = true) }
            }
        }

        // Ordenar por fecha de creación (más recientes primero)
        return filtered.sortedByDescending { it.createdAt }
    }

    fun updateOrderStatus(orderId: Long, newStatus: OrderStatus) {
        Log.d("OrdersViewModel", "updateOrderStatus called: orderId=$orderId, newStatus=$newStatus")
        
        viewModelScope.launch {
            try {
                Log.d("OrdersViewModel", "Setting loading state to true")
                _uiState.update { it.copy(isLoading = true) }
                
                Log.d("OrdersViewModel", "Calling updateOrderStatusUseCase")
                val result = updateOrderStatusUseCase(orderId, newStatus)
                
                result
                    .onSuccess {
                        Log.d("OrdersViewModel", "updateOrderStatusUseCase SUCCESS")
                        _uiState.update { 
                            it.copy(
                                isLoading = false,
                                error = null
                            )
                        }
                        // Force refresh to ensure UI updates
                        Log.d("OrdersViewModel", "Refreshing orders after status update")
                        refreshOrders()
                    }
                    .onFailure { error ->
                        Log.e("OrdersViewModel", "updateOrderStatusUseCase FAILED: ${error.message}", error)
                        _uiState.update { 
                            it.copy(
                                isLoading = false,
                                error = error.message
                            )
                        }
                    }
            } catch (e: Exception) {
                Log.e("OrdersViewModel", "Exception in updateOrderStatus", e)
                _uiState.update { 
                    it.copy(
                        isLoading = false,
                        error = e.message
                    )
                }
            }
        }
    }

    fun cancelOrder(orderId: Long) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            
            cancelOrderUseCase(orderId)
                .onSuccess {
                    _uiState.update { 
                        it.copy(
                            isLoading = false,
                            error = null
                        )
                    }
                    // Force refresh to ensure UI updates
                    refreshOrders()
                }
                .onFailure { error ->
                    _uiState.update { 
                        it.copy(
                            isLoading = false,
                            error = error.message
                        )
                    }
                }
        }
    }

    fun showOrderDetails(order: Order) {
        _uiState.update { 
            it.copy(
                selectedOrder = order,
                showOrderDetails = true
            )
        }
    }

    fun hideOrderDetails() {
        _uiState.update { 
            it.copy(
                selectedOrder = null,
                showOrderDetails = false
            )
        }
    }

    fun showCreateOrder() {
        _uiState.update { 
            it.copy(showCreateOrder = true)
        }
    }

    fun hideCreateOrder() {
        _uiState.update { 
            it.copy(showCreateOrder = false)
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    fun refreshOrders() {
        _uiState.update { it.copy(serverUrl = retrofitProvider.getCurrentServerUrl()) }
        loadOrders()
    }

    fun openEditOrder(order: Order) {
        if (OrderStatus.isCompleted(order.status)) return
        _uiState.update { current ->
            current.copy(
                orderToEdit = order,
                error = null
            )
        }
        viewModelScope.launch {
            try {
                val products = productRepository.getAllActiveProducts().first()
                val modifierOptions = remoteOrderService.getModifierOptionsFromServer().getOrDefault(emptyList())
                val (dynamic, priced, temp) = splitModifierOptionsFromApi(modifierOptions)
                _uiState.update {
                    it.copy(
                        productsForEdit = products,
                        modifierOptionsByCategory = dynamic,
                        pricedModifierSectionsByCategory = priced,
                        temperaturaOptionsByCategory = temp,
                        error = null
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        // Keep dialog open even if product refresh fails.
                        error = e.message ?: "No se pudo cargar el catálogo para edición"
                    )
                }
            }
        }
    }

    fun dismissEditOrder() {
        _uiState.update {
            it.copy(
                orderToEdit = null,
                productsForEdit = emptyList(),
                modifierOptionsByCategory = emptyMap(),
                pricedModifierSectionsByCategory = emptyMap(),
                temperaturaOptionsByCategory = emptyMap()
            )
        }
    }

    fun applyOrderEditsRemote(
        order: Order,
        removedIds: List<Long>,
        updated: List<OrderItem>,
        added: List<OrderItem>,
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                var appliedOps = 0
                for (id in removedIds) {
                    remoteOrderService.removeOrderItemFromServer(order.id, id).getOrThrow()
                    appliedOps++
                }
                for (item in updated) {
                    remoteOrderService.updateOrderItemOnServer(
                        order.id,
                        item.id,
                        item.quantity,
                        item.notes
                    ).getOrThrow()
                    appliedOps++
                }
                for (item in added) {
                    remoteOrderService.addOrderItemOnServer(order.id, item).getOrThrow()
                    appliedOps++
                }
                if (appliedOps == 0) {
                    _uiState.update { it.copy(error = "No hubo cambios para guardar") }
                }
                dismissEditOrder()
                refreshOrders()
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isLoading = false, error = e.message ?: "Error al guardar la orden")
                }
            }
            _uiState.update { it.copy(isLoading = false) }
        }
    }

    private fun startAutoSync() {
        autoSyncJob?.cancel()

        autoSyncJob = viewModelScope.launch {
            try {
                while (isActive) {
                    delay(autoSyncIntervalMs)
                    if (!isActive) break

                    try {
                        val allOrders = getOrdersUseCase().first()
                        val orders = allOrders.filter { it.status != OrderStatus.ARCHIVED }
                        if (ordersAlertBaselineReady) {
                            val ids = orders.map { it.id }.toSet()
                            val newIds = ids - knownOrderIds
                            if (newIds.isNotEmpty()) {
                                _workAlertEvent.tryEmit(Unit)
                            }
                            knownOrderIds.clear()
                            knownOrderIds.addAll(ids)
                        }
                        _orders.value = orders

                        _uiState.update { currentState ->
                            currentState.copy(
                                orders = orders,
                                filteredOrders = filterOrders(orders, currentState.selectedStatus, currentState.searchQuery)
                            )
                        }
                    } catch (e: Exception) {
                        Log.e("OrdersViewModel", "Error in auto-sync", e)
                    }
                }
            } catch (e: Exception) {
                // Ignore cancellation and unexpected errors
            }
        }
    }
}
