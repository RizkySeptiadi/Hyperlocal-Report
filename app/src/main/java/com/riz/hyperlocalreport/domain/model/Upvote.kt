package com.riz.hyperlocalreport.domain.model

import java.util.Date

data class Upvote(
    val upvoteId: String,
    val userId: String,
    val reportId: String,
    val createdAt: Date?
)
