package com.fpf.smartscansdk.core.embeddings

import android.graphics.Bitmap


interface IEmbeddingProvider<T> {
    val embeddingDim: Int
    fun closeSession() = Unit
    suspend fun embed(data: T): FloatArray
    suspend fun embedBatch(data: List<T>): List<FloatArray>
}


typealias TextEmbeddingProvider = IEmbeddingProvider<String>
typealias ImageEmbeddingProvider = IEmbeddingProvider<Bitmap>

