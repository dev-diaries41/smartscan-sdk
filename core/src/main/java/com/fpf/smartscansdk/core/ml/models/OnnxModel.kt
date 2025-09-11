package com.fpf.smartscansdk.core.ml.models

import ai.onnxruntime.OnnxTensorLike
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import java.io.File

class OnnxModel : IModel {
    private val env: OrtEnvironment = OrtEnvironment.getEnvironment()
    private var session: OrtSession? = null

    override fun loadModel(path: String) {
        val bytes = File(path).readBytes()
        session = env.createSession(bytes)
    }

    override fun isLoaded(): Boolean = session != null

    override fun run(inputs: Map<String, OnnxTensorLike>): Map<String, Any> {
        val s = session ?: throw IllegalStateException("Model not loaded")
        s.run(inputs).use { results ->
            return results.associate { it.key to it.value.value }
        }
    }

    override fun close() {
        session?.close()
        session = null
    }
}
