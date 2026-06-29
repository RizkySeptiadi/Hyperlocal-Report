package com.riz.hyperlocalreport.domain.model

import java.util.Date

data class Report(
    val reportId: String,
    val title: String,
    val description: String,
    val category: String,
    val photoUrls: List<String>,
    val latitude: Double,
    val longitude: Double,
    val status: String,
    val upvoteCount: Long,
    val reporterId: String,
    val reporterName: String,
    val areaId: String,
    val createdAt: Date?,
    val updatedAt: Date?,
    val resolvedAt: Date?
)
