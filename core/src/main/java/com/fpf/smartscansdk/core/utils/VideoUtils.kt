package com.fpf.smartscansdk.core.utils

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

suspend fun extractFramesFromVideo(context: Context, videoUri: Uri, width: Int, height: Int,frameCount: Int = 10): List<Bitmap>? =withContext(
    Dispatchers.IO) {
    val retriever = MediaMetadataRetriever()
    return@withContext try {
        retriever.setDataSource(context, videoUri)

        val durationUs = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLong()?.times(1000)
            ?: return@withContext null

        val frameList = mutableListOf<Bitmap>()

        for (i in 0 until frameCount) {
            val frameTimeUs = (i * durationUs) / frameCount
            val bitmap = retriever.getScaledFrameAtTime(frameTimeUs, MediaMetadataRetriever.OPTION_CLOSEST_SYNC,
                width, height
            )

            if (bitmap != null) {
                frameList.add(bitmap)
            } else {
                // Temporary Fix: Break early if null which suggest codec issue with video
                break
            }
        }

        if (frameList.isEmpty()) return@withContext  null

        frameList
    } catch (e: Exception) {
        Log.e("VideoUtils", "Error extracting frames", e)
        null
    } finally {
        retriever.release()
    }
}