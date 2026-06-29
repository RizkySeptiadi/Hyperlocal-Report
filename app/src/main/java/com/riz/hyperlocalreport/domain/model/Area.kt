package com.riz.hyperlocalreport.domain.model

data class Area(
    val areaId: String,
    val name: String,
    val rt: String,
    val rw: String,
    val kelurahan: String,
    val adminIds: List<String>
)
