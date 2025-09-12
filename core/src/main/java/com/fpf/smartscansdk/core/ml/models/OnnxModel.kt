package com.fpf.smartscansdk.core.ml.models

import ai.onnxruntime.OnnxTensorLike
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext

class OnnxModel(override  val loader: IModelLoader<ByteArray>) : BaseModel<OnnxTensorLike>() {
    private val env: OrtEnvironment = OrtEnvironment.getEnvironment()
    private var session: OrtSession? = null

    override suspend fun loadModel()  = coroutineScope {
        withContext(Dispatchers.IO) {
            val bytes = loader.load()
            session = env.createSession(bytes)
        }
    }

    override fun isLoaded(): Boolean = session != null

    override fun run(inputs: Map<String, OnnxTensorLike>): Map<String, Any> {
        val s = session ?: throw IllegalStateException("Model not loaded")
        s.run(inputs).use { results ->
            return results.associate { it.key to it.value.value }
        }
    }

    fun getInputNames(): List<String>? = session?.inputNames?.toList()

    fun getEnv(): OrtEnvironment = env

    override fun close() {
        session?.close()
        session = null
    }
}

