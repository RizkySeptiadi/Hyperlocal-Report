package com.riz.hyperlocalreport.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.riz.hyperlocalreport.domain.model.User
import com.riz.hyperlocalreport.domain.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ProfileViewModel(
    private val authRepository: AuthRepository
) : ViewModel() {

    val currentUser: StateFlow<User?> = authRepository.currentUser

    private val _updateState = MutableStateFlow<ProfileUpdateState>(ProfileUpdateState.Idle)
    val updateState: StateFlow<ProfileUpdateState> = _updateState.asStateFlow()

    fun updateProfile(name: String, phone: String) {
        viewModelScope.launch {
            _updateState.value = ProfileUpdateState.Loading
            val result = authRepository.updateProfile(name, phone)
            if (result.isSuccess) {
                _updateState.value = ProfileUpdateState.Success
            } else {
                _updateState.value = ProfileUpdateState.Error(result.exceptionOrNull()?.message ?: "Unknown error")
            }
        }
    }
    
    fun resetUpdateState() {
        _updateState.value = ProfileUpdateState.Idle
    }

    fun logout() {
        viewModelScope.launch {
            authRepository.logout()
        }
    }
}

sealed class ProfileUpdateState {
    object Idle : ProfileUpdateState()
    object Loading : ProfileUpdateState()
    object Success : ProfileUpdateState()
    data class Error(val message: String) : ProfileUpdateState()
}
