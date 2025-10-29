package com.fpf.smartscansdk.core.data.images

import androidx.room.*

@Dao
interface ImageEmbeddingDao {

    @Query("SELECT * FROM image_embeddings ORDER BY date DESC")
    suspend fun getAllEmbeddingsSync(): List<ImageEmbeddingEntity>

    @Query("DELETE FROM image_embeddings")
    suspend fun deleteAll()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertImageEmbedding(imageEmbedding: ImageEmbeddingEntity)

}