package com.fpf.smartscansdk.core.ml.embeddings

import android.content.Context
import android.graphics.Bitmap

data class Embedding(
    val id: Long,
    val date: Long,
    val embeddings: FloatArray
)

data class PrototypeEmbedding(
    val id: String,
    val date: Long,
    val embeddings: FloatArray
)


interface IEmbeddingStore {
    suspend fun add(newEmbeddings: List<Embedding>)
    suspend fun remove(ids: List<Long>)
    fun clear()
}

interface IRetriever {
    suspend fun query(
        embedding: FloatArray,
        topK: Int,
        threshold: Float
    ): List<Embedding>
}


interface IEmbeddingProvider<T> {
    val embeddingDim: Int? get() = null
    fun closeSession() = Unit
    suspend fun embed(data: T): FloatArray
}


typealias TextEmbeddingProvider = IEmbeddingProvider<String>
typealias ImageEmbeddingProvider = IEmbeddingProvider<Bitmap>


