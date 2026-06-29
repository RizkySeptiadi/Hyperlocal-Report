package com.riz.hyperlocalreport.core.common

import android.content.Context
import android.content.SharedPreferences

sealed interface AppSessionState {
    object Loading : AppSessionState
    object Guest : AppSessionState
    data class Authenticated(val uid: String) : AppSessionState
}

class SessionManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    fun getSessionMode(): String {
        return prefs.getString(KEY_SESSION_MODE, MODE_UNSET) ?: MODE_UNSET
    }

    fun setSessionMode(mode: String) {
        prefs.edit().putString(KEY_SESSION_MODE, mode).apply()
    }

    fun clearSession() {
        prefs.edit().remove(KEY_SESSION_MODE).apply()
    }

    companion object {
        private const val PREF_NAME = "hyperlocal_session_prefs"
        private const val KEY_SESSION_MODE = "session_mode"

        const val MODE_UNSET = "unset"
        const val MODE_GUEST = "guest"
        const val MODE_AUTHENTICATED = "authenticated"
    }
}
