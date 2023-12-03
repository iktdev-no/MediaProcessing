package no.iktdev.mediaprocessing.shared.kafka.dto.events_result

import no.iktdev.mediaprocessing.shared.kafka.dto.MessageDataWrapper
import no.iktdev.streamit.library.kafka.dto.Status

data class CoverInfoPerformed(
    override val status: Status,
    val url: String,
    val outDir: String,
    val outFileBaseName: String
)
    : MessageDataWrapper(status)