package com.fpf.smartscansdk.core.ml.models

import android.content.res.Resources
import androidx.annotation.RawRes
import java.io.File


interface IModelLoader<T> {
    suspend fun load(): T
}

sealed interface ModelPathLike
data class FilePath(val path: String) : ModelPathLike
data class ResourceId(@RawRes val resId: Int) : ModelPathLike


class FileOnnxLoader(private val path: String) : IModelLoader<ByteArray> {
    override suspend fun load(): ByteArray = File(path).readBytes()
}

class ResourceOnnxLoader(private val resources: Resources, @RawRes private val resId: Int) : IModelLoader<ByteArray> {
    override suspend fun load(): ByteArray = resources.openRawResource(resId).readBytes()
}
