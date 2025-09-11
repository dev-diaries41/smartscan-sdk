package com.fpf.smartscansdk.extensions.embeddings

import android.util.Log
import com.fpf.smartscansdk.core.ml.embeddings.Embedding
import com.fpf.smartscansdk.core.ml.embeddings.IEmbeddingStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel

class FileEmbeddingStore(private val file: File, private val embeddingLength: Int):
    IEmbeddingStore {

    companion object {
        const val CLIP_EMBEDDING_LENGTH = 512
        const val TAG = "FileEmbeddingStore"
    }

    override suspend fun save(embeddingsList: List<Embedding>): Unit = withContext(Dispatchers.IO){
        // total bytes: 4 (count) + per-entry (id(8) + date(8) + EMBEDDING_LEN*4)
        val totalBytes = 4 + embeddingsList.size * (8 + 8 + embeddingLength * 4)
        val buffer = ByteBuffer.allocate(totalBytes).order(ByteOrder.LITTLE_ENDIAN)

        buffer.putInt(embeddingsList.size)
        for (embedding in embeddingsList) {
            if (embedding.embeddings.size != embeddingLength) {
                throw IllegalArgumentException("Embedding length must be $embeddingLength")
            }
            buffer.putLong(embedding.id)
            buffer.putLong(embedding.date)
            for (f in embedding.embeddings) {
                buffer.putFloat(f)
            }
        }

        buffer.flip()
        FileOutputStream(file).channel.use { ch ->
            ch.write(buffer)
        }
    }

    override suspend fun load(): List<Embedding> = withContext(Dispatchers.IO){
        FileInputStream(file).channel.use { ch ->
            val fileSize = ch.size()
            val buffer = ch.map(FileChannel.MapMode.READ_ONLY, 0, fileSize).order(ByteOrder.LITTLE_ENDIAN)

            val count = buffer.int
            val list = ArrayList<Embedding>(count)

            repeat(count) {
                val id = buffer.long
                val date = buffer.long
                val floats = FloatArray(embeddingLength)
                val fb = buffer.asFloatBuffer()
                fb.get(floats)
                buffer.position(buffer.position() + embeddingLength * 4)
                list.add(Embedding(id, date, floats))
            }

            list
        }
    }

    override suspend fun append(newEmbeddings: List<Embedding>): Unit = withContext(Dispatchers.IO) {
        if (!file.exists()) {
            save(newEmbeddings)
            return@withContext
        }

        RandomAccessFile(file, "rw").use { raf ->
            val channel = raf.channel

            // Read the 4-byte header as little-endian
            val headerBuf = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN)
            channel.position(0)
            val read = channel.read(headerBuf)
            if (read != 4) {
                throw IOException("Failed to read header count (file too small/corrupted)")
            }
            headerBuf.flip()
            val existingCount = headerBuf.int

            // Basic validation: each existing entry is at least id(8)+date(8)+EMBEDDING_LEN*4
            val minEntryBytes = 8 + 8 + embeddingLength * 4
            val maxCountFromSize = (channel.size() / minEntryBytes).toInt()
            if (existingCount < 0 || existingCount > maxCountFromSize + 10_000) {
                throw IOException("Corrupt embeddings header: count=$existingCount, fileSize=${channel.size()}")
            }

            val newCount = existingCount + newEmbeddings.size

            // Write the updated count back as little-endian
            headerBuf.clear()
            headerBuf.putInt(newCount).flip()
            channel.position(0)
            while (headerBuf.hasRemaining()) channel.write(headerBuf)

            // Move to the end to append new entries
            channel.position(channel.size())

            for (embedding in newEmbeddings) {
                if (embedding.embeddings.size != embeddingLength) {
                    throw IllegalArgumentException("Embedding length must be $embeddingLength")
                }
                val entryBytes = (8 + 8) + embeddingLength * 4
                val buf = ByteBuffer.allocate(entryBytes).order(ByteOrder.LITTLE_ENDIAN)
                buf.putLong(embedding.id)
                buf.putLong(embedding.date)
                for (f in embedding.embeddings) buf.putFloat(f)
                buf.flip()
                while (buf.hasRemaining()) {
                    channel.write(buf)
                }
            }
            channel.force(false)
        }
    }

    override suspend fun remove(ids: List<Long>): Unit = withContext(Dispatchers.IO) {
        if (ids.isEmpty()) return@withContext

        try {
            val embeddings = load()
            val remaining = embeddings.filter { it.id !in ids }
            save(remaining)
            Log.i(TAG, "Removed ${ids.size} stale embeddings")
        } catch (e: Exception) {
            Log.e(TAG, "Error Removing embeddings", e)
        }
    }

}