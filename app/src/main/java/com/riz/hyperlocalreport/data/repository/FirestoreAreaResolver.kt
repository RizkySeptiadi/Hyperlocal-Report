package com.riz.hyperlocalreport.data.repository

import android.content.Context
import android.location.Geocoder
import com.google.firebase.firestore.FirebaseFirestore
import com.riz.hyperlocalreport.core.common.Constants
import com.riz.hyperlocalreport.data.model.AreaDto
import com.riz.hyperlocalreport.data.model.toDomain
import com.riz.hyperlocalreport.domain.model.Area
import com.riz.hyperlocalreport.domain.model.LocationResult
import com.riz.hyperlocalreport.domain.repository.AreaResolver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.Locale

class FirestoreAreaResolver(
    private val context: Context,
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) : AreaResolver {

    override suspend fun resolveArea(latitude: Double, longitude: Double): Result<LocationResult> {
        return withContext(Dispatchers.IO) {
            try {
                val geocoder = Geocoder(context, Locale.getDefault())
                val addresses = geocoder.getFromLocation(latitude, longitude, 1)
                
                if (addresses.isNullOrEmpty()) {
                    return@withContext Result.success(LocationResult(null, "Unknown Location ($latitude, $longitude)"))
                }

                val address = addresses[0]
                // SubLocality usually corresponds to Kelurahan/Village in Indonesia
                val kelurahan = address.subLocality ?: address.locality
                val addressName = address.getAddressLine(0) ?: kelurahan ?: "Unknown Location"
                
                if (kelurahan.isNullOrBlank()) {
                    return@withContext Result.success(LocationResult(null, addressName))
                }

                val snapshot = firestore.collection(Constants.Collections.AREAS)
                    .whereEqualTo("kelurahan", kelurahan)
                    .limit(1)
                    .get()
                    .await()

                if (snapshot.isEmpty) {
                    // Create a dynamic synthesized Area for unsupported regions
                    // This allows the user to still view and create reports in their locality
                    // even if the admin hasn't officially registered it.
                    val dynamicArea = Area(
                        areaId = kelurahan, // Use the locality name as the areaId for grouping
                        name = kelurahan,
                        rt = "",
                        rw = "",
                        kelurahan = kelurahan,
                        adminIds = emptyList()
                    )
                    Result.success(LocationResult(dynamicArea, addressName))
                } else {
                    val areaDto = snapshot.documents[0].toObject(AreaDto::class.java)
                    Result.success(LocationResult(areaDto?.toDomain(), addressName))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
}
