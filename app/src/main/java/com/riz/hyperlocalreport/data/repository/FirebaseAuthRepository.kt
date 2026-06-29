package com.riz.hyperlocalreport.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging
import com.riz.hyperlocalreport.core.common.Constants
import com.riz.hyperlocalreport.data.model.UserDto
import com.riz.hyperlocalreport.data.model.toDomain
import com.riz.hyperlocalreport.domain.model.User
import com.riz.hyperlocalreport.domain.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await
import java.util.Date

class FirebaseAuthRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) : AuthRepository {

    private val _currentUser = MutableStateFlow<User?>(null)
    override val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    override suspend fun login(email: String, password: String): Result<Unit> {
        return try {
            auth.signInWithEmailAndPassword(email, password).await()
            val user = auth.currentUser
            if (user != null) {
                // Fetch latest FCM token
                val token = try { FirebaseMessaging.getInstance().token.await() } catch (e: Exception) { null }
                if (token != null) {
                    firestore.collection(Constants.Collections.USERS).document(user.uid)
                        .update("fcmToken", token).await()
                }

                val snapshot = firestore.collection(Constants.Collections.USERS).document(user.uid).get().await()
                val profile = snapshot.toObject(UserDto::class.java)
                _currentUser.value = profile?.toDomain()
                if (profile != null) Result.success(Unit) else Result.failure(Exception("Profile not found"))
            } else {
                Result.failure(Exception("Login failed"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun register(
        name: String,
        email: String,
        phone: String,
        password: String,
        areaId: String
    ): Result<Unit> {
        return try {
            val authResult = auth.createUserWithEmailAndPassword(email, password).await()
            val user = authResult.user ?: throw Exception("Registration failed")
            
            val token = try { FirebaseMessaging.getInstance().token.await() } catch (e: Exception) { null }
            
            val userProfile = hashMapOf(
                "userId" to user.uid,
                "name" to name,
                "email" to email,
                "phone" to phone,
                "role" to Constants.Roles.WARGA,
                "areaId" to areaId,
                "fcmToken" to token,
                "createdAt" to FieldValue.serverTimestamp()
            )

            try {
                firestore.collection(Constants.Collections.USERS).document(user.uid).set(userProfile).await()
                _currentUser.value = User(
                    userId = user.uid,
                    name = name,
                    email = email,
                    phone = phone,
                    role = Constants.Roles.WARGA,
                    areaId = areaId,
                    fcmToken = token,
                    createdAt = Date() // Approximate for local state until reload
                )
                Result.success(Unit)
            } catch (e: Exception) {
                user.delete().await()
                throw Exception("Failed to create profile. Account deleted.")
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun logout() {
        auth.signOut()
        _currentUser.value = null
    }

    override suspend fun checkSession(): Boolean {
        val user = auth.currentUser
        return if (user != null) {
            try {
                val snapshot = firestore.collection(Constants.Collections.USERS).document(user.uid).get().await()
                val profile = snapshot.toObject(UserDto::class.java)
                if (profile != null) {
                    _currentUser.value = profile.toDomain()
                    true
                } else {
                    logout()
                    false
                }
            } catch (e: Exception) {
                false
            }
        } else {
            false
        }
    }

    override suspend fun updateProfile(name: String, phone: String): Result<Unit> {
        return try {
            val user = auth.currentUser ?: return Result.failure(Exception("Not authenticated"))
            firestore.collection(Constants.Collections.USERS).document(user.uid)
                .update(
                    mapOf(
                        "name" to name,
                        "phone" to phone
                    )
                ).await()
            val profileSnapshot = firestore.collection(Constants.Collections.USERS).document(user.uid).get().await()
            val profile = profileSnapshot.toObject(UserDto::class.java)
            _currentUser.value = profile?.toDomain()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun googleSignIn(idToken: String): Result<Unit> {
        return try {
            val credential = com.google.firebase.auth.GoogleAuthProvider.getCredential(idToken, null)
            val authResult = auth.signInWithCredential(credential).await()
            val user = authResult.user ?: throw Exception("Google login failed")

            val snapshot = firestore.collection(Constants.Collections.USERS).document(user.uid).get().await()
            if (!snapshot.exists()) {
                val token = try { FirebaseMessaging.getInstance().token.await() } catch (e: Exception) { null }
                val name = user.displayName ?: "Google User"
                val email = user.email ?: ""
                val userProfile = hashMapOf(
                    "userId" to user.uid,
                    "name" to name,
                    "email" to email,
                    "phone" to "",
                    "role" to Constants.Roles.WARGA,
                    "areaId" to "",
                    "fcmToken" to token,
                    "createdAt" to FieldValue.serverTimestamp()
                )
                firestore.collection(Constants.Collections.USERS).document(user.uid).set(userProfile).await()
            } else {
                val token = try { FirebaseMessaging.getInstance().token.await() } catch (e: Exception) { null }
                if (token != null) {
                    firestore.collection(Constants.Collections.USERS).document(user.uid)
                        .update("fcmToken", token).await()
                }
            }

            val finalSnapshot = firestore.collection(Constants.Collections.USERS).document(user.uid).get().await()
            val profile = finalSnapshot.toObject(UserDto::class.java)
            _currentUser.value = profile?.toDomain()
            if (profile != null) Result.success(Unit) else Result.failure(Exception("Profile not found"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
