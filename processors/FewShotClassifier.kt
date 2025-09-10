package com.fpf.smartscansdk.processors

import android.content.Context
import androidx.core.net.toUri
import com.fpf.smartscansdk.clip.Embedder
import com.fpf.smartscansdk.clip.getSimilarities
import com.fpf.smartscansdk.clip.getTopN
import com.fpf.smartscansdk.utils.getBitmapFromUri
import com.fpf.smartscansdk.clip.PrototypeEmbedding

suspend fun classify(context: Context, imageUri: String,embedder: Embedder, prototypeList: List<PrototypeEmbedding>,): ClassificationResult{
    val bitmap = getBitmapFromUri(context, imageUri.toUri())
    val imageEmbedding = embedder.generateImageEmbedding(bitmap)
    bitmap.recycle()

    val similarities = getSimilarities(imageEmbedding, prototypeList.map { it.embeddings })
    val top2 = getTopN(similarities, 2)

    if(top2.isEmpty()) return ClassificationResult.Failure(itemId = imageUri)

    val bestIndex = top2[0]
    val bestSim = similarities[bestIndex]
    val secondSim = top2.getOrNull(1)?.let { similarities[it] } ?: 0f

    val threshold = 0.4f
    val minMargin = 0.05f

    if (bestSim < threshold) return ClassificationResult.Failure(itemId = imageUri) // don't move if below threshold
    if((bestSim - secondSim) < minMargin) return ClassificationResult.Failure(itemId = imageUri) // don't move if gap between best and second is too small

    val classId = prototypeList.getOrNull(bestIndex)?.id

    return if (classId == null) {
        ClassificationResult.Failure(itemId = imageUri)
    } else {
        ClassificationResult.Success(itemId = imageUri, classId)
    }
}


sealed class ClassificationResult {
    data class Success(val itemId: String, val classId: String) : ClassificationResult()
    data class Failure(val itemId: String) : ClassificationResult()
}
