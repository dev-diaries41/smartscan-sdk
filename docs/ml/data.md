# Core Data Types

## `IModelLoader<T>`

Interface for loading model data.

| Method   | Type      | Description                    |
| -------- | --------- | ------------------------------ |
| `load()` | `suspend` | Returns model data of type `T` |

---

## `ModelSource`

Represents the source of a model.

| Type         | Description                     |
| ------------ | ------------------------------- |
| `FilePath`   | Model located at a file path    |
| `ResourceId` | Model bundled as a raw resource |

---

## `TensorData`

Sealed interface representing typed tensor data compatible with ONNX runtime.

| Type                 | Description                                     |
| -------------------- | ----------------------------------------------- |
| `FloatBufferTensor`  | FloatBuffer-based tensor                        |
| `IntBufferTensor`    | IntBuffer-based tensor                          |
| `LongBufferTensor`   | LongBuffer-based tensor                         |
| `DoubleBufferTensor` | DoubleBuffer-based tensor                       |
| `ShortBufferTensor`  | ShortBuffer-based tensor with optional type     |
| `ByteBufferTensor`   | ByteBuffer-based tensor with explicit ONNX type |

All tensor types store their `shape` as a `LongArray`.

---
