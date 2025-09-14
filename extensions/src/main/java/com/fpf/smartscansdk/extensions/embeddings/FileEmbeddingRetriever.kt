package com.fpf.smartscansdk.extensions.embeddings

import com.fpf.smartscansdk.core.ml.embeddings.Embedding
import com.fpf.smartscansdk.core.ml.embeddings.IRetriever
import com.fpf.smartscansdk.core.ml.embeddings.getSimilarities
import com.fpf.smartscansdk.core.ml.embeddings.getTopN

class FileEmbeddingRetriever(
    private val store: FileEmbeddingStore
): IRetriever {
    override suspend fun query(
        embedding: FloatArray,
        topK: Int,
        threshold: Float
    ): List<Embedding> {

        val storedEmbeddings = store.getAll()

        if (storedEmbeddings.isEmpty()) {
            return emptyList()
        }

        val similarities = getSimilarities(embedding, storedEmbeddings.map { it.embeddings })
        val results = getTopN(similarities, topK, threshold)

        if (results.isEmpty()) {
            return emptyList()
        }

        return results.map{idx -> storedEmbeddings[idx]}
    }
}

