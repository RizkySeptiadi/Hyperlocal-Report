package com.riz.hyperlocalreport.ui.profile

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.riz.hyperlocalreport.HyperLocalReportApp
import com.riz.hyperlocalreport.core.common.SessionManager
import com.riz.hyperlocalreport.databinding.FragmentProfileBinding
import com.riz.hyperlocalreport.ui.auth.LoginActivity
import com.bumptech.glide.Glide
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import kotlinx.coroutines.launch

class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    private val pickImageLauncher = registerForActivityResult(androidx.activity.result.contract.ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            val appContainer = (requireActivity().application as HyperLocalReportApp).container
            val factory = ProfileViewModelFactory(appContainer.authRepository)
            val viewModel = ViewModelProvider(this, factory)[ProfileViewModel::class.java]
            viewModel.uploadProfilePicture(it)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val appContainer = (requireActivity().application as HyperLocalReportApp).container
        val factory = ProfileViewModelFactory(appContainer.authRepository)
        val viewModel = ViewModelProvider(this, factory)[ProfileViewModel::class.java]

        setupGuestState(appContainer.sessionManager)
        
        binding.ivEditAvatar.setOnClickListener {
            pickImageLauncher.launch("image/*")
        }
        
        binding.btnEditProfile.setOnClickListener {
            val user = viewModel.currentUser.value ?: return@setOnClickListener
            val dialogView = LayoutInflater.from(requireContext()).inflate(com.riz.hyperlocalreport.R.layout.dialog_edit_profile, null)
            val etName = dialogView.findViewById<TextInputEditText>(com.riz.hyperlocalreport.R.id.et_name)
            val etPhone = dialogView.findViewById<TextInputEditText>(com.riz.hyperlocalreport.R.id.et_phone)
            etName.setText(user.name)
            etPhone.setText(user.phone)

            MaterialAlertDialogBuilder(requireContext())
                .setTitle("Ubah Profil")
                .setView(dialogView)
                .setPositiveButton("Simpan") { _, _ ->
                    val newName = etName.text.toString().trim()
                    val newPhone = etPhone.text.toString().trim()
                    if (newName.isNotEmpty() && newPhone.isNotEmpty()) {
                        viewModel.updateProfile(newName, newPhone)
                    } else {
                        Toast.makeText(requireContext(), "Harap isi semua bidang", Toast.LENGTH_SHORT).show()
                    }
                }
                .setNegativeButton("Batal", null)
                .show()
        }

        binding.btnLogout.setOnClickListener {
            showLogoutConfirmation(viewModel, appContainer.sessionManager)
        }
        
        binding.btnGuestLogin.setOnClickListener {
            startActivity(Intent(requireContext(), LoginActivity::class.java))
            requireActivity().finish()
        }

        setupSettings()

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.currentUser.collect { user ->
                if (user != null) {
                    binding.tvName.text = user.name
                    binding.tvEmail.text = user.email
                    binding.tvPhone.text = user.phone
                    binding.tvStatusWarga.text = user.role.replaceFirstChar { it.uppercase() }
                    binding.tvRegisteredArea.text = user.areaId.ifEmpty { "Dynamic (Auto)" }.replace("KEL_", "").replace("_", " ")
                    if (!user.profileImageUrl.isNullOrEmpty()) {
                        Glide.with(this@ProfileFragment)
                            .load(user.profileImageUrl)
                            .circleCrop()
                            .into(binding.ivAvatar)
                    }
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            appContainer.areaSessionManager.areaState.collect { state ->
                when (state) {
                    is com.riz.hyperlocalreport.core.common.AreaState.Available -> {
                        binding.tvCurrentLocation.text = state.addressName
                    }
                    is com.riz.hyperlocalreport.core.common.AreaState.UnsupportedLocation -> {
                        binding.tvCurrentLocation.text = state.addressName
                    }
                    is com.riz.hyperlocalreport.core.common.AreaState.Error -> {
                        binding.tvCurrentLocation.text = state.cachedAddressName ?: "Error resolving location"
                    }
                    else -> {
                        binding.tvCurrentLocation.text = "Loading..."
                    }
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.updateState.collect { state ->
                when (state) {
                    is ProfileUpdateState.Idle -> {
                        binding.progressBar.visibility = View.GONE
                    }
                    is ProfileUpdateState.Loading -> {
                        binding.progressBar.visibility = View.VISIBLE
                    }
                    is ProfileUpdateState.Success -> {
                        binding.progressBar.visibility = View.GONE
                        Toast.makeText(requireContext(), "Profile updated successfully", Toast.LENGTH_SHORT).show()
                        viewModel.resetUpdateState()
                    }
                    is ProfileUpdateState.Error -> {
                        binding.progressBar.visibility = View.GONE
                        Toast.makeText(requireContext(), state.message, Toast.LENGTH_LONG).show()
                        viewModel.resetUpdateState()
                    }
                }
            }
        }
    }

    private fun setupSettings() {
        val prefs = requireContext().getSharedPreferences("app_settings", android.content.Context.MODE_PRIVATE)
        val isNotifEnabled = prefs.getBoolean("notif_enabled", true)
        binding.switchNotifikasi.isChecked = isNotifEnabled
        
        binding.llNotifikasi.setOnClickListener {
            binding.switchNotifikasi.isChecked = !binding.switchNotifikasi.isChecked
        }
        
        binding.switchNotifikasi.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("notif_enabled", isChecked).apply()
            if (isChecked) {
                com.google.firebase.messaging.FirebaseMessaging.getInstance().subscribeToTopic("report_updates")
                Toast.makeText(requireContext(), "Notifikasi diaktifkan", Toast.LENGTH_SHORT).show()
            } else {
                com.google.firebase.messaging.FirebaseMessaging.getInstance().unsubscribeFromTopic("report_updates")
                Toast.makeText(requireContext(), "Notifikasi dinonaktifkan", Toast.LENGTH_SHORT).show()
            }
        }

        val currentLang = prefs.getString("language", "id") ?: "id"
        binding.tvBahasa.text = if (currentLang == "id") "Indonesia" else "English"

        binding.llBahasa.setOnClickListener {
            val languages = arrayOf("Indonesia", "English")
            MaterialAlertDialogBuilder(requireContext())
                .setTitle("Pilih Bahasa")
                .setItems(languages) { _, which ->
                    val langCode = if (which == 0) "id" else "en"
                    prefs.edit().putString("language", langCode).apply()
                    AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(langCode))
                }
                .show()
        }

        binding.llBantuan.setOnClickListener {
            val emailIntent = Intent(Intent.ACTION_SENDTO).apply {
                data = android.net.Uri.parse("mailto:support@hyperlocalreport.com")
                putExtra(Intent.EXTRA_SUBJECT, "Bantuan HyperLocal Report")
            }
            try {
                startActivity(emailIntent)
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Tidak ada aplikasi email terpasang", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    private fun setupGuestState(sessionManager: SessionManager) {
        if (sessionManager.getSessionMode() == SessionManager.MODE_GUEST) {
            binding.guestContainer.visibility = View.VISIBLE
            // Hide the rest of the profile
            binding.tvName.visibility = View.GONE
            binding.tvEmail.visibility = View.GONE
            binding.btnEditProfile.visibility = View.GONE
            binding.cardDomisili.visibility = View.GONE
            binding.cardPengaturan.visibility = View.GONE
            binding.titleDomisili.visibility = View.GONE
            binding.titlePengaturan.visibility = View.GONE
            binding.btnLogout.visibility = View.GONE
        } else {
            binding.guestContainer.visibility = View.GONE
        }
    }

    private fun showLogoutConfirmation(viewModel: ProfileViewModel, sessionManager: SessionManager) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Logout")
            .setMessage("Are you sure you want to log out?")
            .setPositiveButton("Logout") { _, _ ->
                viewModel.logout()
                sessionManager.clearSession()
                startActivity(Intent(requireContext(), LoginActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                })
                requireActivity().finish()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
