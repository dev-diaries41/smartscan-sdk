package com.fpf.smartscansdk.core.ml.models

import ai.onnxruntime.OnnxTensorLike
import android.content.res.Resources
import androidx.annotation.RawRes

interface IModel : AutoCloseable {
    fun loadModel(path: String)
    fun loadModel(resources: Resources, @RawRes resourceId: Int)
    fun isLoaded(): Boolean
    fun run(inputs: Map<String, OnnxTensorLike>): Map<String, Any>
    override fun close()
}




