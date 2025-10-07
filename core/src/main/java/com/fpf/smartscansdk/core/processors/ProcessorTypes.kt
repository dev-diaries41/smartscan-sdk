package com.fpf.smartscansdk.core.processors

import android.content.Context

sealed class Metrics {
    data class Success(val totalProcessed: Int = 0, val timeElapsed: Long = 0L) : Metrics()
    data class Failure(val processedBeforeFailure: Int, val timeElapsed: Long, val error: Exception) : Metrics()
}


data class ProcessOptions(
    val memory: MemoryOptions = MemoryOptions(),
    val batchSize: Int = 10
)

