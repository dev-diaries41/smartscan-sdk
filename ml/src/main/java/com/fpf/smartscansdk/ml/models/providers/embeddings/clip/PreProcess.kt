package com.fpf.smartscansdk.ml.models.providers.embeddings.clip

import android.graphics.Bitmap
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import androidx.core.graphics.get
import com.fpf.smartscansdk.core.media.centerCrop

fun preProcess(bitmap: Bitmap): FloatBuffer {
    val cropped = centerCrop(bitmap, ClipConfig.IMAGE_SIZE_X)

    val numFloats = ClipConfig.DIM_BATCH_SIZE * ClipConfig.DIM_PIXEL_SIZE * ClipConfig.IMAGE_SIZE_Y * ClipConfig.IMAGE_SIZE_X
    val byteBuffer = ByteBuffer
        .allocateDirect(numFloats * 4)
        .order(ByteOrder.nativeOrder())
    val floatBuffer = byteBuffer.asFloatBuffer()
    for (c in 0 until ClipConfig.DIM_PIXEL_SIZE) {
        for (y in 0 until ClipConfig.IMAGE_SIZE_Y) {
            for (x in 0 until ClipConfig.IMAGE_SIZE_X) {
                val px = cropped[x, y]
                val v = when (c) {
                    0 -> (px shr 16 and 0xFF) / 255f  // R
                    1 -> (px shr  8 and 0xFF) / 255f  // G
                    else -> (px and 0xFF) / 255f  // B
                }
                val norm = when (c) {
                    0 -> (v - 0.485f) / 0.229f
                    1 -> (v - 0.456f) / 0.224f
                    else -> (v - 0.406f) / 0.225f
                }
                floatBuffer.put(norm)
            }
        }
    }
    floatBuffer.rewind()
    return floatBuffer
}