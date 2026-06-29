package com.riz.hyperlocalreport.domain.repository

import android.location.Location

interface LocationRepository {
    suspend fun getCurrentLocation(): Result<Location>
    suspend fun getLastKnownLocation(): Result<Location?>
    fun isLocationEnabled(): Boolean
}
