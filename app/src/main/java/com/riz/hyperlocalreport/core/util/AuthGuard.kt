package com.riz.hyperlocalreport.core.util

import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AlertDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.auth.FirebaseAuth
import com.riz.hyperlocalreport.ui.auth.LoginActivity

fun Context.requireAuthenticatedUser(
    onAuthenticated: () -> Unit
) {
    if (FirebaseAuth.getInstance().currentUser != null) {
        onAuthenticated()
    } else {
        showLoginRequiredDialog()
    }
}

fun Context.showLoginRequiredDialog() {
    MaterialAlertDialogBuilder(this)
        .setTitle("Sign in required")
        .setMessage("You need to sign in to use this feature. You can continue browsing without an account.")
        .setPositiveButton("Sign In") { dialog, _ ->
            dialog.dismiss()
            startActivity(Intent(this, LoginActivity::class.java))
        }
        .setNegativeButton("Cancel") { dialog, _ ->
            dialog.dismiss()
        }
        .show()
}
