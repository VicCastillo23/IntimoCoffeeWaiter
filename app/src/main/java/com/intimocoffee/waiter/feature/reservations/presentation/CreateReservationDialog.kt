package com.intimocoffee.waiter.feature.reservations.presentation

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.intimocoffee.waiter.feature.reservations.domain.model.Reservation
import com.intimocoffee.waiter.feature.tables.domain.model.Table
import kotlinx.datetime.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateReservationDialog(
    editingReservation: Reservation?,
    availableTables: List<Table>,
    onDismiss: () -> Unit,
    onSave: (
        tableId: Long,
        customerName: String,
        customerPhone: String?,
        customerEmail: String?,
        partySize: Int,
        reservationDate: LocalDateTime,
        duration: Int,
        notes: String?
    ) -> Unit
) {
    var customerName by remember { mutableStateOf(editingReservation?.customerName ?: "") }
    var customerPhone by remember { mutableStateOf(editingReservation?.customerPhone ?: "") }
    var customerEmail by remember { mutableStateOf(editingReservation?.customerEmail ?: "") }
    var partySize by remember { mutableStateOf(editingReservation?.partySize?.toString() ?: "2") }
    var selectedTableId by remember { mutableStateOf(editingReservation?.tableId ?: availableTables.firstOrNull()?.id ?: 1L) }
    var notes by remember { mutableStateOf(editingReservation?.notes ?: "") }
    var duration by remember { mutableStateOf(editingReservation?.duration?.toString() ?: "120") }
    
    // Date and Time selection
    val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
    var selectedDate by remember { 
        mutableStateOf(editingReservation?.reservationDate?.date ?: now.date) 
    }
    var selectedHour by remember { 
        mutableStateOf(editingReservation?.reservationDate?.hour ?: 19) 
    }
    var selectedMinute by remember { 
        mutableStateOf(editingReservation?.reservationDate?.minute ?: 0) 
    }
    
    var expandedTable by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }

    val isEditing = editingReservation != null

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { 
            Text(if (isEditing) "Editar Reserva" else "Nueva Reserva") 
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Customer Information Section
                Text(
                    text = "Información del Cliente",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary
                )
                
                OutlinedTextField(
                    value = customerName,
                    onValueChange = { customerName = it },
                    label = { Text("Nombre del cliente *") },
                    placeholder = { Text("ej: María García") },
                    modifier = Modifier.fillMaxWidth(),
                    isError = customerName.isBlank(),
                    leadingIcon = {
                        Icon(Icons.Default.Person, contentDescription = null)
                    }
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = customerPhone,
                        onValueChange = { customerPhone = it },
                        label = { Text("Teléfono") },
                        placeholder = { Text("+57 300 123 4567") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        modifier = Modifier.weight(1f),
                        leadingIcon = {
                            Icon(Icons.Default.Phone, contentDescription = null)
                        }
                    )

                    OutlinedTextField(
                        value = partySize,
                        onValueChange = { partySize = it },
                        label = { Text("Personas *") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(0.8f),
                        isError = partySize.toIntOrNull()?.let { it <= 0 } ?: true,
                        leadingIcon = {
                            Icon(Icons.Default.People, contentDescription = null)
                        }
                    )
                }

                OutlinedTextField(
                    value = customerEmail,
                    onValueChange = { customerEmail = it },
                    label = { Text("Email (opcional)") },
                    placeholder = { Text("cliente@email.com") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    modifier = Modifier.fillMaxWidth(),
                    leadingIcon = {
                        Icon(Icons.Default.Email, contentDescription = null)
                    }
                )

                Divider()

                // Reservation Details Section
                Text(
                    text = "Detalles de la Reserva",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary
                )

                // Table Selection
                ExposedDropdownMenuBox(
                    expanded = expandedTable,
                    onExpandedChange = { expandedTable = !expandedTable }
                ) {
                    OutlinedTextField(
                        value = availableTables.find { it.id == selectedTableId }?.displayName ?: "Mesa $selectedTableId",
                        onValueChange = { },
                        readOnly = true,
                        label = { Text("Mesa *") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedTable) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth(),
                        leadingIcon = {
                            Icon(Icons.Default.TableRestaurant, contentDescription = null)
                        }
                    )
                    
                    ExposedDropdownMenu(
                        expanded = expandedTable,
                        onDismissRequest = { expandedTable = false }
                    ) {
                        availableTables.forEach { table ->
                            DropdownMenuItem(
                                text = { 
                                    Column {
                                        Text(table.displayName)
                                        Text(
                                            text = "${table.capacity} personas - ${table.zone}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                },
                                onClick = {
                                    selectedTableId = table.id
                                    expandedTable = false
                                }
                            )
                        }
                    }
                }

                // Date and Time Selection
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = "${selectedDate.dayOfMonth}/${selectedDate.monthNumber}/${selectedDate.year}",
                        onValueChange = { },
                        readOnly = true,
                        label = { Text("Fecha *") },
                        modifier = Modifier.weight(1f),
                        leadingIcon = {
                            Icon(Icons.Default.DateRange, contentDescription = null)
                        },
                        trailingIcon = {
                            IconButton(onClick = { showDatePicker = true }) {
                                Icon(Icons.Default.EditCalendar, contentDescription = "Seleccionar fecha")
                            }
                        }
                    )

                    OutlinedTextField(
                        value = "${selectedHour.toString().padStart(2, '0')}:${selectedMinute.toString().padStart(2, '0')}",
                        onValueChange = { },
                        readOnly = true,
                        label = { Text("Hora *") },
                        modifier = Modifier.weight(0.8f),
                        leadingIcon = {
                            Icon(Icons.Default.AccessTime, contentDescription = null)
                        },
                        trailingIcon = {
                            IconButton(onClick = { showTimePicker = true }) {
                                Icon(Icons.Default.Schedule, contentDescription = "Seleccionar hora")
                            }
                        }
                    )
                }

                OutlinedTextField(
                    value = duration,
                    onValueChange = { duration = it },
                    label = { Text("Duración (minutos)") },
                    placeholder = { Text("120") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    isError = duration.toIntOrNull()?.let { it <= 0 } ?: true,
                    leadingIcon = {
                        Icon(Icons.Default.Timer, contentDescription = null)
                    },
                    supportingText = {
                        Text("Duración estimada de la reserva")
                    }
                )

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notas especiales") },
                    placeholder = { Text("Cumpleaños, alergias, preferencias...") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3,
                    leadingIcon = {
                        Icon(Icons.Default.Notes, contentDescription = null)
                    }
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val reservationDateTime = LocalDateTime(
                        selectedDate.year,
                        selectedDate.month,
                        selectedDate.dayOfMonth,
                        selectedHour,
                        selectedMinute
                    )
                    
                    val parsedPartySize = partySize.toIntOrNull() ?: 2
                    val parsedDuration = duration.toIntOrNull() ?: 120
                    
                    onSave(
                        selectedTableId,
                        customerName,
                        customerPhone.takeIf { it.isNotBlank() },
                        customerEmail.takeIf { it.isNotBlank() },
                        parsedPartySize,
                        reservationDateTime,
                        parsedDuration,
                        notes.takeIf { it.isNotBlank() }
                    )
                },
                enabled = customerName.isNotBlank() && 
                         partySize.toIntOrNull()?.let { it > 0 } == true &&
                         duration.toIntOrNull()?.let { it > 0 } == true
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

    // Date Picker Dialog
    if (showDatePicker) {
        DatePickerDialog(
            selectedDate = selectedDate,
            onDateSelected = { date ->
                selectedDate = date
                showDatePicker = false
            },
            onDismiss = { showDatePicker = false }
        )
    }

    // Time Picker Dialog
    if (showTimePicker) {
        TimePickerDialog(
            selectedHour = selectedHour,
            selectedMinute = selectedMinute,
            onTimeSelected = { hour, minute ->
                selectedHour = hour
                selectedMinute = minute
                showTimePicker = false
            },
            onDismiss = { showTimePicker = false }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DatePickerDialog(
    selectedDate: LocalDate,
    onDateSelected: (LocalDate) -> Unit,
    onDismiss: () -> Unit
) {
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = selectedDate.toEpochDays() * 24 * 60 * 60 * 1000L
    )

    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        val epochDay = millis / (24 * 60 * 60 * 1000L)
                        val date = LocalDate.fromEpochDays(epochDay.toInt())
                        onDateSelected(date)
                    }
                }
            ) {
                Text("Aceptar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    ) {
        DatePicker(state = datePickerState)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimePickerDialog(
    selectedHour: Int,
    selectedMinute: Int,
    onTimeSelected: (Int, Int) -> Unit,
    onDismiss: () -> Unit
) {
    val timePickerState = rememberTimePickerState(
        initialHour = selectedHour,
        initialMinute = selectedMinute,
        is24Hour = true
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Seleccionar Hora") },
        text = {
            TimePicker(
                state = timePickerState,
                modifier = Modifier.padding(16.dp)
            )
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onTimeSelected(timePickerState.hour, timePickerState.minute)
                }
            ) {
                Text("Aceptar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}