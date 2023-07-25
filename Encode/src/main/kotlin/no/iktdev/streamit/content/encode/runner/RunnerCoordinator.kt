package no.iktdev.streamit.content.encode.runner

import com.google.gson.Gson
import kotlinx.coroutines.*
import no.iktdev.streamit.content.encode.EncodeEnv
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
private val logger = KotlinLogging.logger {}

@Service
class RunnerCoordinator {
    private val logger = KotlinLogging.logger {}

    val producer = DefaultProducer(CommonConfig.kafkaTopic)

    val executor: ExecutorService = ThreadPoolExecutor(
        EncodeEnv.maxRunners,
        EncodeEnv.maxRunners,
        0L,
        TimeUnit.MILLISECONDS,
        LinkedBlockingQueue()
    )
    val dispatcher: CoroutineDispatcher = executor.asCoroutineDispatcher()
    val scope = CoroutineScope(dispatcher)

    fun addEncodeMessageToQueue(message: Message) {
        producer.sendMessage(KafkaEvents.EVENT_ENCODER_STARTED_VIDEO_FILE.event, message.withNewStatus(Status(StatusType.PENDING)))
        scope.launch {
            try {
                if (message.data != null && message.data is EncodeWork) {
                    val data: EncodeWork = message.data as EncodeWork
                    val encodeDaemon = EncodeDaemon(message.referenceId, data, encodeListener)
                    logger.info { "${message.referenceId} Starting encoding ${data.workId}" }
                    encodeDaemon.runUsingWorkItem()
                } else {
                    producer.sendMessage(KafkaEvents.EVENT_ENCODER_STARTED_VIDEO_FILE.event, message.withNewStatus(Status(StatusType.ERROR, "Data is not an instance of EncodeWork or null")))
                }
            } catch (e: Exception) {
                e.printStackTrace()
                producer.sendMessage(KafkaEvents.EVENT_ENCODER_STARTED_VIDEO_FILE.event, message.withNewStatus(Status(StatusType.ERROR, e.message)))
            }

        }
    }

    fun addExtractMessageToQueue(message: Message) {
        producer.sendMessage(KafkaEvents.EVENT_ENCODER_STARTED_SUBTITLE_FILE.event, message.withNewStatus(Status(StatusType.PENDING)))
        scope.launch {
            try {
                if (message.data != null && message.data is ExtractWork) {
                    val data: ExtractWork = message.data as ExtractWork
                    val extractDaemon = ExtractDaemon(message.referenceId, data, extractListener)
                    logger.info { "${message.referenceId} Starting extraction ${data.workId}" }
                    extractDaemon.runUsingWorkItem()
                } else {
                    producer.sendMessage(KafkaEvents.EVENT_ENCODER_STARTED_SUBTITLE_FILE.event, message.withNewStatus(Status(StatusType.ERROR, "Data is not an instance of ExtractWork")))
                }
            } catch (e: Exception) {
                e.printStackTrace()
                producer.sendMessage(KafkaEvents.EVENT_ENCODER_STARTED_SUBTITLE_FILE.event, message.withNewStatus(Status(StatusType.ERROR, e.message)))
            }
        }
    }





    val encodeListener = object: IEncodeListener {
        override fun onStarted(referenceId: String, work: EncodeWork) {
            logger.info { "Work started for $referenceId with WorkId ${work.workId} @ ${work.outFile}" }
            producer.sendMessage(KafkaEvents.EVENT_ENCODER_STARTED_VIDEO_FILE.event, Message(referenceId, Status(statusType =  StatusType.SUCCESS), work))
        }

        override fun onError(referenceId: String, work: EncodeWork, code: Int) {
            logger.error { "Work failed for $referenceId with WorkId ${work.workId} @ ${work.outFile}: Error $code" }
            producer.sendMessage(KafkaEvents.EVENT_ENCODER_ENDED_VIDEO_FILE.event, Message(referenceId, Status(StatusType.ERROR, message = code.toString()), work))
        }

        override fun onProgress(referenceId: String, work: EncodeWork, progress: Progress) {
            logger.info { "Work progress for $referenceId with WorkId ${work.workId} @ ${work.outFile}: Progress: ${Gson().toJson(progress)}" }
        }

        override fun onEnded(referenceId: String, work: EncodeWork) {
            logger.info { "Work ended for $referenceId with WorkId ${work.workId} @ ${work.outFile}" }
            producer.sendMessage(KafkaEvents.EVENT_ENCODER_ENDED_VIDEO_FILE.event, Message(referenceId, Status(statusType =  StatusType.SUCCESS), work))
        }
    }

    val extractListener = object : IExtractListener {
        override fun onStarted(referenceId: String, work: ExtractWork) {
            logger.info { "Work started for $referenceId with WorkId ${work.workId} @ ${work.outFile}: Started" }
            producer.sendMessage(KafkaEvents.EVENT_ENCODER_STARTED_SUBTITLE_FILE.event, Message(referenceId, Status(statusType =  StatusType.SUCCESS), work))
        }

        override fun onError(referenceId: String, work: ExtractWork, code: Int) {
            logger.error { "Work failed for $referenceId with WorkId ${work.workId} @ ${work.outFile}: Error $code" }
            producer.sendMessage(KafkaEvents.EVENT_ENCODER_ENDED_SUBTITLE_FILE.event, Message(referenceId, Status(StatusType.ERROR, code.toString()), work))
        }

        override fun onEnded(referenceId: String, work: ExtractWork) {
            logger.info { "Work ended for $referenceId with WorkId ${work.workId} @ ${work.outFile}: Ended" }
            producer.sendMessage(KafkaEvents.EVENT_ENCODER_ENDED_SUBTITLE_FILE.event, Message(referenceId, Status(statusType =  StatusType.SUCCESS), work))
        }

    }

}
