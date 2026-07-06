package com.riz.hyperlocalreport.domain.repository

import com.riz.hyperlocalreport.domain.model.User
import kotlinx.coroutines.flow.StateFlow

interface AuthRepository {
    val currentUser: StateFlow<User?>
    suspend fun login(email: String, password: String): Result<Unit>
    suspend fun register(name: String, email: String, phone: String, password: String, areaId: String): Result<Unit>
    suspend fun logout()
    suspend fun checkSession(): Boolean
    suspend fun googleSignIn(idToken: String): Result<Unit>
    suspend fun updateProfile(name: String, phone: String): Result<Unit>
    suspend fun uploadProfilePicture(uri: android.net.Uri): Result<String>
}
