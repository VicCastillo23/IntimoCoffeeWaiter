package com.intimocoffee.waiter.feature.tables.presentation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.intimocoffee.waiter.feature.orders.domain.model.Order
import com.intimocoffee.waiter.feature.orders.domain.model.OrderItem
import com.intimocoffee.waiter.feature.products.domain.model.Product
import com.intimocoffee.waiter.feature.tables.domain.model.Table
import java.math.BigDecimal
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*
import kotlinx.datetime.*

enum class PaymentMethod(val displayName: String) {
    CASH("Efectivo"),
    CARD("Tarjeta"),
    TRANSFER("Transferencia"),
    MIXED("Mixto")
}

data class SimpleTicket(
    val orderNumber: String,
    val tableName: String,
    val items: List<SimpleTicket.Item>,
    val subtotal: String,
    val tax: String,
    val total: String,
    val paymentMethod: PaymentMethod,
    val timestamp: String
) {
    data class Item(
        val name: String,
        val quantity: Int,
        val unitPrice: String,
        val total: String,
        val notes: String?
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CloseTableDialog(
    order: Order,
    table: Table,
    onDismiss: () -> Unit,
    onConfirm: (PaymentMethod) -> Unit,
    onDivideAccount: () -> Unit = {}
) {
    var selectedPaymentMethod by remember { mutableStateOf(PaymentMethod.CASH) }
    var showPaymentMethodMenu by remember { mutableStateOf(false) }
    
    val currencyFormat = NumberFormat.getCurrencyInstance(Locale.US)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Default.Receipt, contentDescription = null)
                Text("Cerrar ${table.displayName}")
            }
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Order summary
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
                            "Resumen de la Cuenta",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Subtotal:")
                            Text(currencyFormat.format(order.subtotal))
                        }
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Impuestos:")
                            Text(currencyFormat.format(order.tax))
                        }
                        
                        if (order.discount.compareTo(java.math.BigDecimal.ZERO) > 0) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Descuento:")
                                Text(
                                    "-${currencyFormat.format(order.discount)}",
                                    color = Color(0xFF4CAF50)
                                )
                            }
                        }
                        
                        Divider()
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                "Total a pagar:",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                currencyFormat.format(order.total),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
                
                // Payment method selection
                Column {
                    Text(
                        "Método de pago",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    ExposedDropdownMenuBox(
                        expanded = showPaymentMethodMenu,
                        onExpandedChange = { showPaymentMethodMenu = it }
                    ) {
                        OutlinedTextField(
                            value = selectedPaymentMethod.displayName,
                            onValueChange = { },
                            readOnly = true,
                            trailingIcon = { 
                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = showPaymentMethodMenu) 
                            },
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth()
                        )
                        
                        ExposedDropdownMenu(
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
                                                modifier = Modifier.size(20.dp)
                                            )
                                            Text(method.displayName)
                                        }
                                    },
                                    onClick = {
                                        selectedPaymentMethod = method
                                        showPaymentMethodMenu = false
                                    }
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = { onDivideAccount() }
                ) {
                    Icon(Icons.Default.AccountBalance, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Dividir Cuenta")
                }
                
                Button(
                    onClick = { onConfirm(selectedPaymentMethod) }
                ) {
                    Text("Procesar Pago")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}

@Composable
fun TicketDialog(
    ticket: SimpleTicket,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.9f)
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
                    Text(
                        "Ticket de Venta",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Cerrar")
                    }
                }
                
                Divider()
                
                // Ticket content
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        TicketContent(ticket = ticket)
                    }
                }
            }
        }
    }
}

