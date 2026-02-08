package no.iktdev.mediaprocessing

import no.iktdev.eventi.models.store.TaskStatus
import no.iktdev.mediaprocessing.shared.common.event_task_contract.events.FilePrepareForWorkResultEvent

fun defaultFilePrepareForWorkResultEvent(): FilePrepareForWorkResultEvent {
    return FilePrepareForWorkResultEvent(TaskStatus.Completed, "build/test-intermediate/Test.mkv")
}