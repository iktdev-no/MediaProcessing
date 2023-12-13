package no.iktdev.mediaprocessing.coordinator.reader

import no.iktdev.exfl.coroutines.Coroutines


class MediaStreamsAnalyze {
    val io = Coroutines.io()
/*
    val listener = DefaultMessageListener(SharedConfig.kafkaTopic) { event ->
        if (event.key == KafkaEvents.EVENT_MEDIA_READ_STREAM_PERFORMED) {
            if (event.value.data?.status == Status.COMPLETED) {

            }
        }
    }*/

}