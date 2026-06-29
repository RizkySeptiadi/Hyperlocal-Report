package com.riz.hyperlocalreport.domain.repository

import com.riz.hyperlocalreport.domain.model.Area

interface AreaRepository {
    suspend fun getAreaById(areaId: String): Result<Area>
}
