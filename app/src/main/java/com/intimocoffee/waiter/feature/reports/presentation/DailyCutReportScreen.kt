package com.intimocoffee.waiter.feature.reports.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import com.intimocoffee.waiter.feature.reports.domain.model.*
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DailyCutReportDialog(
    onDismiss: () -> Unit,
    viewModel: DailyCutReportViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    
    LaunchedEffect(Unit) {
        viewModel.generateDailyCutReport()
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false
        )
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp, 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Corte Diario",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                    
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Cerrar"
                        )
                    }
                }

                if (uiState.isLoading) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            CircularProgressIndicator()
                            Text(
                                text = "Generando reporte...",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                } else if (uiState.error != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Error,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(48.dp)
                            )
                            Text(
                                text = "Error al generar reporte",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.error
                            )
                            Text(
                                text = uiState.error ?: "Error desconocido",
                                style = MaterialTheme.typography.bodyMedium,
                                textAlign = TextAlign.Center
                            )
                            Button(
                                onClick = { viewModel.generateDailyCutReport() }
                            ) {
                                Text("Reintentar")
                            }
                        }
                    }
                } else {
                    uiState.report?.let { report ->
                        DailyCutReportContent(
                            report = report,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 24.dp, vertical = 8.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DailyCutReportContent(
    report: DailyCutReport,
    modifier: Modifier = Modifier
) {
    val currencyFormat = NumberFormat.getCurrencyInstance(Locale.US)
    val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())

    Column(
        modifier = modifier.verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Report Header Info
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
                    text = "Fecha: ${report.date}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Generado: ${dateFormat.format(Date())}",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = "Por: ${report.generatedBy}",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        // Sales Summary
        ReportSection(
            title = "Resumen de Ventas",
            icon = Icons.Default.AttachMoney
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ReportItem("Ingresos Totales", currencyFormat.format(report.salesSummary.totalRevenue))
                ReportItem("Subtotal", currencyFormat.format(report.salesSummary.totalSubtotal))
                ReportItem("Impuestos", currencyFormat.format(report.salesSummary.totalTax))
                ReportItem("Descuentos", currencyFormat.format(report.salesSummary.totalDiscount))
                ReportItem("Ticket Promedio", currencyFormat.format(report.salesSummary.averageOrderValue))
            }
        }

        // Orders Summary
        ReportSection(
            title = "Resumen de Órdenes",
            icon = Icons.Default.Receipt
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ReportItem("Total Órdenes", report.ordersSummary.totalOrders.toString())
                ReportItem("Completadas", report.ordersSummary.completedOrders.toString())
                ReportItem("Canceladas", report.ordersSummary.cancelledOrders.toString())
                ReportItem("Tamaño Promedio", "${report.ordersSummary.averageOrderSize} items")
                
                if (report.ordersSummary.peakHours.isNotEmpty()) {
                    ReportItem("Horas Pico", report.ordersSummary.peakHours.joinToString(", "))
                }
            }
        }

        // Customer Summary
        ReportSection(
            title = "Resumen de Clientes",
            icon = Icons.Default.People
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ReportItem("Total Clientes", report.customersSummary.totalCustomers.toString())
                ReportItem("Clientes Nuevos", report.customersSummary.newCustomers.toString())
                ReportItem("Clientes Frecuentes", report.customersSummary.returningCustomers.toString())
                ReportItem("Gasto Promedio", currencyFormat.format(report.customersSummary.averageSpendPerCustomer))
            }
        }

        // Reservations Summary
        ReportSection(
            title = "Resumen de Reservas",
            icon = Icons.Default.EventAvailable
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ReportItem("Total Reservas", report.reservationsSummary.totalReservations.toString())
                ReportItem("Confirmadas", report.reservationsSummary.confirmedReservations.toString())
                ReportItem("Canceladas", report.reservationsSummary.cancelledReservations.toString())
                ReportItem("No Show", report.reservationsSummary.noShowReservations.toString())
                ReportItem("Tamaño Promedio Mesa", String.format("%.1f personas", report.reservationsSummary.averagePartySize))
                ReportItem("Ocupación", String.format("%.1f%%", report.reservationsSummary.occupancyRate))
            }
        }

        // Top Selling Products
        if (report.inventorySummary.topSellingProducts.isNotEmpty()) {
            ReportSection(
                title = "Productos Más Vendidos",
                icon = Icons.Default.Inventory
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    report.inventorySummary.topSellingProducts.take(5).forEach { product ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = product.productName,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = "${product.quantitySold} unidades",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Text(
                                text = currencyFormat.format(product.revenue),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF4CAF50)
                            )
                        }
                    }
                }
            }
        }

        // Stock Issues
        val hasStockIssues = report.inventorySummary.lowStockProducts.isNotEmpty() || 
                           report.inventorySummary.outOfStockProducts.isNotEmpty()

        if (hasStockIssues) {
            ReportSection(
                title = "Alertas de Inventario",
                icon = Icons.Default.Warning,
                alertLevel = true
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (report.inventorySummary.outOfStockProducts.isNotEmpty()) {
                        Text(
                            text = "Sin Stock (${report.inventorySummary.outOfStockProducts.size})",
                            style = MaterialTheme.typography.titleSmall,
                            color = Color(0xFFF44336),
                            fontWeight = FontWeight.Bold
                        )
                        report.inventorySummary.outOfStockProducts.take(3).forEach { product ->
                            Text(
                                text = "• ${product.productName}",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFFF44336)
                            )
                        }
                    }
                    
                    if (report.inventorySummary.lowStockProducts.isNotEmpty()) {
                        Text(
                            text = "Stock Bajo (${report.inventorySummary.lowStockProducts.size})",
                            style = MaterialTheme.typography.titleSmall,
                            color = Color(0xFFFF9800),
                            fontWeight = FontWeight.Bold
                        )
                        report.inventorySummary.lowStockProducts.take(3).forEach { product ->
                            Text(
                                text = "• ${product.productName} (${product.currentStock} restantes)",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFFFF9800)
                            )
                        }
                    }
                }
            }
        }

        // Payment Methods
        ReportSection(
            title = "Métodos de Pago",
            icon = Icons.Default.Payment
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                report.paymentMethodsBreakdown.forEach { payment ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = payment.method,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "${payment.count} transacciones",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = currencyFormat.format(payment.amount),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${payment.percentage.toInt()}%",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun ReportSection(
    title: String,
    icon: ImageVector,
    alertLevel: Boolean = false,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (alertLevel) {
                Color(0xFFFFF3E0)
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            }
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (alertLevel) Color(0xFFFF9800) else MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (alertLevel) Color(0xFFFF9800) else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            content()
        }
    }
}

@Composable
private fun ReportItem(
    label: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold
        )
    }
}