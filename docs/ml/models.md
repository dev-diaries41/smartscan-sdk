# **Models**

## Overview

Provides a unified framework for managing machine learning models in the SDK.
Supports on-device ONNX models with flexible input/output handling, type-safe tensor representations, and resource management.

Key features:

* Abstract base model interface with lifecycle management (`load`, `run`, `close`)
* Pluggable model loaders (`FilePath`, `ResourceId`)
* Typed tensor representations for ONNX runtime compatibility
* Lightweight ONNX model wrapper with session and environment management

---

## `BaseModel<InputTensor>`

Abstract base class for ML models in the SDK.

| Member        | Type                            | Description                               |
| ------------- | ------------------------------- | ----------------------------------------- |
| `loader`      | `IModelLoader<*>`               | Abstract loader that provides model bytes |
| `loadModel()` | `suspend fun`                   | Loads the model into memory               |
| `isLoaded()`  | `fun`: `Boolean`                | Checks if the model has been loaded       |
| `run(inputs)` | `fun(Map<String, InputTensor>)` | Runs inference with input tensors         |
| `close()`     | `fun` (from `AutoCloseable`)    | Releases model resources and memory       |

**Notes:**

* `InputTensor` is generic to allow different tensor types (e.g., `TensorData`).
* `BaseModel` enforces resource cleanup via `AutoCloseable`.

---

### Model Loaders

| Class                | Description                                   |
| -------------------- | --------------------------------------------- |
| `FileOnnxLoader`     | Loads model bytes from a file path            |
| `ResourceOnnxLoader` | Loads model bytes from a raw Android resource |

**Example:**

```kotlin
val loader = FileOnnxLoader("/models/my_model.onnx")
val bytes = loader.load()
```

---

## **ONNX Model Wrapper**

### `OnnxModel`

Concrete `BaseModel<TensorData>` implementation using **ONNX Runtime**.

**Key responsibilities:**

* Load ONNX model bytes into an `OrtSession`
* Run inference on typed tensors (`TensorData`)
* Handle automatic creation and disposal of `OnnxTensor` objects
* Provide access to input names and ONNX environment

---

### **Properties**

| Property  | Type                      | Description              |
| --------- | ------------------------- | ------------------------ |
| `loader`  | `IModelLoader<ByteArray>` | Source of model bytes    |
| `env`     | `OrtEnvironment`          | ONNX runtime environment |
| `session` | `OrtSession?`             | Active model session     |

---

### **Methods**

| Method            | Description                                             |
| ----------------- | ------------------------------------------------------- |
| `loadModel()`     | Loads model bytes and creates an ONNX session           |
| `isLoaded()`      | Returns `true` if session is initialized                |
| `run(inputs)`     | Runs inference, returns a map of output names to values |
| `getInputNames()` | Returns a list of input names expected by the model     |
| `getEnv()`        | Returns the `OrtEnvironment` used                       |
| `close()`         | Closes the session and frees resources                  |

**Private helper:**

* `createOnnxTensor(tensorData: TensorData)` – Converts a `TensorData` object into an ONNX runtime tensor.

---

### **Usage Example**

```kotlin
val loader = FileOnnxLoader("/models/clip_image.onnx")
val model = OnnxModel(loader)

model.loadModel()
if(model.isLoaded()) {
    val inputs = mapOf(
        "input" to TensorData.FloatBufferTensor(floatBuffer, longArrayOf(1,3,224,224))
    )
    val outputs = model.run(inputs)
    println(outputs)
}

model.close()
```

---

## **Extending**

To implement a custom model:

1. Extend `BaseModel<InputTensor>` with your tensor type.
2. Provide a loader implementing `IModelLoader<*>`.
3. Override `run()` to handle inference logic.
4. Ensure proper resource cleanup in `close()`.

---