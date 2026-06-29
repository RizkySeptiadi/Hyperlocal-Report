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
import com.riz.hyperlocalreport.HyperLocalReportApp
import com.riz.hyperlocalreport.core.common.SessionManager
import com.riz.hyperlocalreport.databinding.FragmentProfileBinding
import com.riz.hyperlocalreport.ui.auth.LoginActivity
import kotlinx.coroutines.launch

class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

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
        
        binding.btnEditProfile.setOnClickListener {
            // Placeholder: the actual edit flow isn't in scope, but we show toast
            Toast.makeText(requireContext(), "Edit Profil tidak tersedia saat ini", Toast.LENGTH_SHORT).show()
        }

        binding.btnLogout.setOnClickListener {
            showLogoutConfirmation(viewModel, appContainer.sessionManager)
        }
        
        binding.btnGuestLogin.setOnClickListener {
            startActivity(Intent(requireContext(), LoginActivity::class.java))
            requireActivity().finish()
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.currentUser.collect { user ->
                if (user != null) {
                    binding.tvName.text = user.name
                    binding.tvEmail.text = user.email
                    binding.tvPhone.text = user.phone
                    binding.tvStatusWarga.text = user.role.replaceFirstChar { it.uppercase() }
                    binding.tvRegisteredArea.text = user.areaId.ifEmpty { "Dynamic (Auto)" }.replace("KEL_", "").replace("_", " ")
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
