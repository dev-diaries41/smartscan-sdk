# **Indexers**

## Overview

Provides batched, on-device indexing of media content (images and videos) into embeddings for semantic search and classification.

Key features:

* Processes media in batches using `BatchProcessor`.
* Supports optional progress and lifecycle callbacks via `IProcessorListener`.
* Optimized for on-device performance with memory-mapped file stores.

**Design constraints:**

* Full embedding index must be loaded in memory for efficient vector search.
* File-based `IEmbeddingStore` is preferred over Room due to up to 100× faster index loading.
* For the `VideoIndexer` video frame extraction may fail for some codecs; callers should handle exceptions.

---

## **ImageIndexer**

Processes images from the device `MediaStore` and generates embeddings.

### **Constructor**

| Parameter  | Type                                   | Description                           |
|------------|----------------------------------------| ------------------------------------- |
| `embedder` | `ImageEmbeddingProvider`               | Embedder for generating image embeddings |
| `context`  | `Context`                              | Context|
| `listener` | `IProcessorListener<Long, Embedding>?` | Optional processor listener           |
| `options`  | `ProcessOptions`                       | Configurable batch and memory options |
| `store`    | `IEmbeddingStore`                      | Storage for generated embeddings      |

---

### **Methods**

| Method                            | Description                                                                                                                               |
| --------------------------------- | ----------------------------------------------------------------------------------------------------------------------------------------- |
| `onProcess(context, id)`          | Loads the image with the given MediaStore ID, generates a bitmap, embeds it using `ImageEmbeddingProvider`, and returns an `Embedding` object. |
| `onBatchComplete(context, batch)` | Persists the batch of embeddings to `IEmbeddingStore`.                                                                                    |

---

### **Usage Example**

```kotlin
val imageEmbedder = ClipImageEmbedder(context, ResourceId(R.raw.image_encoder_quant_int8))
val imageStore = FileEmbeddingStore(File(context.filesDir, "image_index.bin"), imageEmbedder.embeddingDim, useCache = false) // cache not needed for indexing
val imageIndexer = ImageIndexer(imageEmbedder, context=context, listener = null, store = imageStore) //optionally pass a listener to handle events
val ids = getImageIds() // placeholder function to get MediaStore image ids
val metrics = imageIndexer.run(ids)
println("Indexed ${metrics.totalProcessed} images in ${metrics.timeElapsed}ms")
```

---

## **VideoIndexer**

Processes videos from the device `MediaStore`, extracts frames, generates embeddings for each frame, and computes a prototype embedding for the video.

### **Constructor**

| Parameter    | Type                                   | Description                                                  |
|--------------|----------------------------------------| ------------------------------------------------------------ |
| `embedder`   | `ImageEmbeddingProvider`               | Embedder for generating embeddings from video frames         |
| `frameCount` | `Int`                                  | Number of frames to extract per video (default: 10)          |
| `width`      | `Int`                                  | Width to resize frames   |
| `height`     | `Int`                                  | Height to resize frames  |
| `context`    | `Context`                              | Context                     |
| `listener`   | `IProcessorListener<Long, Embedding>?` | Optional processor listener                                  |
| `options`    | `ProcessOptions`                       | Configurable batch and memory options                        |
| `store`      | `IEmbeddingStore`                      | Storage for generated embeddings                             |

---

### **Methods**

| Method                            | Description                                                                                                                                                                                                                |
| --------------------------------- | -------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `onProcess(context, id)`          | Loads the video with the given MediaStore ID, extracts `frameCount` frames, embeds each frame with `ClipImageEmbedder`, generates a prototype embedding, and returns an `Embedding`. Throws if frames cannot be extracted. |
| `onBatchComplete(context, batch)` | Persists the batch of embeddings to `IEmbeddingStore`.                                                                                                                                                                     |

---

### **Usage Example**

```kotlin
val imageEmbedder = ClipImageEmbedder(context, ResourceId(R.raw.image_encoder_quant_int8))
val videoStore = FileEmbeddingStore(File(context.filesDir,  "video_index.bin"), imageEmbedder.embeddingDim, useCache = false )
val videoIndexer = VideoIndexer(imageEmbedder, context=context, listener = null, store = videoStore, width = ClipConfig.IMAGE_SIZE_X, height = ClipConfig.IMAGE_SIZE_Y)
val ids = getVideoIds() // placeholder function to get MediaStore video ids
val metrics = videoIndexer.run(videoIds)
println("Indexed ${metrics.totalProcessed} videos in ${metrics.timeElapsed}ms")
```

---

## **Extending**

To implement a custom indexer:

1. Extend `BatchProcessor<MediaId, Embedding>`.
2. Implement `onProcess()` to generate an embedding for a single media item.
3. Implement `onBatchComplete()` to persist batch embeddings.
4. Optionally provide a listener for progress or lifecycle events.
5. Consider memory constraints and batch size via `ProcessOptions`.

---
