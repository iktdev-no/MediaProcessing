package no.iktdev.mediaprocessing.coordinator.reader

import no.iktdev.exfl.coroutines.Coroutines
import no.iktdev.mediaprocessing.shared.SharedConfig
import no.iktdev.mediaprocessing.shared.kafka.core.KafkaEvents
import no.iktdev.mediaprocessing.shared.kafka.core.DefaultMessageListener
import no.iktdev.streamit.library.kafka.dto.Status
import org.springframework.stereotype.Service

@Service
class MediaStreamsAnalyze {
    val io = Coroutines.io()

    val listener = DefaultMessageListener(SharedConfig.kafkaTopic) { event ->
        if (event.key() == KafkaEvents.EVENT_MEDIA_READ_STREAM_PERFORMED.event) {
            if (event.value().data?.status == Status.COMPLETED) {

            }
        }
    }

}