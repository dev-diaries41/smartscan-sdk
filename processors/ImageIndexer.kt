package com.fpf.smartscansdk.processors

import android.app.Application
import android.content.ContentUris
import android.provider.MediaStore
import android.util.Log
import com.fpf.smartscansdk.utils.getBitmapFromUri
import com.fpf.smartscansdk.utils.MemoryUtils
import com.fpf.smartscansdk.clip.Embedding
import com.fpf.smartscansdk.clip.Embedder
import com.fpf.smartscansdk.clip.appendEmbeddingsToFile
import com.fpf.smartscansdk.clip.loadEmbeddingsFromFile
import com.fpf.smartscansdk.clip.saveEmbeddingsToFile
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.atomic.AtomicInteger

class ImageIndexer(
    private val application: Application,
    private val listener: IIndexListener? = null
) {

    companion object {
        private const val TAG = "ImageIndexer"
        private const val BATCH_SIZE = 10
        const val INDEX_FILENAME = "image_index.bin"
    }

    suspend fun run(ids: List<Long>, embeddingHandler: Embedder): Int = withContext(Dispatchers.IO) {
        val processedCount = AtomicInteger(0)
        val startTime = System.currentTimeMillis()

        try {
            if (ids.isEmpty()) {
                Log.i(TAG, "No images found.")
                return@withContext 0
            }
            val file = File(application.filesDir, INDEX_FILENAME)

            // don't store full embeddings in var here to reduce memory usage
            val existingIds: Set<Long> = if(file.exists()){ loadEmbeddingsFromFile(file)
                .map { it.id }
                .toSet()
            } else emptySet<Long>()
            val imagesToProcess = ids.filterNot { existingIds.contains(it) }
            val idsToPurge = existingIds.minus(ids.toSet()).toList()

            var totalProcessed = 0

            val memoryUtils = MemoryUtils(application.applicationContext)

            for (batch in imagesToProcess.chunked(BATCH_SIZE)) {
                val currentConcurrency = memoryUtils.calculateConcurrencyLevel()
                // Log.i(TAG, "Current allowed concurrency: $currentConcurrency | Free Memory: ${memoryUtils.getFreeMemory() / (1024 * 1024)} MB")

                val semaphore = Semaphore(currentConcurrency)
                val batchEmb = ArrayList<Embedding>(BATCH_SIZE)
                val deferredResults = batch.map { id ->
                    async {
                        semaphore.withPermit {
                            try {
                                val contentUri = ContentUris.withAppendedId(
                                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id
                                )
                                val bitmap = getBitmapFromUri(application, contentUri)
                                val embedding = withContext(NonCancellable) {
                                    embeddingHandler.generateImageEmbedding(bitmap)
                                }

                                bitmap.recycle()
                                batchEmb.add(
                                    Embedding(
                                        id = id,
                                        date = System.currentTimeMillis(),
                                        embeddings = embedding
                                    )
                                )
                                val current = processedCount.incrementAndGet()
                                listener?.onProgress(current, imagesToProcess.size)
                                return@async 1
                            } catch (e: Exception) {
                                Log.e(TAG, "Failed to process image $id", e)
                            }
                            return@async 0
                        }
                    }
                }

                totalProcessed += deferredResults.awaitAll().sum()
                appendEmbeddingsToFile(file, batchEmb)
            }
            val endTime = System.currentTimeMillis()
            val completionTime = endTime - startTime
            listener?.onComplete(application, totalProcessed, completionTime)
            purge(idsToPurge, file)
            totalProcessed
        }
        catch (e: CancellationException) {
            throw e
        }
        catch (e: Exception) {
            listener?.onError(application, e)
            Log.e(TAG, "Error indexing images: ${e.message}", e)
            0
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