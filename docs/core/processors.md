# **Processors**

## Overview

Provides a framework for asynchronous, batched processing of arbitrary data in the SDK.
Supports:

* Configurable batch sizes and memory constraints
* Progress reporting and lifecycle callbacks
* Metrics for success and failure
* Concurrency-aware execution for long-running operations

This module is ideal for tasks like embedding generation, model inference, or any other repeated computation on large datasets.

---

## **Core Data Types**

### `ProcessorStatus`

Represents the current state of a processor.

| Status     | Description                         |
| ---------- | ----------------------------------- |
| `IDLE`     | Processor is not active             |
| `ACTIVE`   | Processor is currently running      |
| `COMPLETE` | Processor has finished successfully |
| `FAILED`   | Processor has encountered an error  |

---

### `Metrics`

Encapsulates processing results.

| Type      | Properties                                                                 | Description                                                        |
| --------- | -------------------------------------------------------------------------- | ------------------------------------------------------------------ |
| `Success` | `totalProcessed: Int`<br>`timeElapsed: Long`                               | Number of items processed and total duration                       |
| `Failure` | `processedBeforeFailure: Int`<br>`timeElapsed: Long`<br>`error: Exception` | Number of items processed before failure, duration, and error info |

---

### `ProcessOptions`

Configuration for processor execution.

| Property    | Type            | Description                          |
| ----------- | --------------- | ------------------------------------ |
| `memory`    | `MemoryOptions` | Memory constraints for processing    |
| `batchSize` | `Int`           | Number of items to process per batch |

---

### `IProcessorListener<Input, Output>`

Interface for receiving processor lifecycle callbacks.

| Method                            | Description                                      |
| --------------------------------- | ------------------------------------------------ |
| `onActive(context)`               | Called when processing starts                    |
| `onBatchComplete(context, batch)` | Called after each batch is processed             |
| `onComplete(context, metrics)`    | Called when all items are processed successfully |
| `onProgress(context, progress)`   | Called to report progress (0–1)                  |
| `onError(context, error, item)`   | Called for individual item errors                |
| `onFail(context, failureMetrics)` | Called when processing fails                     |

**Notes:**

* All methods are suspendable except `onError`.
* Subclasses may override only the callbacks they need.

---

## **BatchProcessor**

Abstract base class for batched, asynchronous processing of items.

### **Properties**

| Property      | Type                                 | Description                             |
| ------------- | ------------------------------------ | --------------------------------------- |
| `application` | `Application`                        | Provides context for processing tasks   |
| `listener`    | `IProcessorListener<Input, Output>?` | Optional callback listener              |
| `options`     | `ProcessOptions`                     | Configures batch size and memory limits |

---

### **Methods**

| Method                            | Description                                                                                                                                                    |
| --------------------------------- | -------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `run(items: List<Input>)`         | Executes processing over the list of items in batches. Returns `Metrics` indicating success or failure. Handles concurrency, batching, and listener callbacks. |
| `onProcess(context, item)`        | Abstract. Implement logic to process a single item and produce an output.                                                                                      |
| `onBatchComplete(context, batch)` | Abstract. Implement batch-level behavior, e.g., aggregation or storage of batch results. Can delegate to listener.                                             |

---

### **Behavior**

1. Splits items into batches of `options.batchSize`.
2. Calculates optimal concurrency per batch based on `MemoryOptions`.
3. Runs each item asynchronously within a concurrency-controlled semaphore.
4. Tracks progress and updates listener.
5. Returns `Metrics.Success` if all items succeed.
6. Returns `Metrics.Failure` if an exception occurs.

---

### **Usage Example**

```kotlin
class MyProcessor(application: Application) : BatchProcessor<String, Int>(application) {
    override suspend fun onProcess(context: Context, item: String): Int {
        // Convert string to its length
        return item.length
    }

    override suspend fun onBatchComplete(context: Context, batch: List<Int>) {
        println("Processed batch: $batch")
    }
}

val processor = MyProcessor(app)
val items = listOf("apple", "banana", "cherry")
val metrics = processor.run(items)
println("Processed ${metrics.totalProcessed} items in ${metrics.timeElapsed}ms")
```

---

### **Extending**

To implement a custom processor:

1. Extend `BatchProcessor<Input, Output>`.
2. Implement `onProcess` to handle individual items.
3. Implement `onBatchComplete` to handle batch-level results.
4. Optionally provide an `IProcessorListener` to observe lifecycle events.
5. Configure `ProcessOptions` for batching and memory limits.

---
``