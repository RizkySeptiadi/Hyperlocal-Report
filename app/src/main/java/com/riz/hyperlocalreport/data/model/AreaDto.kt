package com.riz.hyperlocalreport.data.model

import com.riz.hyperlocalreport.domain.model.Area

data class AreaDto(
    val areaId: String = "",
    val name: String = "",
    val rt: String = "",
    val rw: String = "",
    val kelurahan: String = "",
    val adminIds: List<String> = emptyList()
)

fun AreaDto.toDomain() = Area(
    areaId = areaId,
    name = name,
    rt = rt,
    rw = rw,
    kelurahan = kelurahan,
    adminIds = adminIds
)
