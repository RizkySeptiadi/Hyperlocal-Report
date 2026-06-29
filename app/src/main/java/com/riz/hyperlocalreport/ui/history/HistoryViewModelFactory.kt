package com.riz.hyperlocalreport.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.riz.hyperlocalreport.domain.repository.AuthRepository
import com.riz.hyperlocalreport.domain.repository.ReportRepository

class HistoryViewModelFactory(
    private val authRepository: AuthRepository,
    private val reportRepository: ReportRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(HistoryViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return HistoryViewModel(authRepository, reportRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
