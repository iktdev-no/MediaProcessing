package no.iktdev.mediaprocessing.shared.common.event_task_contract.tasks

import no.iktdev.eventi.models.Task
import no.iktdev.mediaprocessing.shared.common.model.task.data.LinearEncodeData
import no.iktdev.mediaprocessing.shared.common.model.task.data.SegmentEncodeData

data class SegmentedEncodeTask(
    val data: SegmentEncodeData
): Task() {
}

