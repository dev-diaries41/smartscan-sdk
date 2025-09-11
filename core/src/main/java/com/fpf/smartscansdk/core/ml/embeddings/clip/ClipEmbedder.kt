package com.fpf.smartscansdk.core.ml.embeddings.clip

import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import android.content.Context
import android.content.res.Resources
import android.graphics.Bitmap
import android.util.JsonReader
import com.fpf.smartscansdk.core.R
import com.fpf.smartscansdk.core.ml.models.IModel
import com.fpf.smartscansdk.core.ml.models.OnnxModel
import com.fpf.smartscansdk.core.ml.models.embeddings.ImageEmbeddingProvider
import com.fpf.smartscansdk.core.ml.models.embeddings.TextEmbeddingProvider
import com.fpf.smartscansdk.core.ml.models.embeddings.clip.ClipConfig.DIM_BATCH_SIZE
import com.fpf.smartscansdk.core.ml.models.embeddings.clip.ClipConfig.DIM_PIXEL_SIZE
import com.fpf.smartscansdk.core.ml.models.embeddings.clip.ClipConfig.IMAGE_SIZE_X
import com.fpf.smartscansdk.core.ml.models.embeddings.clip.ClipConfig.IMAGE_SIZE_Y
import com.fpf.smartscansdk.core.ml.models.embeddings.clip.ClipTokenizer
import com.fpf.smartscansdk.core.ml.models.embeddings.clip.preProcess
import com.fpf.smartscansdk.core.ml.models.embeddings.normalizeL2
import com.fpf.smartscansdk.core.utils.MemoryUtils
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
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
    private val imageModel: IModel? = imageModelPath?.let { OnnxModel().apply { loadModel(it) } }
    private val textModel: IModel? = textModelPath?.let { OnnxModel().apply { loadModel(it) } }

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

            OnnxTensor.createTensor(modelEnv(), imgData, inputShape).use { inputTensor ->
                val inputName = requireNotNull(modelInputName(model))
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

            OnnxTensor.createTensor(modelEnv(), inputIds, inputShape).use { inputTensor ->
                val inputName = requireNotNull(modelInputName(model))
                val output = model.run(mapOf(inputName to inputTensor))
                normalizeL2((output.values.first() as Array<FloatArray>)[0])
            }
        }

    override suspend fun generatePrototypeEmbedding(context: Context, bitmaps: List<Bitmap>): FloatArray =
        withContext(Dispatchers.Default) {
            if (bitmaps.isEmpty()) throw IllegalArgumentException("Bitmap list is empty")

            val memoryUtils = MemoryUtils(context)
            val allEmbeddings = mutableListOf<FloatArray>()

            for (chunk in bitmaps.chunked(10)) {
                val semaphore = Semaphore(memoryUtils.calculateConcurrencyLevel())
                val deferred = chunk.map { bmp ->
                    async {
                        semaphore.withPermit {
                            try { generateImageEmbedding(bmp) } catch (_: Exception) { null }
                        }
                    }
                }
                allEmbeddings.addAll(deferred.awaitAll().filterNotNull())
            }

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

    private fun modelEnv() = OrtEnvironment.getEnvironment()
    private fun modelInputName(model: IModel): String? {
        val impl = model as? OnnxModel ?: return null
        val s = impl.javaClass.getDeclaredField("session").apply { isAccessible = true }
            .get(impl) as? OrtSession
        return s?.inputNames?.firstOrNull()
    }
}
