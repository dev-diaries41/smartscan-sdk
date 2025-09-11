package com.fpf.smartscansdk.core.ml.models.embeddings

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
    suspend fun save(embeddings: List<Embedding>)
    suspend fun load(): List<Embedding>
    suspend fun append(newEmbeddings: List<Embedding>)
    suspend fun remove(ids: List<Long>)
}


interface EmbeddingProvider {
    val embeddingDim: Int? get() = null

    fun closeSession() = Unit
}

interface ImageEmbeddingProvider : EmbeddingProvider {
    suspend fun generateImageEmbedding(bitmap: Bitmap): FloatArray
    suspend fun generatePrototypeEmbedding(context: Context, bitmaps: List<Bitmap>): FloatArray
}

interface TextEmbeddingProvider : EmbeddingProvider {
    suspend fun generateTextEmbedding(text: String): FloatArray
}

