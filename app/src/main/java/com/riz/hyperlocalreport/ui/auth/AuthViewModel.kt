package com.riz.hyperlocalreport.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.riz.hyperlocalreport.core.common.UiState
import com.riz.hyperlocalreport.domain.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AuthViewModel(private val authRepository: AuthRepository) : ViewModel() {

    private val _loginState = MutableStateFlow<UiState<Unit>>(UiState.Empty)
    val loginState: StateFlow<UiState<Unit>> = _loginState.asStateFlow()

    private val _registerState = MutableStateFlow<UiState<Unit>>(UiState.Empty)
    val registerState: StateFlow<UiState<Unit>> = _registerState.asStateFlow()

    fun login(email: String, pass: String) {
        if (email.isBlank() || pass.isBlank()) {
            _loginState.value = UiState.Error("Email and password are required")
            return
        }
        viewModelScope.launch {
            _loginState.value = UiState.Loading
            val result = authRepository.login(email, pass)
            if (result.isSuccess) {
                _loginState.value = UiState.Success(Unit)
            } else {
                _loginState.value = UiState.Error(result.exceptionOrNull()?.message ?: "Login failed")
            }
        }
    }

    fun loginWithGoogle(idToken: String) {
        viewModelScope.launch {
            _loginState.value = UiState.Loading
            val result = authRepository.googleSignIn(idToken)
            if (result.isSuccess) {
                _loginState.value = UiState.Success(Unit)
            } else {
                _loginState.value = UiState.Error(result.exceptionOrNull()?.message ?: "Google login failed")
            }
        }
    }

    fun register(name: String, email: String, phone: String, area: String, pass: String, confirmPass: String) {
        if (name.isBlank() || email.isBlank() || phone.isBlank() || area.isBlank() || pass.isBlank() || confirmPass.isBlank()) {
            _registerState.value = UiState.Error("All fields are required")
            return
        }
        if (pass.length < 6) {
            _registerState.value = UiState.Error("Password must be at least 6 characters")
            return
        }
        if (pass != confirmPass) {
            _registerState.value = UiState.Error("Passwords do not match")
            return
        }
        // Basic phone validation for Indonesia (08... or +62...)
        if (!phone.matches(Regex("^(0|\\+62)\\d{8,13}$"))) {
            _registerState.value = UiState.Error("Invalid phone number format")
            return
        }
        
        viewModelScope.launch {
            _registerState.value = UiState.Loading
            val result = authRepository.register(name, email, phone, pass, area)
            if (result.isSuccess) {
                _registerState.value = UiState.Success(Unit)
            } else {
                _registerState.value = UiState.Error(result.exceptionOrNull()?.message ?: "Registration failed")
            }
        }
    }
}
