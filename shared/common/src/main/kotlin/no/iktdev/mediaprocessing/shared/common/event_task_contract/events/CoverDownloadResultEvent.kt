package no.iktdev.mediaprocessing.shared.common.event_task_contract.events

import no.iktdev.eventi.models.store.TaskStatus
import no.iktdev.files.FileHash
import no.iktdev.files.IFile
import no.iktdev.mediaprocessing.shared.common.dto.files.HashedFile
import no.iktdev.mediaprocessing.shared.common.event_task_contract.TaskResultEvent

class CoverDownloadResultEvent(
    val data: CoverDownloadedData? = null,
    status: TaskStatus,
    error: String? = null
) : TaskResultEvent(status, error) {
    data class CoverDownloadedData(
        val source: String,
        val outputFile: String? = null,
        val downloadedCover: HashedFile? = null
    ) {
        fun deconstruct(): Pair<IFile, FileHash?> {
            if (downloadedCover == null && outputFile == null) {
                throw IllegalStateException("Both output file and downloadedCover properties cannot be null")
            }
            return if (downloadedCover != null) downloadedCover.deconstruct() else
                IFile(outputFile!!) to null
        }
    }

    override fun newStatus(ns: TaskStatus): TaskResultEvent {
        return CoverDownloadResultEvent(data, ns, error).from(this)
    }

}

