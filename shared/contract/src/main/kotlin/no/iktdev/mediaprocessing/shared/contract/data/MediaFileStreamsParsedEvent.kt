package no.iktdev.mediaprocessing.shared.contract.data

import no.iktdev.eventi.data.EventMetadata
import no.iktdev.mediaprocessing.shared.contract.Events
import no.iktdev.mediaprocessing.shared.contract.ffmpeg.ParsedMediaStreams

class MediaFileStreamsParsedEvent(
    override val metadata: EventMetadata,
    override val data: ParsedMediaStreams? = null,
    override val eventType: Events = Events.EventMediaParseStreamPerformed

) : Event()