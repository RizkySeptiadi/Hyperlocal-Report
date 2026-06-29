package com.riz.hyperlocalreport.domain.repository

import com.riz.hyperlocalreport.domain.model.Report
import kotlinx.coroutines.flow.Flow

interface ReportRepository {
    fun observeReportsByArea(areaId: String): Flow<List<Report>>
    fun getReportById(reportId: String): Flow<Report?>
    suspend fun createReport(report: Report): Result<Unit>
    suspend fun updateReportStatus(reportId: String, status: String): Result<Unit>
    suspend fun toggleUpvote(reportId: String, userId: String): Result<Boolean>
    suspend fun uploadPhoto(localUriString: String, path: String): Result<String>
    fun observeReportsByUser(userId: String): Flow<List<Report>>
}
