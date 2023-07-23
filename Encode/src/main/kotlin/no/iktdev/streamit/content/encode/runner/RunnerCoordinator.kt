package no.iktdev.streamit.content.encode.runner

import no.iktdev.streamit.content.encode.EncodeEnv
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import no.iktdev.streamit.content.common.CommonConfig
import no.iktdev.streamit.content.common.dto.reader.work.EncodeWork
import no.iktdev.streamit.content.common.dto.reader.work.ExtractWork
import no.iktdev.streamit.content.encode.progress.Progress
import no.iktdev.streamit.library.kafka.KafkaEvents
import no.iktdev.streamit.library.kafka.dto.Message
import no.iktdev.streamit.library.kafka.dto.Status
import no.iktdev.streamit.library.kafka.dto.StatusType
import no.iktdev.streamit.library.kafka.producer.DefaultProducer
import org.springframework.stereotype.Service
import java.util.concurrent.ExecutorService
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit

@Service
class RunnerCoordinator {
    private val logger = KotlinLogging.logger {}

    val producer = DefaultProducer(CommonConfig.kafkaTopic)

    val encodeExecutor: ExecutorService = ThreadPoolExecutor(
        EncodeEnv.maxRunners,
        EncodeEnv.maxRunners,
        0L,
        TimeUnit.MILLISECONDS,
        LinkedBlockingQueue()
    )

    val extractExecutor: ExecutorService = ThreadPoolExecutor(
        EncodeEnv.maxRunners,
        EncodeEnv.maxRunners,
        0L,
        TimeUnit.MILLISECONDS,
        LinkedBlockingQueue()
    )

    fun addEncodeMessageToQueue(message: Message) {
        encodeExecutor.execute {
            try {
                runBlocking {
                    if (message.data is EncodeWork) {
                        val data: EncodeWork = message.data as EncodeWork
                        val encodeDaemon = EncodeDaemon(message.referenceId, data, encodeListener)
                        encodeDaemon.runUsingWorkItem()
                    } else {
                        producer.sendMessage(KafkaEvents.EVENT_ENCODER_STARTED_VIDEO_FILE.event, message.withNewStatus(Status(StatusType.ERROR, "Data is not an instance of EncodeWork")))
                    }
                }
            } catch (e: Exception) {
                producer.sendMessage(KafkaEvents.EVENT_ENCODER_ENDED_VIDEO_FILE.event, message.withNewStatus(Status(StatusType.ERROR, e.message)))
            }

        }
        producer.sendMessage(KafkaEvents.EVENT_ENCODER_STARTED_VIDEO_FILE.event, message.withNewStatus(Status(StatusType.PENDING)))
    }

    fun addExtractMessageToQueue(message: Message) {
        extractExecutor.execute {
            runBlocking {
                try {
                    if (message.data is ExtractWork) {
                        val data: ExtractWork = message.data as ExtractWork
                        val extractDaemon = ExtractDaemon(message.referenceId, data, extractListener)
                        extractDaemon.runUsingWorkItem()
                    } else {
                        producer.sendMessage(KafkaEvents.EVENT_ENCODER_STARTED_SUBTITLE_FILE.event, message.withNewStatus(Status(StatusType.ERROR, "Data is not an instance of ExtractWork")))
                    }
                } catch (e: Exception) {
                    producer.sendMessage(KafkaEvents.EVENT_ENCODER_ENDED_SUBTITLE_FILE.event, message.withNewStatus(Status(StatusType.ERROR, e.message)))
                }
            }
        }
        producer.sendMessage(KafkaEvents.EVENT_ENCODER_STARTED_SUBTITLE_FILE.event, message.withNewStatus(Status(StatusType.PENDING)))
    }





    val encodeListener = object: IEncodeListener {
        override fun onStarted(referenceId: String, work: EncodeWork) {
            producer.sendMessage(KafkaEvents.EVENT_ENCODER_STARTED_VIDEO_FILE.event, Message(referenceId, Status(StatusType.SUCCESS), work))
        }

        override fun onError(referenceId: String, work: EncodeWork, code: Int) {
            producer.sendMessage(KafkaEvents.EVENT_ENCODER_ENDED_VIDEO_FILE.event, Message(referenceId, Status(StatusType.ERROR, message = code.toString()), work))
        }

        override fun onProgress(referenceId: String, work: EncodeWork, progress: Progress) {
            logger.info { "$referenceId with WorkId ${work.workId} @ ${work.outFile}: Progress: ${progress.speed}" }
        }

        override fun onEnded(referenceId: String, work: EncodeWork) {
            producer.sendMessage(KafkaEvents.EVENT_ENCODER_ENDED_VIDEO_FILE.event, Message(referenceId, Status(StatusType.SUCCESS), work))
        }
    }

    val extractListener = object : IExtractListener {
        override fun onStarted(referenceId: String, work: ExtractWork) {
            producer.sendMessage(KafkaEvents.EVENT_ENCODER_STARTED_SUBTITLE_FILE.event, Message(referenceId, Status(StatusType.SUCCESS), work))
        }

        override fun onError(referenceId: String, work: ExtractWork, code: Int) {
            producer.sendMessage(KafkaEvents.EVENT_ENCODER_ENDED_SUBTITLE_FILE.event, Message(referenceId, Status(StatusType.ERROR), work))
        }

        override fun onEnded(referenceId: String, work: ExtractWork) {
            producer.sendMessage(KafkaEvents.EVENT_ENCODER_ENDED_SUBTITLE_FILE.event, Message(referenceId, Status(StatusType.SUCCESS), work))
        }

    }

}
