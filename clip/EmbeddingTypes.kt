package com.fpf.smartscansdk.clip

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
