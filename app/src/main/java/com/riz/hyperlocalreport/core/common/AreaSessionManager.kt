package com.riz.hyperlocalreport.core.common

import android.content.Context
import android.content.SharedPreferences
import com.riz.hyperlocalreport.domain.model.Area
import com.riz.hyperlocalreport.domain.repository.AreaResolver
import com.riz.hyperlocalreport.domain.repository.LocationRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AreaSessionManager(
    private val context: Context,
    private val locationRepository: LocationRepository,
    private val areaResolver: AreaResolver,
    private val sharedPreferences: SharedPreferences
) {
    private val _areaState = MutableStateFlow<AreaState>(AreaState.Uninitialized)
    val areaState: StateFlow<AreaState> = _areaState.asStateFlow()

    private val scope = CoroutineScope(Dispatchers.Main)

    init {
        loadCachedArea()
    }

    private fun loadCachedArea() {
        val cachedAreaId = sharedPreferences.getString("cached_area_id", null)
        val cachedAreaName = sharedPreferences.getString("cached_area_name", null)
        val cachedAddressName = sharedPreferences.getString("cached_address_name", null)
        val cachedLat = sharedPreferences.getFloat("cached_area_lat", 0f).toDouble()
        val cachedLng = sharedPreferences.getFloat("cached_area_lng", 0f).toDouble()
        
        if (cachedAreaId != null && cachedAreaName != null && cachedAddressName != null) {
            val area = Area(
                areaId = cachedAreaId,
                name = cachedAreaName,
                rt = "", // Simplified for cache
                rw = "", // Simplified for cache
                kelurahan = cachedAreaName,
                adminIds = emptyList()
            )
            _areaState.value = AreaState.Available(
                area = area,
                addressName = cachedAddressName,
                latitude = cachedLat,
                longitude = cachedLng,
                isCached = true
            )
        }
    }

    fun startResolution(hasPermission: Boolean) {
        if (!hasPermission) {
            _areaState.value = AreaState.PermissionRequired
            return
        }

        if (!locationRepository.isLocationEnabled()) {
            _areaState.value = AreaState.LocationDisabled
            return
        }

        _areaState.value = AreaState.LoadingLocation

        scope.launch {
            val locationResult = locationRepository.getCurrentLocation()
            if (locationResult.isSuccess) {
                val location = locationResult.getOrThrow()
                _areaState.value = AreaState.ResolvingArea
                
                val resolveResult = areaResolver.resolveArea(location.latitude, location.longitude)
                if (resolveResult.isSuccess) {
                    val locationRes = resolveResult.getOrNull()
                    if (locationRes?.area != null) {
                        saveCachedArea(locationRes.area, locationRes.addressName, location.latitude, location.longitude)
                        _areaState.value = AreaState.Available(
                            area = locationRes.area,
                            addressName = locationRes.addressName,
                            latitude = location.latitude,
                            longitude = location.longitude,
                            isCached = false
                        )
                    } else {
                        _areaState.value = AreaState.UnsupportedLocation(
                            addressName = locationRes?.addressName ?: "Unknown Location",
                            latitude = location.latitude,
                            longitude = location.longitude
                        )
                    }
                } else {
                    _areaState.value = AreaState.Error(
                        message = resolveResult.exceptionOrNull()?.message ?: "Failed to resolve area",
                        cachedArea = getCachedAreaOrNull(),
                        cachedAddressName = getCachedAddressNameOrNull()
                    )
                }
            } else {
                // Fallback to last known location if current location fails
                val lastKnownResult = locationRepository.getLastKnownLocation()
                if (lastKnownResult.isSuccess && lastKnownResult.getOrNull() != null) {
                    val location = lastKnownResult.getOrNull()!!
                    _areaState.value = AreaState.ResolvingArea
                    val resolveResult = areaResolver.resolveArea(location.latitude, location.longitude)
                    if (resolveResult.isSuccess && resolveResult.getOrNull()?.area != null) {
                        val locationRes = resolveResult.getOrNull()!!
                        saveCachedArea(locationRes.area!!, locationRes.addressName, location.latitude, location.longitude)
                        _areaState.value = AreaState.Available(locationRes.area, locationRes.addressName, location.latitude, location.longitude, false)
                        return@launch
                    }
                }
                
                _areaState.value = AreaState.Error(
                    message = locationResult.exceptionOrNull()?.message ?: "Failed to get location",
                    cachedArea = getCachedAreaOrNull(),
                    cachedAddressName = getCachedAddressNameOrNull()
                )
            }
        }
    }

    private fun saveCachedArea(area: Area, addressName: String, lat: Double, lng: Double) {
        sharedPreferences.edit()
            .putString("cached_area_id", area.areaId)
            .putString("cached_area_name", area.kelurahan)
            .putString("cached_address_name", addressName)
            .putFloat("cached_area_lat", lat.toFloat())
            .putFloat("cached_area_lng", lng.toFloat())
            .apply()
    }

    private fun getCachedAreaOrNull(): Area? {
        val state = _areaState.value
        return if (state is AreaState.Available && state.isCached) state.area else null
    }

    private fun getCachedAddressNameOrNull(): String? {
        val state = _areaState.value
        return if (state is AreaState.Available && state.isCached) state.addressName else null
    }

    fun currentAreaOrNull(): Area? {
        val state = _areaState.value
        return if (state is AreaState.Available) state.area else null
    }
}
