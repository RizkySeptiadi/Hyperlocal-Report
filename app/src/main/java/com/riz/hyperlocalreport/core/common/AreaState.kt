package com.riz.hyperlocalreport.core.common

import com.riz.hyperlocalreport.domain.model.Area

sealed interface AreaState {
    data object Uninitialized : AreaState
    data object PermissionRequired : AreaState
    data object LocationDisabled : AreaState
    data object LoadingLocation : AreaState
    data object ResolvingArea : AreaState

    data class Available(
        val area: Area,
        val addressName: String,
        val latitude: Double,
        val longitude: Double,
        val isCached: Boolean = false,
        val resolvedAt: Long = System.currentTimeMillis()
    ) : AreaState

    data class UnsupportedLocation(
        val addressName: String,
        val latitude: Double,
        val longitude: Double
    ) : AreaState

    data class Error(
        val message: String,
        val cachedArea: Area? = null,
        val cachedAddressName: String? = null
    ) : AreaState
}
