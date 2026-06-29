package com.riz.hyperlocalreport.core.util

import com.riz.hyperlocalreport.core.common.Constants

fun String.toDisplayStatus(): String {
    return when (this.uppercase()) {
        Constants.ReportStatus.NEW -> "Baru"
        Constants.ReportStatus.VERIFIED -> "Terverifikasi"
        Constants.ReportStatus.PROCESSING -> "Diproses"
        Constants.ReportStatus.DONE -> "Selesai"
        Constants.ReportStatus.REJECTED -> "Ditolak"
        else -> this
    }
}

fun String.toDbStatus(): String {
    return when (this.uppercase()) {
        "BARU", "NEW" -> Constants.ReportStatus.NEW
        "TERVERIFIKASI", "VERIFIED" -> Constants.ReportStatus.VERIFIED
        "DIPROSES", "PROCESSING" -> Constants.ReportStatus.PROCESSING
        "SELESAI", "DONE" -> Constants.ReportStatus.DONE
        "DITOLAK", "REJECTED" -> Constants.ReportStatus.REJECTED
        else -> this
    }
}

fun String.toDisplayCategory(): String {
    return when (this.uppercase()) {
        Constants.Categories.ROAD -> "Jalan"
        Constants.Categories.LIGHTING -> "Lampu"
        Constants.Categories.WASTE -> "Sampah"
        Constants.Categories.DRAINAGE -> "Drainase"
        Constants.Categories.PUBLIC_FACILITY -> "Fasilitas Umum"
        Constants.Categories.SECURITY -> "Keamanan"
        else -> this
    }
}

fun String.toDbCategory(): String {
    return when (this.uppercase()) {
        "JALAN", "ROAD" -> Constants.Categories.ROAD
        "LAMPU", "LIGHTING" -> Constants.Categories.LIGHTING
        "SAMPAH", "WASTE" -> Constants.Categories.WASTE
        "DRAINASE", "DRAINAGE" -> Constants.Categories.DRAINAGE
        "FASILITAS UMUM", "PUBLIC_FACILITY" -> Constants.Categories.PUBLIC_FACILITY
        "KEAMANAN", "SECURITY" -> Constants.Categories.SECURITY
        else -> this
    }
}
