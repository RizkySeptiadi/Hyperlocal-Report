package com.riz.hyperlocalreport.data.model

import com.google.firebase.Timestamp
import com.riz.hyperlocalreport.domain.model.Notification

data class NotificationDto(
    val notificationId: String = "",
    val userId: String = "",
    val reportId: String? = null,
    val title: String = "",
    val message: String = "",
    val type: String = "",
    @get:JvmName("getIsRead")
    val isRead: Boolean = false,
    val createdAt: Timestamp? = null
)

fun NotificationDto.toDomain() = Notification(
    notificationId = notificationId,
    userId = userId,
    reportId = reportId,
    title = title,
    message = message,
    type = type,
    isRead = isRead,
    createdAt = createdAt?.toDate()
)
