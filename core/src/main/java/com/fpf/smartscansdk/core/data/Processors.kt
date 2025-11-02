package com.fpf.smartscansdk.core.data


sealed class Metrics {
    data class Success(val totalProcessed: Int = 0, val timeElapsed: Long = 0L) : Metrics()
    data class Failure(val processedBeforeFailure: Int, val timeElapsed: Long, val error: Exception) : Metrics()
}

data class MemoryOptions(
    val lowMemoryThreshold: Long = 800L * 1024 * 1024,
    val highMemoryThreshold: Long = 1_600L * 1024 * 1024,
    val minConcurrency: Int = 1,
    val maxConcurrency: Int = 4
)

data class ProcessOptions(
    val memory: MemoryOptions = MemoryOptions(),
    val batchSize: Int = 10
)

