package com.intimocoffee.waiter.feature.tables.presentation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.intimocoffee.waiter.feature.orders.domain.model.Order
import com.intimocoffee.waiter.feature.orders.domain.model.OrderItem
import com.intimocoffee.waiter.feature.tables.domain.model.*
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.NumberFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BillSplitMainDialog(
    orders: List<Order>,
    tableName: String,
    onDismiss: () -> Unit,
    onSplitComplete: (BillSplit, PaymentMethod) -> Unit
) {
    var currentStep by remember { mutableIntStateOf(1) }
    var selectedSplitType by remember { mutableStateOf<SplitType?>(null) }
    var personCount by remember { mutableIntStateOf(2) }
    var billSplit by remember { mutableStateOf<BillSplit?>(null) }
    
    val totalAmount = orders.fold(BigDecimal.ZERO) { acc, order -> acc.add(order.total) }
    val totalSubtotal = orders.fold(BigDecimal.ZERO) { acc, order -> acc.add(order.subtotal) }
    val totalTax = orders.fold(BigDecimal.ZERO) { acc, order -> acc.add(order.tax) }
    val allItems = orders.flatMap { it.items }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.9f)
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            "División de Cuenta",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            tableName,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Cerrar")
                    }
                }
                
                Divider()
                
                // Steps indicator
                StepsIndicator(
                    currentStep = currentStep,
                    splitType = selectedSplitType,
                    modifier = Modifier.padding(16.dp)
                )
                
                // Content based on current step
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 16.dp)
                ) {
                    when (currentStep) {
                        1 -> {
                            SplitTypeSelectionStep(
                                totalAmount = totalAmount,
                                itemsCount = allItems.size,
                                selectedType = selectedSplitType,
                                onTypeSelected = { selectedSplitType = it }
                            )
                        }
                        2 -> {
                            when (selectedSplitType) {
                                SplitType.BY_PRODUCTS -> {
                                    ProductAssignmentStep(
                                        items = allItems,
                                        personCount = personCount,
                                        totalSubtotal = totalSubtotal,
                                        totalTax = totalTax,
                                        onPersonCountChanged = { personCount = it },
                                        onBillSplitReady = { billSplit = it }
                                    )
                                }
                                SplitType.EQUAL_PARTS -> {
                                    EqualPartsStep(
                                        personCount = personCount,
                                        totalSubtotal = totalSubtotal,
                                        totalTax = totalTax,
                                        totalAmount = totalAmount,
                                        onPersonCountChanged = { personCount = it },
                                        onBillSplitReady = { billSplit = it }
                                    )
                                }
                                else -> {
                                    // Error state
                                }
                            }
                        }
                        3 -> {
                            billSplit?.let { split ->
                                PaymentConfirmationStep(
                                    billSplit = split,
                                    onConfirm = { paymentMethod ->
                                        onSplitComplete(split, paymentMethod)
                                    }
                                )
                            }
                        }
                    }
                }
                
                // Navigation buttons
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    if (currentStep > 1) {
                        OutlinedButton(
                            onClick = { currentStep-- }
                        ) {
                            Icon(Icons.Default.ArrowBack, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Anterior")
                        }
                    } else {
                        Spacer(Modifier.width(1.dp))
                    }
                    
                    val canProceed = when (currentStep) {
                        1 -> selectedSplitType != null
                        2 -> billSplit != null
                        3 -> false // Final step
                        else -> false
                    }
                    
                    if (currentStep < 3) {
                        Button(
                            onClick = { currentStep++ },
                            enabled = canProceed
                        ) {
                            Text("Siguiente")
                            Spacer(Modifier.width(4.dp))
                            Icon(Icons.Default.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StepsIndicator(
    currentStep: Int,
    splitType: SplitType?,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        val steps = listOf(
            "Tipo" to Icons.Default.AccountBalance,
            when (splitType) {
                SplitType.BY_PRODUCTS -> "Productos" to Icons.Default.Assignment
                SplitType.EQUAL_PARTS -> "División" to Icons.Default.PieChart
                else -> "División" to Icons.Default.PieChart
            },
            "Pago" to Icons.Default.Payment
        )
        
        steps.forEachIndexed { index, (label, icon) ->
            val stepNumber = index + 1
            val isCompleted = currentStep > stepNumber
            val isCurrent = currentStep == stepNumber
            
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (isCompleted) {
                        Surface(
                            shape = MaterialTheme.shapes.small,
                            color = MaterialTheme.colorScheme.primary
                        ) {
                            Icon(
                                Icons.Default.Check,
                                contentDescription = null,
                                modifier = Modifier.size(32.dp),
                                tint = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                    } else {
                        Surface(
                            shape = MaterialTheme.shapes.small,
                            color = if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                            border = if (isCurrent) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                        ) {
                            Icon(
                                icon,
                                contentDescription = null,
                                modifier = Modifier.size(32.dp),
                                tint = if (isCurrent) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                
                Text(
                    label,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal
                )
            }
        }
    }
}

@Composable
private fun SplitTypeSelectionStep(
    totalAmount: BigDecimal,
    itemsCount: Int,
    selectedType: SplitType?,
    onTypeSelected: (SplitType) -> Unit
) {
    val currencyFormat = NumberFormat.getCurrencyInstance(Locale.US)
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            "¿Cómo deseas dividir la cuenta?",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    "Resumen de la cuenta",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Total a dividir:")
                    Text(
                        currencyFormat.format(totalAmount),
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Productos:")
                    Text("$itemsCount items")
                }
            }
        }
        
        // Split type options
        SplitTypeOption(
            title = "Por Selección de Productos",
            description = "Cada persona paga únicamente por lo que consumió. Ideal cuando cada uno pidió cosas diferentes.",
            icon = Icons.Default.Assignment,
            isSelected = selectedType == SplitType.BY_PRODUCTS,
            onClick = { onTypeSelected(SplitType.BY_PRODUCTS) }
        )
        
        SplitTypeOption(
            title = "Por Partes Iguales",
            description = "Divide el total de la cuenta entre todas las personas por igual. Rápido y sencillo.",
            icon = Icons.Default.PieChart,
            isSelected = selectedType == SplitType.EQUAL_PARTS,
            onClick = { onTypeSelected(SplitType.EQUAL_PARTS) }
        )
    }
}

@Composable
private fun SplitTypeOption(
    title: String,
    description: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) 
                MaterialTheme.colorScheme.primaryContainer 
            else 
                MaterialTheme.colorScheme.surface
        ),
        border = if (isSelected) 
            BorderStroke(2.dp, MaterialTheme.colorScheme.primary) 
        else null
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                icon,
                contentDescription = null,
                modifier = Modifier.size(32.dp),
                tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
            )
            
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                )
                Text(
                    description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            if (isSelected) {
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun PersonCountSelectionStep(
    personCount: Int,
    onPersonCountChanged: (Int) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxSize(),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Primera mitad: Selector de personas y nombres
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                "¿Entre cuántas personas dividir?",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        "Número de personas",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        IconButton(
                            onClick = { if (personCount > 2) onPersonCountChanged(personCount - 1) },
                            enabled = personCount > 2
                        ) {
                            Icon(Icons.Default.Remove, contentDescription = "Quitar persona")
                        }
                        
                        Text(
                            personCount.toString(),
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        
                        IconButton(
                            onClick = { if (personCount < 10) onPersonCountChanged(personCount + 1) },
                            enabled = personCount < 10
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Agregar persona")
                        }
                    }
                    
                    Text(
                        "Entre 2 y 10 personas",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    )
                }
            }
            
            // Nombres debajo del selector
            PersonNamesSection(personCount = personCount)
        }
        
        // Segunda mitad: Vista previa de productos
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                "Vista previa",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        "Configuración actual:",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        "• $personCount personas",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        "• División por productos seleccionados",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        "• Asignación individual de items",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}

