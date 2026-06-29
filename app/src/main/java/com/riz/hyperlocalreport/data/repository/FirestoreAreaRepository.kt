package com.riz.hyperlocalreport.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.riz.hyperlocalreport.core.common.Constants
import com.riz.hyperlocalreport.data.model.AreaDto
import com.riz.hyperlocalreport.data.model.toDomain
import com.riz.hyperlocalreport.domain.model.Area
import com.riz.hyperlocalreport.domain.repository.AreaRepository
import kotlinx.coroutines.tasks.await

class FirestoreAreaRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) : AreaRepository {
    override suspend fun getAreaById(areaId: String): Result<Area> {
        return try {
            val snapshot = firestore.collection(Constants.Collections.AREAS).document(areaId).get().await()
            val area = snapshot.toObject(AreaDto::class.java)?.toDomain()
            if (area != null) Result.success(area) else Result.failure(Exception("Area not found"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
