package com.intimocoffee.waiter.feature.reports.domain.repository

import com.intimocoffee.waiter.feature.reports.domain.model.DailyCutReport
import kotlinx.datetime.LocalDate
import kotlinx.coroutines.flow.Flow

interface ReportsRepository {
    suspend fun generateDailyCutReport(date: LocalDate, userId: Long): DailyCutReport
    suspend fun getDailyCutReports(startDate: LocalDate, endDate: LocalDate): Flow<List<DailyCutReport>>
    suspend fun saveDailyCutReport(report: DailyCutReport): Boolean
    suspend fun archiveAllPaidOrders(date: LocalDate): Boolean
}
