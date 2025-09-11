package com.fpf.smartscansdk.processors

import android.content.Context
import com.fpf.smartscansdk.utils.MemoryOptions

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
    val onProgress: (suspend (processedCount: Int, total: Int) -> Unit)?
    val onComplete: (suspend (context: Context, metrics: Metrics) -> Unit)?
    val onBatchComplete: (suspend (context: Context, batch: List<TOutput>) -> Unit)?
    suspend fun onProcess(context: Context, item: TInput): TOutput
    val onProcessError: ((context: Context, error: Exception, item: TInput) -> Unit)?
    val onError: (suspend (context: Context, error: Exception) -> Unit)?
}