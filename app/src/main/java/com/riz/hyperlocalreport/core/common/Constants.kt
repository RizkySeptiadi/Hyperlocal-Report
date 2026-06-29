package com.riz.hyperlocalreport.core.common

object Constants {
    object Roles {
        const val WARGA = "WARGA"
        const val ADMIN_RT_RW = "ADMIN_RT_RW"
        const val ADMIN_KELURAHAN = "ADMIN_KELURAHAN"
    }

    object Collections {
        const val USERS = "users"
        const val REPORTS = "reports"
        const val COMMENTS = "comments"
        const val UPVOTES = "upvotes"
        const val AREAS = "areas"
        const val NOTIFICATIONS = "notifications"
    }

    object Categories {
        const val ROAD = "ROAD"
        const val LIGHTING = "LIGHTING"
        const val WASTE = "WASTE"
        const val DRAINAGE = "DRAINAGE"
        const val PUBLIC_FACILITY = "PUBLIC_FACILITY"
        const val SECURITY = "SECURITY"
    }

    object ReportStatus {
        const val NEW = "NEW"
        const val VERIFIED = "VERIFIED"
        const val PROCESSING = "PROCESSING"
        const val DONE = "DONE"
        const val REJECTED = "REJECTED"
    }
}
