package com.fpf.smartscansdk.core.ml.model

import ai.onnxruntime.OnnxTensorLike

interface IModel : AutoCloseable {
    fun loadModel(path: String)
    fun isLoaded(): Boolean
    fun <T> run(inputs: Map<String, OnnxTensorLike>): Map<String, T>
    override fun close()
}



