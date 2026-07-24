package no.iktdev.mediaprocessing.shared.common.event_task_contract.events

import no.iktdev.eventi.models.store.TaskStatus
import no.iktdev.files.FileHash
import no.iktdev.files.IFile
import no.iktdev.files.IFile.Companion.invoke
import no.iktdev.mediaprocessing.shared.common.dto.files.HashedFile
import no.iktdev.mediaprocessing.shared.common.event_task_contract.TaskResultEvent

class ProcesserExtractResultEvent(
    val data: ExtractResult? = null,
    status: TaskStatus,
    error: String? = null,
    logFile: String? = null,
) : TaskResultEvent(status, error, logFile) {
    data class ExtractResult(
        val language: String,
        val cachedOutputFile: String? = null,
        val extractedOutputFile: HashedFile? = null,
    ) {
        fun deconstruct(): Pair<IFile, FileHash?> {
            if (extractedOutputFile == null && cachedOutputFile == null) {
                throw IllegalStateException("Both output file and downloadedCover properties cannot be null")
            }
            return if (extractedOutputFile != null) extractedOutputFile.deconstruct() else
                IFile(cachedOutputFile!!) to null
        }
    }

    override fun newStatus(ns: TaskStatus): TaskResultEvent {
        return ProcesserExtractResultEvent(data, ns, error, logFile).from(this)
    }
}