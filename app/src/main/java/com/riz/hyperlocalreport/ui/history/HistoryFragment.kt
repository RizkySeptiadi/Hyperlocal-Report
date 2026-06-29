package com.riz.hyperlocalreport.ui.history

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.google.android.material.chip.Chip
import com.riz.hyperlocalreport.HyperLocalReportApp
import com.riz.hyperlocalreport.core.common.SessionManager
import com.riz.hyperlocalreport.core.util.toDbStatus
import com.riz.hyperlocalreport.databinding.FragmentHistoryBinding
import com.riz.hyperlocalreport.ui.auth.LoginActivity
import kotlinx.coroutines.launch

class HistoryFragment : Fragment() {

    private var _binding: FragmentHistoryBinding? = null
    private val binding get() = _binding!!
    
    private lateinit var reportAdapter: ReportAdapter
    private var currentFilter: String? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHistoryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val appContainer = (requireActivity().application as HyperLocalReportApp).container
        val factory = HistoryViewModelFactory(appContainer.authRepository, appContainer.reportRepository)
        val viewModel = ViewModelProvider(this, factory)[HistoryViewModel::class.java]

        setupRecyclerView()
        setupFilters(viewModel)

        binding.btnGuestLogin.setOnClickListener {
            startActivity(Intent(requireContext(), LoginActivity::class.java))
            requireActivity().finish()
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.uiState.collect { state ->
                when (state) {
                    is HistoryUiState.Loading -> {
                        binding.progressBar.visibility = View.VISIBLE
                        binding.rvReports.visibility = View.GONE
                        binding.emptyContainer.visibility = View.GONE
                        binding.guestContainer.visibility = View.GONE
                    }
                    is HistoryUiState.Success -> {
                        binding.progressBar.visibility = View.GONE
                        binding.rvReports.visibility = View.VISIBLE
                        binding.emptyContainer.visibility = View.GONE
                        binding.guestContainer.visibility = View.GONE
                        
                        // Apply filter
                        val reports = if (currentFilter != null) {
                            state.reports.filter { it.status == currentFilter }
                        } else {
                            state.reports
                        }
                        reportAdapter.submitList(reports.map { com.riz.hyperlocalreport.domain.model.ReportUiModel(it, false) })
                        
                        if (reports.isEmpty()) {
                            binding.rvReports.visibility = View.GONE
                            binding.emptyContainer.visibility = View.VISIBLE
                            binding.tvEmptyMessage.text = "No reports found for this filter."
                        }
                    }
                    is HistoryUiState.Empty -> {
                        binding.progressBar.visibility = View.GONE
                        binding.rvReports.visibility = View.GONE
                        binding.emptyContainer.visibility = View.VISIBLE
                        binding.guestContainer.visibility = View.GONE
                        binding.tvEmptyMessage.text = "You haven't submitted any reports yet."
                    }
                    is HistoryUiState.Guest -> {
                        binding.progressBar.visibility = View.GONE
                        binding.rvReports.visibility = View.GONE
                        binding.emptyContainer.visibility = View.GONE
                        binding.guestContainer.visibility = View.VISIBLE
                    }
                    is HistoryUiState.Error -> {
                        binding.progressBar.visibility = View.GONE
                        binding.rvReports.visibility = View.GONE
                        binding.emptyContainer.visibility = View.VISIBLE
                        binding.guestContainer.visibility = View.GONE
                        binding.tvEmptyMessage.text = "Failed to load history: ${state.message}"
                    }
                }
            }
        }
    }

    private fun setupRecyclerView() {
        reportAdapter = ReportAdapter(
            onItemClick = { reportId ->
                val intent = Intent(requireContext(), com.riz.hyperlocalreport.ui.report.ReportDetailActivity::class.java).apply {
                    putExtra("reportId", reportId)
                }
                startActivity(intent)
            },
            onUpvoteClick = { reportId ->
                Toast.makeText(requireContext(), "Upvoting from history is not supported yet.", Toast.LENGTH_SHORT).show()
            }
        )
        binding.rvReports.adapter = reportAdapter
    }

    private fun setupFilters(viewModel: HistoryViewModel) {
        binding.chipGroupFilters.setOnCheckedStateChangeListener { group, checkedIds ->
            if (checkedIds.isEmpty()) {
                currentFilter = null
            } else {
                val chip = binding.chipGroupFilters.findViewById<Chip>(checkedIds.first())
                currentFilter = chip?.text?.toString()?.toDbStatus()
            }
            
            // Re-trigger the state collection to apply filter
            val currentState = viewModel.uiState.value
            if (currentState is HistoryUiState.Success) {
                val reports = if (currentFilter != null) {
                    currentState.reports.filter { it.status == currentFilter }
                } else {
                    currentState.reports
                }
                reportAdapter.submitList(reports.map { com.riz.hyperlocalreport.domain.model.ReportUiModel(it, false) })
                
                if (reports.isEmpty()) {
                    binding.rvReports.visibility = View.GONE
                    binding.emptyContainer.visibility = View.VISIBLE
                    binding.tvEmptyMessage.text = "No reports found for this filter."
                } else {
                    binding.rvReports.visibility = View.VISIBLE
                    binding.emptyContainer.visibility = View.GONE
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
