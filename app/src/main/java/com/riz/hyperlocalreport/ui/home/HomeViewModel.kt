package com.riz.hyperlocalreport.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.riz.hyperlocalreport.core.common.AreaSessionManager
import com.riz.hyperlocalreport.core.common.AreaState
import com.riz.hyperlocalreport.domain.model.ReportUiModel
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

class HomeViewModel(
    private val reportRepository: ReportRepository,
    private val areaSessionManager: AreaSessionManager,
    private val authRepository: AuthRepository,
    private val upvoteRepository: UpvoteRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<HomeUiState>(HomeUiState.Loading)
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()
    
    val areaState: StateFlow<AreaState> = areaSessionManager.areaState

    private var reportJob: Job? = null
    
    // Set of reportIds currently being upvoted (optimistic lock)
    private val _upvotingIds = MutableStateFlow<Set<String>>(emptySet())
    
    val userName: StateFlow<String> = MutableStateFlow("")

    init {
        observeAreaState()
        
        viewModelScope.launch {
            authRepository.currentUser.collectLatest { user ->
                (userName as MutableStateFlow).value = user?.name ?: "Warga"
            }
        }
    }

    private fun observeAreaState() {
        viewModelScope.launch {
            areaSessionManager.areaState.collectLatest { state ->
                when (state) {
                    is AreaState.Available -> {
                        loadReportsForArea(state.area.areaId)
                    }
                    is AreaState.PermissionRequired -> _uiState.value = HomeUiState.PermissionRequired
                    is AreaState.LocationDisabled -> _uiState.value = HomeUiState.LocationDisabled
                    is AreaState.LoadingLocation, is AreaState.ResolvingArea -> _uiState.value = HomeUiState.Loading
                    is AreaState.UnsupportedLocation -> _uiState.value = HomeUiState.UnsupportedArea
                    is AreaState.Error -> _uiState.value = HomeUiState.Error(state.message)
                    is AreaState.Uninitialized -> _uiState.value = HomeUiState.Loading
                }
            }
        }
    }

    private fun loadReportsForArea(areaId: String) {
        reportJob?.cancel()
        _uiState.value = HomeUiState.Loading
        reportJob = viewModelScope.launch {
            val userUpvotesFlow = authRepository.currentUser.flatMapLatest { user ->
                if (user != null) {
                    upvoteRepository.observeUserUpvotes(user.userId)
                } else {
                    flowOf(emptySet())
                }
            }

            combine(
                reportRepository.observeReportsByArea(areaId).catch { e -> _uiState.value = HomeUiState.Error(e.message ?: "Unknown error") },
                userUpvotesFlow,
                _upvotingIds
            ) { reports, upvotedIds, upvotingIds ->
                if (reports.isEmpty()) {
                    HomeUiState.Empty
                } else {
                    val uiModels = reports.map { report ->
                        ReportUiModel(
                            report = report,
                            isUpvotedByMe = upvotedIds.contains(report.reportId),
                            isUpvoting = upvotingIds.contains(report.reportId)
                        )
                    }
                    HomeUiState.Success(uiModels)
                }
            }.collectLatest { state ->
                // Avoid overwriting error state if it happened in catch block
                if (_uiState.value !is HomeUiState.Error) {
                    _uiState.value = state
                }
            }
        }
    }

    fun toggleUpvote(reportId: String) {
        val user = authRepository.currentUser.value ?: return
        
        // Prevent duplicate calls
        if (_upvotingIds.value.contains(reportId)) return
        _upvotingIds.value = _upvotingIds.value + reportId

        viewModelScope.launch {
            upvoteRepository.toggleUpvote(user.userId, reportId)
            _upvotingIds.value = _upvotingIds.value - reportId
        }
    }
}

sealed class HomeUiState {
    object Loading : HomeUiState()
    object Empty : HomeUiState()
    object PermissionRequired : HomeUiState()
    object LocationDisabled : HomeUiState()
    object UnsupportedArea : HomeUiState()
    data class Success(val reports: List<ReportUiModel>) : HomeUiState()
    data class Error(val message: String) : HomeUiState()
}
