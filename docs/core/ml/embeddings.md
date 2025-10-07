# **Embeddings**

## Overview

Provides unified interfaces and base data types for generating and managing vector embeddings across different media types.
Implements both generic contracts and reference CLIP-based providers for image and text, plus few-shot classification utilities.

---

## **Core Data Types**

### `Embedding`

Represents a raw embedding vector for a single media item.

| Property     | Type         | Description                                  |
| ------------ | ------------ | -------------------------------------------- |
| `id`         | `Long`       | Unique MediaStore or item ID                 |
| `date`       | `Long`       | Timestamp associated with embedding creation |
| `embeddings` | `FloatArray` | Vector representation                        |

### `PrototypeEmbedding`

Represents a class-level prototype vector for few-shot classification.

| Property     | Type         | Description                                  |
| ------------ | ------------ | -------------------------------------------- |
| `id`         | `String`     | Class identifier                             |
| `date`       | `Long`       | Timestamp associated with prototype creation |
| `embeddings` | `FloatArray` | Averaged vector representation               |

---

## **Interfaces**

### `IEmbeddingStore`

Defines a persistence interface for managing embedding data.
**Responsibilities:**

* Add or remove stored embeddings
* Retrieve all embeddings for in-memory indexing
* Clear cache or local data

| Member     | Type                                              | Description                     |
| ---------- | ------------------------------------------------- | ------------------------------- |
| `isCached` | `Boolean`                                         | Indicates if results are cached |
| `exists`   | `Boolean`                                         | Checks if store data exists     |
| `add()`    | `suspend fun add(newEmbeddings: List<Embedding>)` | Inserts embeddings              |
| `remove()` | `suspend fun remove(ids: List<Long>)`             | Removes embeddings by ID        |
| `getAll()` | `suspend fun getAll(): List<Embedding>`           | Loads full embedding index      |
| `clear()`  | `fun clear()`                                     | Clears local data               |

---

### `IRetriever`

Defines nearest-neighbor or similarity-based retrieval over stored embeddings.

| Method                              | Description                                 |
| ----------------------------------- | ------------------------------------------- |
| `query(embedding, topK, threshold)` | Returns a ranked list of similar embeddings |

---

### `IEmbeddingProvider<T>`

Defines the contract for embedding generators (text, image, etc.).

| Member           | Type          | Description                          |
| ---------------- | ------------- | ------------------------------------ |
| `embeddingDim`   | `Int?`        | Embedding vector dimension           |
| `embed(data: T)` | `suspend fun` | Generates embedding for input        |
| `closeSession()` | `fun`         | Releases underlying model or session |

**Type aliases:**

* `TextEmbeddingProvider = IEmbeddingProvider<String>`
* `ImageEmbeddingProvider = IEmbeddingProvider<Bitmap>`

---

## **Implementations**

### `ClipImageEmbedder`

Reference implementation of `ImageEmbeddingProvider` using a CLIP ONNX model.
Supports on-device embedding generation for bitmaps.

**Key points:**

* Accepts `FilePath` or `ResourceId` model source.
* Requires explicit `initialize()` before embedding.
* Returns 512-D normalized vectors.
* Supports batch processing via `BatchProcessor`.

| Method                         | Description                       |
| ------------------------------ | --------------------------------- |
| `initialize()`                 | Loads the ONNX model into memory  |
| `isInitialized()`              | Checks model state                |
| `embed(bitmap)`                | Generates embedding from a bitmap |
| `embedBatch(context, bitmaps)` | Batch embedding                   |
| `closeSession()`               | Frees model resources             |

#### **Usage Example**

```kotlin
val imageEmbedder = ClipImageEmbedder(resources, ModelSource.FilePath("/models/clip_image.onnx"))
imageEmbedder.initialize()
val embedding = imageEmbedder.embed(bitmap)
imageEmbedder.closeSession()
```

---

### `ClipTextEmbedder`

Reference implementation of `TextEmbeddingProvider` using a CLIP ONNX model and built-in tokenizer.

**Key points:**

* Tokenizes text using CLIP’s BPE vocabulary and merge rules.
* Accepts bundled (`ResourceId`) or local (`FilePath`) models.
* Produces normalized 512-D vectors.
* Includes batch processing support.

| Method                       | Description                   |
| ---------------------------- | ----------------------------- |
| `initialize()`               | Loads model weights           |
| `isInitialized()`            | Checks model state            |
| `embed(text)`                | Encodes and embeds input text |
| `embedBatch(context, texts)` | Batch text embedding          |
| `closeSession()`             | Releases resources            |

#### **Usage Example**

```kotlin
val textEmbedder = ClipTextEmbedder(resources, ModelSource.FilePath("/models/clip_text.onnx"))
textEmbedder.initialize()
val embedding = textEmbedder.embed(text)
textEmbedder.closeSession()
```

---

## **Few-Shot Classification**

### `ClassificationResult`

Represents the outcome of a classification attempt.

| Type      | Description                                                           |
| --------- | --------------------------------------------------------------------- |
| `Success` | Contains `classId` of the closest match and similarity score          |
| `Failure` | Contains a `ClassificationError` indicating why classification failed |

### `ClassificationError`

Enumerates possible failure reasons:

| Error                | Description                                                         |
| -------------------- | ------------------------------------------------------------------- |
| `MINIMUM_CLASS_SIZE` | Not enough class prototypes to perform classification (requires ≥2) |
| `THRESHOLD`          | Top similarity below minimum threshold                              |
| `CONFIDENCE_MARGIN`  | Gap between top 2 similarities too small to be conclusive           |
| `LABELLED_BAD`       | Optional: indicates invalid or corrupted class prototype            |

### `classify`

Performs few-shot classification of a single embedding.

**Signature:**

```kotlin
fun classify(
    embedding: FloatArray,
    classPrototypes: List<PrototypeEmbedding>,
    threshold: Float = 0.4f,
    confidenceMargin: Float = 0.05f
): ClassificationResult
```

**Behavior:**

1. Returns `Failure(MINIMUM_CLASS_SIZE)` if fewer than 2 prototypes.
2. Computes similarities between the embedding and all class prototypes.
3. Finds top 2 most similar prototypes.
4. Returns `Failure(THRESHOLD)` if best similarity < threshold.
5. Returns `Failure(CONFIDENCE_MARGIN)` if top-2 similarity gap < confidenceMargin.
6. Returns `Success(classId, similarity)` if criteria are met.

---

## **Usage Example**

```kotlin
val result = classify(embedding, classPrototypes, threshold = 0.5f)
when(result) {
    is ClassificationResult.Success -> println("Matched class: ${result.classId}, similarity=${result.similarity}")
    is ClassificationResult.Failure -> println("Classification failed: ${result.error}")
}
```

---

## **Extending**

To implement a custom provider:

1. Implement `IEmbeddingProvider<T>` for your data type.
2. Ensure consistent output dimension (`embeddingDim`).
3. Return L2-normalized vectors for compatibility with retrievers.
4. Few-shot classification can directly use `PrototypeEmbedding` outputs.

---