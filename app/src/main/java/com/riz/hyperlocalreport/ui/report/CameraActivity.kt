package com.riz.hyperlocalreport.ui.report

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import com.riz.hyperlocalreport.databinding.ActivityCameraBinding
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class CameraActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCameraBinding
    private var imageCapture: ImageCapture? = null
    private lateinit var cameraExecutor: ExecutorService

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCameraBinding.inflate(layoutInflater)
        setContentView(binding.root)

        startCamera()

        binding.btnCapture.setOnClickListener { takePhoto() }
        cameraExecutor = Executors.newSingleThreadExecutor()
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder()
                .build()
                .also {
                    it.setSurfaceProvider(binding.viewFinder.surfaceProvider)
                }

            imageCapture = ImageCapture.Builder().build()
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture)
            } catch (exc: Exception) {
                Toast.makeText(this, "Failed to start camera", Toast.LENGTH_SHORT).show()
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun takePhoto() {
        val imageCapture = imageCapture ?: return

        val photoFile = File(
            externalCacheDir,
            SimpleDateFormat("yyyy-MM-dd-HH-mm-ss-SSS", Locale.US).format(System.currentTimeMillis()) + ".jpg"
        )

        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

        imageCapture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageSavedCallback {
                override fun onError(exc: ImageCaptureException) {
                    Toast.makeText(this@CameraActivity, "Photo capture failed", Toast.LENGTH_SHORT).show()
                }

                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    try {
                        val bitmap = android.graphics.BitmapFactory.decodeFile(photoFile.absolutePath)
                        
                        // Scale down to max 1024 width/height to save space
                        val maxDimen = 1024
                        val scale = Math.min(maxDimen.toFloat() / bitmap.width, maxDimen.toFloat() / bitmap.height)
                        val scaledBitmap = if (scale < 1f) {
                            android.graphics.Bitmap.createScaledBitmap(
                                bitmap, 
                                (bitmap.width * scale).toInt(), 
                                (bitmap.height * scale).toInt(), 
                                true
                            )
                        } else {
                            bitmap
                        }

                        val compressedFile = File(externalCacheDir, "compressed_${photoFile.name}")
                        val out = java.io.FileOutputStream(compressedFile)
                        scaledBitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 60, out)
                        out.flush()
                        out.close()
                        photoFile.delete()
                        
                        val savedUri = Uri.fromFile(compressedFile)
                        val intent = Intent().apply {
                            putExtra(EXTRA_IMAGE_URI, savedUri.toString())
                        }
                        setResult(Activity.RESULT_OK, intent)
                        finish()
                    } catch (e: Exception) {
                        Toast.makeText(this@CameraActivity, "Compression failed", Toast.LENGTH_SHORT).show()
                        finish()
                    }
                }
            }
        )
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }

    companion object {
        const val EXTRA_IMAGE_URI = "extra_image_uri"
    }
}
