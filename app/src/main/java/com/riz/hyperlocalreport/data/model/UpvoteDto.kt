package com.riz.hyperlocalreport.data.model

import com.google.firebase.Timestamp
import com.riz.hyperlocalreport.domain.model.Upvote

data class UpvoteDto(
    val upvoteId: String = "",
    val userId: String = "",
    val reportId: String = "",
    val createdAt: Timestamp? = null
)

fun UpvoteDto.toDomain() = Upvote(
    upvoteId = upvoteId,
    userId = userId,
    reportId = reportId,
    createdAt = createdAt?.toDate()
)
