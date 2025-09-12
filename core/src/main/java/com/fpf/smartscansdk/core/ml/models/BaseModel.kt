package com.fpf.smartscansdk.core.ml.models

abstract class BaseModel<InputTensor> : AutoCloseable {
    protected abstract val loader: IModelLoader<*> // hidden implementation detail

    abstract suspend fun loadModel()
    abstract fun isLoaded(): Boolean
    abstract fun run(inputs: Map<String, InputTensor>): Map<String, Any>
}





