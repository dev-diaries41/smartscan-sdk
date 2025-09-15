package com.fpf.smartscansdk.extensions.embeddings

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.fpf.smartscansdk.core.ml.embeddings.Embedding
import com.fpf.smartscansdk.extensions.data.images.ImageEmbeddingDatabase
import com.fpf.smartscansdk.extensions.data.images.ImageEmbeddingEntity
import junit.framework.Assert.assertEquals
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import kotlin.random.Random
import kotlin.system.measureNanoTime

@RunWith(AndroidJUnit4::class)
class RoomEmbeddingStoreBenchmarkTest {

    private val embeddingLength = 512
    private val numEmbeddings = 40000

    private fun createEmbedding(id: Long): ImageEmbeddingEntity {
        val values = FloatArray(embeddingLength) { Random.nextFloat() }
        return ImageEmbeddingEntity(id, System.currentTimeMillis(), values)
    }

//    @Test
//    fun benchmarkLoadingRealisticEmbeddings() = runBlocking {
//        // Use your app’s real database setup
//        val context = ApplicationProvider.getApplicationContext<Application>()
//        val db = ImageEmbeddingDatabase.getDatabase(context)
//        val dao = db.imageEmbeddingDao()
//
//        // Insert realistic embeddings
//        val embeddings = (1..numEmbeddings.toLong()).map { createEmbedding(it) }
//        embeddings.forEach { dao.insertImageEmbedding(it) }
//
//        // Measure the time for getAllEmbeddingsSync()
//        val timeNanos = measureNanoTime {
//            val loaded = dao.getAllEmbeddingsSync()
//            assertEquals(numEmbeddings, loaded.size)
//        }
//        println("Benchmark for Room approach: Number of Embeddings: $numEmbeddings  Time taken: ${timeNanos / 1_000_000.0} ms")
//    }


    @Test
    fun `file benchmark loading 2500 realistic embeddings`() = runTest {
        val context = ApplicationProvider.getApplicationContext<Application>()
        val tempDir = File(context.cacheDir, "tempEmbeddings").apply { mkdirs() }
        val store = FileEmbeddingStore(tempDir, "realistic_embeddings.bin", embeddingLength)

        // Create realistic embeddings with pseudo-random float data
        val embeddings = (1..numEmbeddings.toLong()).map { id ->
            val values = FloatArray(embeddingLength) { kotlin.random.Random.nextFloat() }
            Embedding(id, System.currentTimeMillis(), values)
        }

        // Save embeddings to disk
        val saveTime = measureNanoTime {
            store.save(embeddings)
        }
        println("Time to save: ${saveTime / 1_000_000.0} ms")

        // Clear cache to force disk read
        store.clear()
        assert(!store.isLoaded)

        // Measure time to load all embeddings from disk
        val timeNanos = measureNanoTime {
            val loaded = store.getAll()
            assert(loaded.size == numEmbeddings)
        }

        println("Benchmark for File approach: Number of Embeddings: $numEmbeddings  Time taken: ${timeNanos / 1_000_000.0} ms")
    }
}
