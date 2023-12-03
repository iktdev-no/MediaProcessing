package no.iktdev.mediaprocessing.shared.kafka.core

import no.iktdev.mediaprocessing.shared.kafka.core.KafkaEvents
import java.lang.annotation.ElementType

@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.CLASS)
annotation class KafkaBelongsToEvent(vararg val event: KafkaEvents)
