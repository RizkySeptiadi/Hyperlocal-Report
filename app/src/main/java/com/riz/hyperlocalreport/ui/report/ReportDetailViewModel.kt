package com.riz.hyperlocalreport.ui.report

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.riz.hyperlocalreport.core.common.Constants
import com.riz.hyperlocalreport.domain.model.Comment
import com.riz.hyperlocalreport.domain.model.Report
import com.riz.hyperlocalreport.domain.repository.AuthRepository
import com.riz.hyperlocalreport.domain.repository.CommentRepository
import com.riz.hyperlocalreport.domain.repository.ReportRepository
import com.riz.hyperlocalreport.domain.repository.UpvoteRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class ReportDetailViewModel(
    private val reportRepository: ReportRepository,
    private val authRepository: AuthRepository,
    private val commentRepository: CommentRepository,
    private val upvoteRepository: UpvoteRepository,
    private val reportId: String
) : ViewModel() {

    private val _report = MutableStateFlow<Report?>(null)
    val report: StateFlow<Report?> = _report.asStateFlow()

    private val _comments = MutableStateFlow<List<Comment>>(emptyList())
    val comments: StateFlow<List<Comment>> = _comments.asStateFlow()

    private val _isUpvotedByMe = MutableStateFlow(false)
    val isUpvotedByMe: StateFlow<Boolean> = _isUpvotedByMe.asStateFlow()
    
    private val _isUpvoting = MutableStateFlow(false)
    val isUpvoting: StateFlow<Boolean> = _isUpvoting.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _isAdmin = MutableStateFlow(false)
    val isAdmin: StateFlow<Boolean> = _isAdmin.asStateFlow()

    init {
        checkAdminStatus()
        fetchReport()
        fetchComments()
        observeUpvotes()
    }

    private fun checkAdminStatus() {
        viewModelScope.launch {
            authRepository.currentUser.collect { user ->
                _isAdmin.value = user?.role == Constants.Roles.ADMIN_RT_RW || user?.role == Constants.Roles.ADMIN_KELURAHAN
            }
        }
    }

    private fun fetchReport() {
        viewModelScope.launch {
            reportRepository.getReportById(reportId).collect {
                _report.value = it
            }
        }
    }

    private fun fetchComments() {
        viewModelScope.launch {
            commentRepository.observeComments(reportId).collect {
                _comments.value = it
            }
        }
    }
    
    private fun observeUpvotes() {
        viewModelScope.launch {
            authRepository.currentUser.collectLatest { user ->
                if (user != null) {
                    upvoteRepository.observeUserUpvotes(user.userId).collect { upvotedIds ->
                        _isUpvotedByMe.value = upvotedIds.contains(reportId)
                    }
                } else {
                    _isUpvotedByMe.value = false
                }
            }
        }
    }

    fun toggleUpvote() {
        viewModelScope.launch {
            val user = authRepository.currentUser.value
            if (user != null) {
                if (_isUpvoting.value) return@launch
                _isUpvoting.value = true
                val result = upvoteRepository.toggleUpvote(user.userId, reportId)
                if (result.isFailure) {
                    _error.value = result.exceptionOrNull()?.message ?: "Failed to upvote"
                }
                _isUpvoting.value = false
            } else {
                _error.value = "You must be logged in to upvote"
            }
        }
    }

    fun updateStatus(newStatus: String) {
        viewModelScope.launch {
            if (_isAdmin.value) {
                val result = reportRepository.updateReportStatus(reportId, newStatus)
                if (result.isFailure) {
                    _error.value = result.exceptionOrNull()?.message ?: "Failed to update status"
                }
            }
        }
    }

    fun addComment(content: String) {
        viewModelScope.launch {
            val user = authRepository.currentUser.value
            if (user != null && content.isNotBlank()) {
                val comment = Comment(
                    commentId = "",
                    reportId = reportId,
                    userId = user.userId,
                    authorName = user.name,
                    content = content,
                    isAdminReply = _isAdmin.value,
                    createdAt = null
                )
                val result = commentRepository.addComment(comment)
                if (result.isFailure) {
                    _error.value = result.exceptionOrNull()?.message ?: "Failed to add comment"
                }
            }
        }
    }
}
