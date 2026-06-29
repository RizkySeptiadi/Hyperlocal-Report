package com.riz.hyperlocalreport.domain.model

import java.util.Date

data class Comment(
    val commentId: String,
    val reportId: String,
    val userId: String,
    val authorName: String,
    val content: String,
    val isAdminReply: Boolean,
    val createdAt: Date?
)
