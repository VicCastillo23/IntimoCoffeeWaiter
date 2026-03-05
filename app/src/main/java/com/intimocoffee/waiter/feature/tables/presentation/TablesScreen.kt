package com.intimocoffee.waiter.feature.tables.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.runtime.collectAsState
import com.intimocoffee.waiter.feature.tables.domain.model.Table
import com.intimocoffee.waiter.feature.tables.domain.model.TableStatus
import com.intimocoffee.waiter.ui.theme.IntimoCoffeeAppTheme
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TablesScreen(
    viewModel: TablesViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val scope = rememberCoroutineScope()
    
    // Internal navigation state for table details
    var selectedTableId by remember { mutableStateOf<Long?>(null) }
    
    LaunchedEffect(Unit) {
        viewModel.loadTables()
    }
    
    if (selectedTableId != null) {
        // Show table details screen
        SimpleTableDetailsScreen(
            tableId = selectedTableId!!,
            onNavigateBack = { selectedTableId = null },
            onTableClosed = { 
                selectedTableId = null
                viewModel.loadTables() // Refresh tables list
            }
        )
        return
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
                text = "Gestión de Mesas",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold
            )
            
            FilledTonalButton(
                onClick = { viewModel.showAddTableDialog() }
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("Nueva Mesa")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Zone Filter Chips
        if (uiState.availableZones.isNotEmpty()) {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    FilterChip(
                        onClick = { viewModel.filterByZone(null) },
                        label = { Text("Todas") },
                        selected = uiState.selectedZone == null
                    )
                }
                
                items(uiState.availableZones) { zone ->
                    FilterChip(
                        onClick = { viewModel.filterByZone(zone) },
                        label = { Text(zone) },
                        selected = uiState.selectedZone == zone,
                        leadingIcon = if (uiState.selectedZone == zone) {
                            { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                        } else null
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Tables Grid
        when {
            uiState.isLoading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            
            uiState.filteredTables.isEmpty() -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Default.TableRestaurant,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = if (uiState.selectedZone != null) {
                                "No se encontraron mesas en esta zona"
                            } else {
                                "No hay mesas registradas"
                            },
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            
            else -> {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 200.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(uiState.filteredTables) { table ->
                        TableCard(
                            table = table,
                            onViewDetailsClick = { selectedTableId = table.id },
                            onEditClick = { viewModel.showEditTableDialog(table) },
                            onDeactivateClick = {
                                scope.launch {
                                    viewModel.deactivateTable(table.id)
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    // Add/Edit Table Dialog
    if (uiState.showAddTableDialog) {
        AddTableDialog(
            editingTable = uiState.editingTable,
            availableZones = uiState.availableZones,
            onDismiss = { viewModel.hideAddTableDialog() },
            onSave = { number, name, capacity, zone ->
                scope.launch {
                    val editingTable = uiState.editingTable
                    if (editingTable != null) {
                        val updatedTable = editingTable.copy(
                            number = number,
                            name = name.takeIf { it.isNotBlank() },
                            capacity = capacity,
                            zone = zone
                        )
                        viewModel.updateTable(updatedTable)
                    } else {
                        viewModel.createTable(number, name, capacity, zone)
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
private fun TableCard(
    table: Table,
    onViewDetailsClick: () -> Unit,
    onEditClick: () -> Unit,
    onDeactivateClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1.2f),
        colors = CardDefaults.cardColors(
            containerColor = when (table.status) {
                TableStatus.FREE -> MaterialTheme.colorScheme.surface
                TableStatus.OCCUPIED -> MaterialTheme.colorScheme.errorContainer
                TableStatus.RESERVED -> MaterialTheme.colorScheme.secondaryContainer
                TableStatus.OUT_OF_SERVICE -> MaterialTheme.colorScheme.surfaceVariant
            }
        ),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            when (table.status) {
                TableStatus.FREE -> MaterialTheme.colorScheme.primary
                TableStatus.OCCUPIED -> MaterialTheme.colorScheme.error
                TableStatus.RESERVED -> MaterialTheme.colorScheme.secondary
                TableStatus.OUT_OF_SERVICE -> MaterialTheme.colorScheme.outline
            }
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Header with actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = table.displayName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "Mesa ${table.number}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Column {
                    Row {
                        // View details button for occupied tables
                        if (table.status == TableStatus.OCCUPIED) {
                            IconButton(
                                onClick = onViewDetailsClick,
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    Icons.Default.Visibility,
                                    contentDescription = "Ver detalles",
                                    modifier = Modifier.size(16.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                        
                        IconButton(
                            onClick = onEditClick,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                Icons.Default.Edit,
                                contentDescription = "Editar",
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        IconButton(
                            onClick = onDeactivateClick,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "Eliminar",
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }

            // Table info
            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.People,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${table.capacity} personas",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.LocationOn,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = table.zone,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Status badge
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = when (table.status) {
                    TableStatus.FREE -> Color.Green.copy(alpha = 0.1f)
                    TableStatus.OCCUPIED -> Color.Red.copy(alpha = 0.1f)
                    TableStatus.RESERVED -> Color(0xFFFF9800).copy(alpha = 0.1f)
                    TableStatus.OUT_OF_SERVICE -> Color.Gray.copy(alpha = 0.1f)
                },
                shape = RoundedCornerShape(4.dp)
            ) {
                Text(
                    text = table.status.getDisplayName(),
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center,
                    color = when (table.status) {
                        TableStatus.FREE -> Color(0xFF2E7D32)
                        TableStatus.OCCUPIED -> Color(0xFFD32F2F)
                        TableStatus.RESERVED -> Color(0xFFEF6C00)
                        TableStatus.OUT_OF_SERVICE -> Color(0xFF616161)
                    },
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddTableDialog(
    editingTable: Table?,
    availableZones: List<String>,
    onDismiss: () -> Unit,
    onSave: (number: Int, name: String, capacity: Int, zone: String) -> Unit
) {
    var number by remember { mutableStateOf(editingTable?.number?.toString() ?: "") }
    var name by remember { mutableStateOf(editingTable?.name ?: "") }
    var capacity by remember { mutableStateOf(editingTable?.capacity?.toString() ?: "4") }
    var zone by remember { mutableStateOf(editingTable?.zone ?: "Principal") }
    var expanded by remember { mutableStateOf(false) }

    val isEditing = editingTable != null
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { 
            Text(if (isEditing) "Editar Mesa" else "Nueva Mesa") 
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedTextField(
                    value = number,
                    onValueChange = { number = it },
                    label = { Text("Número de Mesa *") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    isError = number.toIntOrNull() == null || number.toInt() <= 0
                )

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nombre (opcional)") },
                    placeholder = { Text("ej: Mesa VIP, Mesa Terraza") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = capacity,
                    onValueChange = { capacity = it },
                    label = { Text("Capacidad *") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    isError = capacity.toIntOrNull() == null || capacity.toInt() <= 0
                )

                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded }
                ) {
                    OutlinedTextField(
                        value = zone,
                        onValueChange = { },
                        readOnly = true,
                        label = { Text("Zona *") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                    )
                    
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        val zones = if (availableZones.isNotEmpty()) availableZones else listOf("Principal", "Terraza", "VIP", "Barra")
                        
                        zones.forEach { zoneOption ->
                            DropdownMenuItem(
                                text = { Text(zoneOption) },
                                onClick = {
                                    zone = zoneOption
                                    expanded = false
                                }
                            )
                        }
                        
                        // Option to add new zone
                        DropdownMenuItem(
                            text = { Text("+ Nueva zona", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)) },
                            onClick = {
                                // For now, just close the dropdown
                                expanded = false
                            }
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val tableNumber = number.toIntOrNull()
                    val tableCapacity = capacity.toIntOrNull()
                    if (tableNumber != null && tableNumber > 0 && tableCapacity != null && tableCapacity > 0) {
                        onSave(tableNumber, name, tableCapacity, zone)
                    }
                },
                enabled = number.toIntOrNull()?.let { it > 0 } == true && 
                         capacity.toIntOrNull()?.let { it > 0 } == true &&
                         zone.isNotBlank()
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
private fun TablesScreenPreview() {
    IntimoCoffeeAppTheme {
        TablesScreen()
    }
}