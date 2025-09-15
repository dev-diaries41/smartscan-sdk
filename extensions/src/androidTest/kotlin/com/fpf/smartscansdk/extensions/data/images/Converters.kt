package com.fpf.smartscansdk.extensions.data.images

import androidx.room.*

class Converters {
    @TypeConverter
    fun fromFloatArray(value: FloatArray): String {
        return value.joinToString(separator = ",")
    }

    @TypeConverter
    fun toFloatArray(value: String): FloatArray {
        return if (value.isEmpty()) floatArrayOf() else value.split(",").map { it.toFloat() }.toFloatArray()
    }
}

