package com.selfie.stickerai.segmentation

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Rect
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.segmentation.Segmentation
import com.google.mlkit.vision.segmentation.SegmentationMask
import com.google.mlkit.vision.segmentation.selfie.SelfieSegmenterOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class SelfieSegmenter(context: Context) {
    
    private val options = SelfieSegmenterOptions.Builder()
        .setDetectorMode(SelfieSegmenterOptions.SINGLE_IMAGE_MODE)
        .build()
    
    private val segmenter = Segmentation.getClient(options)

    suspend fun segment(bitmap: Bitmap): Bitmap? = withContext(Dispatchers.Default) {
        try {
            val image = InputImage.fromBitmap(bitmap, 0)
            val mask = segmenter.process(image).await()
            applyMask(bitmap, mask)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun applyMask(original: Bitmap, mask: SegmentationMask): Bitmap {
        val width = original.width
        val height = original.height
        val maskBuffer = mask.buffer
        val maskWidth = mask.width
        val maskHeight = mask.height
        
        // Create transparent bitmap
        val result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)
        
        // Draw original image
        canvas.drawBitmap(original, 0f, 0f, null)
        
        // Apply mask as alpha channel
        val pixels = IntArray(width * height)
        original.getPixels(pixels, 0, width, 0, 0, width, height)
        
        val maskPixels = FloatArray(maskWidth * maskHeight)
        maskBuffer.rewind()
        maskBuffer.get(maskPixels)
        
        // Scale factor if mask is different size
        val scaleX = maskWidth.toFloat() / width
        val scaleY = maskHeight.toFloat() / height
        
        for (y in 0 until height) {
            for (x in 0 until width) {
                val maskX = (x * scaleX).toInt().coerceIn(0, maskWidth - 1)
                val maskY = (y * scaleY).toInt().coerceIn(0, maskHeight - 1)
                val confidence = maskPixels[maskY * maskWidth + maskX]
                
                val index = y * width + x
                val alpha = (confidence * 255).toInt()
                pixels[index] = (alpha shl 24) or (pixels[index] and 0x00FFFFFF)
            }
        }
        
        result.setPixels(pixels, 0, width, 0, 0, width, height)
        return result
    }

    fun close() {
        segmenter.close()
    }
}
