package com.fpf.smartscansdk.core.indexers

import android.content.ContentUris
import android.content.Context
import android.provider.MediaStore
import com.fpf.smartscansdk.core.data.Embedding
import com.fpf.smartscansdk.core.data.IEmbeddingStore
import com.fpf.smartscansdk.core.data.IProcessorListener
import com.fpf.smartscansdk.core.data.ImageEmbeddingProvider
import com.fpf.smartscansdk.core.data.ProcessOptions
import com.fpf.smartscansdk.core.media.getBitmapFromUri
import com.fpf.smartscansdk.core.processors.BatchProcessor
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext

// ** Design Constraint**: For on-device vector search, the full index needs to be loaded in-memory (or make an Android native VectorDB)
// File-based EmbeddingStore is used over a Room version due to significant faster index loading
// Benchmarks: Memory-Mapped File loading 30-50x speed vs Room (both LiveData and Synchronous),
// These benchmarks strongly favour the  FileEmbeddingStore for optimal on-device search functionality and UX.

class ImageIndexer(
    private val embedder: ImageEmbeddingProvider,
    private val store: IEmbeddingStore,
    private val bitmapMaxSize: Int = 225,
    context: Context,
    listener: IProcessorListener<Long, Embedding>? = null,
    options: ProcessOptions = ProcessOptions(),
    ): BatchProcessor<Long, Embedding>(context, listener, options){

    companion object {
        const val INDEX_FILENAME = "image_index.bin"
    }

    override suspend fun onBatchComplete(context: Context, batch: List<Embedding>) {
        store.add(batch)
    }

    override suspend fun onProcess(context: Context, item: Long): Embedding {
        val contentUri = ContentUris.withAppendedId(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI, item
        )
        val bitmap = getBitmapFromUri(context, contentUri, bitmapMaxSize)
        val embedding = withContext(NonCancellable) {
            embedder.embed(bitmap)
        }
        return Embedding(
            id = item,
            date = System.currentTimeMillis(),
            embeddings = embedding
        )
    }
}