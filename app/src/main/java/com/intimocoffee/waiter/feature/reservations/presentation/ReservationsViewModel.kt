package com.intimocoffee.waiter.feature.reservations.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.intimocoffee.waiter.feature.reservations.domain.model.Reservation
import com.intimocoffee.waiter.feature.reservations.domain.model.ReservationSlot
import com.intimocoffee.waiter.feature.reservations.domain.model.ReservationStatus
import com.intimocoffee.waiter.feature.reservations.domain.repository.ReservationRepository
import com.intimocoffee.waiter.feature.tables.domain.model.Table
import com.intimocoffee.waiter.feature.tables.domain.repository.TableRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.datetime.*
import javax.inject.Inject

data class ReservationsUiState(
    val reservations: List<Reservation> = emptyList(),
    val todaysReservations: List<Reservation> = emptyList(),
    val upcomingReservations: List<Reservation> = emptyList(),
    val availableTables: List<Table> = emptyList(),
    val selectedDate: LocalDate = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date,
    val isLoading: Boolean = false,
    val error: String? = null,
    val showCreateReservationDialog: Boolean = false,
    val editingReservation: Reservation? = null,
    val availableSlots: List<ReservationSlot> = emptyList(),
    val selectedFilter: ReservationFilter = ReservationFilter.ALL
)

enum class ReservationFilter(val displayName: String) {
    ALL("Todas"),
    TODAY("Hoy"),
    UPCOMING("Próximas"),
    CONFIRMED("Confirmadas"),
    PENDING("Pendientes"),
    SEATED("En Mesa"),
    COMPLETED("Completadas")
}

@HiltViewModel
class ReservationsViewModel @Inject constructor(
    private val reservationRepository: ReservationRepository,
    private val tableRepository: TableRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ReservationsUiState())
    val uiState: StateFlow<ReservationsUiState> = _uiState.asStateFlow()

    init {
        loadInitialData()
    }

    private fun loadInitialData() {
        loadReservations()
        loadTables()
        loadTodaysReservations()
        loadUpcomingReservations()
    }

    private fun loadReservations() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            
            try {
                reservationRepository.getAllReservations().collect { reservations ->
                    _uiState.value = _uiState.value.copy(
                        reservations = reservations,
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Error al cargar las reservas: ${e.message}"
                )
            }
        }
    }

    private fun loadTables() {
        viewModelScope.launch {
            try {
                tableRepository.getAllActiveTables().collect { tables ->
                    _uiState.value = _uiState.value.copy(availableTables = tables)
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "Error al cargar las mesas: ${e.message}"
                )
            }
        }
    }

    private fun loadTodaysReservations() {
        viewModelScope.launch {
            try {
                reservationRepository.getTodaysReservations().collect { reservations ->
                    _uiState.value = _uiState.value.copy(todaysReservations = reservations)
                }
            } catch (e: Exception) {
                // Handle error silently for this secondary data
            }
        }
    }

    private fun loadUpcomingReservations() {
        viewModelScope.launch {
            try {
                reservationRepository.getUpcomingReservations().collect { reservations ->
                    _uiState.value = _uiState.value.copy(upcomingReservations = reservations)
                }
            } catch (e: Exception) {
                // Handle error silently for this secondary data
            }
        }
    }

    fun selectFilter(filter: ReservationFilter) {
        _uiState.value = _uiState.value.copy(selectedFilter = filter)
    }

    fun selectDate(date: LocalDate) {
        _uiState.value = _uiState.value.copy(selectedDate = date)
        loadReservationsByDate(date)
    }

    private fun loadReservationsByDate(date: LocalDate) {
        viewModelScope.launch {
            try {
                reservationRepository.getReservationsByDate(date).collect { reservations ->
                    // Update the reservations list with filtered data if needed
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "Error al cargar reservas por fecha: ${e.message}"
                )
            }
        }
    }

    fun showCreateReservationDialog() {
        _uiState.value = _uiState.value.copy(
            showCreateReservationDialog = true,
            editingReservation = null
        )
    }

    fun showEditReservationDialog(reservation: Reservation) {
        _uiState.value = _uiState.value.copy(
            showCreateReservationDialog = true,
            editingReservation = reservation
        )
    }

    fun hideReservationDialog() {
        _uiState.value = _uiState.value.copy(
            showCreateReservationDialog = false,
            editingReservation = null
        )
    }

    suspend fun createReservation(
        tableId: Long,
        customerName: String,
        customerPhone: String?,
        customerEmail: String?,
        partySize: Int,
        reservationDate: LocalDateTime,
        duration: Int,
        notes: String?
    ): Boolean {
        return try {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            val table = _uiState.value.availableTables.find { it.id == tableId }
            val tableName = table?.displayName ?: "Mesa $tableId"
            
            val reservation = Reservation(
                tableId = tableId,
                tableName = tableName,
                customerName = customerName,
                customerPhone = customerPhone?.takeIf { it.isNotBlank() },
                customerEmail = customerEmail?.takeIf { it.isNotBlank() },
                partySize = partySize,
                reservationDate = reservationDate,
                duration = duration,
                status = ReservationStatus.PENDING,
                notes = notes?.takeIf { it.isNotBlank() },
                createdAt = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()),
                updatedAt = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()),
                createdBy = 1L // TODO: Get from auth user
            )
            
            val success = reservationRepository.createReservation(reservation)
            if (success) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    showCreateReservationDialog = false
                )
            } else {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Error al crear la reserva"
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

    suspend fun updateReservationStatus(id: Long, status: ReservationStatus): Boolean {
        return try {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            val success = reservationRepository.updateReservationStatus(id, status)
            if (!success) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Error al actualizar la reserva"
                )
            } else {
                _uiState.value = _uiState.value.copy(isLoading = false)
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

    suspend fun cancelReservation(id: Long): Boolean {
        return updateReservationStatus(id, ReservationStatus.CANCELLED)
    }

    fun loadAvailableSlots(date: LocalDate, tableId: Long? = null, duration: Int = 120) {
        viewModelScope.launch {
            try {
                val slots = reservationRepository.getAvailableSlots(date, tableId, duration)
                _uiState.value = _uiState.value.copy(availableSlots = slots)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "Error al cargar horarios disponibles: ${e.message}"
                )
            }
        }
    }

    fun getFilteredReservations(): List<Reservation> {
        val currentState = _uiState.value
        return when (currentState.selectedFilter) {
            ReservationFilter.ALL -> currentState.reservations
            ReservationFilter.TODAY -> currentState.todaysReservations
            ReservationFilter.UPCOMING -> currentState.upcomingReservations
            ReservationFilter.CONFIRMED -> currentState.reservations.filter { it.status == ReservationStatus.CONFIRMED }
            ReservationFilter.PENDING -> currentState.reservations.filter { it.status == ReservationStatus.PENDING }
            ReservationFilter.SEATED -> currentState.reservations.filter { it.status == ReservationStatus.SEATED }
            ReservationFilter.COMPLETED -> currentState.reservations.filter { it.status == ReservationStatus.COMPLETED }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}