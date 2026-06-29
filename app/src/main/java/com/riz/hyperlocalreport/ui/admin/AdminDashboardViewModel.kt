package com.riz.hyperlocalreport.ui.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.riz.hyperlocalreport.domain.model.ReportUiModel
import com.riz.hyperlocalreport.domain.model.User
import com.riz.hyperlocalreport.domain.repository.AuthRepository
import com.riz.hyperlocalreport.domain.repository.ReportRepository
import com.riz.hyperlocalreport.domain.repository.UpvoteRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch

class AdminDashboardViewModel(
    private val authRepository: AuthRepository,
    private val reportRepository: ReportRepository,
    private val upvoteRepository: UpvoteRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<AdminDashboardUiState>(AdminDashboardUiState.Loading)
    val uiState: StateFlow<AdminDashboardUiState> = _uiState.asStateFlow()
    
    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    private var reportJob: Job? = null
    
    private val _upvotingIds = MutableStateFlow<Set<String>>(emptySet())

    init {
        observeAdminArea()
    }

    private fun observeAdminArea() {
        viewModelScope.launch {
            authRepository.currentUser.collectLatest { user ->
                _currentUser.value = user
                if (user != null && user.areaId.isNotEmpty()) {
                    loadReportsForArea(user.areaId, user.userId)
                } else {
                    _uiState.value = AdminDashboardUiState.Error("No Area ID assigned to this admin account.")
                }
            }
        }
    }

    private fun loadReportsForArea(areaId: String, userId: String) {
        reportJob?.cancel()
        _uiState.value = AdminDashboardUiState.Loading
        reportJob = viewModelScope.launch {
            val userUpvotesFlow = upvoteRepository.observeUserUpvotes(userId)

            combine(
                reportRepository.observeReportsByArea(areaId).catch { e -> _uiState.value = AdminDashboardUiState.Error(e.message ?: "Unknown error") },
                userUpvotesFlow,
                _upvotingIds
            ) { reports, upvotedIds, upvotingIds ->
                if (reports.isEmpty()) {
                    AdminDashboardUiState.Empty
                } else {
                    val uiModels = reports.map { report ->
                        ReportUiModel(
                            report = report,
                            isUpvotedByMe = upvotedIds.contains(report.reportId),
                            isUpvoting = upvotingIds.contains(report.reportId)
                        )
                    }
                    AdminDashboardUiState.Success(uiModels)
                }
            }.collectLatest { state ->
                if (_uiState.value !is AdminDashboardUiState.Error) {
                    _uiState.value = state
                }
            }
        }
    }

    fun toggleUpvote(reportId: String) {
        val user = _currentUser.value ?: return
        
        if (_upvotingIds.value.contains(reportId)) return
        _upvotingIds.value = _upvotingIds.value + reportId

        viewModelScope.launch {
            upvoteRepository.toggleUpvote(user.userId, reportId)
            _upvotingIds.value = _upvotingIds.value - reportId
        }
    }
}

sealed class AdminDashboardUiState {
    object Loading : AdminDashboardUiState()
    object Empty : AdminDashboardUiState()
    data class Success(val reports: List<ReportUiModel>) : AdminDashboardUiState()
    data class Error(val message: String) : AdminDashboardUiState()
}
