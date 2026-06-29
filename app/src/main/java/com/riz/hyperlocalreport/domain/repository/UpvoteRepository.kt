package com.riz.hyperlocalreport.domain.repository

import kotlinx.coroutines.flow.Flow

interface UpvoteRepository {
    suspend fun toggleUpvote(userId: String, reportId: String): Result<Boolean>
    fun observeUserUpvotes(userId: String): Flow<Set<String>>
}
