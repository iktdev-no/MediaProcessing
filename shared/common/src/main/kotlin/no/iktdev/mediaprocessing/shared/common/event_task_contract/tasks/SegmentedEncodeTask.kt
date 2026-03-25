package no.iktdev.mediaprocessing.shared.common.event_task_contract.tasks

import no.iktdev.eventi.models.Task
import no.iktdev.mediaprocessing.shared.common.model.task.data.DefaultEncodeData

data class SegmentedEncodeTask(
    val data: DefaultEncodeData
): Task() {
}

