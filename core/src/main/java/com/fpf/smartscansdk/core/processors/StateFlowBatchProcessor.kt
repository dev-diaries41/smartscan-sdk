package com.fpf.smartscansdk.core.processors

import android.app.Application
import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class StateFlowBatchProcessor<TInput, TOutput>(
    application: Application,
    processor: IProcessor<TInput, TOutput>? = null,
    options: ProcessOptions = ProcessOptions()
) : BatchProcessor<TInput, TOutput>(application, processor, options) {

    private val _progress = MutableStateFlow(0f)
    val progress: StateFlow<Float> = _progress

    private val _status = MutableStateFlow(ProcessorStatus.IDLE)
    val status: StateFlow<ProcessorStatus> = _status

    override suspend fun run(items: List<TInput>): Metrics {
        _status.value = ProcessorStatus.ACTIVE

        val metrics = super.run(items)

        if (metrics is Metrics.Success) {
            _progress.value = 100f
            _status.value = ProcessorStatus.COMPLETE
        } else if (metrics is Metrics.Failure) {
            _status.value = ProcessorStatus.FAILED
        }
        return metrics
    }

    override suspend fun onProgress(context: Context, progress: Float) {
        _progress.value = progress
    }

    fun resetProgress() {
        _progress.value = 0f
        _status.value = ProcessorStatus.IDLE
    }
}
