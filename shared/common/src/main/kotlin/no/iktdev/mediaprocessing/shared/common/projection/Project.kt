package no.iktdev.mediaprocessing.shared.common.projection

import no.iktdev.eventi.models.Event
import no.iktdev.mediaprocessing.shared.common.event_task_contract.events.OperationType
import no.iktdev.mediaprocessing.shared.common.event_task_contract.events.ProcessFlow
import java.io.File

class Project(val events: List<Event>) {
    lateinit var startedWith: StartProjection
        private set
    var metadataTaskStatus: TaskStatus = TaskStatus.NotInitiated
        private set
    var encodeTaskStatus: TaskStatus = TaskStatus.NotInitiated
        private set
    var extreactTaskStatus: TaskStatus = TaskStatus.NotInitiated
        private set
    var convertTaskStatus: TaskStatus = TaskStatus.NotInitiated
        private set
    var coverDownloadTaskStatus: TaskStatus = TaskStatus.NotInitiated
        private set


    init {

    }



    data class StartProjection(
        val inputFile: String,
        val mode: ProcessFlow,
        val tasks: Set<OperationType>
    )




    enum class TaskStatus {
        NotInitiated,
        NotAvailable,
        Pending,
        Skipped,
        Completed,
        Failed
    }

}