package com.riz.hyperlocalreport

import android.app.Application

class HyperLocalReportApp : Application() {
    lateinit var container: AppContainer

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
