package com.fpf.smartscansdk.core.embeddings

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.sqrt


infix fun FloatArray.dot(other: FloatArray) = foldIndexed(0.0) { i, acc, cur -> acc + cur * other[i] }.toFloat()

fun normalizeL2(inputArray: FloatArray): FloatArray {
    var norm = 0.0f
    for (i in inputArray.indices) {
        norm += inputArray[i] * inputArray[i]
    }
    norm = sqrt(norm)
    return inputArray.map { it / norm }.toFloatArray()
}

fun getSimilarities(embedding: FloatArray, comparisonEmbeddings: List<FloatArray>): List<Float> {
    return comparisonEmbeddings.map { embedding dot it }
}

fun getTopN(similarities: List<Float>, n: Int, threshold: Float = 0f): List<Int> {
    return similarities.indices.filter { similarities[it] >= threshold }
        .sortedByDescending { similarities[it] }
        .take(n)
}

suspend fun generatePrototypeEmbedding(rawEmbeddings: List<FloatArray>): FloatArray =
    withContext(Dispatchers.Default) {
        if (rawEmbeddings.isEmpty()) throw IllegalStateException("Missing embeddings")
        val embeddingLength = rawEmbeddings[0].size
        val sum = FloatArray(embeddingLength)
        for (emb in rawEmbeddings) for (i in emb.indices) sum[i] += emb[i]

        normalizeL2(FloatArray(embeddingLength) { i -> sum[i] / rawEmbeddings.size })
    }
