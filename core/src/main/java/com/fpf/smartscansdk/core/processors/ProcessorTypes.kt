package com.fpf.smartscansdk.core.processors

import android.content.Context
import com.fpf.smartscansdk.core.utils.MemoryOptions

enum class ProcessorStatus {IDLE, ACTIVE, COMPLETE, FAILED }

sealed class Metrics {
    data class Success(val totalProcessed: Int = 0, val timeElapsed: Long = 0L) : Metrics()
    data class Failure(val processedBeforeFailure: Int, val timeElapsed: Long, val error: Exception) : Metrics()
}


data class ProcessOptions(
    val memory: MemoryOptions = MemoryOptions(),
    val batchSize: Int = 10
)

interface IProcessor<TInput, TOutput> {
    suspend fun onComplete(context: Context, metrics: Metrics.Success)
    suspend fun onBatchComplete(context: Context, batch: List<TOutput>)
    suspend fun onProcess(context: Context, item: TInput): TOutput
    fun onProcessError(context: Context, error: Exception, item: TInput) {}
    suspend fun onError(context: Context, failureMetrics: Metrics.Failure) {}
}
