package com.selfie.stickerai.export

import android.content.Context
import android.graphics.Bitmap
import android.os.Environment
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class StickerExporter(private val context: Context) {
    
    fun saveSticker(bitmap: Bitmap): File {
        // Process to 512x512 first
        val processed = com.selfie.stickerai.processing.StickerProcessor.createSquareSticker(bitmap)
        
        // Create filename
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val filename = "sticker_$timestamp.webp"
        
        // Save to app cache (for sharing) and optionally to Downloads
        val stickersDir = File(context.cacheDir, "stickers").apply {
            if (!exists()) mkdirs()
        }
        
        val file = File(stickersDir, filename)
        FileOutputStream(file).use { out ->
            // Compress to WebP with transparency
            processed.compress(Bitmap.CompressFormat.WEBP_LOSSLESS, 100, out)
        }
        
        // Also save to Downloads for user access
        saveToDownloads(processed, filename)
        
        return file
    }
    
    private fun saveToDownloads(bitmap: Bitmap, filename: String) {
        try {
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val selfieDir = File(downloadsDir, "SelfieStickerAI").apply {
                if (!exists()) mkdirs()
            }
            val file = File(selfieDir, filename)
            
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.WEBP_LOSSLESS, 100, out)
            }
        } catch (e: Exception) {
            // Fallback: don't crash if external storage not available
            e.printStackTrace()
        }
    }
    
    fun getStickerFile(filename: String): File? {
        val file = File(context.cacheDir, "stickers/$filename")
        return if (file.exists()) file else null
    }
}
