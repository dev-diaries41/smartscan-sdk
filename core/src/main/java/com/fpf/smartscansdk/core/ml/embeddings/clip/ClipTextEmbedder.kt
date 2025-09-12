package com.fpf.smartscansdk.core.ml.embeddings.clip

import ai.onnxruntime.OnnxTensor
import android.app.Application
import android.content.Context
import android.content.res.Resources
import android.util.JsonReader
import com.fpf.smartscansdk.core.R
import com.fpf.smartscansdk.core.ml.embeddings.TextEmbeddingProvider
import com.fpf.smartscansdk.core.ml.models.OnnxModel
import com.fpf.smartscansdk.core.ml.embeddings.normalizeL2
import com.fpf.smartscansdk.core.ml.models.FileOnnxLoader
import com.fpf.smartscansdk.core.ml.models.FilePath
import com.fpf.smartscansdk.core.ml.models.ModelSource
import com.fpf.smartscansdk.core.ml.models.ResourceId
import com.fpf.smartscansdk.core.ml.models.ResourceOnnxLoader
import com.fpf.smartscansdk.core.processors.BatchProcessor
import com.fpf.smartscansdk.core.processors.IProcessor
import kotlinx.coroutines.*
import java.io.BufferedReader
import java.io.InputStreamReader
import java.nio.LongBuffer
import java.util.*


/** CLIP embedder using [IModel] abstraction. */

// Using ModelSource enables using with bundle model or local model which has been downloaded
class ClipTextEmbedder(
    resources: Resources,
    modelSource: ModelSource
) : TextEmbeddingProvider {

    private val textModel: OnnxModel = when(modelSource){
        is FilePath -> OnnxModel(FileOnnxLoader(modelSource.path))
        is ResourceId -> OnnxModel(ResourceOnnxLoader(resources, modelSource.resId))
    }

    private val tokenizerVocab: Map<String, Int> = getVocab(resources)
    private val tokenizerMerges: HashMap<Pair<String, String>, Int> = getMerges(resources)
    private val tokenizer = ClipTokenizer(tokenizerVocab, tokenizerMerges)
    private val tokenBOS = 49406
    private val tokenEOS = 49407

    override val embeddingDim: Int = 512
    private var closed = false

    suspend fun initialize() = textModel.loadModel()

    override suspend fun embed(text: String): FloatArray = withContext(Dispatchers.Default) {
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

    suspend fun embedBatch(context: Context, texts: List<String>): List<FloatArray> {
        val allEmbeddings = mutableListOf<FloatArray>()
        val iProcessor = object : IProcessor<String, FloatArray?> {
            override suspend fun onProcess(context: Context, item: String): FloatArray? {
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
        val processor = BatchProcessor<String, FloatArray?>(context.applicationContext as Application, iProcessor)
        processor.run(texts)
        return allEmbeddings
    }

    override fun closeSession() {
        if (closed) return
        closed = true
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
