package com.fpf.smartscansdk.core.ml.models

interface IModel<InputTensor> : AutoCloseable {
    val loader: IModelLoader<*> // backend handles it
    suspend fun loadModel()
    fun isLoaded(): Boolean
    fun run(inputs: Map<String, InputTensor>): Map<String, Any>

}





