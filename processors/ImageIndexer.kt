package com.fpf.smartscansdk.processors

import android.content.ContentUris
import android.content.Context
import android.provider.MediaStore
import android.util.Log
import com.fpf.smartscansdk.utils.getBitmapFromUri
import com.fpf.smartscansdk.clip.Embedding
import com.fpf.smartscansdk.clip.Embedder
import com.fpf.smartscansdk.clip.appendEmbeddingsToFile
import com.fpf.smartscansdk.clip.loadEmbeddingsFromFile
import com.fpf.smartscansdk.clip.saveEmbeddingsToFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import java.io.File

class ImageIndexer(
    private val context: Context,
    private val embedder: Embedder,
): IProcessor<Long, Embedding> {

    companion object {
        private const val TAG = "ImageIndexer"
        const val INDEX_FILENAME = "image_index.bin"
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