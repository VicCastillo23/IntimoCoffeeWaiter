package com.intimocoffee.waiter.feature.reservations.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.runtime.collectAsState
import com.intimocoffee.waiter.feature.reservations.domain.model.Reservation
import com.intimocoffee.waiter.feature.reservations.domain.model.ReservationStatus
import com.intimocoffee.waiter.feature.tables.domain.model.Table
import com.intimocoffee.waiter.ui.theme.IntimoCoffeeAppTheme
import kotlinx.coroutines.launch
import kotlinx.datetime.*
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReservationsScreen(
    viewModel: ReservationsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        // Data already loaded in ViewModel init
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
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
                text = "Gestión de Reservas",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold
            )
            
            FilledTonalButton(
                onClick = { viewModel.showCreateReservationDialog() }
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("Nueva Reserva")
            }
        }

        // Filter Chips
        LazyRow(
            modifier = Modifier.padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(ReservationFilter.values()) { filter ->
                FilterChip(
                    onClick = { viewModel.selectFilter(filter) },
                    label = { Text(filter.displayName) },
                    selected = uiState.selectedFilter == filter
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Quick Stats
        LazyRow(
            modifier = Modifier.padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                QuickStatCard(
                    title = "Hoy",
                    value = uiState.todaysReservations.size.toString(),
                    icon = Icons.Default.Today,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            item {
                QuickStatCard(
                    title = "Próximas",
                    value = uiState.upcomingReservations.size.toString(),
                    icon = Icons.Default.EventAvailable,
                    color = MaterialTheme.colorScheme.secondary
                )
            }
            item {
                QuickStatCard(
                    title = "En Mesa",
                    value = uiState.reservations.count { it.status == ReservationStatus.SEATED }.toString(),
                    icon = Icons.Default.Restaurant,
                    color = MaterialTheme.colorScheme.secondary
                )
            }
            item {
                QuickStatCard(
                    title = "Pendientes",
                    value = uiState.reservations.count { it.status == ReservationStatus.PENDING }.toString(),
                    icon = Icons.Default.PendingActions,
                    color = MaterialTheme.colorScheme.tertiary
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Reservations List
        when {
            uiState.isLoading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            
            viewModel.getFilteredReservations().isEmpty() -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Default.EventBusy,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "No hay reservas para mostrar",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            
            else -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(viewModel.getFilteredReservations()) { reservation ->
                        ReservationCard(
                            reservation = reservation,
                            onStatusChange = { status ->
                                scope.launch {
                                    viewModel.updateReservationStatus(reservation.id, status)
                                }
                            },
                            onEdit = { viewModel.showEditReservationDialog(reservation) },
                            onCancel = {
                                scope.launch {
                                    viewModel.cancelReservation(reservation.id)
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    // Create/Edit Reservation Dialog
    if (uiState.showCreateReservationDialog) {
        CreateReservationDialog(
            editingReservation = uiState.editingReservation,
            availableTables = uiState.availableTables,
            onDismiss = { viewModel.hideReservationDialog() },
            onSave = { tableId, customerName, customerPhone, customerEmail, partySize, reservationDate, duration, notes ->
                scope.launch {
                    viewModel.createReservation(
                        tableId, customerName, customerPhone, customerEmail, 
                        partySize, reservationDate, duration, notes
                    )
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
private fun QuickStatCard(
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color
) {
    Card(
        modifier = Modifier.width(120.dp),
        colors = CardDefaults.cardColors(
            containerColor = color.copy(alpha = 0.1f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = color
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = color
            )
            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ReservationCard(
    reservation: Reservation,
    onStatusChange: (ReservationStatus) -> Unit,
    onEdit: () -> Unit,
    onCancel: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = when (reservation.status) {
                ReservationStatus.CONFIRMED -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.1f)
                ReservationStatus.PENDING -> MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.1f)
                ReservationStatus.SEATED -> MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.1f)
                ReservationStatus.COMPLETED -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                ReservationStatus.CANCELLED -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.1f)
                else -> MaterialTheme.colorScheme.surface
            }
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = reservation.customerName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = reservation.tableName,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                // Status Chip
                Surface(
                    color = getStatusColor(reservation.status).copy(alpha = 0.2f),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text(
                        text = reservation.status.displayName,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = getStatusColor(reservation.status),
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Details Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                DetailItem(
                    icon = Icons.Default.Schedule,
                    text = formatDateTime(reservation.reservationDate)
                )
                DetailItem(
                    icon = Icons.Default.People,
                    text = "${reservation.partySize} personas"
                )
                if (reservation.duration != 120) {
                    DetailItem(
                        icon = Icons.Default.Timer,
                        text = "${reservation.duration}min"
                    )
                }
            }

            reservation.customerPhone?.let { phone ->
                Spacer(modifier = Modifier.height(4.dp))
                DetailItem(
                    icon = Icons.Default.Phone,
                    text = phone
                )
            }

            reservation.notes?.let { notes ->
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Notas: $notes",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                )
            }

            // Action Buttons
            if (reservation.status != ReservationStatus.CANCELLED && reservation.status != ReservationStatus.COMPLETED) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (reservation.status == ReservationStatus.PENDING) {
                        OutlinedButton(
                            onClick = { onStatusChange(ReservationStatus.CONFIRMED) },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Confirmar")
                        }
                    } else if (reservation.status == ReservationStatus.CONFIRMED) {
                        OutlinedButton(
                            onClick = { onStatusChange(ReservationStatus.SEATED) },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("En Mesa")
                        }
                    } else if (reservation.status == ReservationStatus.SEATED) {
                        OutlinedButton(
                            onClick = { onStatusChange(ReservationStatus.COMPLETED) },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.primary
                            )
                        ) {
                            Text("Completar")
                        }
                    }
                    
                    TextButton(
                        onClick = onEdit,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Editar")
                    }
                    
                    TextButton(
                        onClick = onCancel,
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Text("Cancelar")
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun getStatusColor(status: ReservationStatus): Color {
    return when (status) {
        ReservationStatus.PENDING -> MaterialTheme.colorScheme.tertiary
        ReservationStatus.CONFIRMED -> MaterialTheme.colorScheme.primary
        ReservationStatus.SEATED -> MaterialTheme.colorScheme.secondary
        ReservationStatus.COMPLETED -> MaterialTheme.colorScheme.outline
        ReservationStatus.CANCELLED -> MaterialTheme.colorScheme.error
        ReservationStatus.NO_SHOW -> MaterialTheme.colorScheme.error
    }
}

private fun formatDateTime(dateTime: LocalDateTime): String {
    val date = "${dateTime.dayOfMonth}/${dateTime.monthNumber}"
    val time = "${dateTime.hour.toString().padStart(2, '0')}:${dateTime.minute.toString().padStart(2, '0')}"
    return "$date $time"
}

@Preview(showBackground = true)
@Composable
private fun ReservationsScreenPreview() {
    IntimoCoffeeAppTheme {
        ReservationsScreen()
    }
}