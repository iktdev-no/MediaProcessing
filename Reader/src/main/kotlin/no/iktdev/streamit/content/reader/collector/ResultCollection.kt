package no.iktdev.streamit.content.reader.collector

import no.iktdev.streamit.content.common.deserializers.*
import no.iktdev.streamit.content.common.dto.ContentOutName
import no.iktdev.streamit.content.common.dto.Metadata
import no.iktdev.streamit.content.common.dto.reader.EpisodeInfo
import no.iktdev.streamit.content.common.dto.reader.FileResult
import no.iktdev.streamit.content.common.dto.reader.MovieInfo
import no.iktdev.streamit.content.common.dto.reader.work.EncodeWork
import no.iktdev.streamit.library.kafka.KafkaEvents
import no.iktdev.streamit.library.kafka.dto.Message
import no.iktdev.streamit.library.kafka.listener.collector.DefaultEventCollection
import no.iktdev.streamit.library.kafka.listener.deserializer.deserializeIfSuccessful
import org.apache.kafka.clients.consumer.ConsumerRecord

class ResultCollection: DefaultEventCollection() {

    fun getFirstOrNull(events: KafkaEvents): ConsumerRecord<String, Message>? {
        return getRecords().firstOrNull { it.key() == events.event }
    }

    fun getReferenceId(): String? {
        return getRecords().firstOrNull()?.value()?.referenceId
    }

    /**
     * @see KafkaEvents.EVENT_READER_RECEIVED_FILE
     * @see FileResult for data structure
     */
    fun getFileResult(): FileResult? {
        val record = getRecords().firstOrNull { it.key() == KafkaEvents.EVENT_READER_RECEIVED_FILE.event } ?: return null
        return FileResultDeserializer().deserializeIfSuccessful(record.value())
    }

    /**
     * @see KafkaEvents.EVENT_READER_DETERMINED_FILENAME
     * @see ContentOutName for data structure
     */
    fun getFileName(): ContentOutName? {
        val record = getFirstOrNull(KafkaEvents.EVENT_READER_DETERMINED_FILENAME) ?: return null
        return ContentOutNameDeserializer().deserializeIfSuccessful(record.value())
    }

    /**
     * @see KafkaEvents.EVENT_METADATA_OBTAINED and
     * @see Metadata for datastructure
     */
    fun getMetadata(): Metadata? {
        return firstOrNull(KafkaEvents.EVENT_METADATA_OBTAINED)?.let {
            MetadataResultDeserializer().deserializeIfSuccessful(it.value())
        }
    }

    fun getMovieInfo(): MovieInfo? {
        return firstOrNull(KafkaEvents.EVENT_READER_DETERMINED_MOVIE)?.let {
            MovieInfoDeserializer().deserializeIfSuccessful(it.value())
        }
    }

    fun getSerieInfo(): EpisodeInfo? {
        return firstOrNull(KafkaEvents.EVENT_READER_DETERMINED_SERIE)?.let {
            EpisodeInfoDeserializer().deserializeIfSuccessful(it.value())
        }
    }

    fun getEncodeWork(): EncodeWork? {
        return firstOrNull(KafkaEvents.EVENT_ENCODER_VIDEO_FILE_ENDED)?.let {
            EncodeWorkDeserializer().deserializeIfSuccessful(it.value())
        }
    }

}