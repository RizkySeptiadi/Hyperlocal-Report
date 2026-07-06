package com.riz.hyperlocalreport.data.model

import com.google.firebase.Timestamp
import com.riz.hyperlocalreport.domain.model.User

data class UserDto(
    val userId: String = "",
    val name: String = "",
    val email: String = "",
    val phone: String = "",
    val role: String = "",
    val areaId: String = "",
    val fcmToken: String? = null,
    val profileImageUrl: String? = null,
    val createdAt: Timestamp? = null
)

fun UserDto.toDomain() = User(
    userId = userId,
    name = name,
    email = email,
    phone = phone,
    role = role,
    areaId = areaId,
    fcmToken = fcmToken,
    profileImageUrl = profileImageUrl,
    createdAt = createdAt?.toDate()
)
