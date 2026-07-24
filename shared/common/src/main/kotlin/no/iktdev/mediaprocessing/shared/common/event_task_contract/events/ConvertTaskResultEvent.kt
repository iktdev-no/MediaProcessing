package no.iktdev.mediaprocessing.shared.common.event_task_contract.events

import no.iktdev.eventi.models.store.TaskStatus
import no.iktdev.files.FileHash
import no.iktdev.files.IFile
import no.iktdev.mediaprocessing.shared.common.dto.files.HashedFile
import no.iktdev.mediaprocessing.shared.common.event_task_contract.TaskResultEvent

class ConvertTaskResultEvent(
    val data: ConvertedData?,
    status: TaskStatus,
    error: String? = null,
): TaskResultEvent(status, error) {
    override fun newStatus(ns: TaskStatus): TaskResultEvent {
        return ConvertTaskResultEvent(data, ns, error).from(this)
    }

    data class ConvertedData(
        val language: String,
        val baseName: String,
        val outputFiles: List<String>? = null,
        val convertedFiles: List<HashedFile>? = null
    ) {
        fun deconstruct(): List<Pair<IFile, FileHash?>> {
            if (convertedFiles == null && outputFiles == null) {
                throw IllegalStateException("Both output file and downloadedCover properties cannot be null")
            }
            return convertedFiles?.map { it.deconstruct() } ?: outputFiles!!.map { IFile(it) to null }
        }
    }
}

