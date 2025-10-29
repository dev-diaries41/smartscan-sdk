package com.fpf.smartscansdk.ml.data

import androidx.annotation.RawRes

interface IModelLoader<T> {
    suspend fun load(): T
}

sealed interface ModelSource
data class FilePath(val path: String) : ModelSource
data class ResourceId(@RawRes val resId: Int) : ModelSource
