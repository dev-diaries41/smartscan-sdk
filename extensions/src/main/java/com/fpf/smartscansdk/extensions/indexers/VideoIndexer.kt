package com.fpf.smartscansdk.extensions.indexers

import android.content.ContentUris
import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.MediaStore
import android.util.Log
import com.fpf.smartscansdk.core.ml.embeddings.clip.ClipEmbedder
import com.fpf.smartscansdk.core.ml.embeddings.Embedding
import com.fpf.smartscansdk.core.ml.embeddings.clip.ClipConfig.IMAGE_SIZE_X
import com.fpf.smartscansdk.core.ml.embeddings.clip.ClipConfig.IMAGE_SIZE_Y
import com.fpf.smartscansdk.core.ml.embeddings.generatePrototypeEmbedding
import com.fpf.smartscansdk.extensions.embeddings.FileEmbeddingStore
import com.fpf.smartscansdk.core.processors.IProcessor
import com.fpf.smartscansdk.core.processors.Metrics

// ** Design Constraint**: For on-device vector search, the full index needs to be loaded in-memory (or make an Android native VectorDB)
// File-based EmbeddingStore is used over a Room version due to significant faster index loading
// Benchmarks: Memory-Mapped File loading 30-50x speed vs Room (both LiveData and Synchronous),
// These benchmarks strongly favour the  FileEmbeddingStore for optimal on-device search functionality and UX.

class VideoIndexer(
    private val embedder: ClipEmbedder,
    private val store: FileEmbeddingStore
): IProcessor<Long, Embedding> {

    companion object {
        private const val TAG = "VideoIndexer"
        const val INDEX_FILENAME = "video_index.bin"
    }

    override suspend fun onComplete(context: Context, metrics: Metrics.Success) {
        // showNotification
    }

    override suspend fun onBatchComplete(context: Context, batch: List<Embedding>) {
        store.add(batch)
    }

    override suspend fun onProcess(context: Context, id: Long): Embedding {
        val contentUri = ContentUris.withAppendedId(
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI, id
        )
        val frameBitmaps = extractFramesFromVideo(context, contentUri)

        if(frameBitmaps == null) throw IllegalStateException("Invalid frames")

        val rawEmbeddings = embedder.embedBatch(context.applicationContext, frameBitmaps)
        val embedding: FloatArray = generatePrototypeEmbedding(context, rawEmbeddings)

        return Embedding(
            id = id,
            date = System.currentTimeMillis(),
            embeddings = embedding
        )
    }

    private fun extractFramesFromVideo(context: Context, videoUri: Uri, frameCount: Int = 10): List<Bitmap>? {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(context, videoUri)

            val durationUs = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLong()?.times(1000)
                ?: return null

            val frameList = mutableListOf<Bitmap>()

            for (i in 0 until frameCount) {
                val frameTimeUs = (i * durationUs) / frameCount
                val bitmap = retriever.getScaledFrameAtTime(frameTimeUs, MediaMetadataRetriever.OPTION_CLOSEST_SYNC,
                    IMAGE_SIZE_X, IMAGE_SIZE_Y
                )

                if (bitmap != null) {
                    frameList.add(bitmap)
                } else {
                    // Temporary Fix: Break early if null which suggest codec issue with video
                    break
                }
            }

            if (frameList.isEmpty()) return null

            frameList
        } catch (e: Exception) {
            Log.e(TAG, "Error extracting video frames: $e")
            null
        } finally {
            retriever.release()
        }
    }
}