package no.iktdev.streamit.content.reader.streams

import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import no.iktdev.streamit.content.common.CommonConfig
import no.iktdev.streamit.content.common.deamon.Daemon
import no.iktdev.streamit.content.common.deamon.IDaemon
import no.iktdev.streamit.content.common.dto.reader.FileResult
import no.iktdev.streamit.content.reader.ReaderEnv
import no.iktdev.streamit.content.reader.fileWatcher.FileWatcher
import no.iktdev.streamit.library.kafka.KafkaEvents
import no.iktdev.streamit.library.kafka.KafkaEvents.EVENT_READER_RECEIVED_FILE
import no.iktdev.streamit.library.kafka.consumers.DefaultConsumer
import no.iktdev.streamit.library.kafka.dto.Message
import no.iktdev.streamit.library.kafka.dto.Status
import no.iktdev.streamit.library.kafka.dto.StatusType
import no.iktdev.streamit.library.kafka.listener.SimpleMessageListener
import no.iktdev.streamit.library.kafka.producer.DefaultProducer
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.springframework.stereotype.Service

private val logger = KotlinLogging.logger {}

@Service
class StreamsReader {

    val messageProducer = DefaultProducer(CommonConfig.kafkaTopic)
    val defaultConsumer = DefaultConsumer(subId = "streamReader")


    init {
        object: SimpleMessageListener(topic =  CommonConfig.kafkaTopic, consumer =  defaultConsumer, accepts =  listOf(EVENT_READER_RECEIVED_FILE.event)) {
            override fun onMessageReceived(data: ConsumerRecord<String, Message>) {
                logger.info { "RECORD: ${data.key()}" }
                if (data.value().status.statusType != StatusType.SUCCESS) {
                    logger.info { "Ignoring event: ${data.key()} as status is not Success!" }
                    return
                }
                val dataValue = data.value().dataAs(FileResult::class.java)

                if (dataValue == null) {
                    logger.info { "Ignoring event: ${data.key()} as values is not of expected type!" }
                    return
                }
                logger.info { "Preparing Probe for ${dataValue.file}" }
                val output = mutableListOf<String>()
                val d = Daemon(executable = ReaderEnv.ffprobe, daemonInterface = object:
                    IDaemon {
                    override fun onOutputChanged(line: String) {
                        output.add(line)
                    }

                    override fun onStarted() {
                        logger.info { "Probe started for ${dataValue.file}" }
                    }

                    override fun onError(code: Int) {
                        logger.error { "An error occurred for ${dataValue.file}" }
                    }

                    override fun onEnded() {
                        logger.info { "Probe ended for ${dataValue.file}" }
                    }

                })
                val resultCode = runBlocking {
                    val args = listOf("-v", "quiet", "-print_format", "json", "-show_streams", dataValue.file)
                    d.run(args)
                }

                val message = Message(referenceId = data.value().referenceId, status = Status( statusType =  if (resultCode == 0) StatusType.SUCCESS else StatusType.ERROR), data = output.joinToString("\n"))
                messageProducer.sendMessage(KafkaEvents.EVENT_READER_RECEIVED_STREAMS.event, message)
            }

        }.listen()
    }

}