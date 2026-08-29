package no.iktdev.mediaprocessing.shared.common.event_task_contract.tasks_super

import no.iktdev.eventi.models.Task
import no.iktdev.mediaprocessing.shared.common.event_task_contract.Overrides

abstract class VideoEncodeTask(): Task() {
    abstract val overrides: List<Overrides>?
}