@Composable
fun TicketContent(ticket: SimpleTicket) {
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Business header
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "☕ ÍNTIMO CAFÉ",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                Text(
                    "Sistema POS - Gestión de Restaurante",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                    textAlign = TextAlign.Center
                )
            }
        }
        
        // Order info
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text("Orden: ${ticket.orderNumber}", fontWeight = FontWeight.SemiBold)
                Text("Mesa: ${ticket.tableName}", style = MaterialTheme.typography.bodyMedium)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("Fecha: ${ticket.timestamp}", style = MaterialTheme.typography.bodySmall)
            }
        }
        
        Divider()
        
        // Items
        Text(
            "PRODUCTOS",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        
        ticket.items.forEach { item ->
            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        "${item.quantity}x ${item.name}",
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        item.total,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        "    @ ${item.unitPrice} c/u",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                item.notes?.let { notes ->
                    Text(
                        "    Notas: $notes",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                    )
                }
            }
        }
        
        Divider()
        
        // Totals
        Column(
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Subtotal:")
                Text(ticket.subtotal)
            }
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Impuestos:")
                Text(ticket.tax)
            }
            
            Divider(thickness = 2.dp)
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    "TOTAL:",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    ticket.total,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        
        // Payment method
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        when (ticket.paymentMethod) {
                            PaymentMethod.CASH -> Icons.Default.AttachMoney
                            PaymentMethod.CARD -> Icons.Default.CreditCard
                            PaymentMethod.TRANSFER -> Icons.Default.AccountBalance
                            PaymentMethod.MIXED -> Icons.Default.Payment
                        },
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Text("Método de pago:")
                }
                Text(
                    ticket.paymentMethod.displayName,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
        
        Divider()
        
        // Footer
        Text(
            "¡Gracias por su visita!",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
            fontWeight = FontWeight.Medium
        )
        
        Text(
            "Vuelva pronto ☕",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun MultipleTicketsDialog(
    tickets: List<SimpleTicket>,
    onDismiss: () -> Unit
) {
    var currentTicketIndex by remember { mutableIntStateOf(0) }
    
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
                // Header con navegación entre tickets
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            "Tickets de División",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                        
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer
                            )
                        ) {
                            Text(
                                "${currentTicketIndex + 1}/${tickets.size}",
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        IconButton(
                            onClick = { 
                                if (currentTicketIndex > 0) {
                                    currentTicketIndex--
                                }
                            },
                            enabled = currentTicketIndex > 0
                        ) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Ticket anterior")
                        }
                        
                        IconButton(
                            onClick = { 
                                if (currentTicketIndex < tickets.size - 1) {
                                    currentTicketIndex++
                                }
                            },
                            enabled = currentTicketIndex < tickets.size - 1
                        ) {
                            Icon(Icons.Default.ArrowForward, contentDescription = "Ticket siguiente")
                        }
                        
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = "Cerrar")
                        }
                    }
                }
                
                Divider()
                
                // Contenido del ticket actual
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        if (tickets.isNotEmpty()) {
                            TicketContent(ticket = tickets[currentTicketIndex])
                        }
                    }
                }
                
                // Navegación inferior
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    if (currentTicketIndex > 0) {
                        OutlinedButton(
                            onClick = { currentTicketIndex-- }
                        ) {
                            Icon(Icons.Default.ArrowBack, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Anterior")
                        }
                    } else {
                        Spacer(Modifier.width(1.dp))
                    }
                    
                    Text(
                        "Ticket ${currentTicketIndex + 1} de ${tickets.size}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    if (currentTicketIndex < tickets.size - 1) {
                        Button(
                            onClick = { currentTicketIndex++ }
                        ) {
                            Text("Siguiente")
                            Spacer(Modifier.width(4.dp))
                            Icon(Icons.Default.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp))
                        }
                    } else {
                        Button(
                            onClick = onDismiss
                        ) {
                            Text("Finalizar")
                            Spacer(Modifier.width(4.dp))
                            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AddProductToOrderDialog(
    orders: List<Order>,
    availableProducts: List<Product>,
    onDismiss: () -> Unit,
    onConfirm: (Long, List<OrderItem>) -> Unit
) {
    var selectedOrderId by remember { mutableStateOf(orders.firstOrNull()?.id ?: 0L) }
    var selectedProducts by remember { mutableStateOf<List<Pair<Product, Int>>>(emptyList()) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Text("Agregar productos")
            }
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Order selection
                if (orders.size > 1) {
                    Column {
                        Text(
                            "Seleccionar orden",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        LazyColumn(
                            modifier = Modifier.heightIn(max = 120.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            items(orders) { order ->
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { selectedOrderId = order.id },
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (selectedOrderId == order.id)
                                            MaterialTheme.colorScheme.primaryContainer
                                        else MaterialTheme.colorScheme.surface
                                    ),
                                    border = if (selectedOrderId == order.id)
                                        BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
                                    else null
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text(
                                                "Orden #${order.orderNumber}",
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Medium
                                            )
                                            Text(
                                                order.status.displayName,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        
                                        if (selectedOrderId == order.id) {
                                            Icon(
                                                Icons.Default.CheckCircle,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp)
                        ) {
                            Text(
                                "Agregar a: Orden #${orders.first().orderNumber}",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                orders.first().status.displayName,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                
                // Product selection with quantities
                Column {
                    Text(
                        "Seleccionar productos",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    LazyColumn(
                        modifier = Modifier.heightIn(max = 200.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(availableProducts) { product ->
                            val currentQuantity = selectedProducts.find { it.first.id == product.id }?.second ?: 0
                            
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = if (currentQuantity > 0)
                                        MaterialTheme.colorScheme.primaryContainer
                                    else MaterialTheme.colorScheme.surface
                                )
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Text(
                                                product.name,
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Medium
                                            )
                                            Text(
                                                NumberFormat.getCurrencyInstance(Locale.US).format(product.price),
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                        
                                        // Quantity controls
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            IconButton(
                                                onClick = {
                                                    if (currentQuantity > 0) {
                                                        val newQuantity = currentQuantity - 1
                                                        selectedProducts = if (newQuantity == 0) {
                                                            selectedProducts.filter { it.first.id != product.id }
                                                        } else {
                                                            selectedProducts.map { 
                                                                if (it.first.id == product.id) 
                                                                    it.copy(second = newQuantity) 
                                                                else it 
                                                            }
                                                        }
                                                    }
                                                },
                                                enabled = currentQuantity > 0
                                            ) {
                                                Icon(Icons.Default.Remove, contentDescription = "Quitar")
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
                                                    val newQuantity = currentQuantity + 1
                                                    selectedProducts = if (currentQuantity == 0) {
                                                        selectedProducts + (product to newQuantity)
                                                    } else {
                                                        selectedProducts.map {
                                                            if (it.first.id == product.id)
                                                                it.copy(second = newQuantity)
                                                            else it
                                                        }
                                                    }
                                                }
                                            ) {
                                                Icon(Icons.Default.Add, contentDescription = "Agregar")
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                
                // Show selection summary if any products selected
                if (selectedProducts.isNotEmpty()) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.tertiaryContainer
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                "Productos seleccionados:",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                            
                            selectedProducts.forEach { (product, quantity) ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        "${product.name} x$quantity",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                    Text(
                                        NumberFormat.getCurrencyInstance(Locale.US).format(
                                            product.price.multiply(BigDecimal(quantity))
                                        ),
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                            
                            Divider()
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    "Subtotal:",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    NumberFormat.getCurrencyInstance(Locale.US).format(
                                        selectedProducts.fold(BigDecimal.ZERO) { acc, (product, quantity) ->
                                            acc.add(product.price.multiply(BigDecimal(quantity)))
                                        }
                                    ),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val orderItems = selectedProducts.map { (product, quantity) ->
                        val raw = product.rawId.trim()
                        val parsed = raw.toLongOrNull()
                        val productDatabaseId = if (raw.isNotEmpty() && parsed == null) raw else null
                        val productId = product.id.takeIf { it != 0L } ?: parsed ?: 0L
                        OrderItem(
                            id = 0L,
                            orderId = selectedOrderId,
                            productId = productId,
                            productDatabaseId = productDatabaseId,
                            productName = product.name,
                            productPrice = product.price,
                            quantity = quantity,
                            subtotal = product.price.multiply(BigDecimal(quantity)),
                            notes = null,
                            categoryId = product.categoryId,
                            createdAt = kotlinx.datetime.Clock.System.now().toLocalDateTime(kotlinx.datetime.TimeZone.currentSystemDefault())
                        )
                    }
                    onConfirm(selectedOrderId, orderItems)
                },
                enabled = selectedProducts.isNotEmpty()
            ) {
                Text("Agregar productos (${selectedProducts.size})")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MultipleOrdersCloseTableDialog(
    orders: List<Order>,
    table: Table,
    onDismiss: () -> Unit,
    onConfirm: (PaymentMethod) -> Unit,
    onDivideAccount: () -> Unit
) {
    var selectedPaymentMethod by remember { mutableStateOf(PaymentMethod.CASH) }
    var showPaymentMethodMenu by remember { mutableStateOf(false) }
    
    val currencyFormat = NumberFormat.getCurrencyInstance(Locale.US)
    
    // Calculate totals from all orders
    val totalSubtotal = orders.fold(BigDecimal.ZERO) { acc, order -> acc.add(order.subtotal) }
    val totalTax = orders.fold(BigDecimal.ZERO) { acc, order -> acc.add(order.tax) }
    val totalDiscount = orders.fold(BigDecimal.ZERO) { acc, order -> acc.add(order.discount) }
    val grandTotal = orders.fold(BigDecimal.ZERO) { acc, order -> acc.add(order.total) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Default.Receipt, contentDescription = null)
                Text("Cerrar ${table.displayName}")
            }
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Multiple orders summary
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
                            "Resumen de ${orders.size} orden${if (orders.size > 1) "es" else ""}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        
                        // Show each order briefly
                        orders.sortedByDescending { it.createdAt }.forEach { order ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    "Orden #${order.orderNumber}",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Text(
                                    currencyFormat.format(order.total),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                        
                        Divider()
                        
                        // Totals
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Subtotal:")
                            Text(currencyFormat.format(totalSubtotal))
                        }
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Impuestos:")
                            Text(currencyFormat.format(totalTax))
                        }
                        
                        if (totalDiscount.compareTo(BigDecimal.ZERO) > 0) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Descuento:")
                                Text(
                                    "-${currencyFormat.format(totalDiscount)}",
                                    color = Color(0xFF4CAF50)
                                )
                            }
                        }
                        
                        Divider()
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                "Total a pagar:",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                currencyFormat.format(grandTotal),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
                
                // Payment method selection
                Column {
                    Text(
                        "Método de pago",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    ExposedDropdownMenuBox(
                        expanded = showPaymentMethodMenu,
                        onExpandedChange = { showPaymentMethodMenu = it }
                    ) {
                        OutlinedTextField(
                            value = selectedPaymentMethod.displayName,
                            onValueChange = { },
                            readOnly = true,
                            trailingIcon = { 
                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = showPaymentMethodMenu) 
                            },
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth()
                        )
                        
                        ExposedDropdownMenu(
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
                                                modifier = Modifier.size(20.dp)
                                            )
                                            Text(method.displayName)
                                        }
                                    },
                                    onClick = {
                                        selectedPaymentMethod = method
                                        showPaymentMethodMenu = false
                                    }
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = { onDivideAccount() }
                ) {
                    Icon(Icons.Default.AccountBalance, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Dividir Cuenta")
                }
                
                Button(
                    onClick = { onConfirm(selectedPaymentMethod) }
                ) {
                    Text("Procesar Pago")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}
