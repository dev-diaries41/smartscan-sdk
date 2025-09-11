package com.fpf.smartscansdk.core.ml.models

import ai.onnxruntime.OnnxTensorLike

interface IModel : AutoCloseable {
    fun loadModel(path: String)
    fun isLoaded(): Boolean
    fun run(inputs: Map<String, OnnxTensorLike>): Map<String, Any>
    override fun close()
}




