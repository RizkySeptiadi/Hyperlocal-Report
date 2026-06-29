package com.riz.hyperlocalreport.ui.auth

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.riz.hyperlocalreport.HyperLocalReportApp
import com.riz.hyperlocalreport.MainActivity
import com.riz.hyperlocalreport.core.common.UiState
import com.riz.hyperlocalreport.databinding.ActivityRegisterBinding
import kotlinx.coroutines.launch

class RegisterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegisterBinding
    private val viewModel: AuthViewModel by viewModels {
        AuthViewModelFactory((application as HyperLocalReportApp).container.authRepository)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        com.riz.hyperlocalreport.core.util.KeyboardUtils.setupUI(binding.root, this)

        binding.btnRegister.setOnClickListener {
            val name = binding.etName.text.toString().trim()
            val email = binding.etEmail.text.toString().trim()
            val phone = binding.etPhone.text.toString().trim()
            val area = "" // Handled automatically by live GPS feed
            val password = binding.etPassword.text.toString()
            val confirm = binding.etConfirmPassword.text.toString()

            var isValid = true

            if (name.isEmpty()) {
                binding.tilName.error = "Name is required"
                isValid = false
            } else binding.tilName.error = null

            if (email.isEmpty() || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                binding.tilEmail.error = "Valid email is required"
                isValid = false
            } else binding.tilEmail.error = null

            if (phone.isEmpty()) {
                binding.tilPhone.error = "Phone is required"
                isValid = false
            } else binding.tilPhone.error = null

            if (password.length < 6) {
                binding.tilPassword.error = "Password must be at least 6 characters"
                isValid = false
            } else binding.tilPassword.error = null

            if (password != confirm) {
                binding.tilConfirmPassword.error = "Passwords do not match"
                isValid = false
            } else binding.tilConfirmPassword.error = null

            if (isValid) {
                viewModel.register(name, email, phone, area, password, confirm)
            }
        }

        binding.tvLogin.setOnClickListener {
            finish()
        }

        lifecycleScope.launch {
            viewModel.registerState.collect { state ->
                when (state) {
                    is UiState.Loading -> {
                        binding.progressBar.visibility = View.VISIBLE
                        binding.btnRegister.isEnabled = false
                    }
                    is UiState.Success -> {
                        binding.progressBar.visibility = View.GONE
                        (application as HyperLocalReportApp).container.sessionManager.setSessionMode(
                            com.riz.hyperlocalreport.core.common.SessionManager.MODE_AUTHENTICATED
                        )
                        startActivity(Intent(this@RegisterActivity, MainActivity::class.java))
                        finishAffinity()
                    }
                    is UiState.Error -> {
                        binding.progressBar.visibility = View.GONE
                        binding.btnRegister.isEnabled = true
                        Toast.makeText(this@RegisterActivity, state.message, Toast.LENGTH_SHORT).show()
                    }
                    else -> {
                        binding.progressBar.visibility = View.GONE
                        binding.btnRegister.isEnabled = true
                    }
                }
            }
        }
    }
}
