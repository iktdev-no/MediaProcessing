package no.iktdev.mediaprocessing.shared.contract.data

import no.iktdev.eventi.data.EventImpl
import no.iktdev.eventi.data.EventMetadata
import no.iktdev.mediaprocessing.shared.contract.Events

data class BaseInfoEvent(
    override val metadata: EventMetadata,
    override val eventType: Events = Events.EventMediaReadBaseInfoPerformed,
    override val data: BaseInfo? = null
) : Event()

data class BaseInfo(
    val title: String,
    val sanitizedName: String,
    val searchTitles: List<String> = emptyList<String>(),
)