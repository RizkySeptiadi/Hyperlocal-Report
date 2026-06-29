package com.riz.hyperlocalreport.domain.model

import java.util.Date

data class User(
    val userId: String,
    val name: String,
    val email: String,
    val phone: String,
    val role: String,
    val areaId: String,
    val fcmToken: String?,
    val createdAt: Date?
)
