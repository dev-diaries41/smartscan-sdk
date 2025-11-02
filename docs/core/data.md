# Data

## Embedding Data Types

### `Embedding`

Represents a raw vector for a single item.

| Property     | Type         | Description                                                 |
| ------------ | ------------ | ----------------------------------------------------------- |
| `id`         | `Long`       | Unique identifier, e.g., corresponding to a `MediaStore` ID |
| `date`       | `Long`       | Timestamp associated with the embedding                     |
| `embeddings` | `FloatArray` | Raw vector representing the item                            |

---

### `PrototypeEmbedding`

Represents an aggregated, class-level embedding used for few-shot classification.

| Property     | Type         | Description                             |
| ------------ | ------------ | --------------------------------------- |
| `id`         | `String`     | Class identifier                        |
| `date`       | `Long`       | Timestamp associated with the embedding |
| `embeddings` | `FloatArray` | Aggregated vector for the class         |

---

### `IEmbeddingStore`

Interface for storing embeddings persistently.

| Property / Method                     | Description                                            |
| ------------------------------------- | ------------------------------------------------------ |
| `exists`                              | Returns true if the store exists                       |
| `isCached`                            | Indicates if embeddings are currently cached in memory |
| `add(newEmbeddings: List<Embedding>)` | Adds new embeddings to the store                       |
| `remove(ids: List<Long>)`             | Removes embeddings by ID                               |
| `get()`                               | Returns all stored embeddings                          |
| `clear()`                             | Clears the in-memory cache (does not delete the store) |

---

### `IRetriever`

Interface for querying embeddings.

| Method                                                      | Description                                                                      |
| ----------------------------------------------------------- | -------------------------------------------------------------------------------- |
| `query(embedding: FloatArray, topK: Int, threshold: Float)` | Returns the top-K embeddings most similar to the input that exceed the threshold |

---

### `IEmbeddingProvider<T>`

Generic interface for generating embeddings from data.

| Property / Method           | Description                               |
| --------------------------- | ----------------------------------------- |
| `embeddingDim`              | Dimensionality of the embeddings produced |
| `closeSession()`            | cleanup method                   |
| `embed(data: T)`            | Produces an embedding for a single item   |
| `embedBatch(data: List<T>)` | Produces embeddings for a batch of items  |

**Type Aliases:**

* `TextEmbeddingProvider = IEmbeddingProvider<String>`
* `ImageEmbeddingProvider = IEmbeddingProvider<Bitmap>`

---

## Classification Data Types

### `ClassificationResult`

Represents the result of a classification operation.

| Type      | Properties                               | Description                                                                            |
| --------- | ---------------------------------------- | -------------------------------------------------------------------------------------- |
| `Success` | `classId: String`<br>`similarity: Float` | Indicates successful classification, with the predicted class ID and similarity score. |
| `Failure` | `error: ClassificationError`             | Indicates classification failure, with the reason provided by `ClassificationError`.   |

---

### `ClassificationError`

Enumerates possible reasons for classification failure.

| Value                | Description                                         |
| -------------------- | --------------------------------------------------- |
| `MINIMUM_CLASS_SIZE` | Not enough examples to classify reliably.           |
| `THRESHOLD`          | Similarity below the defined threshold.             |
| `CONFIDENCE_MARGIN`  | Insufficient confidence margin between top classes. |

---

## Processor Data Types

### `Metrics`

Encapsulates processing results.

| Type      | Properties                                                                 | Description                                                        |
|-----------|----------------------------------------------------------------------------|--------------------------------------------------------------------|
| `Success` | `totalProcessed: Int`<br>`timeElapsed: Long`                               | Number of items processed and total duration                       |
| `Failure` | `processedBeforeFailure: Int`<br>`timeElapsed: Long`<br>`error: Exception` | Number of items processed before failure, duration, and error info |

---

### `MemoryOptions`

Configuration for memory-aware processing.

| Property              | Type | Description                               |
|-----------------------|------|-------------------------------------------|
| `lowMemoryThreshold`  | Long | Memory level (in bytes) considered "low"  |
| `highMemoryThreshold` | Long | Memory level (in bytes) considered "high" |
| `minConcurrency`      | Int  | Minimum concurrency allowed               |
| `maxConcurrency`      | Int  | Maximum concurrency allowed               |

**Usage:** Controls the number of items processed concurrently based on available system memory. Works with `MemoryUtils` to dynamically calculate optimal concurrency.

---

### `MemoryUtils`

Utility for memory-aware operations.

**Key Methods:**

* `getFreeMemory()`: Returns the current free memory in bytes.
* `calculateConcurrencyLevel()`: Returns an optimal concurrency level based on available memory and `MemoryOptions`.

---

### `ProcessOptions`

Configuration for processor execution.

| Property    | Type            | Description                          |
|-------------|-----------------|--------------------------------------|
| `memory`    | `MemoryOptions` | Memory constraints for processing    |
| `batchSize` | `Int`           | Number of items to process per batch |

---