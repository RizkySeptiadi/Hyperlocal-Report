package com.riz.hyperlocalreport.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.riz.hyperlocalreport.core.common.AreaSessionManager
import com.riz.hyperlocalreport.domain.repository.ReportRepository

import com.riz.hyperlocalreport.domain.repository.AuthRepository
import com.riz.hyperlocalreport.domain.repository.UpvoteRepository

class HomeViewModelFactory(
    private val reportRepository: ReportRepository,
    private val areaSessionManager: AreaSessionManager,
    private val authRepository: AuthRepository,
    private val upvoteRepository: UpvoteRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(HomeViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return HomeViewModel(reportRepository, areaSessionManager, authRepository, upvoteRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
