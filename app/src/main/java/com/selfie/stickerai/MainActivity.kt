package com.selfie.stickerai

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import com.selfie.stickerai.databinding.ActivityMainBinding
import com.selfie.stickerai.segmentation.SelfieSegmenter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var segmenter: SelfieSegmenter
    private var currentPhotoPath: String? = null
    
    private val CAMERA_PERMISSION = Manifest.permission.CAMERA
    private val STORAGE_PERMISSION = Manifest.permission.READ_EXTERNAL_STORAGE

    private val getContent = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let { processImage(it) }
    }

    private val takePicture = registerForActivityResult(ActivityResultContracts.TakePicture()) { success: Boolean ->
        if (success) {
            currentPhotoPath?.let { path ->
                processImage(Uri.fromFile(File(path)))
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        segmenter = SelfieSegmenter(this)

        binding.cardCamera.setOnClickListener {
            if (checkPermission(CAMERA_PERMISSION)) {
                openCamera()
            } else {
                requestPermission(CAMERA_PERMISSION)
            }
        }

        binding.cardGallery.setOnClickListener {
            if (checkPermission(STORAGE_PERMISSION)) {
                openGallery()
            } else {
                requestPermission(STORAGE_PERMISSION)
            }
        }
    }

    private fun checkPermission(permission: String): Boolean {
        return ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestPermission(permission: String) {
        ActivityCompat.requestPermissions(this, arrayOf(permission), 100)
    }

    private fun openGallery() {
        getContent.launch("image/*")
    }

    private fun openCamera() {
        val photoFile = createImageFile()
        currentPhotoPath = photoFile.absolutePath
        val photoURI = FileProvider.getUriForFile(
            this,
            "${packageName}.fileprovider",
            photoFile
        )
        takePicture.launch(photoURI)
    }

    private fun createImageFile(): File {
        val storageDir = cacheDir
        return File.createTempFile(
            "selfie_${System.currentTimeMillis()}",
            ".jpg",
            storageDir
        )
    }

    private fun processImage(uri: Uri) {
        showLoading(true)
        
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val bitmap = MediaStore.Images.Media.getBitmap(contentResolver, uri)
                val segmentedBitmap = segmenter.segment(bitmap)
                
                withContext(Dispatchers.Main) {
                    showLoading(false)
                    if (segmentedBitmap != null) {
                        navigateToPreview(segmentedBitmap)
                    } else {
                        Toast.makeText(this@MainActivity, "Failed to process image", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    showLoading(false)
                    Toast.makeText(this@MainActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun navigateToPreview(bitmap: android.graphics.Bitmap) {
        val intent = Intent(this, PreviewActivity::class.java).apply {
            putExtra("sticker_bitmap", bitmap)
        }
        startActivity(intent)
    }

    private fun showLoading(show: Boolean) {
        binding.progressBar.visibility = if (show) View.VISIBLE else View.GONE
        binding.tvProcessing.visibility = if (show) View.VISIBLE else View.GONE
        binding.cardCamera.isEnabled = !show
        binding.cardGallery.isEnabled = !show
    }

    override fun onDestroy() {
        super.onDestroy()
        segmenter.close()
    }
}
