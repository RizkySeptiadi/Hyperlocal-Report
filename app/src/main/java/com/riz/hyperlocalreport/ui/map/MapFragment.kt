package com.riz.hyperlocalreport.ui.map

import android.Manifest
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.riz.hyperlocalreport.HyperLocalReportApp
import com.riz.hyperlocalreport.core.common.Constants
import com.riz.hyperlocalreport.core.common.UiState
import com.riz.hyperlocalreport.core.util.toDisplayStatus
import com.riz.hyperlocalreport.databinding.FragmentMapBinding
import com.riz.hyperlocalreport.domain.model.Report
import com.riz.hyperlocalreport.ui.report.ReportDetailActivity
import kotlinx.coroutines.launch
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay

class MapFragment : Fragment() {

    private var _binding: FragmentMapBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: MapViewModel
    private var myLocationOverlay: MyLocationNewOverlay? = null
    private val activeMarkers = mutableMapOf<String, Marker>()

    private val locationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true) {
            enableMyLocation()
        } else {
            Toast.makeText(requireContext(), "Location permission denied.", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMapBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Configure OSMDroid user agent
        Configuration.getInstance().userAgentValue = requireContext().packageName

        val appContainer = (requireActivity().application as HyperLocalReportApp).container
        val factory = MapViewModelFactory(appContainer.reportRepository, appContainer.areaSessionManager)
        viewModel = ViewModelProvider(this, factory)[MapViewModel::class.java]

        setupMap()
        observeViewModel()
        requestLocationPermission()
    }

    private fun setupMap() {
        binding.mapView.setTileSource(TileSourceFactory.MAPNIK)
        binding.mapView.setMultiTouchControls(true)
        binding.mapView.zoomController.setVisibility(CustomZoomButtonsController.Visibility.SHOW_AND_FADEOUT)
        binding.mapView.controller.setZoom(15.0)

        // Default map center (will be overridden by my location or reports)
        val defaultPoint = GeoPoint(-6.200000, 106.816666) // Jakarta as fallback
        binding.mapView.controller.setCenter(defaultPoint)
    }

    private fun requestLocationPermission() {
        locationPermissionLauncher.launch(
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
        )
    }

    private fun enableMyLocation() {
        if (myLocationOverlay == null) {
            myLocationOverlay = MyLocationNewOverlay(GpsMyLocationProvider(requireContext()), binding.mapView)
            myLocationOverlay?.enableMyLocation()
            binding.mapView.overlays.add(myLocationOverlay)
            
            // Wait for location fix then center map once
            myLocationOverlay?.runOnFirstFix {
                requireActivity().runOnUiThread {
                    binding.mapView.controller.animateTo(myLocationOverlay?.myLocation)
                }
            }
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.reportsState.collect { state ->
                when (state) {
                    is UiState.Loading -> {
                        binding.progressBar.visibility = View.VISIBLE
                        binding.mapView.visibility = View.GONE
                        binding.cardLegend.visibility = View.GONE
                        binding.tvStatus.visibility = View.VISIBLE
                        binding.tvStatus.text = "Loading reports..."
                    }
                    is UiState.Success -> {
                        binding.progressBar.visibility = View.GONE
                        binding.mapView.visibility = View.VISIBLE
                        binding.cardLegend.visibility = View.VISIBLE
                        binding.tvStatus.visibility = View.GONE
                        updateMarkers(state.data)
                        
                        if (myLocationOverlay?.myLocation == null && state.data.isNotEmpty()) {
                             val first = state.data.first()
                             binding.mapView.controller.setCenter(GeoPoint(first.latitude, first.longitude))
                        }
                    }
                    is UiState.Empty -> {
                        binding.progressBar.visibility = View.GONE
                        binding.mapView.visibility = View.VISIBLE
                        binding.cardLegend.visibility = View.VISIBLE
                        binding.tvStatus.visibility = View.VISIBLE
                        binding.tvStatus.text = "No reports found."
                        updateMarkers(emptyList())
                    }
                    is UiState.Error -> {
                        binding.progressBar.visibility = View.GONE
                        binding.mapView.visibility = View.VISIBLE
                        binding.cardLegend.visibility = View.VISIBLE
                        binding.tvStatus.visibility = View.VISIBLE
                        binding.tvStatus.text = state.message
                    }
                }
            }
        }
    }

    private fun updateMarkers(reports: List<Report>) {
        val newReportIds = reports.map { it.reportId }.toSet()
        
        // Remove markers that are no longer in the list
        val iterator = activeMarkers.iterator()
        while (iterator.hasNext()) {
            val entry = iterator.next()
            if (!newReportIds.contains(entry.key)) {
                binding.mapView.overlays.remove(entry.value)
                iterator.remove()
            }
        }

        // Add or update markers
        for (report in reports) {
            val existingMarker = activeMarkers[report.reportId]
            if (existingMarker == null) {
                val marker = Marker(binding.mapView)
                marker.position = GeoPoint(report.latitude, report.longitude)
                marker.title = report.title
                marker.snippet = "Status: ${report.status.toDisplayStatus()} | Upvotes: ${report.upvoteCount}\nTap here for details"
                
                // Colorize marker based on status
                val drawable = android.graphics.drawable.GradientDrawable()
                drawable.shape = android.graphics.drawable.GradientDrawable.OVAL
                drawable.setSize(48, 48)
                when (report.status) {
                    Constants.ReportStatus.NEW -> drawable.setColor(Color.parseColor("#F44336"))
                    Constants.ReportStatus.VERIFIED -> drawable.setColor(Color.parseColor("#FF9800"))
                    Constants.ReportStatus.PROCESSING -> drawable.setColor(Color.parseColor("#2196F3"))
                    Constants.ReportStatus.DONE -> drawable.setColor(Color.parseColor("#4CAF50"))
                    else -> drawable.setColor(Color.GRAY)
                }
                marker.icon = drawable
                
                marker.setOnMarkerClickListener { _, _ ->
                    val intent = Intent(requireContext(), ReportDetailActivity::class.java).apply {
                        putExtra("reportId", report.reportId)
                    }
                    startActivity(intent)
                    true
                }

                binding.mapView.overlays.add(marker)
                activeMarkers[report.reportId] = marker
            } else {
                // Update existing marker in case status or upvotes changed
                existingMarker.snippet = "Status: ${report.status.toDisplayStatus()} | Upvotes: ${report.upvoteCount}\nTap here for details"
                val drawable = android.graphics.drawable.GradientDrawable()
                drawable.shape = android.graphics.drawable.GradientDrawable.OVAL
                drawable.setSize(48, 48)
                when (report.status) {
                    Constants.ReportStatus.NEW -> drawable.setColor(Color.parseColor("#F44336"))
                    Constants.ReportStatus.VERIFIED -> drawable.setColor(Color.parseColor("#FF9800"))
                    Constants.ReportStatus.PROCESSING -> drawable.setColor(Color.parseColor("#2196F3"))
                    Constants.ReportStatus.DONE -> drawable.setColor(Color.parseColor("#4CAF50"))
                    else -> drawable.setColor(Color.GRAY)
                }
                existingMarker.icon = drawable
            }
        }
        
        binding.mapView.invalidate()
    }

    override fun onResume() {
        super.onResume()
        binding.mapView.onResume()
        myLocationOverlay?.enableMyLocation()
    }

    override fun onPause() {
        super.onPause()
        binding.mapView.onPause()
        myLocationOverlay?.disableMyLocation()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
