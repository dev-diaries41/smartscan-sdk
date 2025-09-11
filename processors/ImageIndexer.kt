package com.fpf.smartscansdk.processors

import android.content.ContentUris
import android.content.Context
import android.provider.MediaStore
import com.fpf.smartscansdk.utils.getBitmapFromUri
import com.fpf.smartscansdk.clip.Embedding
import com.fpf.smartscansdk.clip.Embedder
import com.fpf.smartscansdk.clip.FileEmbeddingStore
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext

class ImageIndexer(
    private val embedder: Embedder,
    private val store: FileEmbeddingStore
): IProcessor<Long, Embedding> {

    companion object {
        private const val TAG = "ImageIndexer"
        const val INDEX_FILENAME = "image_index.bin"
    }

    override suspend fun onComplete(context: Context, metrics: Metrics) {
        TODO("Not yet implemented")
//        store.remove(idsToPurge)
    }

    override suspend fun onBatchComplete(context: Context, batch: List<Embedding>) {
        store.append(batch)
    }

    override suspend fun onProcess(context: Context, id: Long): Embedding {
        val contentUri = ContentUris.withAppendedId(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id
        )
        val bitmap = getBitmapFromUri(context, contentUri)
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