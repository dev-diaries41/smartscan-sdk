package com.fpf.smartscansdk.processors

import android.app.Application
import android.util.Log
import com.fpf.smartscansdk.utils.MemoryUtils
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicInteger

class BatchProcessor<TInput, TOutput>(
    private val application: Application,
    private val processor: IProcessor<TInput, TOutput>? = null,
    private val options: ProcessOptions = ProcessOptions(),
) {
    companion object {
        const val TAG = "BatchProcessor"
    }

    private val _progress: MutableStateFlow<Float> = MutableStateFlow(0f)
    val progress: StateFlow<Float> = _progress

    private val _status = MutableStateFlow<ProcessorStatus>(ProcessorStatus.IDLE)
    val status: StateFlow<ProcessorStatus> = _status

    suspend fun run(items: List<TInput>): Metrics = withContext(Dispatchers.IO) {
        val processedCount = AtomicInteger(0)
        val startTime = System.currentTimeMillis()
        _status.value = ProcessorStatus.ACTIVE

        try {
            if (items.isEmpty()) {
                Log.w(TAG, "No items to process.")
                _status.value = ProcessorStatus.COMPLETE
                return@withContext Metrics.Success()
            }

            val memoryUtils = MemoryUtils(application.applicationContext, options.memory)

            for (batch in items.chunked(options.batchSize)) {
                val currentConcurrency = memoryUtils.calculateConcurrencyLevel()
                val semaphore = Semaphore(currentConcurrency)

                val deferredResults = batch.map { item ->
                    async {
                        semaphore.withPermit {
                            try {
                                val output = processor?.onProcess(application, item)
                                val current = processedCount.incrementAndGet()
                                _progress.value = (current * 100f) / items.size
                                output
                            } catch (e: Exception) {
                                processor?.onProcessError(application, e, item)
                                null
                            }
                        }
                    }
                }

                val outputBatch = deferredResults.mapNotNull { it.await() }
                processor?.onBatchComplete(application, outputBatch)
            }

            val endTime = System.currentTimeMillis()
            val metrics = Metrics.Success(processedCount.get(), timeElapsed = endTime - startTime)

            processor?.onComplete(application, metrics)
            _status.value = ProcessorStatus.COMPLETE
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
            processor?.onError(application, metrics)
            _status.value = ProcessorStatus.FAILED
            metrics
        }
    }

    fun resetProgress() {
        _progress.value = 0f
        _status.value = ProcessorStatus.IDLE
    }

}





