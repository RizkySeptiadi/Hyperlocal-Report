package com.riz.hyperlocalreport.domain.repository

import com.riz.hyperlocalreport.domain.model.Notification
import kotlinx.coroutines.flow.Flow

interface NotificationRepository {
    fun observeNotifications(userId: String): Flow<List<Notification>>
    suspend fun markAsRead(notificationId: String): Result<Unit>
}
