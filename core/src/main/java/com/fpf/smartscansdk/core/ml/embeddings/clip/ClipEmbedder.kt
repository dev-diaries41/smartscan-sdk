package com.fpf.smartscansdk.core.ml.embeddings.clip

import ai.onnxruntime.OnnxTensor
import android.app.Application
import android.content.Context
import android.content.res.Resources
import android.graphics.Bitmap
import android.util.JsonReader
import com.fpf.smartscansdk.core.R
import com.fpf.smartscansdk.core.ml.models.IModel
import com.fpf.smartscansdk.core.ml.models.OnnxModel
import com.fpf.smartscansdk.core.ml.embeddings.ImageEmbeddingProvider
import com.fpf.smartscansdk.core.ml.embeddings.TextEmbeddingProvider
import com.fpf.smartscansdk.core.ml.embeddings.clip.ClipConfig.DIM_BATCH_SIZE
import com.fpf.smartscansdk.core.ml.embeddings.clip.ClipConfig.DIM_PIXEL_SIZE
import com.fpf.smartscansdk.core.ml.embeddings.clip.ClipConfig.IMAGE_SIZE_X
import com.fpf.smartscansdk.core.ml.embeddings.clip.ClipConfig.IMAGE_SIZE_Y
import com.fpf.smartscansdk.core.ml.embeddings.normalizeL2
import com.fpf.smartscansdk.core.processors.BatchProcessor
import com.fpf.smartscansdk.core.processors.IProcessor
import kotlinx.coroutines.*
import java.io.BufferedReader
import java.io.InputStreamReader
import java.nio.LongBuffer
import java.util.*

/** CLIP embedder using [IModel] abstraction. */
class ClipEmbedder(
    resources: Resources,
    imageModelPath: String? = null,
    textModelPath: String? = null
) : ImageEmbeddingProvider, TextEmbeddingProvider {
    private val imageModel: OnnxModel? = imageModelPath?.let { OnnxModel().apply { loadModel(it) } }
    private val textModel: OnnxModel? = textModelPath?.let { OnnxModel().apply { loadModel(it) } }

    private val tokenizerVocab: Map<String, Int> = getVocab(resources)
    private val tokenizerMerges: HashMap<Pair<String, String>, Int> = getMerges(resources)
    private val tokenizer = ClipTokenizer(tokenizerVocab, tokenizerMerges)
    private val tokenBOS = 49406
    private val tokenEOS = 49407

    override val embeddingDim: Int = 512
    private var closed = false

    override suspend fun generateImageEmbedding(bitmap: Bitmap): FloatArray =
        withContext(Dispatchers.Default) {
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

    override suspend fun generateTextEmbedding(text: String): FloatArray =
        withContext(Dispatchers.Default) {
            val model = textModel ?: throw IllegalStateException("Text model not loaded")
            val clean = Regex("[^A-Za-z0-9 ]").replace(text, "").lowercase()
            var tokens = mutableListOf(tokenBOS) + tokenizer.encode(clean) + tokenEOS
            tokens = tokens.take(77) + List(77 - tokens.size) { 0 }

            val inputIds = LongBuffer.allocate(1 * 77).apply {
                tokens.forEach { put(it.toLong()) }
                rewind()
            }
            val inputShape = longArrayOf(1, 77)

            OnnxTensor.createTensor(model.getEnv(), inputIds, inputShape).use { inputTensor ->
                val inputName = model.getInputNames()?.firstOrNull()
                    ?: throw IllegalStateException("Model inputs not available")
                val output = model.run(mapOf(inputName to inputTensor))
                normalizeL2((output.values.first() as Array<FloatArray>)[0])
            }
        }
    override suspend fun generatePrototypeEmbedding(context: Context, bitmaps: List<Bitmap>): FloatArray =
        withContext(Dispatchers.Default) {
            if (bitmaps.isEmpty()) throw IllegalArgumentException("Bitmap list is empty")

            val allEmbeddings = mutableListOf<FloatArray>()

            val iProcessor = object : IProcessor<Bitmap, FloatArray?> {
                override suspend fun onProcess(context: Context, item: Bitmap): FloatArray? {
                    return try { generateImageEmbedding(item) } catch (_: Exception) { null }
                }

                override suspend fun onBatchComplete(context: Context, outputBatch: List<FloatArray?>) {
                    allEmbeddings.addAll(outputBatch.filterNotNull())
                }
            }

            val processor = BatchProcessor<Bitmap, FloatArray?>(context.applicationContext as Application, iProcessor)
            processor.run(bitmaps)

            if (allEmbeddings.isEmpty()) throw IllegalStateException("No embeddings generated")
            val embeddingLength = allEmbeddings[0].size
            val sum = FloatArray(embeddingLength)
            for (emb in allEmbeddings) for (i in emb.indices) sum[i] += emb[i]

            normalizeL2(FloatArray(embeddingLength) { i -> sum[i] / allEmbeddings.size })
        }


    override fun closeSession() {
        if (closed) return
        closed = true
        (imageModel as? AutoCloseable)?.close()
        (textModel as? AutoCloseable)?.close()
    }

    private fun getVocab(resources: Resources): Map<String, Int> =
        hashMapOf<String, Int>().apply {
            resources.openRawResource(R.raw.vocab).use {
                val reader = JsonReader(InputStreamReader(it, "UTF-8"))
                reader.beginObject()
                while (reader.hasNext()) put(reader.nextName().replace("</w>", " "), reader.nextInt())
                reader.close()
            }
        }

    private fun getMerges(resources: Resources): HashMap<Pair<String, String>, Int> =
        hashMapOf<Pair<String, String>, Int>().apply {
            resources.openRawResource(R.raw.merges).use { stream ->
                BufferedReader(InputStreamReader(stream)).useLines { seq ->
                    seq.drop(1).forEachIndexed { i, s ->
                        val parts = s.split(" ")
                        put(parts[0] to parts[1].replace("</w>", " "), i)
                    }
                }
            }
        }
}
