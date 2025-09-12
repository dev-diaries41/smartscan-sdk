package com.fpf.smartscansdk.core.ml.models

import android.content.res.Resources
import androidx.annotation.RawRes
import java.io.File


interface IModelLoader<T> {
    suspend fun load(): T
}

sealed interface ModelSource
data class FilePath(val path: String) : ModelSource
data class ResourceId(@RawRes val resId: Int) : ModelSource


class FileOnnxLoader(private val path: String) : IModelLoader<ByteArray> {
    override suspend fun load(): ByteArray = File(path).readBytes()
}

class ResourceOnnxLoader(private val resources: Resources, @RawRes private val resId: Int) : IModelLoader<ByteArray> {
    override suspend fun load(): ByteArray = resources.openRawResource(resId).readBytes()
}
