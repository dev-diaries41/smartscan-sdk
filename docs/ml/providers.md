# Providers

## Overview

This section of the documentation covers information about model providers.

---

## Embedding Providers

### `ClipImageEmbedder`

Reference implementation of `ImageEmbeddingProvider` using a CLIP ONNX model. Generates embedding of images.

**Key points:**

* Accepts `FilePath` or `ResourceId` model source.
* Requires explicit `initialize()` before embedding.
* Returns 512-D normalized vectors.
* Supports batch processing.

| Method                         | Description                       |
|--------------------------------|-----------------------------------|
| `initialize()`                 | Loads the ONNX model into memory  |
| `isInitialized()`              | Checks model state                |
| `embed(bitmap)`                | Generates embedding from a bitmap |
| `embedBatch(context, bitmaps)` | Batch embedding                   |
| `closeSession()`               | Frees model resources             |

**Usage Example:**

```kotlin
val imageEmbedder = ClipImageEmbedder(context, ModelSource.FilePath("/models/clip_image.onnx"))
imageEmbedder.initialize()
val embedding = imageEmbedder.embed(bitmap)
imageEmbedder.closeSession()
```

---

### `ClipTextEmbedder`

Reference implementation of `TextEmbeddingProvider` using a CLIP ONNX model and built-in tokenizer. Generates embedding of text.

**Key points:**

* Accepts bundled (`ResourceId`) or local (`FilePath`) models.
* Produces normalized 512-D vectors.
* Includes batch processing support.

| Method                       | Description                   |
|------------------------------|-------------------------------|
| `initialize()`               | Loads model weights           |
| `isInitialized()`            | Checks model state            |
| `embed(text)`                | Encodes and embeds input text |
| `embedBatch(context, texts)` | Batch text embedding          |
| `closeSession()`             | Releases resources            |

**Usage Example:**

```kotlin
val textEmbedder = ClipTextEmbedder(context, ModelSource.FilePath("/models/clip_text.onnx"))
textEmbedder.initialize()
val embedding = textEmbedder.embed(text)
textEmbedder.closeSession()
```

---

### `MiniLMTextEmbedder`

Reference implementation of `TextEmbeddingProvider` using a Mini-LM ONNX model and built-in tokenizer. Generates embedding of text. It is preferred over ClipTextEmbedder for pure text similarity usecases.

**Key points:**

* Accepts bundled (`ResourceId`) or local (`FilePath`) models.
* Produces normalized 512-D vectors.
* Includes batch processing support.

| Method                       | Description                   |
|------------------------------|-------------------------------|
| `initialize()`               | Loads model weights           |
| `isInitialized()`            | Checks model state            |
| `embed(text)`                | Encodes and embeds input text |
| `embedBatch(context, texts)` | Batch text embedding          |
| `closeSession()`             | Releases resources            |

**Usage Example:**

```kotlin
val textEmbedder = MiniLMTextEmbedder(context, ModelSource.FilePath("/models/minilm.onnx"))
textEmbedder.initialize()
val embedding = textEmbedder.embed(text)
textEmbedder.closeSession()
```

---


## **Extending**

To implement a custom embedding provider:

1. Implement `IEmbeddingProvider<T>` for your data type.
2. Ensure consistent output dimension (`embeddingDim`).

---
