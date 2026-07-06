package com.riz.hyperlocalreport.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.riz.hyperlocalreport.core.common.Constants
import com.riz.hyperlocalreport.data.model.ReportDto
import com.riz.hyperlocalreport.data.model.toDomain
import com.riz.hyperlocalreport.domain.model.Report
import com.riz.hyperlocalreport.domain.repository.ReportRepository
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class FirestoreReportRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) : ReportRepository {

    override fun observeAllReports(): Flow<List<Report>> = callbackFlow {
        val listener = firestore.collection(Constants.Collections.REPORTS)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val reports = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(ReportDto::class.java)?.toDomain()
                }?.sortedByDescending { it.createdAt } ?: emptyList()
                trySend(reports)
            }
        awaitClose { listener.remove() }
    }

    override fun getReportById(reportId: String): Flow<Report?> = callbackFlow {
        val listener = firestore.collection(Constants.Collections.REPORTS)
            .document(reportId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(null)
                    return@addSnapshotListener
                }
                trySend(snapshot?.toObject(ReportDto::class.java)?.toDomain())
            }
        awaitClose { listener.remove() }
    }

    override suspend fun createReport(report: Report): Result<Unit> {
        return try {
            val docRef = firestore.collection(Constants.Collections.REPORTS).document()
            val data = hashMapOf(
                "reportId" to docRef.id,
                "title" to report.title,
                "description" to report.description,
                "category" to report.category,
                "photoUrls" to report.photoUrls,
                "latitude" to report.latitude,
                "longitude" to report.longitude,
                "status" to report.status,
                "upvoteCount" to 0L,
                "reporterId" to report.reporterId,
                "reporterName" to report.reporterName,
                "areaId" to report.areaId,
                "createdAt" to com.google.firebase.firestore.FieldValue.serverTimestamp()
            )
            docRef.set(data).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateReportStatus(reportId: String, status: String): Result<Unit> {
        return try {
            firestore.collection(Constants.Collections.REPORTS).document(reportId)
                .update(
                    "status", status,
                    "updatedAt", com.google.firebase.firestore.FieldValue.serverTimestamp()
                ).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun toggleUpvote(reportId: String, userId: String): Result<Boolean> {
        return try {
            val upvoteRef = firestore.collection(Constants.Collections.UPVOTES).document("${reportId}_$userId")
            val reportRef = firestore.collection(Constants.Collections.REPORTS).document(reportId)
            
            val isUpvoted = firestore.runTransaction { transaction ->
                val upvoteSnapshot = transaction.get(upvoteRef)
                if (upvoteSnapshot.exists()) {
                    transaction.delete(upvoteRef)
                    transaction.update(reportRef, "upvoteCount", com.google.firebase.firestore.FieldValue.increment(-1))
                    false
                } else {
                    transaction.set(upvoteRef, hashMapOf("reportId" to reportId, "userId" to userId))
                    transaction.update(reportRef, "upvoteCount", com.google.firebase.firestore.FieldValue.increment(1))
                    true
                }
            }.await()
            
            Result.success(isUpvoted)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun uploadPhoto(localUriString: String, path: String): Result<String> {
        return kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val uri = android.net.Uri.parse(localUriString)
                val pathStr = uri.path ?: return@withContext Result.failure(Exception("Invalid URI path"))
                val file = java.io.File(pathStr)
                val bytes = file.readBytes()
                val base64 = android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP)
                Result.success("base64:$base64")
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    override fun observeReportsByUser(userId: String): Flow<List<Report>> = callbackFlow {
        val subscription = firestore.collection(Constants.Collections.REPORTS)
            .whereEqualTo("reporterId", userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val reports = snapshot.documents.mapNotNull { it.toObject(ReportDto::class.java)?.toDomain() }
                        .sortedByDescending { it.createdAt }
                    trySend(reports)
                }
            }
        awaitClose { subscription.remove() }
    }
}
