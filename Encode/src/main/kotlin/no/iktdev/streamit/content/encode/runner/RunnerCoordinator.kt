package no.iktdev.streamit.content.encode.runner

import com.google.gson.Gson
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.sync.Mutex
import no.iktdev.streamit.content.encode.EncodeEnv
import mu.KotlinLogging
import no.iktdev.exfl.coroutines.Coroutines
import no.iktdev.streamit.content.common.CommonConfig
import no.iktdev.streamit.content.common.dto.reader.work.EncodeWork
import no.iktdev.streamit.content.common.dto.reader.work.ExtractWork
import no.iktdev.streamit.content.encode.progress.Progress
import no.iktdev.streamit.library.kafka.KafkaEvents
import no.iktdev.streamit.library.kafka.dto.Message
import no.iktdev.streamit.library.kafka.dto.Status
import no.iktdev.streamit.library.kafka.dto.StatusType
import no.iktdev.streamit.library.kafka.producer.DefaultProducer
import org.springframework.boot.autoconfigure.couchbase.CouchbaseProperties.Env
import org.springframework.stereotype.Service
import java.util.concurrent.*
import java.util.concurrent.atomic.AtomicInteger

private val logger = KotlinLogging.logger {}

data class ExecutionBlock(
    val type: String,
    val work: suspend () -> Int
)

@Service
class RunnerCoordinator(private var maxConcurrentJobs: Int = 1) {
    private val logger = KotlinLogging.logger {}

    val producer = DefaultProducer(CommonConfig.kafkaTopic)
    final val defaultScope = Coroutines.default()

    private val jobsInProgress = AtomicInteger(0)
    val queue = Channel<ExecutionBlock>(Channel.UNLIMITED)


    init {
        maxConcurrentJobs = EncodeEnv.maxRunners
        repeat(EncodeEnv.maxRunners) {
            launchWorker()
        }
    }

    fun launchWorker() = defaultScope.launch {
        while (true) {
            val workItem = queue.receive() // Coroutine will wait here until a work item is available
            if (jobsInProgress.incrementAndGet() <= maxConcurrentJobs) {
                val job = processWorkItem(workItem)
                job.invokeOnCompletion {
                    jobsInProgress.decrementAndGet()
                }
            }
        }
    }

    private suspend fun processWorkItem(workItem: ExecutionBlock): Job {
        workItem.work()
        return Job()
    }




    fun addEncodeMessageToQueue(message: Message) {
        producer.sendMessage(KafkaEvents.EVENT_ENCODER_VIDEO_FILE_QUEUED.event, message.withNewStatus(Status(StatusType.PENDING)))
        try {
            if (message.data != null && message.data is EncodeWork) {

                val workBlock = suspend {
                    val data: EncodeWork = message.data as EncodeWork
                    val encodeDaemon = EncodeDaemon(message.referenceId, data, encodeListener)
                    logger.info { "\nreferenceId: ${message.referenceId} \nStarting encoding. \nWorkId: ${data.workId}" }
                    encodeDaemon.runUsingWorkItem()
                }
                queue.trySend(ExecutionBlock("encode", workBlock))
                producer.sendMessage(KafkaEvents.EVENT_ENCODER_VIDEO_FILE_QUEUED.event, message.withNewStatus(Status(StatusType.SUCCESS)))
            } else {
                producer.sendMessage(KafkaEvents.EVENT_ENCODER_VIDEO_FILE_QUEUED.event, message.withNewStatus(Status(StatusType.ERROR, "Data is not an instance of EncodeWork or null")))
            }
        } catch (e: Exception) {
            e.printStackTrace()
            producer.sendMessage(KafkaEvents.EVENT_ENCODER_VIDEO_FILE_QUEUED.event, message.withNewStatus(Status(StatusType.ERROR, e.message)))
        }
    }

