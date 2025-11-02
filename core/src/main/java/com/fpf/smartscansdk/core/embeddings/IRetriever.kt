package com.fpf.smartscansdk.core.embeddings

import com.fpf.smartscansdk.core.data.Embedding

interface IRetriever {
    suspend fun query(
        embedding: FloatArray,
        topK: Int,
        threshold: Float
    ): List<Embedding>
}

