package no.iktdev.mediaprocessing.shared.common.event_task_contract.tasks

import no.iktdev.eventi.models.Task

// Task for reading and parsing media files
class MediaReadTask(val fileUri: String): Task() {
}

