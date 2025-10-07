package com.fpf.smartscansdk.core.processors

import android.app.Application
import android.content.Context
import android.util.Log
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicInteger

// For BatchProcessor’s use case—long-running, batched,  asynchronous processing—the Application context should be used.
abstract class BatchProcessor<Input, Output>(
    private val application: Application,
    protected val listener: IProcessorListener<Input, Output>? = null,
    private val options: ProcessOptions = ProcessOptions(),
) {
    companion object {
        const val TAG = "BatchProcessor"
    }

    open suspend fun run(items: List<Input>): Metrics = withContext(Dispatchers.IO) {
        val processedCount = AtomicInteger(0)
        val startTime = System.currentTimeMillis()

        try {
            if (items.isEmpty()) {
                Log.w(TAG, "No items to process.")
                return@withContext Metrics.Success()
            }

            val memoryUtils = MemoryUtils(application, options.memory)

            listener?.onActive(application)

            for (batch in items.chunked(options.batchSize)) {
                val currentConcurrency = memoryUtils.calculateConcurrencyLevel()
                val semaphore = Semaphore(currentConcurrency)

                val deferredResults = batch.map { item ->
                    async {
                        semaphore.withPermit {
                            try {
                                val output = onProcess(application, item)
                                val current = processedCount.incrementAndGet()
                                val progress = current.toFloat() / items.size
                                listener?.onProgress(application, progress)
                                output
                            } catch (e: Exception) {
                                listener?.onError(application, e, item)
                                null
                            }
                        }
                    }
                }

                val outputBatch = deferredResults.mapNotNull { it.await() }
                onBatchComplete(application, outputBatch)
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
            listener?.onFail(application, metrics)
            metrics
        }
    }

    // Subclasses must implement this
    protected abstract suspend fun onProcess(context: Context, item: Input): Output

    // Forces all SDK users to consciously handle batch events rather than optionally relying on listeners.
    // This can prevent subtle bugs where batch-level behavior is forgotten.
    // Subclasses can optionally delegate to listener (client app) by simply calling listener.onBatchComplete in implementation
    protected abstract suspend fun onBatchComplete(context: Context, batch: List<Output>)

}





