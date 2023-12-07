package no.iktdev.mediaprocessing.coordinator.reader

import no.iktdev.exfl.coroutines.Coroutines
import no.iktdev.mediaprocessing.shared.common.SharedConfig
import no.iktdev.mediaprocessing.shared.kafka.core.KafkaEvents
import no.iktdev.mediaprocessing.shared.kafka.core.DefaultMessageListener
import no.iktdev.streamit.library.kafka.dto.Status
import org.springframework.stereotype.Service

class MediaStreamsAnalyze {
    val io = Coroutines.io()

    val listener = DefaultMessageListener(SharedConfig.kafkaTopic) { event ->
        if (event.key == KafkaEvents.EVENT_MEDIA_READ_STREAM_PERFORMED) {
            if (event.value.data?.status == Status.COMPLETED) {

            }
        }
    }

}