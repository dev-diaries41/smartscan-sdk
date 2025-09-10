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
