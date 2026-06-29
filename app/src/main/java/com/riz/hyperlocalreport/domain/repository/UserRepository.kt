package com.riz.hyperlocalreport.domain.repository

import com.riz.hyperlocalreport.domain.model.User
import kotlinx.coroutines.flow.Flow

interface UserRepository {
    fun getUserProfile(userId: String): Flow<User?>
}
