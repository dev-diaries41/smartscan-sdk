package com.fpf.smartscansdk.core.ml.embeddings.clip

import ai.onnxruntime.OnnxTensor
import android.app.Application
import android.content.Context
import android.content.res.Resources
import android.graphics.Bitmap
import com.fpf.smartscansdk.core.ml.embeddings.ImageEmbeddingProvider
import com.fpf.smartscansdk.core.ml.models.IModel
import com.fpf.smartscansdk.core.ml.models.OnnxModel
import com.fpf.smartscansdk.core.ml.embeddings.clip.ClipConfig.DIM_BATCH_SIZE
import com.fpf.smartscansdk.core.ml.embeddings.clip.ClipConfig.DIM_PIXEL_SIZE
import com.fpf.smartscansdk.core.ml.embeddings.clip.ClipConfig.IMAGE_SIZE_X
import com.fpf.smartscansdk.core.ml.embeddings.clip.ClipConfig.IMAGE_SIZE_Y
import com.fpf.smartscansdk.core.ml.embeddings.normalizeL2
import com.fpf.smartscansdk.core.ml.models.FileOnnxLoader
import com.fpf.smartscansdk.core.ml.models.FilePath
import com.fpf.smartscansdk.core.ml.models.ModelPathLike
import com.fpf.smartscansdk.core.ml.models.ResourceId
import com.fpf.smartscansdk.core.ml.models.ResourceOnnxLoader
import com.fpf.smartscansdk.core.processors.BatchProcessor
import com.fpf.smartscansdk.core.processors.IProcessor
import kotlinx.coroutines.*


/** CLIP embedder using [IModel] abstraction. */

// Using ModelPathLike enables using with bundle model or local model which has been downloaded
class ClipImageEmbedder(
    resources: Resources,
    imageModelPath: ModelPathLike,
) : ImageEmbeddingProvider {
    private val imageModel: OnnxModel? = when(imageModelPath){
        is FilePath -> OnnxModel(FileOnnxLoader(imageModelPath.path))
        is ResourceId -> OnnxModel(ResourceOnnxLoader(resources, imageModelPath.resId))
    }

    override val embeddingDim: Int = 512
    private var closed = false

    suspend fun initialize() = coroutineScope {
        val jobs = mutableListOf<Job>()
        imageModel?.let { jobs += launch { withContext(Dispatchers.IO) { it.loadModel() } } }
        jobs.joinAll()
    }

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
        val iProcessor = object : IProcessor<Bitmap, FloatArray?> {
            override suspend fun onProcess(context: Context, item: Bitmap): FloatArray? {
                return try {
                    embed(item)
                } catch (_: Exception) {
                    null
                }
            }
            override suspend fun onBatchComplete(context: Context, outputBatch: List<FloatArray?>) {
                allEmbeddings.addAll(outputBatch.filterNotNull())
            }
        }
        val processor = BatchProcessor<Bitmap, FloatArray?>(context.applicationContext as Application, iProcessor)
        processor.run(bitmaps)
        return allEmbeddings
    }


    override fun closeSession() {
        if (closed) return
        closed = true
        (imageModel as? AutoCloseable)?.close()
    }

}
