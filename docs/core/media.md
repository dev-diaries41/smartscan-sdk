# Media

A collection of image and video processing utilities used across the SDK.

---

## ImageUtils

Provides helper functions for image scaling, cropping, and loading from URIs.

### `centerCrop(bitmap: Bitmap, imageSize: Int): Bitmap`

Crops the input `bitmap` to a square based on the smaller dimension, centers it, and scales it to `imageSize` × `imageSize`.

**Parameters:**

| Name        | Type   | Description                   |
|-------------|--------|-------------------------------|
| `bitmap`    | Bitmap | Source bitmap                 |
| `imageSize` | Int    | Target width and height in px |

**Returns:** Cropped and scaled `Bitmap`.

---

### `getScaledDimensions(width: Int, height: Int, maxSize: Int = 1024): Pair<Int, Int>`

Calculates scaled dimensions for a bitmap, preserving aspect ratio, such that the largest side does not exceed `maxSize`.

**Parameters:**

| Name      | Type | Description                                 |
|-----------|------|---------------------------------------------|
| `width`   | Int  | Original width                              |
| `height`  | Int  | Original height                             |
| `maxSize` | Int  | Maximum allowed width/height (default 1024) |

**Returns:** Pair of scaled `(width, height)`.

---

### `getBitmapFromUri(context: Context, uri: Uri, maxSize: Int): Bitmap`

Loads a bitmap from a content `Uri`, automatically scaling it so the largest side does not exceed `maxSize`.

**Parameters:**

| Name      | Type    | Description             |
|-----------|---------|-------------------------|
| `context` | Context | Android context         |
| `uri`     | Uri     | Source image URI        |
| `maxSize` | Int     | Maximum dimension in px |

**Returns:** Decoded and scaled `Bitmap` with `ARGB_8888` configuration.

---

## VideoUtils

Provides helper functions for extracting frames from video files.

### `extractFramesFromVideo(context: Context, videoUri: Uri, width: Int, height: Int, frameCount: Int = 10): List<Bitmap>?`

Extracts up to `frameCount` evenly spaced frames from a video at the specified resolution.

**Parameters:**

| Name         | Type    | Description                              |
|--------------|---------|------------------------------------------|
| `context`    | Context | Android context                          |
| `videoUri`   | Uri     | Video URI                                |
| `width`      | Int     | Target frame width                       |
| `height`     | Int     | Target frame height                      |
| `frameCount` | Int     | Number of frames to extract (default 10) |

**Returns:** List of `Bitmap` frames, or `null` if extraction fails.

**Behavior:**

* Extracts frames using `MediaMetadataRetriever` with `OPTION_CLOSEST_SYNC`.
* Stops early if a frame cannot be decoded (common codec issues).
* Executes in a coroutine on `Dispatchers.IO`.

---
