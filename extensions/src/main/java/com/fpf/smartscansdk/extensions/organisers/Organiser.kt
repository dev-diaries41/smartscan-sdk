package com.fpf.smartscansdk.extensions.organisers

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.core.net.toUri
import com.fpf.smartscansdk.core.ml.embeddings.ClassificationResult
import com.fpf.smartscansdk.core.ml.embeddings.PrototypeEmbedding
import com.fpf.smartscansdk.core.ml.embeddings.classify
import com.fpf.smartscansdk.core.ml.embeddings.clip.ClipImageEmbedder
import com.fpf.smartscansdk.core.ml.embeddings.clip.ClipConfig
import com.fpf.smartscansdk.core.processors.BatchProcessor
import com.fpf.smartscansdk.core.processors.IProcessorListener
import com.fpf.smartscansdk.core.processors.ProcessOptions
import com.fpf.smartscansdk.core.utils.getBitmapFromUri

class Organiser(
    private val application: Application,
    private val embedder: ClipImageEmbedder,
    private val prototypeList: List<PrototypeEmbedding>,
    private val scanId: Int,
    private val threshold: Float = 0.4f,
    private val confidenceMargin: Float = 0.04f,
    listener: IProcessorListener<Uri, OrganiserResult>,
    options: ProcessOptions = ProcessOptions(),
    ): BatchProcessor<Uri, OrganiserResult>(application, listener, options) {

    companion object {
        private const val TAG = "Organiser"
        const val PREF_KEY_LAST_USED_CLASSIFICATION_DIRS = "last_used_destinations"
    }

    fun close() {
        embedder.closeSession()
    }

    // Delegate to listener (client app) to give control of how classified files are managed
    override suspend fun onBatchComplete(context: Context, batch: List<OrganiserResult>) {
        listener?.onBatchComplete(context, batch)
    }

    override suspend fun onProcess(context: Context, item: Uri): OrganiserResult {
        val bitmap = getBitmapFromUri(application, item, ClipConfig.IMAGE_SIZE_X)
        val embedding = embedder.embed(bitmap)
        val result =  classify(embedding, prototypeList, threshold=threshold, confidenceMargin=confidenceMargin)
        return when(result){
            is ClassificationResult.Success -> {
                OrganiserResult( source = item, destination = result.classId.toUri(), scanId=scanId)
            }
            is ClassificationResult.Failure -> {
                OrganiserResult( source = item, destination = null, scanId=scanId)
            }

        }
    }
}

data class OrganiserResult(
    val destination: Uri?,
    val source: Uri,
    val scanId: Int,
    )
