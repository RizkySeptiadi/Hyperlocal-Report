package com.riz.hyperlocalreport.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.riz.hyperlocalreport.domain.model.Report
import com.riz.hyperlocalreport.domain.repository.AuthRepository
import com.riz.hyperlocalreport.domain.repository.ReportRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class HistoryViewModel(
    private val authRepository: AuthRepository,
    private val reportRepository: ReportRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<HistoryUiState>(HistoryUiState.Loading)
    val uiState: StateFlow<HistoryUiState> = _uiState.asStateFlow()

    init {
        loadHistory()
    }

    private fun loadHistory() {
        viewModelScope.launch {
            val user = authRepository.currentUser.value
            if (user == null) {
                _uiState.value = HistoryUiState.Guest
                return@launch
            }

            reportRepository.observeReportsByUser(user.userId)
                .catch { e ->
                    _uiState.value = HistoryUiState.Error(e.message ?: "Unknown error")
                }
                .collectLatest { reports ->
                    if (reports.isEmpty()) {
                        _uiState.value = HistoryUiState.Empty
                    } else {
                        _uiState.value = HistoryUiState.Success(reports)
                    }
                }
        }
    }
}

sealed class HistoryUiState {
    object Loading : HistoryUiState()
    object Empty : HistoryUiState()
    object Guest : HistoryUiState()
    data class Success(val reports: List<Report>) : HistoryUiState()
    data class Error(val message: String) : HistoryUiState()
}
