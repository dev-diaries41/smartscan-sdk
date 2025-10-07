# **Organiser**

## Overview

Provides batched, on-device classification of images into predefined categories (prototypes).
Designed to work with `ClipImageEmbedder` and few-shot class prototypes for automated organization of media.

Key features:

* Uses `BatchProcessor` for asynchronous, batched processing.
* Generates embeddings per image and classifies against a list of `PrototypeEmbedding`.
* Returns structured `OrganiserResult` indicating source and predicted destination.
* Supports progress reporting and lifecycle events via `IProcessorListener`.
* Configurable similarity `threshold` and `confidenceMargin` for classification.

---

## **Core Data Types**

### `OrganiserResult`

Represents the result of classifying a single media item.

| Property      | Type   | Description                                                            |
| ------------- | ------ | ---------------------------------------------------------------------- |
| `source`      | `Uri`  | Original URI of the media item                                         |
| `destination` | `Uri?` | URI representing the matched class, or `null` if classification failed |
| `scanId`      | `Int`  | Identifier of the current scan session                                 |

---

### `Organiser`

Batch processor that classifies images using embeddings and prototypes.

#### **Constructor**

| Parameter          | Type                                       | Description                                                                                 |
| ------------------ | ------------------------------------------ | ------------------------------------------------------------------------------------------- |
| `application`      | `Application`                              | Application context for processing                                                          |
| `embedder`         | `ClipImageEmbedder`                        | Embedder for generating image embeddings                                                    |
| `prototypeList`    | `List<PrototypeEmbedding>`                 | List of class prototypes for few-shot classification                                        |
| `scanId`           | `Int`                                      | Identifier for the current scan session                                                     |
| `threshold`        | `Float`                                    | Minimum similarity required for classification (default 0.4f)                               |
| `confidenceMargin` | `Float`                                    | Minimum difference between top-2 similarities for conclusive classification (default 0.04f) |
| `listener`         | `IProcessorListener<Uri, OrganiserResult>` | Optional listener for progress and batch events                                             |
| `options`          | `ProcessOptions`                           | Batch size and memory configuration                                                         |

---

### **Methods**

| Method                            | Description                                                                                                   |
| --------------------------------- | ------------------------------------------------------------------------------------------------------------- |
| `onProcess(context, item)`        | Generates an embedding for the given URI, classifies it against prototypes, and returns an `OrganiserResult`. |
| `onBatchComplete(context, batch)` | Delegates batch completion to the provided listener; allows client app to handle classification results.      |
| `close()`                         | Releases the embedder session to free resources.                                                              |

---

### **Behavior**

1. Loads each image URI and converts it to a bitmap.
2. Embeds the bitmap using `ClipImageEmbedder`.
3. Performs few-shot classification against `prototypeList` using `classify()`.
4. Returns `OrganiserResult` with `destination` set to the class URI if classification is successful, or `null` otherwise.
5. Processes items in batches and reports progress via `listener`.

---

### **Usage Example**

```kotlin
val organiser = Organiser(
    application = app,
    embedder = clipImageEmbedder,
    prototypeList = prototypes,
    scanId = 42,
    listener = object : IProcessorListener<Uri, OrganiserResult> {
        override suspend fun onBatchComplete(context: Context, batch: List<OrganiserResult>) {
            println("Processed batch: $batch")
        }
    }
)

val imageUris = listOf<Uri>(uri1, uri2, uri3)
val metrics = organiser.run(imageUris)
println("Processed ${metrics.totalProcessed} items in ${metrics.timeElapsed}ms")

organiser.close()
```

---

### **Extending**

To implement a custom organiser:

1. Extend `BatchProcessor<Uri, OrganiserResult>`.
2. Implement `onProcess()` to embed and classify each media item.
3. Implement `onBatchComplete()` to handle batch-level behavior or delegate to listener.
4. Use `OrganiserResult` to encapsulate source and destination.
5. Ensure resources like `ClipImageEmbedder` are released via `close()`.

---