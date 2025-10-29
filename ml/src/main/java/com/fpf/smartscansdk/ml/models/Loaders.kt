package com.fpf.smartscansdk.ml.models

import android.content.res.Resources
import androidx.annotation.RawRes
import com.fpf.smartscansdk.ml.data.IModelLoader
import java.io.File

class FileOnnxLoader(private val path: String) : IModelLoader<ByteArray> {
    override suspend fun load(): ByteArray = File(path).readBytes()
}

class ResourceOnnxLoader(private val resources: Resources, @RawRes private val resId: Int) : IModelLoader<ByteArray> {
    override suspend fun load(): ByteArray = resources.openRawResource(resId).readBytes()
}
