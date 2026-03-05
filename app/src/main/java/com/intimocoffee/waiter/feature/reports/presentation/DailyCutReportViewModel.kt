package com.intimocoffee.waiter.feature.reports.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.intimocoffee.waiter.feature.reports.domain.repository.ReportsRepository
import com.intimocoffee.waiter.feature.reports.domain.model.DailyCutReport
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import javax.inject.Inject

@HiltViewModel
class DailyCutReportViewModel @Inject constructor(
    private val reportsRepository: ReportsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(DailyCutReportUiState())
    val uiState: StateFlow<DailyCutReportUiState> = _uiState.asStateFlow()

    fun generateDailyCutReport() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                error = null
            )
            
            try {
                val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
                
                // Generate the report first
                val report = reportsRepository.generateDailyCutReport(today, 1L) // TODO: Get real user ID
                
                // Archive all paid orders for today after generating the report
                val archiveSuccess = reportsRepository.archiveAllPaidOrders(today)
                
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    report = report,
                    error = if (!archiveSuccess) "Reporte generado, pero falló el archivado de órdenes" else null
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Error desconocido al generar reporte"
                )
            }
        }
    }
}

data class DailyCutReportUiState(
    val isLoading: Boolean = false,
    val report: DailyCutReport? = null,
    val error: String? = null
)