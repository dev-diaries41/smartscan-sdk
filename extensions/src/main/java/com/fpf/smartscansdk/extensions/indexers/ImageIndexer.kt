package com.fpf.smartscansdk.extensions.indexers

import android.app.Application
import android.content.ContentUris
import android.content.Context
import android.provider.MediaStore
import com.fpf.smartscansdk.core.utils.getBitmapFromUri
import com.fpf.smartscansdk.core.ml.embeddings.Embedding
import com.fpf.smartscansdk.core.ml.embeddings.IEmbeddingStore
import com.fpf.smartscansdk.core.ml.embeddings.clip.ClipConfig
import com.fpf.smartscansdk.core.ml.embeddings.clip.ClipImageEmbedder
import com.fpf.smartscansdk.core.processors.BatchProcessor
import com.fpf.smartscansdk.core.processors.IProcessorListener
import com.fpf.smartscansdk.core.processors.ProcessOptions
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext

// ** Design Constraint**: For on-device vector search, the full index needs to be loaded in-memory (or make an Android native VectorDB)
// File-based EmbeddingStore is used over a Room version due to significant faster index loading
// Benchmarks: Memory-Mapped File loading 30-50x speed vs Room (both LiveData and Synchronous),
// These benchmarks strongly favour the  FileEmbeddingStore for optimal on-device search functionality and UX.

class ImageIndexer(
    private val embedder: ClipImageEmbedder,
    application: Application,
    listener: IProcessorListener<Long, Embedding>? = null,
    options: ProcessOptions = ProcessOptions(),
    private val store: IEmbeddingStore,
    ): BatchProcessor<Long, Embedding>(application, listener, options){

    companion object {
        const val INDEX_FILENAME = "image_index.bin"
    }

    override suspend fun onBatchComplete(context: Context, batch: List<Embedding>) {
        store.add(batch)
    }

    override suspend fun onProcess(context: Context, id: Long): Embedding {
        val contentUri = ContentUris.withAppendedId(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id
        )
        val bitmap = getBitmapFromUri(context, contentUri, ClipConfig.IMAGE_SIZE_X)
        val embedding = withContext(NonCancellable) {
            embedder.embed(bitmap)
        }
        return Embedding(
            id = id,
            date = System.currentTimeMillis(),
            embeddings = embedding
        )
    }
}