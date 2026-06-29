package com.riz.hyperlocalreport

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.riz.hyperlocalreport.core.common.Constants
import com.riz.hyperlocalreport.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        
        val appContainer = (application as HyperLocalReportApp).container
        appContainer.areaSessionManager.startResolution(hasPermission = granted)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHostFragment.navController

        binding.bottomNavigation.setupWithNavController(navController)
        
        // Default resident mode
        setupMenuForRole(Constants.Roles.WARGA)

        checkAndRequestLocationPermission()
    }

    private fun checkAndRequestLocationPermission() {
        val fineLocationGranted = ContextCompat.checkSelfPermission(
            this, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        
        val coarseLocationGranted = ContextCompat.checkSelfPermission(
            this, Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        
        val notificationsGranted = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }

        val hasPermission = fineLocationGranted || coarseLocationGranted

        if (hasPermission) {
            val appContainer = (application as HyperLocalReportApp).container
            appContainer.areaSessionManager.startResolution(hasPermission = true)
        }
        
        val permissionsToRequest = mutableListOf<String>()
        if (!hasPermission) {
            permissionsToRequest.add(Manifest.permission.ACCESS_FINE_LOCATION)
            permissionsToRequest.add(Manifest.permission.ACCESS_COARSE_LOCATION)
        }
        if (!notificationsGranted && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        
        if (permissionsToRequest.isNotEmpty()) {
            requestPermissionLauncher.launch(permissionsToRequest.toTypedArray())
        }
    }

    fun setupMenuForRole(role: String) {
        binding.bottomNavigation.menu.clear()
        if (role == Constants.Roles.ADMIN_RT_RW || role == Constants.Roles.ADMIN_KELURAHAN) {
            binding.bottomNavigation.inflateMenu(R.menu.menu_admin)
            navController.navigate(R.id.adminDashboardFragment)
        } else {
            binding.bottomNavigation.inflateMenu(R.menu.menu_resident)
            // homeFragment is already the startDestination, no need to navigate
        }
    }
}