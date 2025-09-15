package com.fpf.smartscansdk.core.ml.embeddings

import kotlin.math.min

sealed class ClassificationResult {
    data class Success(val classId: String, val similarity: Float ): ClassificationResult()
    data class Failure(val error: ClassificationError, val similarity: Float? = null ): ClassificationResult()
}

enum class ClassificationError{MINIMUM_CLASS_SIZE, THRESHOLD, CONFIDENCE_MARGIN, LABELLED_BAD}

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


fun classifyImproved(embedding: FloatArray, classPrototypes: List<PrototypeEmbedding>,threshold: Float = 0.4f, minMargin: Float = 0.05f, ): ClassificationResult{
    if(classPrototypes.size < 2) return ClassificationResult.Failure(error= ClassificationError.MINIMUM_CLASS_SIZE) // Using a single class prototype leads to many false positives

    // No threshold filter applied here to allow confidence check by comparing top 2 matches
    // A bigger margin of confidence indicates a strong match, which helps reduce false positives
    val similarities = getSimilarities(embedding, classPrototypes.map { it.embeddings })

    val top2 = getTopN(similarities, 2)
    val bestIndex = top2[0]
    val bestSim = similarities[bestIndex]
    val secondSim = top2.getOrNull(1)?.let { similarities[it] } ?: 0f

    if (bestSim < threshold) return ClassificationResult.Failure(error= ClassificationError.THRESHOLD)
    if((bestSim - secondSim) < minMargin) return ClassificationResult.Failure(error= ClassificationError.CONFIDENCE_MARGIN) //inconclusive -  gap between best and second is too small

    val classId = classPrototypes[bestIndex].id
    return ClassificationResult.Success(classId=classId, similarity = bestSim)
}


fun twoStepClassify(embedding: FloatArray, classPrototypes: List<PrototypeEmbedding>, labelledGood: PrototypeEmbedding, labelledBad: PrototypeEmbedding,threshold: Float = 0.4f, minMargin: Float = 0.05f, ): ClassificationResult{
    if(classPrototypes.size < 2) return ClassificationResult.Failure(error= ClassificationError.MINIMUM_CLASS_SIZE) // Using a single class prototype leads to many false positives

    var stepOneResult = classifyImproved(embedding, classPrototypes, threshold, minMargin)
    if(stepOneResult is ClassificationResult.Failure){
        return stepOneResult
    }

    require(stepOneResult is ClassificationResult.Success)

    // Step 2: Compare embedding with prototypes of good and bad labels. This allows for adaptive classification
    val similarityWithGood = embedding dot labelledGood.embeddings
    val similarityWithBad = embedding dot labelledBad.embeddings
    val isMatch: Boolean =  similarityWithGood > similarityWithBad

    if(isMatch ){
        return ClassificationResult.Success(classId=stepOneResult.classId, similarity = similarityWithBad)
    }

    return ClassificationResult.Failure(error=ClassificationError.LABELLED_BAD, similarity = similarityWithGood)
}


