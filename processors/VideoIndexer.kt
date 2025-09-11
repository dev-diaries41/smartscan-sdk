package com.fpf.smartscansdk.processors

import android.content.ContentUris
import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.MediaStore
import android.util.Log
import com.fpf.smartscansdk.clip.Embedder
import com.fpf.smartscansdk.utils.IMAGE_SIZE_X
import com.fpf.smartscansdk.utils.IMAGE_SIZE_Y
import com.fpf.smartscansdk.clip.Embedding
import com.fpf.smartscansdk.clip.appendEmbeddingsToFile
import com.fpf.smartscansdk.clip.loadEmbeddingsFromFile
import com.fpf.smartscansdk.clip.saveEmbeddingsToFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File


class VideoIndexer(
    private val context: Context,
    private val embedder: Embedder,
): IProcessor<Long, Embedding> {

    companion object {
        private const val TAG = "VideoIndexer"
        const val INDEX_FILENAME = "video_index.bin"
    }

    val file = File(context.filesDir, INDEX_FILENAME)

    override suspend fun onComplete(context: Context, metrics: Metrics) {
        TODO("Not yet implemented")
//        purge(idsToPurge, file)
    }

    override suspend fun onBatchComplete(context: Context, batch: List<Embedding>) {
        appendEmbeddingsToFile(file, batch)
    }

    override suspend fun onProcess(context: Context, id: Long): Embedding {
        val contentUri = ContentUris.withAppendedId(
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI, id
        )
        val frameBitmaps = extractFramesFromVideo(context, contentUri)

        if(frameBitmaps == null) throw IllegalStateException("Invalid frames")

        val embedding: FloatArray = embedder.generatePrototypeEmbedding(context, frameBitmaps)

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

    private suspend fun purge(idsToPurge: List<Long>, file: File) = withContext(Dispatchers.IO) {
        if (idsToPurge.isEmpty()) return@withContext

        try {
            val embs = loadEmbeddingsFromFile(file)
            val remaining = embs.filter { it.id !in idsToPurge }
            saveEmbeddingsToFile(file, remaining)
            Log.i(TAG, "Purged ${idsToPurge.size} stale embeddings")
        } catch (e: Exception) {
            Log.e(TAG, "Error purging embeddings", e)
        }
    }
}
