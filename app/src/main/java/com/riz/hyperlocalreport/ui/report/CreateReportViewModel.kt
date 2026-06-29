package com.riz.hyperlocalreport.ui.report

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.riz.hyperlocalreport.core.common.AreaSessionManager
import com.riz.hyperlocalreport.core.common.Constants
import com.riz.hyperlocalreport.domain.model.Report
import com.riz.hyperlocalreport.domain.repository.AuthRepository
import com.riz.hyperlocalreport.domain.repository.ReportRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Date

sealed class CreateReportUiState {
    object Idle : CreateReportUiState()
    object Loading : CreateReportUiState()
    object Success : CreateReportUiState()
    data class Error(val message: String) : CreateReportUiState()
    data class ValidationError(val message: String) : CreateReportUiState()
}

class CreateReportViewModel(
    private val authRepository: AuthRepository,
    private val reportRepository: ReportRepository,
    private val areaSessionManager: AreaSessionManager
) : ViewModel() {

    private val _uiState = MutableStateFlow<CreateReportUiState>(CreateReportUiState.Idle)
    val uiState: StateFlow<CreateReportUiState> = _uiState.asStateFlow()

    fun submitReport(
        title: String,
        description: String,
        category: String,
        photoUri: String?,
        latitude: Double?,
        longitude: Double?
    ) {
        if (title.isBlank() || description.isBlank() || category.isBlank() || photoUri == null || latitude == null || longitude == null) {
            _uiState.value = CreateReportUiState.ValidationError("All fields, photo, and location are required.")
            return
        }

        val user = authRepository.currentUser.value
        if (user == null) {
            _uiState.value = CreateReportUiState.Error("User not authenticated.")
            return
        }

        val area = areaSessionManager.currentAreaOrNull()
        if (area == null) {
            _uiState.value = CreateReportUiState.ValidationError("Current area could not be resolved. Please enable location.")
            return
        }

        _uiState.value = CreateReportUiState.Loading

        viewModelScope.launch {
            try {
                val timestamp = System.currentTimeMillis()
                val fileName = "photo_${timestamp}.jpg"
                val path = "reports/${user.userId}/${fileName}"
                
                val uploadResult = reportRepository.uploadPhoto(photoUri, path)
                val photoUrl = uploadResult.getOrElse {
                    _uiState.value = CreateReportUiState.Error("Photo upload failed: ${it.message}")
                    return@launch
                }

                val report = Report(
                    reportId = "", // Set by repository
                    title = title,
                    description = description,
                    category = category,
                    photoUrls = listOf(photoUrl),
                    latitude = latitude,
                    longitude = longitude,
                    status = Constants.ReportStatus.NEW,
                    upvoteCount = 0L,
                    reporterId = user.userId,
                    reporterName = user.name,
                    areaId = area.areaId,
                    createdAt = Date(),
                    updatedAt = Date(),
                    resolvedAt = null
                )

                val createResult = reportRepository.createReport(report)
                if (createResult.isSuccess) {
                    _uiState.value = CreateReportUiState.Success
                } else {
                    _uiState.value = CreateReportUiState.Error(createResult.exceptionOrNull()?.message ?: "Failed to create report")
                }
            } catch (e: Exception) {
                _uiState.value = CreateReportUiState.Error(e.message ?: "Unknown error")
            }
        }
    }
}
