package com.fpf.smartscansdk.core.ml.embeddings.clip

import ai.onnxruntime.OnnxTensor
import android.app.Application
import android.content.Context
import android.content.res.Resources
import android.graphics.Bitmap
import com.fpf.smartscansdk.core.ml.embeddings.ImageEmbeddingProvider
import com.fpf.smartscansdk.core.ml.models.OnnxModel
import com.fpf.smartscansdk.core.ml.embeddings.clip.ClipConfig.DIM_BATCH_SIZE
import com.fpf.smartscansdk.core.ml.embeddings.clip.ClipConfig.DIM_PIXEL_SIZE
import com.fpf.smartscansdk.core.ml.embeddings.clip.ClipConfig.IMAGE_SIZE_X
import com.fpf.smartscansdk.core.ml.embeddings.clip.ClipConfig.IMAGE_SIZE_Y
import com.fpf.smartscansdk.core.ml.embeddings.normalizeL2
import com.fpf.smartscansdk.core.ml.models.FileOnnxLoader
import com.fpf.smartscansdk.core.ml.models.FilePath
import com.fpf.smartscansdk.core.ml.models.ModelSource
import com.fpf.smartscansdk.core.ml.models.ResourceId
import com.fpf.smartscansdk.core.ml.models.ResourceOnnxLoader
import com.fpf.smartscansdk.core.processors.BatchProcessor
import com.fpf.smartscansdk.core.processors.IProcessorListener
import kotlinx.coroutines.*

// Using ModelSource enables using with bundle model or local model which has been downloaded
class ClipImageEmbedder(
    resources: Resources,
    modelSource: ModelSource,
) : ImageEmbeddingProvider {
    private val imageModel: OnnxModel = when(modelSource){
        is FilePath -> OnnxModel(FileOnnxLoader(modelSource.path))
        is ResourceId -> OnnxModel(ResourceOnnxLoader(resources, modelSource.resId))
    }

    override val embeddingDim: Int = 512
    private var closed = false

    suspend fun initialize() = imageModel.loadModel()

    override suspend fun embed(bitmap: Bitmap): FloatArray = withContext(Dispatchers.Default) {
        val model = imageModel ?: throw IllegalStateException("Image model not loaded")
        val inputShape = longArrayOf(DIM_BATCH_SIZE.toLong(), DIM_PIXEL_SIZE.toLong(),
            IMAGE_SIZE_X.toLong(), IMAGE_SIZE_Y.toLong()
        )
        val imgData = preProcess(bitmap)

        OnnxTensor.createTensor(model.getEnv(), imgData, inputShape).use { inputTensor ->
            val inputName = model.getInputNames()?.firstOrNull()
                ?: throw IllegalStateException("Model inputs not available")
            val output = model.run(mapOf(inputName to inputTensor))
            normalizeL2((output.values.first() as Array<FloatArray>)[0])
        }
    }

    suspend fun embedBatch(context: Context, bitmaps: List<Bitmap>): List<FloatArray> {
        val allEmbeddings = mutableListOf<FloatArray>()

        val listener = object : IProcessorListener<Bitmap, FloatArray> {
            override suspend fun onBatchComplete(context: Context, batch: List<FloatArray>) {
                allEmbeddings.addAll(batch)
            }
        }

        val processor = object : BatchProcessor<Bitmap, FloatArray>(application = context.applicationContext as Application, listener = listener) {
            override suspend fun onProcess(context: Context, item: Bitmap): FloatArray {
                return embed(item)
            }
        }

        processor.run(bitmaps)
        return allEmbeddings
    }

    override fun closeSession() {
        if (closed) return
        closed = true
        (imageModel as? AutoCloseable)?.close()
    }
}
