package no.iktdev.mediaprocessing.shared.common.contract.data

import com.google.gson.JsonObject
import no.iktdev.eventi.data.EventImpl
import no.iktdev.eventi.data.EventMetadata
import no.iktdev.mediaprocessing.shared.common.contract.Events

class MediaFileStreamsReadEvent(
    override val metadata: EventMetadata,
    override val data: JsonObject? = null,
    override val eventType: Events = Events.EventMediaReadStreamPerformed
) : Event()