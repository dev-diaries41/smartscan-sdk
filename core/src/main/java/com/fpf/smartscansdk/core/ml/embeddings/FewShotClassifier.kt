package com.fpf.smartscansdk.core.ml.embeddings

fun classify(embedding: FloatArray, prototypes: List<PrototypeEmbedding>, threshold: Float = 0.4f, minMargin: Float = 0.05f, ): String?{
    val similarities = getSimilarities(embedding, prototypes.map { it.embeddings })
    val top2 = getTopN(similarities, 2)

    if(top2.isEmpty()) return null

    val bestIndex = top2[0]
    val bestSim = similarities[bestIndex]
    val secondSim = top2.getOrNull(1)?.let { similarities[it] } ?: 0f

    if (bestSim < threshold) return null
    if((bestSim - secondSim) < minMargin) return null //inconclusive -  gap between best and second is too small

    val classId = prototypes.getOrNull(bestIndex)?.id
    return classId
}
