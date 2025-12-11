package no.iktdev.mediaprocessing.shared.common.event_task_contract.events

import no.iktdev.eventi.models.Event


data class MediaStreamReadTaskCreatedEvent(
    val fileUri: String
): Event() {
}