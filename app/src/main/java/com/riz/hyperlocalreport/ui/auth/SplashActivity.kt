package com.riz.hyperlocalreport.ui.auth

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.riz.hyperlocalreport.HyperLocalReportApp
import com.riz.hyperlocalreport.MainActivity
import com.riz.hyperlocalreport.R
import kotlinx.coroutines.launch

class SplashActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        val appContainer = (application as HyperLocalReportApp).container
        val authRepository = appContainer.authRepository
        val sessionManager = appContainer.sessionManager

        lifecycleScope.launch {
            val hasFirebaseSession = authRepository.checkSession()
            val sessionMode = sessionManager.getSessionMode()

            if (hasFirebaseSession) {
                sessionManager.setSessionMode(com.riz.hyperlocalreport.core.common.SessionManager.MODE_AUTHENTICATED)
                startActivity(Intent(this@SplashActivity, MainActivity::class.java))
            } else if (sessionMode == com.riz.hyperlocalreport.core.common.SessionManager.MODE_GUEST) {
                startActivity(Intent(this@SplashActivity, MainActivity::class.java))
            } else {
                startActivity(Intent(this@SplashActivity, LoginActivity::class.java))
            }
            finish()
        }
    }
}
