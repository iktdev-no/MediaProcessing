package no.iktdev.mediaprocessing.shared.common.event_task_contract.events

import com.google.gson.JsonObject
import no.iktdev.eventi.models.Event

data class MediaStreamReadEvent(
    val data: JsonObject
): Event() {
}