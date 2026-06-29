package com.riz.hyperlocalreport.domain.model

import java.util.Date

data class Notification(
    val notificationId: String,
    val userId: String,
    val reportId: String?,
    val title: String,
    val message: String,
    val type: String,
    val isRead: Boolean,
    val createdAt: Date?
)
