package com.riz.hyperlocalreport.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.riz.hyperlocalreport.core.common.Constants
import com.riz.hyperlocalreport.domain.repository.UpvoteRepository
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class FirestoreUpvoteRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) : UpvoteRepository {
    override suspend fun toggleUpvote(userId: String, reportId: String): Result<Boolean> {
        val upvoteId = "${userId}_${reportId}"
        val upvoteRef = firestore.collection(Constants.Collections.UPVOTES).document(upvoteId)
        val reportRef = firestore.collection(Constants.Collections.REPORTS).document(reportId)

        return try {
            val isAdded = firestore.runTransaction { transaction ->
                val upvoteSnapshot = transaction.get(upvoteRef)
                val reportSnapshot = transaction.get(reportRef)
                val currentCount = reportSnapshot.getLong("upvoteCount") ?: 0L

                if (upvoteSnapshot.exists()) {
                    transaction.delete(upvoteRef)
                    val newCount = maxOf(0L, currentCount - 1)
                    transaction.update(reportRef, "upvoteCount", newCount)
                    false
                } else {
                    val data = hashMapOf(
                        "upvoteId" to upvoteId,
                        "userId" to userId,
                        "reportId" to reportId,
                        "createdAt" to com.google.firebase.firestore.FieldValue.serverTimestamp()
                    )
                    transaction.set(upvoteRef, data)
                    transaction.update(reportRef, "upvoteCount", currentCount + 1)
                    true
                }
            }.await()
            Result.success(isAdded)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun observeUserUpvotes(userId: String): Flow<Set<String>> = callbackFlow {
        val listener = firestore.collection(Constants.Collections.UPVOTES)
            .whereEqualTo("userId", userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(emptySet())
                    return@addSnapshotListener
                }
                val set = snapshot?.documents?.mapNotNull { it.getString("reportId") }?.toSet() ?: emptySet()
                trySend(set)
            }
        awaitClose { listener.remove() }
    }
}
