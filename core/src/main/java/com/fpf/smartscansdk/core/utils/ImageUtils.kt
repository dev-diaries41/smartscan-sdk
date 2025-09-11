package com.fpf.smartscansdk.core.utils

import android.content.Context
import android.graphics.*
import android.net.Uri
import android.util.LruCache
import androidx.core.graphics.scale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException


fun centerCrop(bitmap: Bitmap, imageSize: Int): Bitmap {
    val cropX: Int
    val cropY: Int
    val cropSize: Int
    if (bitmap.width >= bitmap.height) {
        cropX = bitmap.width / 2 - bitmap.height / 2
        cropY = 0
        cropSize = bitmap.height
    } else {
        cropX = 0
        cropY = bitmap.height / 2 - bitmap.width / 2
        cropSize = bitmap.width
    }
    var bitmapCropped = Bitmap.createBitmap(
        bitmap, cropX, cropY, cropSize, cropSize
    )
    bitmapCropped = bitmapCropped.scale(imageSize, imageSize, false)
    return bitmapCropped
}

fun getScaledDimensions(imgWith: Int, imgHeight: Int, maxSize: Int = 1024): Pair<Int, Int> {
    if (imgWith <= maxSize && imgHeight <= maxSize) {
        return imgWith to imgHeight
    }
    return if (imgWith >= imgHeight) {
        val scale = maxSize.toFloat() / imgWith
        maxSize to (imgHeight * scale).toInt()
    } else {
        val scale = maxSize.toFloat() / imgHeight
        (imgWith * scale).toInt() to maxSize
    }
}

/**
 * A simple LRU Cache to hold Bitmaps to avoid decoding them multiple times.
 */
object BitmapCache {
    private val cache: LruCache<Uri, Bitmap> = object : LruCache<Uri, Bitmap>(calculateMemoryCacheSize()) {
        override fun sizeOf(key: Uri, value: Bitmap): Int {
            return value.byteCount / 1024 // in KB
        }
    }

    private fun calculateMemoryCacheSize(): Int {
        val maxMemory = (Runtime.getRuntime().maxMemory() / 1024).toInt() // in KB
        val calculatedCacheSize = maxMemory / 8
        val maxAllowedCacheSize = 50 * 1024

        return if (calculatedCacheSize > maxAllowedCacheSize) {
            maxAllowedCacheSize
        } else {
            calculatedCacheSize
        }
    }

    fun get(uri: Uri): Bitmap? = cache.get(uri)
    fun put(uri: Uri, bitmap: Bitmap): Bitmap? = cache.put(uri, bitmap)
}



suspend fun loadBitmapFromUri(context: Context, uri: Uri, maxSize: Int): Bitmap? = withContext(Dispatchers.IO) {
    BitmapCache.get(uri) ?: try {
        val source = ImageDecoder.createSource(context.contentResolver, uri)
        val bitmap = ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
            val (w, h) = getScaledDimensions(imgWith  = info.size.width, imgHeight = info.size.height, maxSize)
            decoder.setTargetSize(w, h)
        }
        BitmapCache.put(uri, bitmap)
        bitmap
    } catch (e: IOException) {
        e.printStackTrace()
        null
    }
}

fun getBitmapFromUri(context: Context, uri: Uri, maxSize: Int): Bitmap {
    val source = ImageDecoder.createSource(context.contentResolver, uri)
    return ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
        val (w, h) = getScaledDimensions(info.size.width, info.size.height, maxSize)
        decoder.setTargetSize(w, h)
    }.copy(Bitmap.Config.ARGB_8888, true)
}
