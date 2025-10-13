package com.fpf.smartscansdk.extensions.embeddings

import com.fpf.smartscansdk.core.ml.embeddings.Embedding
import com.fpf.smartscansdk.core.ml.embeddings.IRetriever
import com.fpf.smartscansdk.core.ml.embeddings.getSimilarities
import com.fpf.smartscansdk.core.ml.embeddings.getTopN

class FileEmbeddingRetriever(
    private val store: FileEmbeddingStore
): IRetriever {

    private var cachedIds: List<Long>? = null

    override suspend fun query(
        embedding: FloatArray,
        topK: Int,
        threshold: Float
    ): List<Embedding> {

        cachedIds = null // clear on new search

        val storedEmbeddings = store.getAll()

        if (storedEmbeddings.isEmpty
                ()) return emptyList()

        val similarities = getSimilarities(embedding, storedEmbeddings.map { it.embeddings })
        val resultIndices = getTopN(similarities, topK, threshold)

        if (resultIndices.isEmpty()) return emptyList()

        val idsToCache = mutableListOf<Long>()
        val results = resultIndices.map{idx ->
            idsToCache.add( storedEmbeddings[idx].id)
            storedEmbeddings[idx]
        }
        cachedIds = idsToCache
        return results
    }
}

