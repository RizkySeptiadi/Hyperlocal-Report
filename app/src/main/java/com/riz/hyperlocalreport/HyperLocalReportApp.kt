package com.riz.hyperlocalreport

import android.app.Application

class HyperLocalReportApp : Application() {
    lateinit var container: AppContainer

    override fun onCreate() {
        super.onCreate()
        androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO)
        container = AppContainer(this)
    }
}
