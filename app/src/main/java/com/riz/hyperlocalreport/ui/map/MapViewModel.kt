package com.riz.hyperlocalreport.ui.map

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.riz.hyperlocalreport.core.common.AreaSessionManager
import com.riz.hyperlocalreport.core.common.AreaState
import com.riz.hyperlocalreport.core.common.UiState
import com.riz.hyperlocalreport.domain.model.Report
import com.riz.hyperlocalreport.domain.repository.ReportRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class MapViewModel(
    private val reportRepository: ReportRepository,
    val areaSessionManager: AreaSessionManager
) : ViewModel() {

    @OptIn(ExperimentalCoroutinesApi::class)
    val reportsState: StateFlow<UiState<List<Report>>> = areaSessionManager.areaState
        .flatMapLatest { state ->
            when (state) {
                is AreaState.Available, is AreaState.UnsupportedLocation -> {
                    reportRepository.observeAllReports()
                        .map<List<Report>, UiState<List<Report>>> { reports -> 
                            if (reports.isEmpty()) UiState.Empty
                            else UiState.Success(reports) 
                        }
                        .catch { e -> emit(UiState.Error(e.message ?: "Failed to load reports")) }
                }
                is AreaState.LoadingLocation, AreaState.ResolvingArea -> flowOf(UiState.Loading)
                else -> flowOf(UiState.Empty)
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = UiState.Loading
        )
}
