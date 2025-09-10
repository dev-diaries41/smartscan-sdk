package com.fpf.smartscansdk.processors

import android.app.Application
import android.util.Log
import com.fpf.smartscansdk.utils.MemoryOptions
import com.fpf.smartscansdk.utils.MemoryUtils
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicInteger

class BatchProcessor<T, R>(
    private val application: Application,
    private val processor: IProcessor<T, R>? = null,
    private val options: ProcessOptions = ProcessOptions(),
) {
    companion object {
        const val TAG = "Processor"
    }

    suspend fun run(items: List<T>): Int = withContext(Dispatchers.IO) {
        val processedCount = AtomicInteger(0)
        val startTime = System.currentTimeMillis()

        try {
            if (items.isEmpty()) {
                Log.w(TAG, "No items to process.")
                return@withContext 0
            }

            var totalProcessed = 0

            val memoryUtils = MemoryUtils(application.applicationContext, options.memory)

            for (batch in items.chunked(options.batchSize)) {
                val currentConcurrency = memoryUtils.calculateConcurrencyLevel()
                // Log.i(TAG, "Current allowed concurrency: $currentConcurrency | Free Memory: ${memoryUtils.getFreeMemory() / (1024 * 1024)} MB")

                val semaphore = Semaphore(currentConcurrency)
                val outputBatch = ArrayList<R>(options.batchSize)
                val deferredResults = batch.map { item ->
                    async {
                        semaphore.withPermit {
                            try {
                                val output = processor?.onProcess(application, item)
                                if(output != null){
                                    outputBatch.add(output)
                                }

                                val current = processedCount.incrementAndGet()
                                processor?.onProgress(current, items.size)
                                return@async 1
                            } catch (e: Exception) {
                                processor?.onProcessError(application, e, item)
                            }
                            return@async 0
                        }
                    }
                }

                totalProcessed += deferredResults.awaitAll().sum()
                processor?.onBatchComplete(application, outputBatch)
            }
            val endTime = System.currentTimeMillis()
            val completionTime = endTime - startTime
            processor?.onComplete(application, totalProcessed, completionTime)
            totalProcessed
        }
        catch (e: CancellationException) {
            throw e
        }
        catch (e: Exception) {
            processor?.onError(application, e)
            0
        }
    }
}

data class ProcessOptions(
    val memory: MemoryOptions = MemoryOptions(),
    val batchSize: Int = 10
)

