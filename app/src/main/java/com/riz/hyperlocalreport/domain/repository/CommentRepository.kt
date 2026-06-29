package com.riz.hyperlocalreport.domain.repository

import com.riz.hyperlocalreport.domain.model.Comment
import kotlinx.coroutines.flow.Flow

interface CommentRepository {
    fun observeComments(reportId: String): Flow<List<Comment>>
    suspend fun addComment(comment: Comment): Result<Unit>
}
