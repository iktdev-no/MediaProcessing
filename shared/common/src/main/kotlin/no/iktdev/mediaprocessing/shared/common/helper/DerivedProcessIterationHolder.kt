package no.iktdev.mediaprocessing.shared.common.helper

import no.iktdev.mediaprocessing.shared.common.persistance.PersistentProcessDataMessage

data class DerivedProcessIterationHolder(
    val eventId: String,
    val event: PersistentProcessDataMessage,
    var iterated: Int = 0
)