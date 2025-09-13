package com.fpf.smartscansdk.core.utils

import android.content.Context
import android.graphics.*
import android.net.Uri
import androidx.core.graphics.scale

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

fun getBitmapFromUri(context: Context, uri: Uri, maxSize: Int): Bitmap {
    val source = ImageDecoder.createSource(context.contentResolver, uri)
    return ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
        val (w, h) = getScaledDimensions(info.size.width, info.size.height, maxSize)
        decoder.setTargetSize(w, h)
    }.copy(Bitmap.Config.ARGB_8888, true)
}
