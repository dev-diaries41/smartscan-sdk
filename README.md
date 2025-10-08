# **SmartScanSdk**

## Table of Contents

* [Overview](#overview)
* [Documentation](docs/README.md)
* [Key Structure](#key-structure)
* [Installation](#installation)

  + [1. Install Core Module](#1-install-core-module)
  + [2. Install Extensions Module (Optional)](#2-install-extensions-module-optional)
* [Design Choices](#design-choices)

  + [Core and Extensions](#core-and-extensions)
  + [Constraints](#constraints)
  + [Model](#model)
  + [Embedding Storage](#embedding-storage)
    - [Benchmark Summary](#benchmark-summary) 

* [Gradle / Kotlin Setup Notes](#gradle--kotlin-setup-notes)

<a name="overview"></a>

## **Overview**

SmartScanSdk is a modular Android SDK that powers the **SmartScan app**. It provides tools for:

* Image & video processing
* On-device ML inference
* Semantic media indexing and search
* Few shot classification

The SDK is **extensible**, allowing developers to add ML models or features without bloating the core runtime.

**Long-term vision:** SmartScanSdk was designed with the goal of becoming a **C++ cross-platform SDK** for **search and classification**, capable of running both **offline on edge devices** and in **bulk cloud environments**.

> **Note:** Because of its long-term cross-platform goals, some features may be experimental (extensions). However, the SDK is generally considered stable, as it is actively used in the SmartScan app, aswell as others``.

---

## **Key Structure**

```
SmartScanSdk/
 ├─ core/                                   # Essential functionality
 │   ├─ ml/                                 # On-device ML infra + models
 │   │   ├─ embeddings/                     # Generic + CLIP embeddings
 │   │   └─ models/                         # Model base + loaders
 │   ├─ processors/                         # Batch processing + pipelines
 │   └─ utils/                              # General-purpose helpers
 │
 ├─ extensions/                             # Experimental / Optional features 
 │   ├─ embeddings/                         # File-based or custom embedding stores
 │   ├─ indexers/                           # Media indexers
 │   └─ organisers/                         # Higher-level orchestration
 │
 ├─ build.gradle  
 └─ settings.gradle  
```

**Notes:**

* `core` and `extensions` are standalone Gradle modules.
* Both are set up for **Maven publishing**.

---

## **Installation**

### **1. Install Core Module**

```gradle
implementation("com.github.dev-diaries41:smartscan-core:1.0.0")
```

### **2. Install Extensions Module (Optional)**

```gradle
implementation("com.github.dev-diaries41:smartscan-extensions:1.0.0")
```

> `extensions` depends on `core`, so including it is enough if you need both.

---

## **Design Choices**

### Core and Extensions

* **core** → minimal runtime: shared interfaces, embeddings, model execution, efficient batch/concurrent processing.
* **extensions** → implementations: indexers, retrievers, organisers, embedding stores, and other optional features.

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

The SDK only provides a file based implementation of `IEmbeddingStore`, `FileEmbeddingStore` (in extensions) because the following benchmarks below show much better performance for the loading of embeddings

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
* compileSdk = 36, targetSdk = 34, minSdk = 30
* `core` exposes `androidx.core:core-ktx` and ONNX runtime
* `extensions` depends on `core`
* Maven:

  * `groupId`: `com.github.dev-diaries41`
  * `artifactId`: `core` or `extensions`
  * `version`: configurable (`publishVersion`, default `1.0.0`)

---