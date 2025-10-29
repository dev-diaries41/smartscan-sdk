package com.fpf.smartscansdk.ml.models.providers.embeddings.clip

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import com.fpf.smartscansdk.core.data.ImageEmbeddingProvider
import com.fpf.smartscansdk.core.embeddings.normalizeL2
import com.fpf.smartscansdk.core.processors.BatchProcessor
import com.fpf.smartscansdk.ml.data.FilePath
import com.fpf.smartscansdk.ml.data.ModelSource
import com.fpf.smartscansdk.ml.data.ResourceId
import com.fpf.smartscansdk.ml.data.TensorData
import com.fpf.smartscansdk.ml.models.OnnxModel
import com.fpf.smartscansdk.ml.models.FileOnnxLoader
import com.fpf.smartscansdk.ml.models.ResourceOnnxLoader
import kotlinx.coroutines.*
import java.nio.FloatBuffer

// Using ModelSource enables using with bundle model or local model which has been downloaded
class ClipImageEmbedder(
    private val context: Context,
    modelSource: ModelSource,
) : ImageEmbeddingProvider {
    private val model: OnnxModel = when(modelSource){
        is FilePath -> OnnxModel(FileOnnxLoader(modelSource.path))
        is ResourceId -> OnnxModel(ResourceOnnxLoader(context.resources, modelSource.resId))
    }

    override val embeddingDim: Int = 512
    private var closed = false

    suspend fun initialize() = model.loadModel()

    fun isInitialized() = model.isLoaded()

    override suspend fun embed(bitmap: Bitmap): FloatArray = withContext(Dispatchers.Default) {
        if (!isInitialized()) throw IllegalStateException("Model not initialized")

        val inputShape = longArrayOf(ClipConfig.DIM_BATCH_SIZE.toLong(), ClipConfig.DIM_PIXEL_SIZE.toLong(), ClipConfig.IMAGE_SIZE_X.toLong(), ClipConfig.IMAGE_SIZE_Y.toLong())
        val imgData: FloatBuffer = preProcess(bitmap)
        val inputName = model.getInputNames()?.firstOrNull() ?: throw IllegalStateException("Model inputs not available")
        val output = model.run(mapOf(inputName to TensorData.FloatBufferTensor(imgData, inputShape)))
        normalizeL2((output.values.first() as Array<FloatArray>)[0])
    }

    override suspend fun embedBatch(data: List<Bitmap>): List<FloatArray> {
        val allEmbeddings = mutableListOf<FloatArray>()

        val processor = object : BatchProcessor<Bitmap, FloatArray>(context = context.applicationContext as Application) {
            override suspend fun onProcess(context: Context, item: Bitmap): FloatArray {
                return embed(item)
            }
            override suspend fun onBatchComplete(context: Context, batch: List<FloatArray>) {
                allEmbeddings.addAll(batch)
            }
        }

        processor.run(data)
        return allEmbeddings
    }

    override fun closeSession() {
        if (closed) return
        closed = true
        (model as? AutoCloseable)?.close()
    }
}
