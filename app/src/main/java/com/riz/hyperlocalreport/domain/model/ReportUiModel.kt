package com.riz.hyperlocalreport.domain.model

data class ReportUiModel(
    val report: Report,
    val isUpvotedByMe: Boolean,
    val isUpvoting: Boolean = false
)
