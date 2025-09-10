package com.fpf.smartscansdk.clip

import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import android.content.Context
import android.content.res.Resources
import android.graphics.Bitmap
import android.util.JsonReader
import android.util.Log
import com.fpf.smartscansdk.R
import com.fpf.smartscansdk.utils.DIM_BATCH_SIZE
import com.fpf.smartscansdk.utils.DIM_PIXEL_SIZE
import com.fpf.smartscansdk.utils.IMAGE_SIZE_X
import com.fpf.smartscansdk.utils.IMAGE_SIZE_Y
import com.fpf.smartscansdk.utils.MemoryUtils
import com.fpf.smartscansdk.utils.preProcess
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.nio.LongBuffer
import java.util.*
import kotlin.system.measureTimeMillis
import kotlin.use

class Embedder(resources: Resources, imageModelPath: String? = null, textModelPath: String? = null) {
    private var ortEnv: OrtEnvironment = OrtEnvironment.getEnvironment()
    private var imageSession: OrtSession? = null
    private var textSession: OrtSession? = null

    private val tokenizerVocab: Map<String, Int> = getVocab(resources)
    private val tokenizerMerges: HashMap<Pair<String, String>, Int> = getMerges(resources)
    private val tokenBOS: Int = 49406
    private val tokenEOS: Int = 49407
    private val tokenizer = ClipTokenizer(tokenizerVocab, tokenizerMerges)
    private var closed = false

    init {
        if (imageModelPath != null) {
            imageSession = loadModel(imageModelPath)
        }
        if (textModelPath  != null) {
            textSession = loadModel(textModelPath)
        }
    }

    private fun loadModel(modelPath: String): OrtSession {
        lateinit var session: OrtSession
        val timeTaken = measureTimeMillis {
            val modelBytes = File(modelPath).readBytes()
            session = ortEnv.createSession(modelBytes)
        }
        Log.i("Embedder", "$modelPath model loaded in ${timeTaken}ms")
        return session
    }

    suspend fun generateImageEmbedding(bitmap: Bitmap): FloatArray = withContext(Dispatchers.Default) {
        val session = imageSession ?: throw IllegalStateException("Image model not loaded")
        val inputShape = longArrayOf(DIM_BATCH_SIZE.toLong(), DIM_PIXEL_SIZE.toLong(),
            IMAGE_SIZE_X.toLong(), IMAGE_SIZE_Y.toLong()
        )
        val inputName = session.inputNames.iterator().next()
        val imgData = preProcess(bitmap)

        OnnxTensor.createTensor(ortEnv, imgData, inputShape).use { inputTensor ->
            session.run(Collections.singletonMap(inputName, inputTensor)).use { output ->
                @Suppress("UNCHECKED_CAST")
                val rawOutput = (output[0].value as Array<FloatArray>)[0]
                normalizeL2(rawOutput)
            }
        }
    }

    suspend fun generateTextEmbedding(text: String): FloatArray = withContext(Dispatchers.Default) {
        val session = textSession ?: throw IllegalStateException("Text model not loaded")
        val textClean = Regex("[^A-Za-z0-9 ]").replace(text, "").lowercase()
        var tokens = mutableListOf(tokenBOS) + tokenizer.encode(textClean) + tokenEOS
        tokens = tokens.take(77) + List(77 - tokens.size) { 0 }

        val inputShape = longArrayOf(1, 77)
        val inputIds = LongBuffer.allocate(1 * 77).apply {
            tokens.forEach { put(it.toLong()) }
            rewind()
        }

        val inputName = session.inputNames?.iterator()?.next()

        OnnxTensor.createTensor(ortEnv, inputIds, inputShape).use { inputTensor ->
            session.run(Collections.singletonMap(inputName, inputTensor)).use { output ->
                @Suppress("UNCHECKED_CAST")
                val rawOutput = (output[0].value as Array<FloatArray>)[0]
                normalizeL2(rawOutput)
            }
        }
    }

    suspend fun generatePrototypeEmbedding(context: Context, bitmaps: List<Bitmap>): FloatArray = withContext(Dispatchers.Default) {
        if (bitmaps.isEmpty()) {
            throw IllegalArgumentException("Bitmap list is empty")
        }

        val allEmbeddings = mutableListOf<FloatArray>()
        val memoryUtils = MemoryUtils(context)

        for (chunk in bitmaps.chunked(10)) {
            val currentConcurrency = memoryUtils.calculateConcurrencyLevel()
//            Log.i("generatePrototypeEmbedding", "generatePrototypeEmbedding() - Concurrency: $currentConcurrency | Free Memory: ${
//                memoryUtils.getFreeMemory() / (1024 * 1024)
//            } MB"
//            )

            val semaphore = Semaphore(currentConcurrency)

            val deferredEmbeddings: List<Deferred<FloatArray?>> = chunk.map { bitmap ->
                async {
                    semaphore.withPermit {
                        try {
                            generateImageEmbedding(bitmap)
                        } catch (e: Exception) {
                            Log.e("generatePrototypeEmbedding", "Failed to process bitmap", e)
                            null
                        }
                    }
                }
            }

            val embeddings = deferredEmbeddings.awaitAll().filterNotNull()
            allEmbeddings.addAll(embeddings)
        }

        if (allEmbeddings.isEmpty()) {
            throw IllegalStateException("No embeddings could be generated from the provided bitmaps")
        }

        val embeddingLength = allEmbeddings[0].size
        val sumEmbedding = FloatArray(embeddingLength) { 0f }
        for (emb in allEmbeddings) {
            for (i in 0 until embeddingLength) {
                sumEmbedding[i] += emb[i]
            }
        }

        val avgEmbedding = FloatArray(embeddingLength) { i -> sumEmbedding[i] / allEmbeddings.size }
        normalizeL2(avgEmbedding)
    }

    fun closeSession() {
        if (closed) return  // fix double close bug
        closed = true
        imageSession?.close()
        textSession?.close()
        imageSession = null
        textSession = null
    }

    private fun getVocab(resources: Resources): Map<String, Int> {
        return hashMapOf<String, Int>().apply {
            resources.openRawResource(R.raw.vocab).use {
                val vocabReader = JsonReader(InputStreamReader(it, "UTF-8"))
                vocabReader.beginObject()
                while (vocabReader.hasNext()) {
                    val key = vocabReader.nextName().replace("</w>", " ")
                    val value = vocabReader.nextInt()
                    put(key, value)
                }
                vocabReader.close()
            }
        }
    }

    private fun getMerges(resources: Resources): HashMap<Pair<String, String>, Int> {
        return hashMapOf<Pair<String, String>, Int>().apply {
            resources.openRawResource(R.raw.merges).use {
                val mergesReader = BufferedReader(InputStreamReader(it))
                mergesReader.useLines { seq ->
                    seq.drop(1).forEachIndexed { i, s ->
                        val list = s.split(" ")
                        val keyTuple = list[0] to list[1].replace("</w>", " ")
                        put(keyTuple, i)
                    }
                }
            }
        }
    }
}
