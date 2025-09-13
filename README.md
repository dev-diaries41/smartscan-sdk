# **SmartScanSdk**

## **Overview**

SmartScanSdk is a modular Android SDK that powers the **SmartScan app**. It provides tools for:

* **Image & video processing**
* **On-device ML inference**
* **Semantic media indexing and search**

The SDK is **highly extensible**, allowing developers to plug in new ML models or features without bloating the core runtime.

---

## **Key Structure**

```
SmartScanSdk/
 ├─ core/                                   # Minimal, essential functionality
 │   ├─ build.gradle                         # Core module Gradle, Maven publishing enabled
 │   ├─ ml/                                 # On-device ML infra + models
 │   │   ├─ embeddings/                     # Interfaces + generic embedding handling
 │   │   │   ├─ clip/                       # CLIP-specific preprocessing + inference
 │   │   │   │   ├─ ByteEncoder.kt
 │   │   │   │   ├─ ClipEmbedder.kt
 │   │   │   │   ├─ Constants.kt
 │   │   │   │   ├─ PreProcess.kt
 │   │   │   │   └─ Tokenizer.kt
 │   │   │   ├─ EmbeddingTypes.kt
 │   │   │   ├─ EmbeddingUtils.kt
 │   │   │   └─ FewShotClassifier.kt
 │   │   └─ models/
 │   │       ├─ ModelTypes.kt
 │   │       └─ OnnxModel.kt
 │   │
 │   ├─ processors/                         # Batch processing and pipelines
 │   │   ├─ BatchProcessor.kt
 │   │   ├─ ProcessorTypes.kt
 │   │   └─ StateFlowBatchProcessor.kt
 │   │
 │   └─ utils/                              # General-purpose helpers
 │       ├─ FileUtils.kt
 │       ├─ ImageUtils.kt                   # bitmap/frame preprocessing, scaling, caching
 │       ├─ MemoryUtils.kt
 │       └─ VideoUtils.kt         # frame extraction, video sampling
 │
 ├─ extensions/                             # Optional, pluggable features
 │   ├─ build.gradle                         # Extensions module Gradle, depends on core
 │   ├─ embeddings/                         # File-based or custom embedding stores
 │   │   └─ FileEmbeddingStore.kt
 │   └─ indexers/                           # Media indexing helpers
 │       ├─ ImageIndexer.kt
 │       └─ VideoIndexer.kt
 │
 └─ settings.gradle / root build.gradle     # Project-level config, Maven publishing setup
```

**Notes:**

* Both `core` and `extensions` are standalone Gradle modules — they can be installed separately depending on your needs.
* Both modules are setup for **Maven publishing** via `maven-publish`.

---

## **Installation**

### **1. Install Core Module**

Add the dependency in your app module:

```gradle
implementation("com.github.dev-diaries41:core:1.0.0")
```

### **2. Install Extensions Module (Optional)**

Add the dependency in your app module:

```gradle
implementation("com.github.dev-diaries41:extensions:1.0.0")
```

> `extensions` depends on `core` transitively, so adding `extensions` alone is sufficient if you need both.

---

## Benchmark Summary
### **The Room Approach**

The schema for the Room version was `id (long), date (long), embedding (float array)`. In the SearchViewModel the index was loaded as LiveData. Benchmarks for loading:

* 640 entries: 1237.5ms
* 2.45k entries: 2737.2ms

---

### **The File Approach**

The file approach saves the index as a binary file, then loads it with a memory-mapped file. This allows for much faster reads. Benchmarks for the same datasets:

* 640 entries: 32ms
* 2.45k entries: 135ms

That’s roughly 40× faster at 640 entries and 20× faster at 2.45k. More importantly, the time scales linearly with the number of entries. At 50k entries the load time should still be only 2–3 seconds, which is the same time Room takes just to load 2.45k.

<img src="./benchmarks/smartscan-load-benchmark.png" alt="smartscan-load-benchmark" style="width:300px">


## **Design Choices**

### Core and Extensions

* **core** → minimal runtime: shared interfaces, embedding + model execution, efficient batch/concurrent processing, and only the abstractions required for extension.
* **extensions** → concrete implementations: indexers, retrievers, organisers, embedding stores, and other optional ML or app-level features built on top of core.

### Constraints

* For offline on-device vector search on Android, the full index needs to be loaded in-memory due to lack of native VectorDB support for android. (could maybe make a custom VectorDB?)
* Some users of the SmartScan app have 40K+ images so fast processing and loading of index is essential for good UX of search functionality.
* A healthy balance between speed and memory/cpu usage is required to satisfy good UX and manage variable constrained CPU and Memory resources due to:
  - Androids range of devices, where low end devices can have as little as 1GB Memory and mid end devices about 4-8 GB.
  - Available memory is also constrained because of contention with other apps in use on the user device.
  - Faster speed by means of concurrency, leads to More CPU Usage, which can cause device heating and faster battery drain

For all the above reasons its important concurrency is handled dynamically and efficiently, hence the use of `BatchProcessor` shown below

