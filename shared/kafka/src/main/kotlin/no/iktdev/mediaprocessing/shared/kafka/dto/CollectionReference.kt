package no.iktdev.mediaprocessing.shared.kafka.dto

import java.util.*

open class CollectionReference(
    @Transient open val referenceId: String = UUID.randomUUID().toString(),
) {}