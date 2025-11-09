package no.iktdev.mediaprocessing.shared.common.event_task_contract.events

import no.iktdev.eventi.models.Event

data class MediaReadyEvent(
    val fileUri: String
): Event() {
}