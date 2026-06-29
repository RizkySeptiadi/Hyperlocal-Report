package com.riz.hyperlocalreport.ui.report

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.riz.hyperlocalreport.core.common.AreaSessionManager
import com.riz.hyperlocalreport.domain.repository.AuthRepository
import com.riz.hyperlocalreport.domain.repository.ReportRepository

class CreateReportViewModelFactory(
    private val authRepository: AuthRepository,
    private val reportRepository: ReportRepository,
    private val areaSessionManager: AreaSessionManager
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CreateReportViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return CreateReportViewModel(authRepository, reportRepository, areaSessionManager) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
