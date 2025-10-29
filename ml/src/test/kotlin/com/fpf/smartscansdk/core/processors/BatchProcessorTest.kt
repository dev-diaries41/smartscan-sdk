package com.fpf.smartscansdk.core.processors

import android.app.Application
import android.util.Log
import io.mockk.*
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class BatchProcessorTest {

    private lateinit var mockApp: Application
    private lateinit var mockListener: IProcessorListener<Int, Int>

    @BeforeEach
    fun setup() {
        mockApp = mockk(relaxed = true)
        mockListener = mockk(relaxed = true)
        mockkStatic(android.util.Log::class)
        every { Log.d(any<String>(), any<String>()) } returns 0
        every { Log.e(any<String>(), any<String>()) } returns 0
        every { Log.i(any<String>(), any<String>()) } returns 0
        every { Log.w(any<String>(), any<String>()) } returns 0

        // Mock MemoryUtils constructor to avoid real memory checks
        mockkConstructor(MemoryUtils::class)
        every { anyConstructed<MemoryUtils>().calculateConcurrencyLevel() } returns 2
    }

    // Simple concrete subclass for testing
    class TestProcessor(
        app: Application,
        listener: IProcessorListener<Int, Int>,
        private val failOn: Set<Int> = emptySet(),
        options: ProcessOptions = ProcessOptions(batchSize = 2)
    ) : BatchProcessor<Int, Int>(app, listener, options) {

        override suspend fun onProcess(context: android.content.Context, item: Int): Int {
            if (item in failOn) throw RuntimeException("Failed item $item")
            return item * 2
        }

        override suspend fun onBatchComplete(context: android.content.Context, batch: List<Int>) {
            // no-op for testing
        }
    }

    @Test
    fun `run processes all items successfully`() = runBlocking {
        val processor = TestProcessor(mockApp, mockListener)
        val items = listOf(1, 2, 3, 4)

        val metrics = processor.run(items)

        assertTrue(metrics is Metrics.Success)
        assertEquals(4, metrics.totalProcessed)

        coVerify { mockListener.onActive(mockApp) }
        coVerify { mockListener.onProgress(mockApp, match { it in 0f..1f }) }
        coVerify { mockListener.onComplete(mockApp, any()) }
        coVerify(exactly = 0) { mockListener.onError(any(), any(), any()) }
    }

    @Test
    fun `run handles empty input`() = runBlocking {
        val processor = TestProcessor(mockApp, mockListener)
        val items = emptyList<Int>()

        val metrics = processor.run(items)

        assertTrue(metrics is Metrics.Success)
        assertEquals(0, metrics.totalProcessed)

        coVerify(exactly = 0) { mockListener.onProgress(any(), any()) }
        coVerify ( exactly = 1){mockListener.onComplete(mockApp, any()) }
    }

    @Test
    fun `run handles item failures`() = runBlocking {
        val processor = TestProcessor(mockApp, mockListener, failOn = setOf(2, 4))
        val items = listOf(1, 2, 3, 4)

        val metrics = processor.run(items)

        assertTrue(metrics is Metrics.Success) // failures are logged but do not abort
        assertEquals(2, metrics.totalProcessed) // only successful items counted

        verify {
            mockListener.onError(mockApp, match { it.message?.contains("Failed item") == true }, any())
        }
    }

    @Test
    fun `run handles exceptions gracefully`() = runBlocking {
        val processor = TestProcessor(mockApp, mockListener, failOn = setOf(2))
        val items = listOf(1, 2, 3)

        val metrics = processor.run(items)

        assertTrue(metrics is Metrics.Success)
        assertEquals(2, metrics.totalProcessed)

        coVerify { mockListener.onError(mockApp, match { it.message?.contains("Failed item 2") == true }, 2) }
    }
}
