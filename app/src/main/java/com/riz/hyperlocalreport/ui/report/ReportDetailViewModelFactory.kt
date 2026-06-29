package com.riz.hyperlocalreport.ui.report

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.riz.hyperlocalreport.domain.repository.AuthRepository
import com.riz.hyperlocalreport.domain.repository.CommentRepository
import com.riz.hyperlocalreport.domain.repository.ReportRepository
import com.riz.hyperlocalreport.domain.repository.UpvoteRepository

class ReportDetailViewModelFactory(
    private val reportRepository: ReportRepository,
    private val authRepository: AuthRepository,
    private val commentRepository: CommentRepository,
    private val upvoteRepository: UpvoteRepository,
    private val reportId: String
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ReportDetailViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ReportDetailViewModel(reportRepository, authRepository, commentRepository, upvoteRepository, reportId) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
