package com.riz.hyperlocalreport

import android.content.Context
import com.google.android.gms.location.LocationServices
import com.riz.hyperlocalreport.core.common.AreaSessionManager
import com.riz.hyperlocalreport.core.common.SessionManager
import com.riz.hyperlocalreport.data.repository.FirebaseAuthRepository
import com.riz.hyperlocalreport.data.repository.FirestoreCommentRepository
import com.riz.hyperlocalreport.data.repository.FirestoreReportRepository
import com.riz.hyperlocalreport.data.repository.FirestoreAreaResolver
import com.riz.hyperlocalreport.data.repository.PlayServicesLocationRepository
import com.riz.hyperlocalreport.domain.repository.AuthRepository
import com.riz.hyperlocalreport.domain.repository.CommentRepository
import com.riz.hyperlocalreport.domain.repository.ReportRepository
import com.riz.hyperlocalreport.data.repository.FirestoreUpvoteRepository
import com.riz.hyperlocalreport.domain.repository.UpvoteRepository
import com.riz.hyperlocalreport.domain.repository.AreaResolver
import com.riz.hyperlocalreport.domain.repository.LocationRepository

class AppContainer(private val context: Context) {
    val sessionManager: SessionManager by lazy {
        SessionManager(context)
    }

    val locationRepository: LocationRepository by lazy {
        PlayServicesLocationRepository(
            context,
            LocationServices.getFusedLocationProviderClient(context)
        )
    }

    val areaResolver: AreaResolver by lazy {
        FirestoreAreaResolver(context)
    }

    val areaSessionManager: AreaSessionManager by lazy {
        AreaSessionManager(
            context,
            locationRepository,
            areaResolver,
            context.getSharedPreferences("area_prefs", Context.MODE_PRIVATE)
        )
    }

    val authRepository: AuthRepository by lazy {
        FirebaseAuthRepository()
    }
    
    val reportRepository: ReportRepository by lazy {
        FirestoreReportRepository()
    }

    val commentRepository: CommentRepository by lazy {
        FirestoreCommentRepository()
    }

    val upvoteRepository: UpvoteRepository by lazy {
        FirestoreUpvoteRepository()
    }
}
