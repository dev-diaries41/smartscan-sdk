package com.fpf.smartscansdk.core.embeddings

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.fpf.smartscansdk.core.data.images.ImageEmbeddingDatabase
import com.fpf.smartscansdk.core.data.images.ImageEmbeddingEntity
import com.fpf.smartscansdk.core.data.images.toEmbedding
import junit.framework.Assert.assertEquals
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import kotlin.random.Random
import kotlin.system.measureNanoTime
import android.util.Log

@RunWith(AndroidJUnit4::class)
class EmbeddingLoadingBenchmarkTest {

    companion object {
        const val BENCHMARK_PATH = "embedding_benchmark_results.txt"
        const val TAG = "EmbeddingLoadingBenchmarkTest"
    }

    private val embeddingLength = 512
    private val numEmbeddings = 5000

    private fun createEmbedding(id: Long): ImageEmbeddingEntity {
        val values = FloatArray(embeddingLength) { Random.nextFloat() }
        return ImageEmbeddingEntity(id, System.currentTimeMillis(), values)
    }

    @Test
    fun benchmarkRoomVsFileWithSameEmbeddings() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Application>()

        val embeddings = (1..numEmbeddings.toLong()).map { createEmbedding(it) }

        val db = ImageEmbeddingDatabase.getDatabase(context)
        val dao = db.imageEmbeddingDao()
        dao.deleteAll() // force reload

        embeddings.forEach { dao.insertImageEmbedding(it) }

        val roomTime = measureNanoTime {
            val loadedRoom = dao.getAllEmbeddingsSync()
            assertEquals(numEmbeddings, loadedRoom.size)
        }
        val roomTimeMs = roomTime / 1_000_000.0
        logBenchmarkResult(numEmbeddings, roomTimeMs, "Room")



        val embeddingsFile = File(context.cacheDir, "embeddings.bin")
        val store = FileEmbeddingStore(embeddingsFile,  embeddingLength)

        store.add(embeddings.map { it.toEmbedding() })
        store.clear() // force reload

        val fileTime = measureNanoTime {
            val loadedFile = store.get()
            assertEquals(numEmbeddings, loadedFile.size)
        }

        val fileTimeMs = fileTime / 1_000_000.0
        logBenchmarkResult(numEmbeddings, fileTimeMs, "File")
    }

    private fun logBenchmarkResult( size: Int, time: Double, type: String) {
        Log.d(TAG,  "$type: $size embeddings, Time: $time ms")
    }
}
