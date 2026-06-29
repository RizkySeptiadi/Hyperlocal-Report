package com.riz.hyperlocalreport.ui.map

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.riz.hyperlocalreport.core.common.AreaSessionManager
import com.riz.hyperlocalreport.domain.repository.ReportRepository

class MapViewModelFactory(
    private val reportRepository: ReportRepository,
    private val areaSessionManager: AreaSessionManager
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MapViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MapViewModel(reportRepository, areaSessionManager) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
