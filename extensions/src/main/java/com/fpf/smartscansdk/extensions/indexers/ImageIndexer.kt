package com.fpf.smartscansdk.extensions.indexers

import android.content.ContentUris
import android.content.Context
import android.provider.MediaStore
import com.fpf.smartscansdk.core.utils.getBitmapFromUri
import com.fpf.smartscansdk.core.ml.embeddings.Embedding
import com.fpf.smartscansdk.core.ml.embeddings.clip.ClipConfig
import com.fpf.smartscansdk.core.ml.embeddings.clip.ClipEmbedder
import com.fpf.smartscansdk.core.processors.IProcessor
import com.fpf.smartscansdk.core.processors.Metrics
import com.fpf.smartscansdk.extensions.embeddings.FileEmbeddingStore
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext

class ImageIndexer(
    private val embedder: ClipEmbedder,
    private val store: FileEmbeddingStore
): IProcessor<Long, Embedding> {

    companion object {
        private const val TAG = "ImageIndexer"
        const val INDEX_FILENAME = "image_index.bin"
    }

    override suspend fun onComplete(context: Context, metrics: Metrics.Success) {
//        store.remove(idsToPurge)
    }

    override suspend fun onBatchComplete(context: Context, batch: List<Embedding>) {
        store.append(batch)
    }

    override suspend fun onProcess(context: Context, id: Long): Embedding {
        val contentUri = ContentUris.withAppendedId(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id
        )
        val bitmap = getBitmapFromUri(context, contentUri, ClipConfig.IMAGE_SIZE_X)
        val embedding = withContext(NonCancellable) {
            embedder.generateImageEmbedding(bitmap)
        }
        return Embedding(
            id = id,
            date = System.currentTimeMillis(),
            embeddings = embedding
        )
    }
}