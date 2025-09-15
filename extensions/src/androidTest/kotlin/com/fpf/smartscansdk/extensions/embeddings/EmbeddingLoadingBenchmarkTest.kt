package com.fpf.smartscansdk.extensions.embeddings

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.fpf.smartscansdk.extensions.data.images.ImageEmbeddingDatabase
import com.fpf.smartscansdk.extensions.data.images.ImageEmbeddingEntity
import com.fpf.smartscansdk.extensions.data.images.toEmbedding
import junit.framework.Assert.assertEquals
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import kotlin.random.Random
import kotlin.system.measureNanoTime

@RunWith(AndroidJUnit4::class)
class RoomEmbeddingStoreBenchmarkTest {

    private val embeddingLength = 512
    private val numEmbeddings = 5000

    private fun createEmbedding(id: Long): ImageEmbeddingEntity {
        val values = FloatArray(embeddingLength) { Random.nextFloat() }
        return ImageEmbeddingEntity(id, System.currentTimeMillis(), values)
    }

    @Test
    fun benchmarkRoomVsFileWithSameEmbeddings() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Application>()

        // 1️⃣ Generate embeddings once
        val embeddings = (1..numEmbeddings.toLong()).map { createEmbedding(it) }

        // -------------------
        // 2️⃣ Room benchmark
        // -------------------
        val db = ImageEmbeddingDatabase.getDatabase(context)
        val dao = db.imageEmbeddingDao()

        // Insert embeddings
        embeddings.forEach { dao.insertImageEmbedding(it) }

        val roomTime = measureNanoTime {
            val loadedRoom = dao.getAllEmbeddingsSync()
            assertEquals(numEmbeddings, loadedRoom.size)
        }
        println("Benchmark for Room approach: Number of Embeddings: $numEmbeddings  Time taken: ${roomTime / 1_000_000.0} ms")

        // -------------------
        // 3️⃣ File benchmark
        // -------------------
        val tempDir = File(context.cacheDir, "tempEmbeddings").apply { mkdirs() }
        val store = FileEmbeddingStore(tempDir, "embeddings.bin", embeddingLength)

        // Save embeddings
        store.save(embeddings.map { it.toEmbedding() })
        store.clear() // force reload

        val fileTime = measureNanoTime {
            val loadedFile = store.getAll()
            assertEquals(numEmbeddings, loadedFile.size)
        }
        println("Benchmark for File approach: Number of Embeddings: $numEmbeddings  Time taken: ${fileTime / 1_000_000.0} ms")
    }

}
