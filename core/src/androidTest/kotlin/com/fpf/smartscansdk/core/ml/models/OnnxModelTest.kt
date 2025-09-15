package com.fpf.smartscansdk.core.ml.models


import android.content.Context
import androidx.test.core.app.ApplicationProvider
import ai.onnxruntime.*
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.Assert.assertTrue
import org.junit.Assert.assertFalse
import org.junit.Assert.assertEquals


class OnnxModelInstrumentedTest {

    private lateinit var loader: IModelLoader<ByteArray>
    private lateinit var model: OnnxModel
    private lateinit var session: OrtSession

    private val context: Context = ApplicationProvider.getApplicationContext()

    @Before
    fun setup() {
        loader = mockk()

        // Mock static OrtEnvironment before model creation
        mockkStatic(OrtEnvironment::class)
        val mockEnv = mockk<OrtEnvironment>(relaxed = true)
        every { OrtEnvironment.getEnvironment() } returns mockEnv

        // Mock session creation
        session = mockk(relaxed = true)
        every { mockEnv.createSession(any<ByteArray>()) } returns session

        // Construct the model
        model = OnnxModel(loader)

        // Ensure loader.load() is stubbed and explicitly load the model so session is set
        runBlocking {
            coEvery { loader.load() } returns "fake_model".toByteArray()
            model.loadModel()
        }
    }


    @After
    fun teardown() {
        try {
            model.close()
        } catch (_: Exception) {
            // ignore
        }
        unmockkAll()
    }

    @Test
    fun `isLoaded returns true when session is set`() {
        assertTrue(model.isLoaded())
    }

    @Test
    fun `run returns mapped output`() {
        // Mock OnnxValue
        val value = mockk<OnnxValue>()
        every { value.value } returns floatArrayOf(1.0f)

        // Mock entry
        val entry = mockk<MutableMap.MutableEntry<String, OnnxValue>>()
        every { entry.key } returns "out"
        every { entry.value } returns value

        // Mock result iterator
        val result = mockk<OrtSession.Result>()
        every { result.iterator() } returns mutableListOf(entry).iterator()
        every { result.close() } just Runs

        // Mock session.run()
        every { session.run(any<Map<String, OnnxTensorLike>>()) } returns result

        val inputs = mapOf("in" to mockk<OnnxTensorLike>())
        val output = model.run(inputs)

        assertTrue(output.containsKey("out"))
        assertEquals(floatArrayOf(1.0f).toList(), (output["out"] as FloatArray).toList())
    }

    @Test
    fun `getInputNames returns session input names`() {
        // Use a LinkedHashSet to keep deterministic order
        every { session.inputNames } returns linkedSetOf("input1", "input2")

        val inputNames = model.getInputNames()
        assertEquals(listOf("input1", "input2"), inputNames)
    }

    @Test
    fun `close closes session`() {
        model.close()
        // allow at least one close call since teardown may also close
        verify(atLeast = 1) { session.close() }
        assertFalse(model.isLoaded())
    }

    @Test
    fun `loadModel loads bytes and creates session`() = runBlocking {
        val bytes = "fake_model".toByteArray()
        coEvery { loader.load() } returns bytes

        // Use real loadModel which will call env.createSession(bytes)
        model.loadModel()

        assertTrue(model.isLoaded())
    }
}
