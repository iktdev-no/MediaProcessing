package no.iktdev.streamit.content.reader.streams

import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import no.iktdev.streamit.content.common.CommonConfig
import no.iktdev.streamit.content.common.deamon.Daemon
import no.iktdev.streamit.content.common.deamon.IDaemon
import no.iktdev.streamit.content.reader.ReaderEnv
import no.iktdev.streamit.library.kafka.KnownEvents
import no.iktdev.streamit.library.kafka.KnownEvents.EVENT_READER_RECEIVED_FILE
import no.iktdev.streamit.library.kafka.Message
import no.iktdev.streamit.library.kafka.Status
import no.iktdev.streamit.library.kafka.StatusType
import no.iktdev.streamit.library.kafka.consumers.DefaultConsumer
import no.iktdev.streamit.library.kafka.listener.EventMessageListener
import no.iktdev.streamit.library.kafka.producer.DefaultProducer
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.springframework.stereotype.Service

private val logger = KotlinLogging.logger {}
@Service
class StreamsReader {

    val messageProducer = DefaultProducer(CommonConfig.kafkaConsumerId)
    val defaultConsumer = DefaultConsumer().apply {
       // autoCommit = false
    }
    init {
        object: EventMessageListener(CommonConfig.kafkaConsumerId, defaultConsumer, listOf(EVENT_READER_RECEIVED_FILE.event)) {
            override fun onMessage(data: ConsumerRecord<String, Message>) {
                if (data.value().status.statusType != StatusType.SUCCESS) {
                    logger.info { "Ignoring event: ${data.key()} as status is not Success!" }
                    return
                } else if (data.value().data !is String) {
                    logger.info { "Ignoring event: ${data.key()} as values is not of expected type!" }
                    return
                }
                logger.info { "Preparing Probe for ${data.value().data}" }
                val output = mutableListOf<String>()
                val d = Daemon(executable = ReaderEnv.ffprobe, parameters =  listOf("-v", "quiet", "-print_format", "json", "-show_streams", data.value().data as String), daemonInterface = object:
                    IDaemon {
                    override fun onOutputChanged(line: String) {
                        output.add(line)
                    }

                    override fun onStarted() {
                        logger.info { "Probe started for ${data.value().data}" }
                    }

                    override fun onError() {
                        logger.error { "An error occurred for ${data.value().data}" }
                    }

                    override fun onEnded() {
                        logger.info { "Probe ended for ${data.value().data}" }
                    }

                })
                val resultCode = runBlocking {
                    d.run()
                }

                val message = Message(status = Status( statusType =  if (resultCode == 0) StatusType.SUCCESS else StatusType.ERROR), data = output.joinToString("\n"))
                messageProducer.sendMessage(KnownEvents.EVENT_READER_RECEIVED_STREAMS.event, message)
            }
        }.listen()
    }
}