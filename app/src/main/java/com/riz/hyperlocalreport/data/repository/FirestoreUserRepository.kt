package com.riz.hyperlocalreport.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.riz.hyperlocalreport.core.common.Constants
import com.riz.hyperlocalreport.data.model.UserDto
import com.riz.hyperlocalreport.data.model.toDomain
import com.riz.hyperlocalreport.domain.model.User
import com.riz.hyperlocalreport.domain.repository.UserRepository
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

class FirestoreUserRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) : UserRepository {
    override fun getUserProfile(userId: String): Flow<User?> = callbackFlow {
        val listener = firestore.collection(Constants.Collections.USERS)
            .document(userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(null)
                    return@addSnapshotListener
                }
                val dto = snapshot?.toObject(UserDto::class.java)
                trySend(dto?.toDomain())
            }
        awaitClose { listener.remove() }
    }
}
