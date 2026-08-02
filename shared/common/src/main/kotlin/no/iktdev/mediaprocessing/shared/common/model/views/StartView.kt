package no.iktdev.mediaprocessing.shared.common.model.views

import no.iktdev.files.IFile
import no.iktdev.mediaprocessing.shared.common.event_task_contract.events.OperationType
import no.iktdev.mediaprocessing.shared.common.event_task_contract.events.StartFlow

data class StartView(
    val inputFile: IFile,
    val mode: StartFlow,
    val tasks: Set<OperationType>
    )