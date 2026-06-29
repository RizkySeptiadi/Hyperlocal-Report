package com.riz.hyperlocalreport.data.model

import com.google.firebase.Timestamp
import com.riz.hyperlocalreport.domain.model.Comment

data class CommentDto(
    val commentId: String = "",
    val reportId: String = "",
    val userId: String = "",
    val authorName: String = "",
    val content: String = "",
    @get:JvmName("getIsAdminReply")
    val isAdminReply: Boolean = false,
    val createdAt: Timestamp? = null
)

fun CommentDto.toDomain() = Comment(
    commentId = commentId,
    reportId = reportId,
    userId = userId,
    authorName = authorName,
    content = content,
    isAdminReply = isAdminReply,
    createdAt = createdAt?.toDate()
)
