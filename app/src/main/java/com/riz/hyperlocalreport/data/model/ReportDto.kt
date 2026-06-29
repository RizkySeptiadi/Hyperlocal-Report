package com.riz.hyperlocalreport.data.model

import com.google.firebase.Timestamp
import com.riz.hyperlocalreport.domain.model.Report

data class ReportDto(
    val reportId: String = "",
    val title: String = "",
    val description: String = "",
    val category: String = "",
    val photoUrls: List<String> = emptyList(),
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val status: String = "",
    val upvoteCount: Long = 0L,
    val reporterId: String = "",
    val reporterName: String = "",
    val areaId: String = "",
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null,
    val resolvedAt: Timestamp? = null
)

fun ReportDto.toDomain() = Report(
    reportId = reportId,
    title = title,
    description = description,
    category = category,
    photoUrls = photoUrls,
    latitude = latitude,
    longitude = longitude,
    status = status,
    upvoteCount = upvoteCount,
    reporterId = reporterId,
    reporterName = reporterName,
    areaId = areaId,
    createdAt = createdAt?.toDate(),
    updatedAt = updatedAt?.toDate(),
    resolvedAt = resolvedAt?.toDate()
)
