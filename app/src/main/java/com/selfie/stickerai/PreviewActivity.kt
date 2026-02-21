package com.selfie.stickerai

import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.selfie.stickerai.databinding.ActivityPreviewBinding
import com.selfie.stickerai.export.StickerExporter
import com.selfie.stickerai.processing.StickerProcessor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class PreviewActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPreviewBinding
    private lateinit var stickerBitmap: Bitmap
    private lateinit var exporter: StickerExporter
    private var savedFile: File? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPreviewBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        exporter = StickerExporter(this)
        
        stickerBitmap = intent.getParcelableExtra("sticker_bitmap") 
            ?: run {
                Toast.makeText(this, "Error loading sticker", Toast.LENGTH_SHORT).show()
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
        binding.ivSticker.setImageBitmap(stickerBitmap)
    }

    private fun saveSticker() {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val file = exporter.saveSticker(stickerBitmap)
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
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val file = savedFile ?: exporter.saveSticker(stickerBitmap)
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
}
