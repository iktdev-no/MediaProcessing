package no.iktdev.mediaprocessing.shared.common.event_task_contract.events

import no.iktdev.eventi.models.store.TaskStatus
import no.iktdev.files.FileHash
import no.iktdev.files.IFile
import no.iktdev.files.IFile.Companion.invoke
import no.iktdev.mediaprocessing.shared.common.dto.files.HashedFile
import no.iktdev.mediaprocessing.shared.common.event_task_contract.TaskResultEvent

class ProcesserEncodeResultEvent(
    val data: EncodeResult? = null,
    logFile: String? = null,
    status: TaskStatus,
    error: String? = null
) : TaskResultEvent(status, error, logFile) {
    data class EncodeResult(
        val cachedOutputFile: String? = null,
        val cachedSegmentFiles: List<String>? = null,
        val encodedOutputFile: HashedFile? = null
    ) {
        fun deconstruct(): Pair<IFile, FileHash?> {
            if (encodedOutputFile == null && cachedOutputFile == null) {
                throw IllegalStateException("Both output file and downloadedCover properties cannot be null")
            }
            return encodedOutputFile?.deconstruct() ?: (IFile(cachedOutputFile!!) to null)
        }
    }
}