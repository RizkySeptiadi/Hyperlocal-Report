package com.riz.hyperlocalreport.ui.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.riz.hyperlocalreport.domain.repository.AuthRepository
import com.riz.hyperlocalreport.domain.repository.ReportRepository
import com.riz.hyperlocalreport.domain.repository.UpvoteRepository

class AdminDashboardViewModelFactory(
    private val authRepository: AuthRepository,
    private val reportRepository: ReportRepository,
    private val upvoteRepository: UpvoteRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AdminDashboardViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return AdminDashboardViewModel(authRepository, reportRepository, upvoteRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
