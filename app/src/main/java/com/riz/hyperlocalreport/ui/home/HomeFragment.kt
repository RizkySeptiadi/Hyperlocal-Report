package com.riz.hyperlocalreport.ui.home

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.google.android.material.chip.Chip
import com.riz.hyperlocalreport.AppContainer
import com.riz.hyperlocalreport.HyperLocalReportApp
import com.riz.hyperlocalreport.core.common.AreaState
import com.riz.hyperlocalreport.core.util.requireAuthenticatedUser
import com.riz.hyperlocalreport.core.util.toDbCategory
import com.riz.hyperlocalreport.core.util.toDbStatus
import com.riz.hyperlocalreport.databinding.FragmentHomeBinding
import com.riz.hyperlocalreport.ui.auth.LoginActivity
import com.riz.hyperlocalreport.ui.history.ReportAdapter
import com.riz.hyperlocalreport.ui.report.CreateReportActivity
import com.riz.hyperlocalreport.ui.report.ReportDetailActivity
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    
    private lateinit var reportAdapter: ReportAdapter
    private var currentCategoryFilter: String? = null
    private var currentStatusFilter: String? = null
    private lateinit var appContainer: AppContainer

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val app = requireActivity().application as HyperLocalReportApp
        appContainer = app.container
        
        val factory = HomeViewModelFactory(
            appContainer.reportRepository, 
            appContainer.areaSessionManager,
            appContainer.authRepository,
            appContainer.upvoteRepository
        )
        val viewModel = ViewModelProvider(this, factory)[HomeViewModel::class.java]

        setupRecyclerView()
        setupFilters(viewModel)

        binding.ivNotifications.setOnClickListener {
            Toast.makeText(requireContext(), "No new notifications", Toast.LENGTH_SHORT).show()
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.userName.collectLatest { name ->
                binding.tvGreeting.text = "Halo, $name 👋"
            }
        }

        binding.etSearch.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: android.text.Editable?) {
                val currentState = viewModel.uiState.value
                if (currentState is HomeUiState.Success) {
                    applyFiltersAndSubmit(currentState.reports)
                }
            }
        })

        binding.fabAddReport.setOnClickListener {
            requireContext().requireAuthenticatedUser {
                startActivity(Intent(requireContext(), CreateReportActivity::class.java))
            }
        }

        binding.btnGuestLogin.setOnClickListener {
            openSettingsOrRetryLocation()
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.areaState.collect { state ->
                when (state) {
                    is AreaState.Available -> {
                        // we removed the area subtitle from home to match mockup
                    }
                    is AreaState.UnsupportedLocation -> {
                        Toast.makeText(requireContext(), "Unsupported Area", Toast.LENGTH_SHORT).show()
                    }
                    is AreaState.PermissionRequired -> {
                        Toast.makeText(requireContext(), "Permission Required", Toast.LENGTH_SHORT).show()
                    }
                    is AreaState.LocationDisabled -> {
                        Toast.makeText(requireContext(), "Location Disabled", Toast.LENGTH_SHORT).show()
                    }
                    is AreaState.Error -> {
                        Toast.makeText(requireContext(), "Error finding location", Toast.LENGTH_SHORT).show()
                    }
                    else -> {}
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.uiState.collect { state ->
                when (state) {
                    is HomeUiState.Loading -> {
                        binding.progressBar.visibility = View.VISIBLE
                        binding.rvReports.visibility = View.GONE
                        binding.emptyContainer.visibility = View.GONE
                        binding.guestContainer.visibility = View.GONE
                    }
                    is HomeUiState.Success -> {
                        binding.progressBar.visibility = View.GONE
                        binding.rvReports.visibility = View.VISIBLE
                        binding.emptyContainer.visibility = View.GONE
                        binding.guestContainer.visibility = View.GONE
                        applyFiltersAndSubmit(state.reports)
                    }
                    is HomeUiState.Empty -> {
                        binding.progressBar.visibility = View.GONE
                        binding.rvReports.visibility = View.GONE
                        binding.emptyContainer.visibility = View.VISIBLE
                        binding.guestContainer.visibility = View.GONE
                        binding.tvEmptyMessage.text = "No reports found in your area yet."
                    }
                    is HomeUiState.PermissionRequired -> {
                        binding.progressBar.visibility = View.GONE
                        binding.rvReports.visibility = View.GONE
                        binding.emptyContainer.visibility = View.GONE
                        binding.guestContainer.visibility = View.VISIBLE
                        binding.tvEmptyMessage.text = "Permission Required"
                        binding.btnGuestLogin.text = "Open Settings"
                    }
                    is HomeUiState.LocationDisabled -> {
                        binding.progressBar.visibility = View.GONE
                        binding.rvReports.visibility = View.GONE
                        binding.emptyContainer.visibility = View.GONE
                        binding.guestContainer.visibility = View.VISIBLE
                        binding.tvEmptyMessage.text = "Location Disabled"
                        binding.btnGuestLogin.text = "Enable Location"
                    }
                    is HomeUiState.UnsupportedArea -> {
                        binding.progressBar.visibility = View.GONE
                        binding.rvReports.visibility = View.GONE
                        binding.emptyContainer.visibility = View.VISIBLE
                        binding.guestContainer.visibility = View.GONE
                        binding.tvEmptyMessage.text = "Your current location is outside supported areas."
                    }
                    is HomeUiState.Error -> {
                        binding.progressBar.visibility = View.GONE
                        binding.rvReports.visibility = View.GONE
                        binding.emptyContainer.visibility = View.VISIBLE
                        binding.guestContainer.visibility = View.GONE
                        binding.tvEmptyMessage.text = "Error: ${state.message}"
                    }
                }
            }
        }
    }

    private fun openSettingsOrRetryLocation() {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", requireContext().packageName, null)
        }
        startActivity(intent)
    }

    private fun setupRecyclerView() {
        reportAdapter = ReportAdapter(
            onItemClick = { reportId ->
                val intent = Intent(requireContext(), ReportDetailActivity::class.java).apply {
                    putExtra("reportId", reportId)
                }
                startActivity(intent)
            },
            onUpvoteClick = { reportId ->
                val viewModel = ViewModelProvider(this)[HomeViewModel::class.java]
                viewModel.toggleUpvote(reportId)
            }
        )
        binding.rvReports.adapter = reportAdapter
    }

    private fun setupFilters(viewModel: HomeViewModel) {
        binding.chipGroupFilters.setOnCheckedStateChangeListener { group, checkedIds ->
            if (checkedIds.isEmpty()) {
                currentCategoryFilter = null
                currentStatusFilter = null
            } else {
                val chip = group.findViewById<Chip>(checkedIds.first())
                val filterText = chip.text.toString()
                if (filterText in listOf("Baru", "Selesai")) {
                    currentStatusFilter = filterText.toDbStatus()
                    currentCategoryFilter = null
                } else {
                    currentCategoryFilter = filterText.toDbCategory()
                    currentStatusFilter = null
                }
            }
            
            val currentState = viewModel.uiState.value
            if (currentState is HomeUiState.Success) {
                applyFiltersAndSubmit(currentState.reports)
            }
        }
    }

    private fun applyFiltersAndSubmit(allReports: List<com.riz.hyperlocalreport.domain.model.ReportUiModel>) {
        val searchQuery = binding.etSearch.text?.toString()?.trim() ?: ""
        
        val filtered = allReports.filter { uiModel ->
            val report = uiModel.report
            val matchCategory = currentCategoryFilter?.let { report.category == it } ?: true
            val matchStatus = currentStatusFilter?.let { report.status == it } ?: true
            val matchSearch = if (searchQuery.isNotEmpty()) {
                report.title.contains(searchQuery, ignoreCase = true) || 
                report.description.contains(searchQuery, ignoreCase = true)
            } else {
                true
            }
            matchCategory && matchStatus && matchSearch
        }
        
        reportAdapter.submitList(filtered)
        
        if (filtered.isEmpty()) {
            binding.rvReports.visibility = View.GONE
            binding.emptyContainer.visibility = View.VISIBLE
            binding.tvEmptyMessage.text = "Belum ada laporan di sekitar Anda."
        } else {
            binding.rvReports.visibility = View.VISIBLE
            binding.emptyContainer.visibility = View.GONE
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
