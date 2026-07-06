package com.riz.hyperlocalreport.ui.report

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.preference.PreferenceManager
import androidx.appcompat.app.AppCompatActivity
import com.riz.hyperlocalreport.databinding.ActivityMapSelectionBinding
import org.osmdroid.config.Configuration
import org.osmdroid.util.GeoPoint

class MapSelectionActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMapSelectionBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        Configuration.getInstance().load(applicationContext, PreferenceManager.getDefaultSharedPreferences(applicationContext))

        binding = ActivityMapSelectionBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.toolbar.setNavigationOnClickListener { finish() }

        binding.mapView.setMultiTouchControls(true)
        binding.mapView.controller.setZoom(18.0)

        val initialLat = intent.getDoubleExtra(EXTRA_LATITUDE, 0.0)
        val initialLng = intent.getDoubleExtra(EXTRA_LONGITUDE, 0.0)

        if (initialLat != 0.0 && initialLng != 0.0) {
            val startPoint = GeoPoint(initialLat, initialLng)
            binding.mapView.controller.setCenter(startPoint)
        } else {
            // Default center if no location provided (e.g., Bandung)
            binding.mapView.controller.setCenter(GeoPoint(-6.914744, 107.609810))
        }

        binding.btnSelectLocation.setOnClickListener {
            val center = binding.mapView.mapCenter
            val resultIntent = Intent().apply {
                putExtra(EXTRA_LATITUDE, center.latitude)
                putExtra(EXTRA_LONGITUDE, center.longitude)
            }
            setResult(Activity.RESULT_OK, resultIntent)
            finish()
        }
    }

    override fun onResume() {
        super.onResume()
        binding.mapView.onResume()
    }

    override fun onPause() {
        super.onPause()
        binding.mapView.onPause()
    }

    companion object {
        const val EXTRA_LATITUDE = "extra_latitude"
        const val EXTRA_LONGITUDE = "extra_longitude"
    }
}
