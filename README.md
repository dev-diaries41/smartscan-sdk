# **SmartScanSdk**

## Table of Contents

* [Overview](#overview)
* [Documentation](docs/README.md)
* [Key Structure](#key-structure)
* [Installation](#installation)

  + [1. Install Core Module](#1-install-core-module)
  + [2. Install ML Module (Optional)](#2-install-ml-module-optional)
* [Design Choices](#design-choices)

  + [Core and ML](#core-and-ml)
  + [Constraints](#constraints)
  + [Model](#model)
  + [Embedding Storage](#embedding-storage)
    - [Benchmark Summary](#benchmark-summary) 

* [Gradle / Kotlin Setup Notes](#gradle--kotlin-setup-notes)


## **Overview**

SmartScanSdk is a modular Android SDK that powers the **SmartScan app**. It provides tools for:

* Image & video processing
* On-device ML inference
* Semantic media indexing and search
* Few shot classification
* Efficient batch processing


> **Note:** The SDK is designed to be flexible, but its primary use is for the SmartScan app and other apps I am developing. It is also subject to rapid experimental changes.

---

## **Key Structure**

```
SmartScanSdk/
 ├─ core/                                   # Essential functionality
 │   ├─ data/                               # Data classes and processor interfaces
 │   ├─ embeddings/                         # Embedding utilities and file-based stores
 │   ├─ indexers/                           # Image and video indexers
 │   ├─ media/                              # Media helpers (image/video utils)
 │   └─ processors/                         # Batch processing and memory helpers
 │
 └─ ml/                                     # On-device ML infrastructure + models
     ├─ data/                               # Model loaders and data classes
     └─ models/                             # Base ML models and providers
         └─ providers/
             └─ embeddings/                 # Embedding providers
                 ├─ clip/                   # CLIP image & text embedder
                 └─ FewShotClassifier.kt    # Few-shot classifier

├─ build.gradle  
└─ settings.gradle  
```

**Notes:**

* `core` and `ml` are standalone Gradle modules.
* Both are set up for **Maven publishing**.
* The structure replaces the old `core` and `extensions` module in versions ≤1.0.4

 ---

## **Installation**

### **1. Install Core Module**

```gradle
implementation("com.github.dev-diaries41.smartscan-sdk:smartscan-core:1.1.0")
```

### **2. Install ML Module (Optional)**

```gradle
implementation("com.github.dev-diaries41.smartscan-sdk:smartscan-ml:1.1.0")
```

> `ml` depends on `core`, so including it is enough if you need both.

---

## **Design Choices**

### Core and ML

* **core** → minimal runtime: shared interfaces, data classes, embeddings, media helpers, processor execution, and efficient batch/concurrent processing.
* **ml** → ML infrastructure and models: model loaders, base models, embedding providers (e.g., CLIP), and few-shot classifiers. Optional or experimental ML-related features can be added under `ml/providers`.

This structure replaces the old `core` and `extensions` modules from versions 1.0.4 and below. It provides more clarity and allows consumers to use core non-ML functionality independently. For the most part, the code itself remains unchanged; only the file organization has been updated. Documentation will be updated shortly.

---

### Constraints

* Full index must be loaded in-memory on Android (no native vector DB support).
* Some users have 40K+ images, so fast processing and loading are critical.
* Balance speed and memory/CPU use across devices (1GB–8GB memory range).
* Concurrency improves speed but increases CPU usage, heat, and battery drain.

To mitigate the constraints described above, all bulk ml relate processing is done using dynamic, concurrent batch processing via the use of `BatchProcessor`, which uses available memory to self-adjust concurrency between batches

### Model

Supports models stored locally or bundled in the app.

---

### Embedding Storage

The SDK only provides a file based implementation of `IEmbeddingStore`, `FileEmbeddingStore` (in core) because the following benchmarks below show much better performance for the loading of embeddings

#### **Benchmark Summary**

**Real-Life Test Results**

| Embeddings | Room Time (ms) | File Time (ms) |
|------------|----------------|----------------|
| 640        | 1,237.5        | 32.0           |
| 2,450      | 2,737.2        | 135.0          |


**Instrumented Test Benchmarks**

| Embeddings | Room Time (ms) | File Time (ms) |
|------------|----------------|----------------|
| 2,500      | 5,337.50       | 72.05          |
| 5,000      | 8,095.87       | 126.63         |
| 10,000     | 16,420.67      | 236.51         |
| 20,000     | 36,622.81      | 605.51         |
| 40,000     | 89,363.28      | 939.50         |

![SmartScan Load Benchmark](./benchmarks/smartscan-load-benchmark.png)

File-based memory-mapped loading is significantly faster and scales better.

___

## **Gradle / Kotlin Setup Notes**

* Java 17 / Kotlin JVM 17
* `compileSdk = 36`, `targetSdk = 34`, `minSdk = 30`
* `core` exposes `androidx.core:core-ktx`
* `ml` depends on `core` and ONNX Runtime
* Maven publishing:

  * `groupId`: `com.github.dev-diaries41`
  * `artifactId`: `core` or `ml`
  * `version`: configurable (`publishVersion`, default `1.0.0`)
  
 ---