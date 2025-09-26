package com.fpf.smartscansdk.extensions.indexers

import android.app.Application
import android.content.ContentUris
import android.content.Context
import android.provider.MediaStore
import com.fpf.smartscansdk.core.ml.embeddings.Embedding
import com.fpf.smartscansdk.core.ml.embeddings.IEmbeddingStore
import com.fpf.smartscansdk.core.ml.embeddings.clip.ClipConfig.CLIP_EMBEDDING_LENGTH
import com.fpf.smartscansdk.core.ml.embeddings.clip.ClipConfig.IMAGE_SIZE_X
import com.fpf.smartscansdk.core.ml.embeddings.clip.ClipConfig.IMAGE_SIZE_Y
import com.fpf.smartscansdk.core.ml.embeddings.clip.ClipImageEmbedder
import com.fpf.smartscansdk.core.ml.embeddings.generatePrototypeEmbedding
import com.fpf.smartscansdk.core.processors.BatchProcessor
import com.fpf.smartscansdk.core.processors.IProcessorListener
import com.fpf.smartscansdk.core.processors.ProcessOptions
import com.fpf.smartscansdk.core.utils.extractFramesFromVideo
import com.fpf.smartscansdk.extensions.embeddings.FileEmbeddingStore

// ** Design Constraint**: For on-device vector search, the full index needs to be loaded in-memory (or make an Android native VectorDB)
// File-based EmbeddingStore is used over a Room version due to significant faster index loading
// Benchmarks: Memory-Mapped File loading 30-50x speed vs Room (both LiveData and Synchronous),
// These benchmarks strongly favour the  FileEmbeddingStore for optimal on-device search functionality and UX.
// **IMPORTANT**: Video frame extraction can fail due to codec incompatibility

class VideoIndexer(
    private val embedder: ClipImageEmbedder,
    private val frameCount: Int = 10,
    private val width: Int = IMAGE_SIZE_X,
    private val height: Int = IMAGE_SIZE_Y,
    application: Application,
    listener: IProcessorListener<Long, Embedding>? = null,
    options: ProcessOptions = ProcessOptions(),
    private val store: IEmbeddingStore,
    ): BatchProcessor<Long, Embedding>(application, listener, options){

    companion object {
        const val INDEX_FILENAME = "video_index.bin"
    }

    override suspend fun onBatchComplete(context: Context, batch: List<Embedding>) {
        store.add(batch)
    }

    override suspend fun onProcess(context: Context, id: Long): Embedding {
        val contentUri = ContentUris.withAppendedId(
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI, id
        )
        val frameBitmaps = extractFramesFromVideo(context, contentUri, width = width, height = height, frameCount = frameCount)

        if(frameBitmaps == null) throw IllegalStateException("Invalid frames")

        val rawEmbeddings = embedder.embedBatch(context.applicationContext, frameBitmaps)
        val embedding: FloatArray = generatePrototypeEmbedding(rawEmbeddings)

        return Embedding(
            id = id,
            date = System.currentTimeMillis(),
            embeddings = embedding
        )
    }

}