@Composable
private fun PersonNamesSection(
    personCount: Int
) {
    var personNames by remember { mutableStateOf(List(personCount) { "" }) }
    
    LaunchedEffect(personCount) {
        personNames = List(personCount) { index -> 
            personNames.getOrNull(index) ?: ""
        }
    }
    
    Card {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                "Nombres (opcional)",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            
            personNames.forEachIndexed { index, name ->
                OutlinedTextField(
                    value = name,
                    onValueChange = { newName ->
                        personNames = personNames.toMutableList().apply {
                            set(index, newName)
                        }
                    },
                    label = { Text("Persona ${index + 1}") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@Composable
private fun ProductAssignmentStep(
    items: List<OrderItem>,
    personCount: Int,
    totalSubtotal: BigDecimal,
    totalTax: BigDecimal,
    onPersonCountChanged: (Int) -> Unit,
    onBillSplitReady: (BillSplit) -> Unit
) {
    val currencyFormat = NumberFormat.getCurrencyInstance(Locale.US)
    
    // State for managing product assignments
    var assignments by remember {
        mutableStateOf(
            items.map { item ->
                ProductAssignment(
                    orderItem = item,
                    assignedPersons = emptySet(),
                    splitQuantity = emptyMap()
                )
            }
        )
    }
    
    var personNames by remember { mutableStateOf(List(personCount) { "" }) }
    
    // Update person names list when personCount changes
    LaunchedEffect(personCount) {
        personNames = List(personCount) { index ->
            personNames.getOrNull(index) ?: ""
        }
    }
    
    // Calculate splits whenever assignments change
    LaunchedEffect(assignments, personNames) {
        val personSplits = (1..personCount).map { personNumber ->
            val assignedItems = assignments.filter { assignment ->
                assignment.splitQuantity.containsKey(personNumber)
            }.map { assignment ->
                val quantity = assignment.splitQuantity[personNumber] ?: 0
                if (quantity > 0) {
                    assignment.orderItem.copy(
                        quantity = quantity,
                        subtotal = assignment.orderItem.productPrice.multiply(BigDecimal(quantity))
                    )
                } else null
            }.filterNotNull()
            
            val personSubtotal = assignedItems.fold(BigDecimal.ZERO) { acc, item -> acc.add(item.subtotal) }
            val personTax = personSubtotal.multiply(totalTax.divide(totalSubtotal, 4, RoundingMode.HALF_UP))
            val personTotal = personSubtotal.add(personTax)
            
            PersonSplit(
                personNumber = personNumber,
                name = personNames[personNumber - 1],
                items = assignedItems,
                subtotal = personSubtotal,
                tax = personTax,
                total = personTotal
            )
        }
        
        val billSplit = BillSplit(
            type = SplitType.BY_PRODUCTS,
            personCount = personCount,
            persons = personSplits,
            originalTotal = totalSubtotal.add(totalTax),
            originalSubtotal = totalSubtotal,
            originalTax = totalTax
        )
        
        // Only notify when all items are assigned
        if (assignments.all { it.isFullyAssigned }) {
            onBillSplitReady(billSplit)
        }
    }
    
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Header con título y controles de número de personas
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Asigna productos a cada persona",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Progress indicator compacto
                val totalItems = items.sumOf { it.quantity }
                val assignedItems = assignments.sumOf { it.splitQuantity.values.sum() }
                
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (assignedItems == totalItems) 
                            MaterialTheme.colorScheme.primaryContainer 
                        else 
                            MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Text(
                        "$assignedItems/$totalItems",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (assignedItems == totalItems) 
                            MaterialTheme.colorScheme.primary 
                        else 
                            MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                // Selector de número de personas
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        IconButton(
                            onClick = { if (personCount > 2) onPersonCountChanged(personCount - 1) },
                            enabled = personCount > 2,
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                Icons.Default.Remove, 
                                contentDescription = "Quitar persona",
                                modifier = Modifier.size(14.dp)
                            )
                        }
                        
                        Text(
                            "$personCount",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.widthIn(min = 16.dp),
                            textAlign = TextAlign.Center
                        )
                        
                        IconButton(
                            onClick = { if (personCount < 10) onPersonCountChanged(personCount + 1) },
                            enabled = personCount < 10,
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                Icons.Default.Add, 
                                contentDescription = "Agregar persona",
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
            }
        }
        
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Columna izquierda: Nombres de personas (más compacto)
            Column(
                modifier = Modifier.weight(0.3f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    "Personas",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(personCount) { index ->
                        OutlinedTextField(
                            value = personNames.getOrElse(index) { "" },
                            onValueChange = { newName ->
                                personNames = personNames.toMutableList().apply {
                                    set(index, newName)
                                }
                            },
                            label = { 
                                Text(
                                    "P${index + 1}", 
                                    style = MaterialTheme.typography.labelSmall
                                ) 
                            },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            textStyle = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
            
            // Columna derecha: Lista de productos (más espacio)
            Column(
                modifier = Modifier.weight(0.7f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    "Productos a asignar",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(assignments.size) { index ->
                        CompactProductAssignmentCard(
                            assignment = assignments[index],
                            personCount = personCount,
                            personNames = personNames,
                            onAssignmentChanged = { newAssignment ->
                                assignments = assignments.toMutableList().apply {
                                    set(index, newAssignment)
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CompactProductAssignmentCard(
    assignment: ProductAssignment,
    personCount: Int,
    personNames: List<String>,
    onAssignmentChanged: (ProductAssignment) -> Unit
) {
    val currencyFormat = NumberFormat.getCurrencyInstance(Locale.US)
    
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (assignment.isFullyAssigned)
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
            else if (assignment.unassignedQuantity < assignment.orderItem.quantity)
                MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f)
            else
                MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Product info - más compacto
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        assignment.orderItem.productName,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1
                    )
                    Text(
                        currencyFormat.format(assignment.orderItem.productPrice),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        "Cant: ${assignment.orderItem.quantity}",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Medium
                    )
                    if (assignment.unassignedQuantity > 0) {
                        Text(
                            "Falta: ${assignment.unassignedQuantity}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    } else {
                        Text(
                            "✓ OK",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF4CAF50)
                        )
                    }
                }
            }
            
            // Person assignment controls - layout horizontal compacto
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                (1..personCount).forEach { personNumber ->
                    val currentQuantity = assignment.splitQuantity[personNumber] ?: 0
                    val personName = personNames.getOrNull(personNumber - 1)?.takeIf { it.isNotBlank() }
                        ?: "P$personNumber"
                    
                    Card(
                        modifier = Modifier.weight(1f),
                        colors = CardDefaults.cardColors(
                            containerColor = if (currentQuantity > 0) 
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                            else 
                                MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(4.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                personName,
                                style = MaterialTheme.typography.labelSmall,
                                maxLines = 1,
                                textAlign = TextAlign.Center
                            )
                            
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                IconButton(
                                    onClick = {
                                        if (currentQuantity > 0) {
                                            val newSplitQuantity = assignment.splitQuantity.toMutableMap().apply {
                                                if (currentQuantity == 1) {
                                                    remove(personNumber)
                                                } else {
                                                    put(personNumber, currentQuantity - 1)
                                                }
                                            }
                                            onAssignmentChanged(
                                                assignment.copy(
                                                    splitQuantity = newSplitQuantity,
                                                    assignedPersons = newSplitQuantity.keys
                                                )
                                            )
                                        }
                                    },
                                    enabled = currentQuantity > 0,
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Remove,
                                        contentDescription = "Quitar",
                                        modifier = Modifier.size(12.dp)
                                    )
                                }
                                
                                Text(
                                    currentQuantity.toString(),
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.widthIn(min = 16.dp)
                                )
                                
                                IconButton(
                                    onClick = {
                                        if (assignment.unassignedQuantity > 0) {
                                            val newSplitQuantity = assignment.splitQuantity.toMutableMap().apply {
                                                put(personNumber, currentQuantity + 1)
                                            }
                                            onAssignmentChanged(
                                                assignment.copy(
                                                    splitQuantity = newSplitQuantity,
                                                    assignedPersons = newSplitQuantity.keys
                                                )
                                            )
                                        }
                                    },
                                    enabled = assignment.unassignedQuantity > 0,
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Add,
                                        contentDescription = "Agregar",
                                        modifier = Modifier.size(12.dp)
                                    )
                                }
                            }
                            
                            if (currentQuantity > 0) {
                                Text(
                                    currencyFormat.format(
                                        assignment.orderItem.productPrice.multiply(BigDecimal(currentQuantity))
                                    ),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ProductAssignmentCard(
    assignment: ProductAssignment,
    personCount: Int,
    personNames: List<String>,
    onAssignmentChanged: (ProductAssignment) -> Unit
) {
    val currencyFormat = NumberFormat.getCurrencyInstance(Locale.US)
    
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (assignment.isFullyAssigned)
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
            else if (assignment.unassignedQuantity < assignment.orderItem.quantity)
                MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f)
            else
                MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Product info
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        assignment.orderItem.productName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        "${currencyFormat.format(assignment.orderItem.productPrice)} c/u",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        "Cantidad: ${assignment.orderItem.quantity}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                    if (assignment.unassignedQuantity > 0) {
                        Text(
                            "Faltante: ${assignment.unassignedQuantity}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    } else {
                        Text(
                            "✓ Completo",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF4CAF50)
                        )
                    }
                }
            }
            
            // Person assignment controls
            (1..personCount).forEach { personNumber ->
                val currentQuantity = assignment.splitQuantity[personNumber] ?: 0
                val personName = personNames.getOrNull(personNumber - 1)?.takeIf { it.isNotBlank() }
                    ?: "Persona $personNumber"
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        personName,
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        IconButton(
                            onClick = {
                                if (currentQuantity > 0) {
                                    val newSplitQuantity = assignment.splitQuantity.toMutableMap().apply {
                                        if (currentQuantity == 1) {
                                            remove(personNumber)
                                        } else {
                                            put(personNumber, currentQuantity - 1)
                                        }
                                    }
                                    onAssignmentChanged(
                                        assignment.copy(
                                            splitQuantity = newSplitQuantity,
                                            assignedPersons = newSplitQuantity.keys
                                        )
                                    )
                                }
                            },
                            enabled = currentQuantity > 0
                        ) {
                            Icon(
                                Icons.Default.Remove,
                                contentDescription = "Quitar",
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        
                        Text(
                            currentQuantity.toString(),
                            modifier = Modifier.widthIn(min = 24.dp),
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                        
                        IconButton(
                            onClick = {
                                if (assignment.unassignedQuantity > 0) {
                                    val newSplitQuantity = assignment.splitQuantity.toMutableMap().apply {
                                        put(personNumber, currentQuantity + 1)
                                    }
                                    onAssignmentChanged(
                                        assignment.copy(
                                            splitQuantity = newSplitQuantity,
                                            assignedPersons = newSplitQuantity.keys
                                        )
                                    )
                                }
                            },
                            enabled = assignment.unassignedQuantity > 0
                        ) {
                            Icon(
                                Icons.Default.Add,
                                contentDescription = "Agregar",
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        
                        if (currentQuantity > 0) {
                            Text(
                                "= ${currencyFormat.format(
                                    assignment.orderItem.productPrice.multiply(BigDecimal(currentQuantity))
                                )}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EqualPartsStep(
    personCount: Int,
    totalSubtotal: BigDecimal,
    totalTax: BigDecimal,
    totalAmount: BigDecimal,
    onPersonCountChanged: (Int) -> Unit,
    onBillSplitReady: (BillSplit) -> Unit
) {
    val currencyFormat = NumberFormat.getCurrencyInstance(Locale.US)
    var personNames by remember { mutableStateOf(List(personCount) { "" }) }
    
    // Update person names list when personCount changes
    LaunchedEffect(personCount) {
        personNames = List(personCount) { index ->
            personNames.getOrNull(index) ?: ""
        }
    }
    
    // Calculate equal splits
    LaunchedEffect(personCount, personNames) {
        val perPersonSubtotal = totalSubtotal.divide(BigDecimal(personCount), 2, RoundingMode.HALF_UP)
        val perPersonTax = totalTax.divide(BigDecimal(personCount), 2, RoundingMode.HALF_UP)
        val perPersonTotal = totalAmount.divide(BigDecimal(personCount), 2, RoundingMode.HALF_UP)
        
        val personSplits = (1..personCount).map { personNumber ->
            PersonSplit(
                personNumber = personNumber,
                name = personNames[personNumber - 1],
                subtotal = perPersonSubtotal,
                tax = perPersonTax,
                total = perPersonTotal
            )
        }
        
        val billSplit = BillSplit(
            type = SplitType.EQUAL_PARTS,
            personCount = personCount,
            persons = personSplits,
            originalTotal = totalAmount,
            originalSubtotal = totalSubtotal,
            originalTax = totalTax
        )
        
        onBillSplitReady(billSplit)
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header con título y selector de personas
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "División por partes iguales",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            
            // Selector de número de personas
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                )
            ) {
                Row(
                    modifier = Modifier.padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    IconButton(
                        onClick = { if (personCount > 2) onPersonCountChanged(personCount - 1) },
                        enabled = personCount > 2
                    ) {
                        Icon(Icons.Default.Remove, contentDescription = "Quitar persona")
                    }
                    
                    Text(
                        "$personCount personas",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    
                    IconButton(
                        onClick = { if (personCount < 10) onPersonCountChanged(personCount + 1) },
                        enabled = personCount < 10
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Agregar persona")
                    }
                }
            }
        }
        
        // Summary card
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    "Resumen de división",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Total original:")
                    Text(
                        currencyFormat.format(totalAmount),
                        fontWeight = FontWeight.Bold
                    )
                }
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Personas:")
                    Text(
                        "$personCount",
                        fontWeight = FontWeight.Bold
                    )
                }
                
                Divider()
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        "Cada persona paga:",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        currencyFormat.format(totalAmount.divide(BigDecimal(personCount), 2, RoundingMode.HALF_UP)),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
        
        // Person names input
        Card {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    "Nombres de las personas (opcional)",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                
                personNames.forEachIndexed { index, name ->
                    OutlinedTextField(
                        value = name,
                        onValueChange = { newName ->
                            personNames = personNames.toMutableList().apply {
                                set(index, newName)
                            }
                        },
                        label = { Text("Persona ${index + 1}") },
                        modifier = Modifier.fillMaxWidth(),
                        leadingIcon = {
                            Icon(Icons.Default.Person, contentDescription = null)
                        },
                        singleLine = true
                    )
                }
            }
        }
    }
}

@Composable
private fun PaymentConfirmationStep(
    billSplit: BillSplit,
    onConfirm: (PaymentMethod) -> Unit
) {
    val currencyFormat = NumberFormat.getCurrencyInstance(Locale.US)
    var selectedPaymentMethod by remember { mutableStateOf(PaymentMethod.CASH) }
    var showPaymentMethodMenu by remember { mutableStateOf(false) }
    
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Header compacto
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Confirmar división",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Text(
                    "${billSplit.persons.size} personas • ${currencyFormat.format(billSplit.originalTotal)}",
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
        
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Lista de personas (lado izquierdo, más compacto)
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(billSplit.persons.size) { index ->
                    val person = billSplit.persons[index]
                    CompactPersonSummaryCard(
                        person = person,
                        billSplit = billSplit,
                        currencyFormat = currencyFormat
                    )
                }
            }
            
            // Panel de pago (lado derecho)
            Column(
                modifier = Modifier.weight(0.4f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    "Método de pago",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                
                Card {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = selectedPaymentMethod.displayName,
                            onValueChange = { },
                            readOnly = true,
                            trailingIcon = { 
                                IconButton(onClick = { showPaymentMethodMenu = !showPaymentMethodMenu }) {
                                    Icon(Icons.Default.ArrowDropDown, contentDescription = "Seleccionar")
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showPaymentMethodMenu = !showPaymentMethodMenu },
                            textStyle = MaterialTheme.typography.bodySmall
                        )
                        
                        DropdownMenu(
                            expanded = showPaymentMethodMenu,
                            onDismissRequest = { showPaymentMethodMenu = false }
                        ) {
                            PaymentMethod.values().forEach { method ->
                                DropdownMenuItem(
                                    text = { 
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Icon(
                                                when (method) {
                                                    PaymentMethod.CASH -> Icons.Default.AttachMoney
                                                    PaymentMethod.CARD -> Icons.Default.CreditCard
                                                    PaymentMethod.TRANSFER -> Icons.Default.AccountBalance
                                                    PaymentMethod.MIXED -> Icons.Default.Payment
                                                },
                                                contentDescription = null,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Text(
                                                method.displayName,
                                                style = MaterialTheme.typography.bodySmall
                                            )
                                        }
                                    },
                                    onClick = {
                                        selectedPaymentMethod = method
                                        showPaymentMethodMenu = false
                                    }
                                )
                            }
                        }
                        
                        Button(
                            onClick = { onConfirm(selectedPaymentMethod) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Payment, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(
                                "Procesar",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
                
                // Resumen total compacto
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            "Resumen",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "Tipo: ${when (billSplit.type) {
                                SplitType.BY_PRODUCTS -> "Por productos"
                                SplitType.EQUAL_PARTS -> "Partes iguales"
                            }}",
                            style = MaterialTheme.typography.labelSmall
                        )
                        Text(
                            "Total: ${currencyFormat.format(billSplit.originalTotal)}",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CompactPersonSummaryCard(
    person: com.intimocoffee.waiter.feature.tables.domain.model.PersonSplit,
    billSplit: BillSplit,
    currencyFormat: NumberFormat
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    person.displayName,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1
                )
                Text(
                    currencyFormat.format(person.total),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            
            if (billSplit.type == SplitType.BY_PRODUCTS && person.items.isNotEmpty()) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    person.items.forEach { item -> // Mostrar TODOS los productos
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "${item.quantity}x ${item.productName}",
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.weight(1f, false), // No expandir completamente
                                maxLines = 1,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.width(4.dp)) // Espacio mínimo
                            Text(
                                currencyFormat.format(item.subtotal),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }
    }
}
