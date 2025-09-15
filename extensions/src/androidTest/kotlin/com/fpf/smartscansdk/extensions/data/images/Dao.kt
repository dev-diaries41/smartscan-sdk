package com.fpf.smartscansdk.extensions.data.images

import androidx.room.*

@Dao
interface ImageEmbeddingDao {

    @Query("SELECT * FROM image_embeddings ORDER BY date DESC")
    suspend fun getAllEmbeddingsSync(): List<ImageEmbeddingEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertImageEmbedding(imageEmbedding: ImageEmbeddingEntity)

}