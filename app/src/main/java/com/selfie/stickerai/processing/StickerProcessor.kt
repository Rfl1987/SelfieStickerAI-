package com.selfie.stickerai.processing

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode

object StickerProcessor {
    
    const val STICKER_SIZE = 512

    fun createSquareSticker(bitmap: Bitmap): Bitmap {
        // Find bounding box of non-transparent pixels
        val width = bitmap.width
        val height = bitmap.height
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)
        
        var minX = width
        var minY = height
        var maxX = 0
        var maxY = 0
        
        for (y in 0 until height) {
            for (x in 0 until width) {
                val alpha = (pixels[y * width + x] shr 24) and 0xFF
                if (alpha > 10) { // Threshold for transparency
                    if (x < minX) minX = x
                    if (x > maxX) maxX = x
                    if (y < minY) minY = y
                    if (y > maxY) maxY = y
                }
            }
        }
        
        // If no content found, return centered crop
        if (minX >= maxX || minY >= maxY) {
            return cropToSquare(bitmap)
        }
        
        // Add padding (20%)
        val contentWidth = maxX - minX
        val contentHeight = maxY - minY
        val padding = (Math.max(contentWidth, contentHeight) * 0.2).toInt()
        
        val cropLeft = (minX - padding).coerceIn(0, width)
        val cropTop = (minY - padding).coerceIn(0, height)
        val cropRight = (maxX + padding).coerceIn(0, width)
        val cropBottom = (maxY + padding).coerceIn(0, height)
        
        val cropWidth = cropRight - cropLeft
        val cropHeight = cropBottom - cropTop
        
        // Create square crop
        val size = Math.max(cropWidth, cropHeight)
        val finalLeft = cropLeft - (size - cropWidth) / 2
        val finalTop = cropTop - (size - cropHeight) / 2
        
        // Ensure bounds
        val safeLeft = finalLeft.coerceIn(0, width - size)
        val safeTop = finalTop.coerceIn(0, height - size)
        val safeSize = Math.min(size, Math.min(width - safeLeft, height - safeTop))
        
        // Crop and resize to 512x512
        val cropped = Bitmap.createBitmap(bitmap, safeLeft, safeTop, safeSize, safeSize)
        return Bitmap.createScaledBitmap(cropped, STICKER_SIZE, STICKER_SIZE, true)
    }
    
    private fun cropToSquare(bitmap: Bitmap): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        val size = Math.min(width, height)
        val x = (width - size) / 2
        val y = (height - size) / 2
        val cropped = Bitmap.createBitmap(bitmap, x, y, size, size)
        return Bitmap.createScaledBitmap(cropped, STICKER_SIZE, STICKER_SIZE, true)
    }
    
    fun addWhiteBorder(bitmap: Bitmap, borderSize: Int = 20): Bitmap {
        val size = bitmap.width + (borderSize * 2)
        val result = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)
        
        // Draw white border (optional, for visibility)
        val paint = Paint().apply {
            color = android.graphics.Color.WHITE
            style = Paint.Style.FILL
        }
        canvas.drawRect(0f, 0f, size.toFloat(), size.toFloat(), paint)
        
        // Draw original bitmap centered
        canvas.drawBitmap(bitmap, borderSize.toFloat(), borderSize.toFloat(), null)
        
        return result
    }
}
