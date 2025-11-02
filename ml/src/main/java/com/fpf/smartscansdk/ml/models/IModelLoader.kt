package com.fpf.smartscansdk.ml.models

interface IModelLoader<T> {
    suspend fun load(): T
}