package com.riz.hyperlocalreport.ui.admin

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.google.android.material.chip.Chip
import com.riz.hyperlocalreport.HyperLocalReportApp
import com.riz.hyperlocalreport.core.util.toDbStatus
import com.riz.hyperlocalreport.databinding.FragmentAdminDashboardBinding
import com.riz.hyperlocalreport.ui.history.ReportAdapter
import com.riz.hyperlocalreport.ui.report.ReportDetailActivity
import kotlinx.coroutines.launch

class AdminDashboardFragment : Fragment() {

    private var _binding: FragmentAdminDashboardBinding? = null
    private val binding get() = _binding!!
    
    private lateinit var reportAdapter: ReportAdapter
    private var currentStatusFilter: String? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAdminDashboardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val appContainer = (requireActivity().application as HyperLocalReportApp).container
        val factory = AdminDashboardViewModelFactory(
            appContainer.authRepository,
            appContainer.reportRepository,
            appContainer.upvoteRepository
        )
        val viewModel = ViewModelProvider(this, factory)[AdminDashboardViewModel::class.java]

        setupRecyclerView()
        setupFilters(viewModel)

        binding.ivBack.setOnClickListener {
            requireActivity().onBackPressed()
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.currentUser.collect { user ->
                // Removed area subtitle binding to match mockup
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.uiState.collect { state ->
                when (state) {
                    is AdminDashboardUiState.Loading -> {
                        binding.progressBar.visibility = View.VISIBLE
                        binding.rvReports.visibility = View.GONE
                        binding.emptyContainer.visibility = View.GONE
                    }
                    is AdminDashboardUiState.Success -> {
                        binding.progressBar.visibility = View.GONE
                        binding.rvReports.visibility = View.VISIBLE
                        binding.emptyContainer.visibility = View.GONE
                        updateSummaryCards(state.reports)
                        applyFiltersAndSubmit(state.reports)
                    }
                    is AdminDashboardUiState.Empty -> {
                        binding.progressBar.visibility = View.GONE
                        binding.rvReports.visibility = View.GONE
                        binding.emptyContainer.visibility = View.VISIBLE
                        binding.tvEmptyMessage.text = "Belum ada laporan di area ini."
                        
                        binding.tvCountTotal.text = "0"
                        binding.tvCountNew.text = "0"
                        binding.tvCountProcessing.text = "0"
                        binding.tvCountDone.text = "0"
                    }
                    is AdminDashboardUiState.Error -> {
                        binding.progressBar.visibility = View.GONE
                        binding.rvReports.visibility = View.GONE
                        binding.emptyContainer.visibility = View.VISIBLE
                        binding.tvEmptyMessage.text = "Error: ${state.message}"
                    }
                }
            }
        }
    }
    
    private fun updateSummaryCards(reports: List<com.riz.hyperlocalreport.domain.model.ReportUiModel>) {
        val total = reports.size
        val baru = reports.count { it.report.status == com.riz.hyperlocalreport.core.common.Constants.ReportStatus.NEW }
        val proses = reports.count { it.report.status == com.riz.hyperlocalreport.core.common.Constants.ReportStatus.PROCESSING }
        val selesai = reports.count { it.report.status == com.riz.hyperlocalreport.core.common.Constants.ReportStatus.DONE }
        
        binding.tvCountTotal.text = total.toString()
        binding.tvCountNew.text = baru.toString()
        binding.tvCountProcessing.text = proses.toString()
        binding.tvCountDone.text = selesai.toString()
    }

    private fun setupRecyclerView() {
        reportAdapter = ReportAdapter(
            onItemClick = { reportId ->
                val intent = Intent(requireContext(), ReportDetailActivity::class.java).apply {
                    putExtra("reportId", reportId)
                    putExtra("isAdminMode", true)
                }
                startActivity(intent)
            },
            onUpvoteClick = { reportId ->
                val viewModel = ViewModelProvider(this)[AdminDashboardViewModel::class.java]
                viewModel.toggleUpvote(reportId)
            }
        )
        binding.rvReports.adapter = reportAdapter
    }

    private fun setupFilters(viewModel: AdminDashboardViewModel) {
        binding.chipGroupFilters.setOnCheckedStateChangeListener { group, checkedIds ->
            if (checkedIds.isEmpty()) {
                currentStatusFilter = null
            } else {
                val chip = group.findViewById<Chip>(checkedIds.first())
                val filterText = chip.text.toString()
                if (filterText == "Semua") {
                    currentStatusFilter = null
                } else {
                    currentStatusFilter = filterText.toDbStatus()
                }
            }
            
            val currentState = viewModel.uiState.value
            if (currentState is AdminDashboardUiState.Success) {
                applyFiltersAndSubmit(currentState.reports)
            }
        }
    }

    private fun applyFiltersAndSubmit(allReports: List<com.riz.hyperlocalreport.domain.model.ReportUiModel>) {
        val filtered = allReports.filter { uiModel ->
            val report = uiModel.report
            currentStatusFilter?.let { report.status == it } ?: true
        }
        
        reportAdapter.submitList(filtered)
        
        if (filtered.isEmpty()) {
            binding.rvReports.visibility = View.GONE
            binding.emptyContainer.visibility = View.VISIBLE
            binding.tvEmptyMessage.text = "Belum ada laporan dengan status tersebut."
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
