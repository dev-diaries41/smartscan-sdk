package com.fpf.smartscansdk.ml.data

import androidx.annotation.RawRes

sealed interface ModelSource
data class FilePath(val path: String) : ModelSource
data class ResourceId(@RawRes val resId: Int) : ModelSource
