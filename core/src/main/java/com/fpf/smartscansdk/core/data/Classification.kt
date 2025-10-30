package com.fpf.smartscansdk.core.data

sealed class ClassificationResult {
    data class Success(val classId: String, val similarity: Float ): ClassificationResult()
    data class Failure(val error: ClassificationError ): ClassificationResult()
}

enum class ClassificationError{MINIMUM_CLASS_SIZE, THRESHOLD, CONFIDENCE_MARGIN}

