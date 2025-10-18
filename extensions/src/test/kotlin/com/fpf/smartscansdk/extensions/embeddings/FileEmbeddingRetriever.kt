package com.fpf.smartscansdk.extensions.embeddings

import com.fpf.smartscansdk.core.ml.embeddings.Embedding
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.io.TempDir
import org.junit.jupiter.api.Test
import java.io.File

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class FileEmbeddingRetrieverTest {

    @TempDir
    lateinit var tempDir: File

    private val embeddingLength = 4

    private fun embedding(id: Long, date: Long, values: FloatArray) =
        Embedding(id, date, values)

    private fun createStore(file: File = File(tempDir, "embeddings.bin"), useCache: Boolean = true) =
        FileEmbeddingStore(file, embeddingLength, useCache = useCache)

    @Test
    fun `query batch retrieval with start and end works`() = runTest {
        val store = createStore()
        val retriever = FileEmbeddingRetriever(store)

        val embeddings = listOf(
            embedding(1, 100, floatArrayOf(0.1f, 0.2f, 0.3f, 0.4f)),
            embedding(2, 200, floatArrayOf(0.5f, 0.6f, 0.7f, 0.8f)),
            embedding(3, 300, floatArrayOf(0.9f, 1.0f, 1.1f, 1.2f))
        )
        store.add(embeddings)

        // trigger initial query to populate cachedIds
        retriever.query(floatArrayOf(0.1f, 0.2f, 0.3f, 0.4f), topK = 3, threshold = 0f)

        // fetch first two cached embeddings (order-agnostic)
        val batch1 = retriever.query(0, 2)
        assertEquals(2, batch1.size)
        assertTrue(batch1.map { it.id }.all { it in listOf(1L, 2L, 3L) })

        // fetch last cached embedding
        val batch2 = retriever.query(2, 3)
        assertEquals(1, batch2.size)
        assertTrue(batch2[0].id in listOf(1L, 2L, 3L))

        // out-of-bounds requests return empty
        val batch3 = retriever.query(3, 5)
        assertEquals(0, batch3.size)
    }
}
