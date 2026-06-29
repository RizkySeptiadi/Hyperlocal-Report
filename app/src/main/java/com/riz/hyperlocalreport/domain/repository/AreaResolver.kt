package com.riz.hyperlocalreport.domain.repository

import com.riz.hyperlocalreport.domain.model.LocationResult

interface AreaResolver {
    suspend fun resolveArea(latitude: Double, longitude: Double): Result<LocationResult>
}
