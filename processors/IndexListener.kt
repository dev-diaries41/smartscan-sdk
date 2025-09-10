package com.fpf.smartscansdk.processors

import android.content.Context

interface IIndexListener {
    fun onProgress(processedCount: Int, total: Int)
    fun onComplete(context: Context, totalProcessed: Int, processingTime: Long)
    fun onError(context: Context, error: Exception)
}

enum class IndexStatus {IDLE, INDEXING, COMPLETE, ERROR }
