package com.intimocoffee.waiter.feature.products.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.intimocoffee.waiter.feature.products.domain.model.Category
import com.intimocoffee.waiter.feature.products.domain.model.Product
import com.intimocoffee.waiter.feature.products.domain.repository.ProductRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.math.BigDecimal
import javax.inject.Inject

data class ProductsUiState(
    val products: List<Product> = emptyList(),
    val categories: List<Category> = Category.getDefaultCategories(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val showAddProductDialog: Boolean = false,
    val editingProduct: Product? = null,
    val searchQuery: String = "",
    val selectedCategoryId: Long? = null
) {
    val filteredProducts: List<Product>
        get() = products.filter { product ->
            val matchesSearch = if (searchQuery.isBlank()) true else {
                product.name.contains(searchQuery, ignoreCase = true) ||
                product.description?.contains(searchQuery, ignoreCase = true) == true ||
                product.barcode?.contains(searchQuery, ignoreCase = true) == true
            }
            val matchesCategory = selectedCategoryId?.let { product.categoryId == it } ?: true
            matchesSearch && matchesCategory
        }.sortedBy { it.name }
}

@HiltViewModel
class ProductsViewModel @Inject constructor(
    private val productRepository: ProductRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProductsUiState())
    val uiState: StateFlow<ProductsUiState> = _uiState.asStateFlow()

    init {
        loadProducts()
    }

    fun loadProducts() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            try {
                productRepository.getAllActiveProducts().collect { products ->
                    _uiState.value = _uiState.value.copy(
                        products = products,
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Error al cargar los productos: ${e.message}"
                )
            }
        }
    }

    fun searchProducts(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
    }

    fun filterByCategory(categoryId: Long?) {
        _uiState.value = _uiState.value.copy(selectedCategoryId = categoryId)
    }

    fun showAddProductDialog() {
        _uiState.value = _uiState.value.copy(
            showAddProductDialog = true,
            editingProduct = null
        )
    }

    fun showEditProductDialog(product: Product) {
        _uiState.value = _uiState.value.copy(
            showAddProductDialog = true,
            editingProduct = product
        )
    }

    fun hideAddProductDialog() {
        _uiState.value = _uiState.value.copy(
            showAddProductDialog = false,
            editingProduct = null
        )
    }

    suspend fun createProduct(
        name: String,
        description: String,
        price: BigDecimal,
        categoryId: Long,
        stockQuantity: Int?,
        minStockLevel: Int?,
        barcode: String?
    ): Boolean {
        return try {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            val product = Product(
                id = 0L, // Will be generated
                name = name,
                description = description.takeIf { it.isNotBlank() },
                price = price,
                categoryId = categoryId,
                categoryName = _uiState.value.categories.find { it.id == categoryId }?.name,
                imageUrl = null,
                isActive = true,
                stockQuantity = stockQuantity,
                minStockLevel = minStockLevel,
                barcode = barcode?.takeIf { it.isNotBlank() }
            )
            
            val success = productRepository.createProduct(product)
            if (success) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    showAddProductDialog = false
                )
            } else {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Error al crear el producto"
                )
            }
            success
        } catch (e: Exception) {
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                error = "Error: ${e.message}"
            )
            false
        }
    }

    suspend fun updateProduct(product: Product): Boolean {
        return try {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            val success = productRepository.updateProduct(product)
            if (success) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    showAddProductDialog = false,
                    editingProduct = null
                )
            } else {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Error al actualizar el producto"
                )
            }
            success
        } catch (e: Exception) {
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                error = "Error: ${e.message}"
            )
            false
        }
    }

    suspend fun deactivateProduct(productId: Long): Boolean {
        return try {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            val success = productRepository.deactivateProduct(productId)
            if (!success) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Error al desactivar el producto"
                )
            }
            success
        } catch (e: Exception) {
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                error = "Error: ${e.message}"
            )
            false
        }
    }

    fun getCategoryById(categoryId: Long): Category? {
        return _uiState.value.categories.find { it.id == categoryId }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}