package com.intimocoffee.waiter.feature.inventory.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.intimocoffee.waiter.feature.inventory.domain.repository.InventoryRepository
import com.intimocoffee.waiter.feature.inventory.domain.model.*
import com.intimocoffee.waiter.feature.inventory.presentation.components.StockAdjustmentType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import javax.inject.Inject

@HiltViewModel
class InventoryViewModel @Inject constructor(
    private val inventoryRepository: InventoryRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(InventoryUiState())
    val uiState: StateFlow<InventoryUiState> = _uiState.asStateFlow()
    
    private val _allProducts = MutableStateFlow<List<ProductStockDetails>>(emptyList())
    
    init {
        loadInventoryData()
    }
    
    fun loadInventoryData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            
            try {
                // Load stock summary
                val summary = inventoryRepository.getTodayStockSummary()
                
                // Load stock alerts
                val alertsFlow = inventoryRepository.getStockAlerts()
                
                // Load products stock details
                val productsFlow = inventoryRepository.getAllProductsStock()
                
                // Combine all data
                combine(
                    flowOf(summary),
                    alertsFlow,
                    productsFlow
                ) { stockSummary, alerts, products ->
                    _allProducts.value = products
                    
                    _uiState.update { state ->
                        state.copy(
                            isLoading = false,
                            stockSummary = stockSummary,
                            alerts = alerts,
                            filteredProducts = filterProducts(products, state.selectedFilter),
                            error = null
                        )
                    }
                }.collect()
                
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(
                        isLoading = false,
                        error = "Error al cargar los datos de inventario: ${e.message}"
                    )
                }
            }
        }
    }
    
    fun refreshData() {
        loadInventoryData()
    }
    
    fun filterProducts(filter: String) {
        _uiState.update { state ->
            state.copy(
                selectedFilter = filter,
                filteredProducts = filterProducts(_allProducts.value, filter)
            )
        }
    }
    
    private fun filterProducts(products: List<ProductStockDetails>, filter: String): List<ProductStockDetails> {
        return when (filter) {
            "low" -> products.filter { 
                it.currentStock <= it.minStockLevel && it.currentStock > 0 
            }
            "out" -> products.filter { it.currentStock == 0 }
            "critical" -> products.filter { 
                it.alerts.any { alert -> 
                    alert.alertType in listOf(StockAlertType.OUT_OF_STOCK, StockAlertType.CRITICAL) 
                }
            }
            else -> products.sortedBy { it.productName }
        }
    }
    
    fun showProductDetails(productId: Long) {
        _uiState.update { it.copy(selectedProductId = productId, showProductDetails = true) }
    }
    
    fun hideProductDetails() {
        _uiState.update { it.copy(selectedProductId = null, showProductDetails = false) }
    }
    
    fun showStockAdjustmentDialog() {
        _uiState.update { it.copy(showStockAdjustmentDialog = true) }
    }
    
    fun hideStockAdjustmentDialog() {
        _uiState.update { it.copy(showStockAdjustmentDialog = false) }
    }
    
    fun showAllAlerts() {
        _uiState.update { it.copy(showAllAlertsDialog = true) }
    }
    
    fun hideAllAlerts() {
        _uiState.update { it.copy(showAllAlertsDialog = false) }
    }
    
    fun adjustStock(
        productId: Long, 
        newQuantity: Int, 
        adjustmentType: StockAdjustmentType, 
        reason: String, 
        notes: String? = null
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            
            try {
                // Create enhanced reason with adjustment type
                val enhancedReason = "${adjustmentType.displayName}: $reason"
                
                val adjustment = StockAdjustment(
                    productId = productId,
                    newQuantity = newQuantity,
                    reason = enhancedReason,
                    notes = buildString {
                        append("Tipo: ${adjustmentType.description}")
                        if (notes?.isNotBlank() == true) {
                            append("\nNotas: $notes")
                        }
                        append("\nFecha: ${java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", java.util.Locale.getDefault()).format(java.util.Date())}")
                    }
                )
                
                val success = inventoryRepository.adjustStock(adjustment, 1L) // TODO: Get real user ID
                
                if (success) {
                    _uiState.update { it.copy(showStockAdjustmentDialog = false) }
                    refreshData()
                } else {
                    _uiState.update { 
                        it.copy(
                            isLoading = false,
                            error = "Error al ajustar el stock"
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(
                        isLoading = false,
                        error = "Error al ajustar el stock: ${e.message}"
                    )
                }
            }
        }
    }
    
    fun restockProduct(productId: Long, addQuantity: Int, notes: String? = null) {
        viewModelScope.launch {
            android.util.Log.d("InventoryViewModel", "Starting restock for product $productId, adding $addQuantity units")
            _uiState.update { it.copy(isLoading = true) }
            
            try {
                // Get current product stock
                val currentProducts = _allProducts.value
                val product = currentProducts.find { it.productId == productId }
                
                if (product != null) {
                    val newQuantity = product.currentStock + addQuantity
                    val reason = "Reabastecimiento: +$addQuantity unidades"
                    android.util.Log.d("InventoryViewModel", "Product found: ${product.productName}, current: ${product.currentStock}, new: $newQuantity")
                    
                    val adjustment = StockAdjustment(
                        productId = productId,
                        newQuantity = newQuantity,
                        reason = reason,
                        notes = buildString {
                            append("Restock automático")
                            if (notes?.isNotBlank() == true) {
                                append("\nNotas: $notes")
                            }
                            append("\nCantidad agregada: +$addQuantity")
                            append("\nStock anterior: ${product.currentStock}")
                            append("\nStock nuevo: $newQuantity")
                            append("\nFecha: ${java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", java.util.Locale.getDefault()).format(java.util.Date())}")
                        }
                    )
                    
                    android.util.Log.d("InventoryViewModel", "Calling inventoryRepository.adjustStock...")
                    val success = inventoryRepository.adjustStock(adjustment, 1L) // TODO: Get real user ID
                    android.util.Log.d("InventoryViewModel", "adjustStock result: $success")
                    
                    if (success) {
                        android.util.Log.d("InventoryViewModel", "Stock adjustment successful, refreshing data")
                        // NO cerrar el diálogo automáticamente para permitir múltiples restocks
                        _uiState.update { it.copy(isLoading = false) }
                        refreshData()
                    } else {
                        android.util.Log.e("InventoryViewModel", "Stock adjustment failed")
                        _uiState.update { 
                            it.copy(
                                isLoading = false,
                                error = "Error al reabastecer el producto"
                            )
                        }
                    }
                } else {
                    _uiState.update { 
                        it.copy(
                            isLoading = false,
                            error = "Producto no encontrado"
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(
                        isLoading = false,
                        error = "Error al reabastecer: ${e.message}"
                    )
                }
            }
        }
    }
    
    fun markAlertAsViewed(productId: Long) {
        viewModelScope.launch {
            inventoryRepository.markAlertAsViewed(productId)
            // Refresh alerts to update UI
            refreshData()
        }
    }
    
    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}

data class InventoryUiState(
    val isLoading: Boolean = false,
    val stockSummary: StockSummary? = null,
    val alerts: List<StockAlert> = emptyList(),
    val filteredProducts: List<ProductStockDetails> = emptyList(),
    val selectedFilter: String = "all",
    val selectedProductId: Long? = null,
    val showProductDetails: Boolean = false,
    val showStockAdjustmentDialog: Boolean = false,
    val showAllAlertsDialog: Boolean = false,
    val error: String? = null
)