```kotlin
// For BatchProcessor’s use case—long-running, batched,  asynchronous processing—the Application context should be used.
open class BatchProcessor<TInput, TOutput>(
    private val application: Application,
    private val processor: IProcessor<TInput, TOutput>? = null,
    private val options: ProcessOptions = ProcessOptions(),
) {
    companion object {
        const val TAG = "BatchProcessor"
    }

    open suspend fun run(items: List<TInput>): Metrics = withContext(Dispatchers.IO) {
        val processedCount = AtomicInteger(0)
        val startTime = System.currentTimeMillis()

        try {
            if (items.isEmpty()) {
                Log.w(TAG, "No items to process.")
                return@withContext Metrics.Success()
            }

            val memoryUtils = MemoryUtils(application, options.memory)

            for (batch in items.chunked(options.batchSize)) {
                val currentConcurrency = memoryUtils.calculateConcurrencyLevel()
                val semaphore = Semaphore(currentConcurrency)

                val deferredResults = batch.map { item ->
                    async {
                        semaphore.withPermit {
                            try {
                                val output = processor?.onProcess(application, item)
                                val current = processedCount.incrementAndGet()
                                val progress = (current * 100f) / items.size
                                onProgress(progress)
                                output
                            } catch (e: Exception) {
                                processor?.onProcessError(application, e, item)
                                null
                            }
                        }
                    }
                }

                val outputBatch = deferredResults.mapNotNull { it.await() }
                processor?.onBatchComplete(application, outputBatch)
            }

            val endTime = System.currentTimeMillis()
            val metrics = Metrics.Success(processedCount.get(), timeElapsed = endTime - startTime)

            processor?.onComplete(application, metrics)
            metrics
        }
        catch (e: CancellationException) {
            throw e
        }
        catch (e: Exception) {
            val metrics = Metrics.Failure(
                processedBeforeFailure = processedCount.get(),
                timeElapsed = System.currentTimeMillis() - startTime,
                error = e
            )
            processor?.onError(application, metrics)
            metrics
        }
    }

    open suspend fun onProgress(progress: Float){}

}
```

```kotlin
// State Flow version for UI observable progress updates 

class StateFlowBatchProcessor<TInput, TOutput>(
    application: Application,
    processor: IProcessor<TInput, TOutput>? = null,
    options: ProcessOptions = ProcessOptions()
) : BatchProcessor<TInput, TOutput>(application, processor, options) {

    private val _progress = MutableStateFlow(0f)
    val progress: StateFlow<Float> = _progress

    private val _status = MutableStateFlow(ProcessorStatus.IDLE)
    val status: StateFlow<ProcessorStatus> = _status

    override suspend fun run(items: List<TInput>): Metrics {
        _status.value = ProcessorStatus.ACTIVE

        val metrics = super.run(items)

        if (metrics is Metrics.Success) {
            _progress.value = 100f
            _status.value = ProcessorStatus.COMPLETE
        } else if (metrics is Metrics.Failure) {
            _status.value = ProcessorStatus.FAILED
        }
        return metrics
    }

    override suspend fun onProgress(progress: Float) {
        _progress.value = progress
    }

    fun resetProgress() {
        _progress.value = 0f
        _status.value = ProcessorStatus.IDLE
    }
}
```

## Model and Loaders
The architecture separates **model loading** from **inference execution**, enabling type-safe, backend-specific models while keeping the SDK core agnostic. This modular approach allows adding new loaders or backends independently, simplifying testing and portability. Overall, it ensures a clean, extensible design suitable for multi-platform support.



```kotlin
abstract class BaseModel<InputTensor> : AutoCloseable {
    protected abstract val loader: IModelLoader<*> // hidden implementation detail

    abstract suspend fun loadModel()
    abstract fun isLoaded(): Boolean // optional, may be removed
    abstract fun run(inputs: Map<String, InputTensor>): Map<String, Any>
}

```
* `FilePath` - Allows loading model from a local file path, enabling the use of downloadable models
**Important Note: The SmartScan app already uses `Resource` based loading**

```kotlin
// Loaders.kt

interface IModelLoader<T> {
    suspend fun load(): T
}

sealed interface ModelSource
data class FilePath(val path: String) : ModelSource
data class ResourceId(@RawRes val resId: Int) : ModelSource


class FileOnnxLoader(private val path: String) : IModelLoader<ByteArray> {
    override suspend fun load(): ByteArray = File(path).readBytes()
}

class ResourceOnnxLoader(private val resources: Resources, @RawRes private val resId: Int) : IModelLoader<ByteArray> {
    override suspend fun load(): ByteArray = resources.openRawResource(resId).readBytes()
}
```

---

## **Gradle / Kotlin Setup Notes**

* Both modules target **Java 17** and Kotlin JVM 17.
* Lint target SDK = 34, compile SDK = 36, min SDK = 30.
* `core` exposes essential Android KTX libraries (`androidx.core:core-ktx`) and ONNX runtime.
* `extensions` pulls in `core` transitively, so consumers only need to include `extensions` if using optional features.
* Maven artifact details:

    * `groupId`: `com.github.dev-diaries41`
    * `artifactId`: module name (`core` / `extensions`)
    * `version`: configurable via `publishVersion` or defaults to `1.0.0`

---