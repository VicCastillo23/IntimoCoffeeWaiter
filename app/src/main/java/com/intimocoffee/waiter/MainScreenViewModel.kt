package com.intimocoffee.waiter

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.intimocoffee.waiter.feature.auth.domain.model.User
import com.intimocoffee.waiter.feature.auth.domain.repository.AuthRepository
import com.intimocoffee.waiter.feature.dashboard.data.repository.DashboardRepository
import com.intimocoffee.waiter.feature.dashboard.domain.model.DashboardStats
import com.intimocoffee.waiter.feature.reservations.domain.model.Reservation
import com.intimocoffee.waiter.feature.reservations.domain.repository.ReservationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainScreenViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val dashboardRepository: DashboardRepository,
    private val reservationRepository: ReservationRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(MainScreenUiState())
    val uiState: StateFlow<MainScreenUiState> = _uiState.asStateFlow()
    
    fun loadCurrentUser() {
        viewModelScope.launch {
            val user = authRepository.getCurrentUser()
            _uiState.value = _uiState.value.copy(currentUser = user)
        }
    }
    
    fun loadDashboardData() {
        viewModelScope.launch {
            dashboardRepository.getDashboardStats().collectLatest { stats ->
                _uiState.value = _uiState.value.copy(
                    dashboardStats = stats,
                    isLoading = false
                )
            }
        }
        
        // Cargar reservas de hoy
        viewModelScope.launch {
            reservationRepository.getTodaysReservations().collectLatest { reservations ->
                _uiState.value = _uiState.value.copy(
                    todaysReservations = reservations
                )
            }
        }
    }
    
    fun selectNavItem(route: String) {
        _uiState.value = _uiState.value.copy(selectedNavItem = route)
    }
    
    fun logout() {
        viewModelScope.launch {
            authRepository.logout()
            _uiState.value = _uiState.value.copy(shouldLogout = true)
        }
    }
    
    fun showDailyCutConfirmationDialog() {
        _uiState.value = _uiState.value.copy(showDailyCutConfirmationDialog = true)
    }
    
    fun hideDailyCutConfirmationDialog() {
        _uiState.value = _uiState.value.copy(showDailyCutConfirmationDialog = false)
    }
    
    fun showDailyCutDialog() {
        _uiState.value = _uiState.value.copy(
            showDailyCutDialog = true,
            showDailyCutConfirmationDialog = false
        )
    }
    
    fun hideDailyCutDialog() {
        _uiState.value = _uiState.value.copy(showDailyCutDialog = false)
    }
}

data class MainScreenUiState(
    val currentUser: User? = null,
    val selectedNavItem: String = "pos",
    val shouldLogout: Boolean = false,
    val dashboardStats: DashboardStats? = null,
    val todaysReservations: List<Reservation> = emptyList(),
    val isLoading: Boolean = true,
    val showDailyCutDialog: Boolean = false,
    val showDailyCutConfirmationDialog: Boolean = false
)
