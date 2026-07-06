package com.riz.hyperlocalreport.ui.report

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.preference.PreferenceManager
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.riz.hyperlocalreport.HyperLocalReportApp
import com.riz.hyperlocalreport.core.common.Constants
import com.riz.hyperlocalreport.core.util.requireAuthenticatedUser
import com.riz.hyperlocalreport.databinding.ActivityCreateReportBinding
import kotlinx.coroutines.launch
import org.osmdroid.config.Configuration
import org.osmdroid.util.GeoPoint
import com.bumptech.glide.Glide

class CreateReportActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCreateReportBinding
    private val viewModel: CreateReportViewModel by viewModels {
        val appContainer = (application as HyperLocalReportApp).container
        CreateReportViewModelFactory(appContainer.authRepository, appContainer.reportRepository, appContainer.areaSessionManager)
    }

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var currentLocation: GeoPoint? = null
    private var photoUri: String? = null

    private val cameraPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) {
            launchCameraActivity()
        } else {
            Toast.makeText(this, "Camera permission required", Toast.LENGTH_SHORT).show()
        }
    }

    private val locationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true) {
            getCurrentLocation()
        } else {
            Toast.makeText(this, "Location permission required", Toast.LENGTH_SHORT).show()
        }
    }

    private val cameraActivityLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val uriString = result.data?.getStringExtra(CameraActivity.EXTRA_IMAGE_URI)
            if (uriString != null) {
                photoUri = uriString
                Glide.with(this).load(Uri.parse(uriString)).into(binding.ivPreview)
            }
        }
    }
    
    private val mapSelectionLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val lat = result.data?.getDoubleExtra(MapSelectionActivity.EXTRA_LATITUDE, 0.0) ?: 0.0
            val lng = result.data?.getDoubleExtra(MapSelectionActivity.EXTRA_LONGITUDE, 0.0) ?: 0.0
            if (lat != 0.0 && lng != 0.0) {
                currentLocation = GeoPoint(lat, lng)
                updateLocationText(lat, lng)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        Configuration.getInstance().load(applicationContext, PreferenceManager.getDefaultSharedPreferences(applicationContext))
        
        binding = ActivityCreateReportBinding.inflate(layoutInflater)
        setContentView(binding.root)

        com.riz.hyperlocalreport.core.util.KeyboardUtils.setupUI(binding.root, this)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        setupToolbar()
        setupCategoryDropdown()
        setupListeners()
        observeViewModel()
        
        requestLocationPermission()
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener { finish() }
    }

    private fun setupCategoryDropdown() {
        val categories = listOf(
            Constants.Categories.ROAD,
            Constants.Categories.LIGHTING,
            Constants.Categories.WASTE,
            Constants.Categories.DRAINAGE,
            Constants.Categories.PUBLIC_FACILITY,
            Constants.Categories.SECURITY
        )
        val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, categories)
        binding.actvCategory.setAdapter(adapter)
    }

    private fun updateLocationText(lat: Double, lng: Double) {
        val latStr = String.format("%.5f", lat)
        val lngStr = String.format("%.5f", lng)
        binding.tvLocation.text = "Lokasi: $latStr, $lngStr"
    }

    private fun setupListeners() {
        binding.btnTakePhoto.setOnClickListener {
            requireAuthenticatedUser {
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                    launchCameraActivity()
                } else {
                    cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                }
            }
        }
        
        binding.btnChangeLocation.setOnClickListener {
            val intent = Intent(this, MapSelectionActivity::class.java)
            currentLocation?.let {
                intent.putExtra(MapSelectionActivity.EXTRA_LATITUDE, it.latitude)
                intent.putExtra(MapSelectionActivity.EXTRA_LONGITUDE, it.longitude)
            }
            mapSelectionLauncher.launch(intent)
        }

        binding.btnSubmit.setOnClickListener {
            requireAuthenticatedUser {
                val title = binding.etTitle.text.toString().trim()
                val description = binding.etDescription.text.toString().trim()
                val category = binding.actvCategory.text.toString().trim()

                var isValid = true

                if (title.isEmpty()) {
                    binding.tilTitle.error = "Title is required"
                    isValid = false
                } else binding.tilTitle.error = null

                if (description.isEmpty()) {
                    binding.tilDescription.error = "Description is required"
                    isValid = false
                } else binding.tilDescription.error = null

                if (category.isEmpty()) {
                    binding.tilCategory.error = "Category is required"
                    isValid = false
                } else binding.tilCategory.error = null
                
                if (photoUri == null) {
                    Toast.makeText(this, "Please take a photo", Toast.LENGTH_SHORT).show()
                    isValid = false
                }

                if (currentLocation == null) {
                    Toast.makeText(this, "Location is required", Toast.LENGTH_SHORT).show()
                    isValid = false
                }

                if (isValid) {
                    viewModel.submitReport(
                        title = title,
                        description = description,
                        category = category,
                        photoUri = photoUri,
                        latitude = currentLocation?.latitude,
                        longitude = currentLocation?.longitude
                    )
                }
            }
        }
    }

    private fun launchCameraActivity() {
        cameraActivityLauncher.launch(Intent(this, CameraActivity::class.java))
    }

    private fun requestLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            getCurrentLocation()
        } else {
            locationPermissionLauncher.launch(
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
            )
        }
    }

    private fun getCurrentLocation() {
        try {
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                if (location != null && currentLocation == null) { // Only set if not already set by map picker
                    val geoPoint = GeoPoint(location.latitude, location.longitude)
                    currentLocation = geoPoint
                    updateLocationText(location.latitude, location.longitude)
                } else if (currentLocation == null) {
                    binding.tvLocation.text = "Lokasi tidak ditemukan. Harap pilih di peta."
                }
            }
        } catch (e: SecurityException) {
            binding.tvLocation.text = "Izin lokasi ditolak. Harap pilih di peta."
        }
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            viewModel.uiState.collect { state ->
                when (state) {
                    is CreateReportUiState.Idle -> {
                        setLoading(false)
                    }
                    is CreateReportUiState.Loading -> {
                        setLoading(true)
                    }
                    is CreateReportUiState.Success -> {
                        setLoading(false)
                        Toast.makeText(this@CreateReportActivity, "Report submitted!", Toast.LENGTH_SHORT).show()
                        finish()
                    }
                    is CreateReportUiState.Error -> {
                        setLoading(false)
                        Toast.makeText(this@CreateReportActivity, state.message, Toast.LENGTH_LONG).show()
                    }
                    is CreateReportUiState.ValidationError -> {
                        setLoading(false)
                        Toast.makeText(this@CreateReportActivity, state.message, Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }

    private fun setLoading(isLoading: Boolean) {
        binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        binding.btnSubmit.isEnabled = !isLoading
        binding.btnTakePhoto.isEnabled = !isLoading
        binding.btnChangeLocation.isEnabled = !isLoading
    }
}
