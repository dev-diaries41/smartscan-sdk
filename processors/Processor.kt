package com.fpf.smartscansdk.processors

import android.app.Application
import android.content.Context
import android.util.Log
import com.fpf.smartscansdk.utils.MemoryUtils
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicInteger

class ProcessorManager<T, R>(
    private val application: Application,
    private val processor: IProcessor<T, R>? = null,
    private val batchSize: Int = 10,
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

            val memoryUtils = MemoryUtils(application.applicationContext)

            for (batch in items.chunked(batchSize)) {
                val currentConcurrency = memoryUtils.calculateConcurrencyLevel()
                // Log.i(TAG, "Current allowed concurrency: $currentConcurrency | Free Memory: ${memoryUtils.getFreeMemory() / (1024 * 1024)} MB")

                val semaphore = Semaphore(currentConcurrency)
                val batchEmb = ArrayList<R>(batchSize)
                val deferredResults = batch.map { item ->
                    async {
                        semaphore.withPermit {
                            try {
                                val returnVal = processor?.onProcess(application, item)
                                if(returnVal != null){
                                    batchEmb.add(returnVal)
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
                processor?.onBatchComplete(application, batchEmb)
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

interface IProcessor<T, R> {
    fun onProgress(processedCount: Int, total: Int)
    fun onComplete(context: Context, totalProcessed: Int, processingTime: Long)
    fun onBatchComplete(context: Context, batch: List<R>)
    fun onProcess(context: Context, item: T): R
    fun onProcessError(context: Context, error: Exception, item: T){
//         replace item.toString with id once i update T to have id
        Log.e(ProcessorManager.TAG, "Error processing item: ${error.message + "\n" + item.toString()}", error)
    }
    fun onError(context: Context, error: Exception){
        Log.e(ProcessorManager.TAG, "Error processing items: ${error.message}", error)
    }
}