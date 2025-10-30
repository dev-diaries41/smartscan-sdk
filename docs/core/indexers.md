# **Indexers**

## Overview

Provides batched, on-device indexing of media content (images and videos) into embeddings for semantic search and classification.

Key features:

* Leverages `ClipImageEmbedder` for generating embeddings.
* Processes media in batches using `BatchProcessor`.
* Stores embeddings in `IEmbeddingStore` for fast, in-memory access.
* Supports optional progress and lifecycle callbacks via `IProcessorListener`.
* Optimized for on-device performance with memory-mapped file stores.

**Design constraints:**

* Full embedding index must be loaded in memory for efficient vector search.
* File-based `EmbeddingStore` is preferred over Room due to 30–50× faster index loading.
* Video frame extraction may fail for some codecs; callers should handle exceptions.

---

## **ImageIndexer**

Processes images from the device `MediaStore` and generates embeddings.

### **Constructor**

| Parameter     | Type                                   | Description                              |
| ------------- | -------------------------------------- | ---------------------------------------- |
| `embedder`    | `ClipImageEmbedder`                    | Embedder for generating image embeddings |
| `application` | `Application`                          | Application context for batch processing |
| `listener`    | `IProcessorListener<Long, Embedding>?` | Optional processor listener              |
| `options`     | `ProcessOptions`                       | Configurable batch and memory options    |
| `store`       | `IEmbeddingStore`                      | Storage for generated embeddings         |

---

### **Methods**

| Method                            | Description                                                                                                                               |
| --------------------------------- | ----------------------------------------------------------------------------------------------------------------------------------------- |
| `onProcess(context, id)`          | Loads the image with the given MediaStore ID, generates a bitmap, embeds it using `ClipImageEmbedder`, and returns an `Embedding` object. |
| `onBatchComplete(context, batch)` | Persists the batch of embeddings to `IEmbeddingStore`.                                                                                    |

---

### **Usage Example**

```kotlin
val imageIndexer = ImageIndexer(
    embedder = clipImageEmbedder,
    application = app,
    store = fileEmbeddingStore
)

val mediaIds = listOf<Long>(123, 456, 789)
val metrics = imageIndexer.run(mediaIds)
println("Indexed ${metrics.totalProcessed} images in ${metrics.timeElapsed}ms")
```

---

## **VideoIndexer**

Processes videos from the device `MediaStore`, extracts frames, generates embeddings for each frame, and computes a prototype embedding for the video.

### **Constructor**

| Parameter     | Type                                   | Description                                                  |
| ------------- | -------------------------------------- | ------------------------------------------------------------ |
| `embedder`    | `ClipImageEmbedder`                    | Embedder for generating embeddings from video frames         |
| `frameCount`  | `Int`                                  | Number of frames to extract per video (default: 10)          |
| `width`       | `Int`                                  | Width to resize frames (default: `ClipConfig.IMAGE_SIZE_X`)  |
| `height`      | `Int`                                  | Height to resize frames (default: `ClipConfig.IMAGE_SIZE_Y`) |
| `application` | `Application`                          | Application context for batch processing                     |
| `listener`    | `IProcessorListener<Long, Embedding>?` | Optional processor listener                                  |
| `options`     | `ProcessOptions`                       | Configurable batch and memory options                        |
| `store`       | `IEmbeddingStore`                      | Storage for generated embeddings                             |

---

### **Methods**

| Method                            | Description                                                                                                                                                                                                                |
| --------------------------------- | -------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `onProcess(context, id)`          | Loads the video with the given MediaStore ID, extracts `frameCount` frames, embeds each frame with `ClipImageEmbedder`, generates a prototype embedding, and returns an `Embedding`. Throws if frames cannot be extracted. |
| `onBatchComplete(context, batch)` | Persists the batch of embeddings to `IEmbeddingStore`.                                                                                                                                                                     |

---

### **Usage Example**

```kotlin
val videoIndexer = VideoIndexer(
    embedder = clipImageEmbedder,
    frameCount = 10,
    store = fileEmbeddingStore,
    application = app
)

val videoIds = listOf<Long>(101, 102, 103)
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