    fun addExtractMessageToQueue(message: Message) {
        producer.sendMessage(KafkaEvents.EVENT_ENCODER_SUBTITLE_FILE_QUEUED.event, message.withNewStatus(Status(StatusType.PENDING)))
        try {
            if (message.data != null && message.data is ExtractWork) {
                val workBlock = suspend {
                    val data: ExtractWork = message.data as ExtractWork
                    val extractDaemon = ExtractDaemon(message.referenceId, data, extractListener)
                    logger.info { "\nreferenceId: ${message.referenceId} \nStarting extracting. \nWorkId: ${data.workId}" }
                    extractDaemon.runUsingWorkItem()
                }
                queue.trySend(ExecutionBlock("extract", workBlock))
                producer.sendMessage(KafkaEvents.EVENT_ENCODER_SUBTITLE_FILE_QUEUED.event, message.withNewStatus(Status(StatusType.SUCCESS)))
            } else {
                producer.sendMessage(KafkaEvents.EVENT_ENCODER_SUBTITLE_FILE_QUEUED.event, message.withNewStatus(Status(StatusType.ERROR, "Data is not an instance of ExtractWork")))
            }
        } catch (e: Exception) {
            e.printStackTrace()
            producer.sendMessage(KafkaEvents.EVENT_ENCODER_SUBTITLE_FILE_QUEUED.event, message.withNewStatus(Status(StatusType.ERROR, e.message)))
        }
    }





    val encodeListener = object: IEncodeListener {
        override fun onStarted(referenceId: String, work: EncodeWork) {
            logger.info { "\nreferenceId: $referenceId \nWorkId ${work.workId}  \nEncode: Started\n${work.outFile}" }
            producer.sendMessage(KafkaEvents.EVENT_ENCODER_VIDEO_FILE_STARTED.event, Message(referenceId, Status(statusType =  StatusType.SUCCESS), work))
        }

        override fun onError(referenceId: String, work: EncodeWork, code: Int) {
            logger.error { "\nreferenceId: $referenceId \nWorkId ${work.workId}  \nEncode: Failed\n${work.outFile} \nError: $code" }
            producer.sendMessage(KafkaEvents.EVENT_ENCODER_VIDEO_FILE_ENDED.event, Message(referenceId, Status(StatusType.ERROR, message = code.toString()), work))
        }

        override fun onProgress(referenceId: String, work: EncodeWork, progress: Progress) {
            logger.info { "Work progress for $referenceId with WorkId ${work.workId} @ ${work.outFile}: Progress: ${Gson().toJson(progress)}" }
        }

        override fun onEnded(referenceId: String, work: EncodeWork) {
            logger.info { "\nreferenceId: $referenceId \nWorkId ${work.workId}  \nEncode: Ended\n${work.outFile}" }
            producer.sendMessage(KafkaEvents.EVENT_ENCODER_VIDEO_FILE_ENDED.event, Message(referenceId, Status(statusType =  StatusType.SUCCESS), work))
        }
    }

    val extractListener = object : IExtractListener {
        override fun onStarted(referenceId: String, work: ExtractWork) {
            logger.info { "\nreferenceId: $referenceId \nWorkId ${work.workId}  \nExtract: Started\n${work.outFile}" }
            producer.sendMessage(KafkaEvents.EVENT_ENCODER_SUBTITLE_FILE_STARTED.event, Message(referenceId, Status(statusType =  StatusType.SUCCESS), work))
        }

        override fun onError(referenceId: String, work: ExtractWork, code: Int) {
            logger.error { "\nreferenceId: $referenceId \nWorkId ${work.workId}  \nExtract: Failed\n${work.outFile} \nError: $code" }

            producer.sendMessage(KafkaEvents.EVENT_ENCODER_SUBTITLE_FILE_ENDED.event, Message(referenceId, Status(StatusType.ERROR, code.toString()), work))
        }

        override fun onEnded(referenceId: String, work: ExtractWork) {
            logger.info { "\nreferenceId: $referenceId \nWorkId ${work.workId}  \nExtract: Ended\n${work.outFile}" }
            producer.sendMessage(KafkaEvents.EVENT_ENCODER_SUBTITLE_FILE_ENDED.event, Message(referenceId, Status(statusType =  StatusType.SUCCESS), work))
        }

    }

}
