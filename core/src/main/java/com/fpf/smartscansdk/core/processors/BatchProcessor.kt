package com.fpf.smartscansdk.core.processors

import android.app.Application
import android.content.Context
import android.util.Log
import com.fpf.smartscansdk.core.utils.MemoryUtils
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicInteger

// For BatchProcessor’s use case—long-running, batched,  asynchronous processing—the Application context should be used.
abstract class BatchProcessor<TInput, TOutput>(
    private val application: Application,
    private val listener: IProcessorListener<TInput, TOutput>? = null,
    private val options: ProcessOptions = ProcessOptions(),
) {
    companion object {
        const val TAG = "BatchProcessor"
    }

    open suspend fun run(items: List<TInput>): Metrics = withContext(Dispatchers.IO) {
        val processedCount = AtomicInteger(0)
        val startTime = System.currentTimeMillis()

        try {
            if (items.isEmpty()) {
                Log.w(TAG, "No items to process.")
                return@withContext Metrics.Success()
            }

            val memoryUtils = MemoryUtils(application, options.memory)

            for (batch in items.chunked(options.batchSize)) {
                val currentConcurrency = memoryUtils.calculateConcurrencyLevel()
                val semaphore = Semaphore(currentConcurrency)

                val deferredResults = batch.map { item ->
                    async {
                        semaphore.withPermit {
                            try {
                                val output = onProcess(application, item)
                                val current = processedCount.incrementAndGet()
                                val progress = (current * 100f) / items.size
                                listener?.onProgress(application, progress)
                                output
                            } catch (e: Exception) {
                                listener?.onProcessError(application, e, item)
                                null
                            }
                        }
                    }
                }

                val outputBatch = deferredResults.mapNotNull { it.await() }
                listener?.onBatchComplete(application, outputBatch)
            }

            val endTime = System.currentTimeMillis()
            val metrics = Metrics.Success(processedCount.get(), timeElapsed = endTime - startTime)

            listener?.onComplete(application, metrics)
            metrics
        }
        catch (e: CancellationException) {
            throw e
        }
        catch (e: Exception) {
            val metrics = Metrics.Failure(
                processedBeforeFailure = processedCount.get(),
                timeElapsed = System.currentTimeMillis() - startTime,
                error = e
            )
            listener?.onError(application, metrics)
            metrics
        }
    }

    // Subclasses must implement this
    protected abstract suspend fun onProcess(context: Context, item: TInput): TOutput

}





