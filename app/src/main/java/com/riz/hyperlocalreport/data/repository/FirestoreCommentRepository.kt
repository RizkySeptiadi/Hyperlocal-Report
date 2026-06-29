package com.riz.hyperlocalreport.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.riz.hyperlocalreport.core.common.Constants
import com.riz.hyperlocalreport.data.model.CommentDto
import com.riz.hyperlocalreport.data.model.toDomain
import com.riz.hyperlocalreport.domain.model.Comment
import com.riz.hyperlocalreport.domain.repository.CommentRepository
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class FirestoreCommentRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) : CommentRepository {
    override fun observeComments(reportId: String): Flow<List<Comment>> = callbackFlow {
        val listener = firestore.collection(Constants.Collections.COMMENTS)
            .whereEqualTo("reportId", reportId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val comments = snapshot?.documents?.mapNotNull {
                    it.toObject(CommentDto::class.java)?.toDomain()
                }?.sortedBy { it.createdAt?.time ?: 0L } ?: emptyList()
                trySend(comments)
            }
        awaitClose { listener.remove() }
    }

    override suspend fun addComment(comment: Comment): Result<Unit> {
        return try {
            val docRef = firestore.collection(Constants.Collections.COMMENTS).document()
            val data = hashMapOf(
                "commentId" to docRef.id,
                "reportId" to comment.reportId,
                "userId" to comment.userId,
                "authorName" to comment.authorName,
                "content" to comment.content,
                "isAdminReply" to comment.isAdminReply,
                "createdAt" to com.google.firebase.firestore.FieldValue.serverTimestamp()
            )
            docRef.set(data).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
