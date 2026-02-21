package com.selfie.stickerai

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.selfie.stickerai.databinding.ActivityPreviewBinding
import com.selfie.stickerai.export.StickerExporter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class PreviewActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPreviewBinding
    private var stickerBitmap: Bitmap? = null
    private lateinit var exporter: StickerExporter
    private var savedFile: File? = null
    private var tempFilePath: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPreviewBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        exporter = StickerExporter(this)
        
        // Load from file path instead of Parcelable (fixes large bitmap issue)
        tempFilePath = intent.getStringExtra("sticker_path")
        if (tempFilePath == null) {
            Toast.makeText(this, "Error: No image path", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        
        try {
            stickerBitmap = BitmapFactory.decodeFile(tempFilePath)
            if (stickerBitmap == null) {
                Toast.makeText(this, "Error loading sticker", Toast.LENGTH_SHORT).show()
                finish()
                return
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Error loading image: ${e.message}", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        setupUI()
        displaySticker()
    }

    private fun setupUI() {
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }

        binding.fabSave.setOnClickListener {
            saveSticker()
        }

        binding.btnShareWhatsApp.setOnClickListener {
            shareToWhatsApp()
        }

        binding.btnShareTelegram.setOnClickListener {
            shareToTelegram()
        }
    }

    private fun displaySticker() {
        stickerBitmap?.let {
            binding.ivSticker.setImageBitmap(it)
        }
    }

    private fun saveSticker() {
        val bitmap = stickerBitmap ?: return
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val file = exporter.saveSticker(bitmap)
                savedFile = file
                
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@PreviewActivity, "Sticker saved!", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@PreviewActivity, "Save failed: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun shareToWhatsApp() {
        shareSticker("com.whatsapp", "WhatsApp not installed")
    }

    private fun shareToTelegram() {
        shareSticker("org.telegram.messenger", "Telegram not installed")
    }

    private fun shareSticker(packageName: String, errorMsg: String) {
        val bitmap = stickerBitmap ?: return
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val file = savedFile ?: exporter.saveSticker(bitmap)
                savedFile = file
                
                val uri = androidx.core.content.FileProvider.getUriForFile(
                    this@PreviewActivity,
                    "${packageName}.fileprovider",
                    file
                )

                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "image/webp"
                    setPackage(packageName)
                    putExtra(Intent.EXTRA_STREAM, uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }

                withContext(Dispatchers.Main) {
                    try {
                        startActivity(intent)
                    } catch (e: Exception) {
                        Toast.makeText(this@PreviewActivity, errorMsg, Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@PreviewActivity, "Share failed: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        // Clean up temp file
        tempFilePath?.let { path ->
            try {
                File(path).delete()
            } catch (e: Exception) {
                // Ignore cleanup errors
            }
        }
    }
}
