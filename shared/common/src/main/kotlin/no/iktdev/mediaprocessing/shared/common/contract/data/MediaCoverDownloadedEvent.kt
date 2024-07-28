package no.iktdev.mediaprocessing.shared.common.contract.data

import no.iktdev.eventi.data.EventMetadata
import no.iktdev.mediaprocessing.shared.common.contract.Events

data class MediaCoverDownloadedEvent(
    override val metadata: EventMetadata,
    override val eventType: Events = Events.EventWorkDownloadCoverPerformed,
    override val data: DownloadedCover? = null
) : Event() {
}

data class DownloadedCover(
    val absoluteFilePath: String
)