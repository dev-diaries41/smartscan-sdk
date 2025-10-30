# **File-Based Embeddings**

## Overview

Provides a high-performance, file-backed storage and retrieval mechanism for embeddings, optimized for on-device vector search.

Key features:

* Full embedding index is memory-mapped for fast in-memory access.
* Supports batched writes to prevent OOM during save.
* Optional in-memory caching for repeated queries.
* Implements `IEmbeddingStore` and `IRetriever` for seamless integration with core embedding pipelines.
* Recommended for on-device scenarios where Room/DB performance is insufficient (30–50× faster for large indexes).

---

## **FileEmbeddingStore**

Persistent storage for embeddings in a binary file.

### **Constructor**

| Parameter         | Type      | Description                                                            |
| ----------------- | --------- | ---------------------------------------------------------------------- |
| `dir`             | `File`    | Directory to store the embedding file                                  |
| `filename`        | `String`  | File name of the binary embedding store                                |
| `embeddingLength` | `Int`     | Dimensionality of the stored embeddings                                |
| `useCache`        | `Boolean` | Whether to maintain an in-memory cache of embeddings (default: `true`) |

---

### **Properties**

| Property   | Type      | Description                                   |
| ---------- | --------- | --------------------------------------------- |
| `exists`   | `Boolean` | Checks if the file exists                     |
| `isCached` | `Boolean` | Indicates if the in-memory cache is populated |

---

### **Methods**

| Method               | Description                                                                            |
| -------------------- | -------------------------------------------------------------------------------------- |
| `getAll()`           | Loads all embeddings from the file (or cache if available). Returns `List<Embedding>`. |
| `add(newEmbeddings)` | Adds new embeddings to the store. Updates file header and appends entries.             |
| `remove(ids)`        | Removes embeddings by ID and saves updated file.                                       |
| `clear()`            | Clears in-memory cache (does not delete file).                                         |

**Behavior Notes:**

* Uses little-endian binary encoding.
* Batch writes in chunks of 1000 embeddings to prevent memory pressure.
* `embeddingLength` is validated on every write.
* Designed for fast in-memory index usage for similarity search.

---

### **Usage Example**

```kotlin
val store = FileEmbeddingStore(
    dir = context.filesDir,
    filename = "image_index.bin",
    embeddingLength = 512
)

// Add embeddings
store.add(listOf(embedding1, embedding2))

// Retrieve all embeddings
val embeddings = store.getAll()

// Remove embeddings
store.remove(listOf(embedding1.id))
```

---

## **FileEmbeddingRetriever**

Retriever for nearest-neighbor queries over a `FileEmbeddingStore`.

### **Constructor**

| Parameter | Type                 | Description                             |
| --------- | -------------------- | --------------------------------------- |
| `store`   | `FileEmbeddingStore` | The file-based embedding store to query |

---

### **Methods**

| Method                              | Description                                                                                           |
| ----------------------------------- | ----------------------------------------------------------------------------------------------------- |
| `query(embedding, topK, threshold)` | Returns the top-K most similar embeddings to the input vector that exceed the similarity `threshold`. |

**Behavior Notes:**

* Loads all embeddings from the `FileEmbeddingStore`.
* Computes cosine similarity (or L2-normalized distance) against stored embeddings.
* Returns a ranked list of `Embedding` objects or an empty list if none meet the threshold.

---

### **Usage Example**

```kotlin
val retriever = FileEmbeddingRetriever(store)
val results = retriever.query(inputEmbedding, topK = 5, threshold = 0.5f)

results.forEach { embedding ->
    println("Found embedding with id=${embedding.id}")
}
```

---

### **Extending**

To implement a custom file-based embedding solution:

1. Extend `IEmbeddingStore` to manage reading/writing from custom storage formats.
2. Ensure all embeddings are memory-mapped or loaded for efficient retrieval.
3. Implement an `IRetriever` to compute similarities and return top-K matches.
4. Consider optional caching for frequently accessed embeddings.

---
