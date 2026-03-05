package com.intimocoffee.waiter.feature.tables.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.intimocoffee.waiter.feature.tables.domain.model.Table
import com.intimocoffee.waiter.feature.tables.domain.model.TableStatus
import com.intimocoffee.waiter.feature.tables.domain.repository.TableRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TablesUiState(
    val tables: List<Table> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val showAddTableDialog: Boolean = false,
    val editingTable: Table? = null,
    val searchQuery: String = "",
    val selectedZone: String? = null
) {
    val availableZones: List<String>
        get() = tables.map { it.zone }.distinct().sorted()
    
    val filteredTables: List<Table>
        get() = tables.filter { table ->
            val matchesSearch = if (searchQuery.isBlank()) true else {
                table.number.toString().contains(searchQuery, ignoreCase = true) ||
                table.name?.contains(searchQuery, ignoreCase = true) == true ||
                table.zone.contains(searchQuery, ignoreCase = true)
            }
            val matchesZone = selectedZone?.let { table.zone == it } ?: true
            matchesSearch && matchesZone
        }.sortedBy { it.number }
}

@HiltViewModel
class TablesViewModel @Inject constructor(
    private val tableRepository: TableRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(TablesUiState())
    val uiState: StateFlow<TablesUiState> = _uiState.asStateFlow()

    init {
        loadTables()
    }

    fun loadTables() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            try {
                tableRepository.getAllActiveTables().collect { tables ->
                    _uiState.value = _uiState.value.copy(
                        tables = tables,
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Error al cargar las mesas: ${e.message}"
                )
            }
        }
    }

    fun searchTables(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
    }

    fun filterByZone(zone: String?) {
        _uiState.value = _uiState.value.copy(selectedZone = zone)
    }

    fun showAddTableDialog() {
        _uiState.value = _uiState.value.copy(
            showAddTableDialog = true,
            editingTable = null
        )
    }

    fun showEditTableDialog(table: Table) {
        _uiState.value = _uiState.value.copy(
            showAddTableDialog = true,
            editingTable = table
        )
    }

    fun hideAddTableDialog() {
        _uiState.value = _uiState.value.copy(
            showAddTableDialog = false,
            editingTable = null
        )
    }

    suspend fun createTable(
        number: Int,
        name: String,
        capacity: Int,
        zone: String,
        positionX: Float = 0f,
        positionY: Float = 0f
    ): Boolean {
        return try {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            val table = Table(
                id = 0L, // Will be generated
                number = number,
                name = name.takeIf { it.isNotBlank() },
                capacity = capacity,
                zone = zone,
                status = TableStatus.FREE,
                currentOrderId = null,
                positionX = positionX,
                positionY = positionY,
                isActive = true
            )
            
            val success = tableRepository.createTable(table)
            if (success) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    showAddTableDialog = false
                )
            } else {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Error al crear la mesa"
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

    suspend fun updateTable(table: Table): Boolean {
        return try {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            val success = tableRepository.updateTable(table)
            if (success) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    showAddTableDialog = false,
                    editingTable = null
                )
            } else {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Error al actualizar la mesa"
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

    suspend fun deactivateTable(tableId: Long): Boolean {
        return try {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            val success = tableRepository.deactivateTable(tableId)
            if (!success) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Error al desactivar la mesa"
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

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}