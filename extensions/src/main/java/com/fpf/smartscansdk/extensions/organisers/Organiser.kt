package com.fpf.smartscansdk.extensions.organisers

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.core.net.toUri
import com.fpf.smartscansdk.core.ml.embeddings.PrototypeEmbedding
import com.fpf.smartscansdk.core.ml.embeddings.classify
import com.fpf.smartscansdk.core.ml.embeddings.clip.ClipImageEmbedder
import com.fpf.smartscansdk.core.ml.embeddings.clip.ClipConfig
import com.fpf.smartscansdk.core.processors.BatchProcessor
import com.fpf.smartscansdk.core.processors.IProcessorListener
import com.fpf.smartscansdk.core.processors.ProcessOptions
import com.fpf.smartscansdk.core.utils.getBitmapFromUri

// In onBatchComplete move files
// In onComplete save lastUsedPrototysClasses
// Pass scanId on creation
class Organiser(
    private val application: Application,
    private val embedder: ClipImageEmbedder,
    private val prototypeList: List<PrototypeEmbedding>,
    private val scanId: Long,
    listener: IProcessorListener<Uri, OrganiserResult>,
    options: ProcessOptions = ProcessOptions(),
    ): BatchProcessor<Uri, OrganiserResult>(application, listener, options) {
    companion object {
        private const val TAG = "Organiser"
        private const val PREF_KEY_LAST_USED_CLASSIFICATION_DIRS = "last_used_destinations"
    }

    fun close() {
        embedder.closeSession()
    }

    // Delegate to listener (client app) to give control of how classified files are managed
    override suspend fun onBatchComplete(context: Context, batch: List<OrganiserResult>) {
        listener?.onBatchComplete(context, batch)
    }

    // classId (destinationUriStr) and item(source uri) both required to move files in batches
    override suspend fun onProcess(context: Context, item: Uri): OrganiserResult {
        val bitmap = getBitmapFromUri(application, item, ClipConfig.IMAGE_SIZE_X)
        val embedding = embedder.embed(bitmap)
        val classId =  classify(embedding, prototypeList)
        return OrganiserResult( source = item, destination = classId?.toUri(), scanId=scanId)
    }
}

data class OrganiserResult(
    val destination: Uri?,
    val source: Uri,
    val scanId: Long,
    )
