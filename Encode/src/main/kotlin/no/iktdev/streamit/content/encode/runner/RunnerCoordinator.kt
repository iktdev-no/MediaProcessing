package no.iktdev.streamit.content.encode.runner

import com.google.gson.Gson
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import no.iktdev.streamit.content.encode.EncodeEnv
import mu.KotlinLogging
import no.iktdev.exfl.coroutines.Coroutines
import no.iktdev.streamit.content.common.CommonConfig
import no.iktdev.streamit.content.common.dto.State
import no.iktdev.streamit.content.common.dto.WorkOrderItem
import no.iktdev.streamit.content.common.dto.reader.work.EncodeWork
import no.iktdev.streamit.content.common.dto.reader.work.ExtractWork
import no.iktdev.streamit.content.encode.encoderItems
import no.iktdev.streamit.content.encode.extractItems
import no.iktdev.streamit.content.encode.progress.Progress
import no.iktdev.streamit.content.encode.progressMap
import no.iktdev.streamit.library.kafka.KafkaEvents
import no.iktdev.streamit.library.kafka.dto.Message
import no.iktdev.streamit.library.kafka.dto.Status
import no.iktdev.streamit.library.kafka.dto.StatusType
import no.iktdev.streamit.library.kafka.producer.DefaultProducer
import org.springframework.stereotype.Service
import java.util.concurrent.atomic.AtomicInteger

private val logger = KotlinLogging.logger {}

data class ExecutionBlock(
    val workId: String,
    val type: String,
    val work: suspend () -> Int
)

@Service
class RunnerCoordinator(
    private var maxConcurrentJobs: Int = 1,
) {
    private val logger = KotlinLogging.logger {}

    val producer = DefaultProducer(CommonConfig.kafkaTopic)
    final val defaultScope = Coroutines.default()

    private val jobsInProgress = AtomicInteger(0)
    private var inProgressJobs = mutableListOf<Job>()
    val queue = Channel<ExecutionBlock>(Channel.UNLIMITED)


    init {
        maxConcurrentJobs = EncodeEnv.maxRunners
        repeat(EncodeEnv.maxRunners) {
            launchWorker()
        }
    }

    fun launchWorker() = defaultScope.launch {
        while (true) {
            logger.info("Worker is waiting for a work item...")
            val workItem = queue.receive() // Coroutine will wait here until a work item is available
            logger.info("Worker received a work item.")
            if (jobsInProgress.get() < maxConcurrentJobs) {
                jobsInProgress.incrementAndGet()
                val job = processWorkItem(workItem)
                inProgressJobs.add(job)
                job.invokeOnCompletion {
                    logger.info { "OnCompletion invoked!\n\nWorkId: ${workItem.workId}-${workItem.type} \n\tCurrent active worksers: ${jobsInProgress.get()}" }
                    val workers = jobsInProgress.decrementAndGet()
                    logger.info { "Worker Released: $workers" }
                    logger.info { "Available: ${workers}/${maxConcurrentJobs}" }
                    inProgressJobs.remove(job)
                }
            }
            logger.info { "Available workers: ${jobsInProgress.get()}/$maxConcurrentJobs" }

        }
    }

    private suspend fun processWorkItem(workItem: ExecutionBlock): Job {
        logger.info { "Processing work: ${workItem.type}" }
        workItem.work()
        return Job().apply { complete() }
    }


    fun addEncodeMessageToQueue(message: Message) {
        producer.sendMessage(
            KafkaEvents.EVENT_ENCODER_VIDEO_FILE_QUEUED.event,
            message.withNewStatus(Status(StatusType.PENDING))
        )
        try {
            if (message.data != null && message.data is EncodeWork) {
                val work = message.data as EncodeWork
                encoderItems.put(
                    message.referenceId, WorkOrderItem(
                        id = message.referenceId,
                        inputFile = work.inFile,
                        outputFile = work.outFile,
                        collection = work.collection,
                        state = State.QUEUED
                    )
                )

                val workBlock = suspend {
                    val data: EncodeWork = work
                    val encodeDaemon = EncodeDaemon(message.referenceId, data, encodeListener)
                    logger.info { "\nreferenceId: ${message.referenceId} \nStarting encoding. \nWorkId: ${data.workId}" }
                    encodeDaemon.runUsingWorkItem()
                }
                val result = queue.trySend(ExecutionBlock(work.workId, "encode", workBlock))
                val statusType = when (result.isClosed) {
                    true -> StatusType.IGNORED // Køen er lukket, jobben ble ignorert
                    false -> {
                        if (result.isSuccess) {
                            StatusType.SUCCESS // Jobben ble sendt til køen
                        } else {
                            StatusType.ERROR // Feil ved sending av jobben
                        }
                    }
                }
                producer.sendMessage(
                    KafkaEvents.EVENT_ENCODER_VIDEO_FILE_QUEUED.event,
                    message.withNewStatus(Status(statusType))
                )
            } else {
                producer.sendMessage(
                    KafkaEvents.EVENT_ENCODER_VIDEO_FILE_QUEUED.event,
                    message.withNewStatus(Status(StatusType.ERROR, "Data is not an instance of EncodeWork or null"))
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
            producer.sendMessage(
                KafkaEvents.EVENT_ENCODER_VIDEO_FILE_QUEUED.event,
                message.withNewStatus(Status(StatusType.ERROR, e.message))
            )
        }
    }

    fun addExtractMessageToQueue(message: Message) {
        producer.sendMessage(
            KafkaEvents.EVENT_ENCODER_SUBTITLE_FILE_QUEUED.event,
            message.withNewStatus(Status(StatusType.PENDING))
        )
        try {
            if (message.data != null && message.data is ExtractWork) {
                val work = message.data as ExtractWork
                extractItems.put(
                    message.referenceId, WorkOrderItem(
                        id = message.referenceId,
                        inputFile = work.inFile,
                        outputFile = work.outFile,
                        collection = work.collection,
                        state = State.QUEUED
                    )
                )
                val workBlock = suspend {
                    val data: ExtractWork = work
                    val extractDaemon = ExtractDaemon(message.referenceId, data, extractListener)
                    logger.info { "\nreferenceId: ${message.referenceId} \nStarting extracting. \nWorkId: ${data.workId}" }
                    extractDaemon.runUsingWorkItem()
                }
                val result = queue.trySend(ExecutionBlock(work.workId, "extract", workBlock))
                val statusType = when (result.isClosed) {
                    true -> StatusType.IGNORED // Køen er lukket, jobben ble ignorert
                    false -> {
                        if (result.isSuccess) {
                            StatusType.SUCCESS // Jobben ble sendt til køen
                        } else {
                            StatusType.ERROR // Feil ved sending av jobben
                        }
                    }
                }
                producer.sendMessage(
                    KafkaEvents.EVENT_ENCODER_SUBTITLE_FILE_QUEUED.event,
                    message.withNewStatus(Status(statusType))
                )
            } else {
                producer.sendMessage(
                    KafkaEvents.EVENT_ENCODER_SUBTITLE_FILE_QUEUED.event,
                    message.withNewStatus(Status(StatusType.ERROR, "Data is not an instance of ExtractWork"))
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
            producer.sendMessage(
                KafkaEvents.EVENT_ENCODER_SUBTITLE_FILE_QUEUED.event,
                message.withNewStatus(Status(StatusType.ERROR, e.message))
            )
        }
    }


    val encodeListener = object : IEncodeListener {
        override fun onStarted(referenceId: String, work: EncodeWork) {
            logger.info { "\nreferenceId: $referenceId \nWorkId ${work.workId}  \nEncode: Started\n${work.outFile}" }
            producer.sendMessage(
                KafkaEvents.EVENT_ENCODER_VIDEO_FILE_STARTED.event,
                Message(referenceId, Status(statusType = StatusType.SUCCESS), work)
            )
            encoderItems.put(
                referenceId, WorkOrderItem(
                    id = referenceId,
                    inputFile = work.inFile,
                    outputFile = work.outFile,
                    collection = work.collection,
                    state = State.STARTED
                )
            )
        }

        override fun onError(referenceId: String, work: EncodeWork, code: Int) {
            logger.error { "\nreferenceId: $referenceId \nWorkId ${work.workId}  \nEncode: Failed\n${work.outFile} \nError: $code" }
            producer.sendMessage(
                KafkaEvents.EVENT_ENCODER_VIDEO_FILE_ENDED.event,
                Message(referenceId, Status(StatusType.ERROR, message = code.toString()), work)
            )
            encoderItems.put(
                referenceId, WorkOrderItem(
                    id = referenceId,
                    inputFile = work.inFile,
                    outputFile = work.outFile,
                    collection = work.collection,
                    state = State.FAILURE
                )
            )
        }

        override fun onProgress(referenceId: String, work: EncodeWork, progress: Progress) {
            logger.debug {
                "Work progress for $referenceId with WorkId ${work.workId} @ ${work.outFile}: Progress: ${
                    Gson().toJson(
                        progress
                    )
                }"
            }
            progressMap.put(work.workId, progress)
            encoderItems.put(
                referenceId, WorkOrderItem(
                    id = referenceId,
                    inputFile = work.inFile,
                    outputFile = work.outFile,
                    collection = work.collection,
                    state = State.UPDATED,
                    progress = progress.progress,
                    remainingTime = progress.estimatedCompletionSeconds
                )
            )
        }

        override fun onEnded(referenceId: String, work: EncodeWork) {
            logger.info { "\nreferenceId: $referenceId \nWorkId ${work.workId}  \nEncode: Ended\n${work.outFile}" }
            producer.sendMessage(
                KafkaEvents.EVENT_ENCODER_VIDEO_FILE_ENDED.event,
                Message(referenceId, Status(statusType = StatusType.SUCCESS), work)
            )
            encoderItems.put(
                referenceId, WorkOrderItem(
                    id = referenceId,
                    inputFile = work.inFile,
                    outputFile = work.outFile,
                    collection = work.collection,
                    state = State.ENDED,
                    progress = 100,
                    remainingTime = null
                )
            )
        }
    }

    val extractListener = object : IExtractListener {
        override fun onStarted(referenceId: String, work: ExtractWork) {
            logger.info { "\nreferenceId: $referenceId \nWorkId ${work.workId}  \nExtract: Started\n${work.outFile}" }
            producer.sendMessage(
                KafkaEvents.EVENT_ENCODER_SUBTITLE_FILE_STARTED.event,
                Message(referenceId, Status(statusType = StatusType.SUCCESS), work)
            )
            extractItems.put(
                referenceId, WorkOrderItem(
                    id = referenceId,
                    inputFile = work.inFile,
                    outputFile = work.outFile,
                    collection = work.collection,
                    state = State.STARTED
                )
            )
        }

        override fun onError(referenceId: String, work: ExtractWork, code: Int) {
            logger.error { "\nreferenceId: $referenceId \nWorkId ${work.workId}  \nExtract: Failed\n${work.outFile} \nError: $code" }

            producer.sendMessage(
                KafkaEvents.EVENT_ENCODER_SUBTITLE_FILE_ENDED.event,
                Message(referenceId, Status(StatusType.ERROR, code.toString()), work)
            )
            extractItems.put(
                referenceId, WorkOrderItem(
                    id = referenceId,
                    inputFile = work.inFile,
                    outputFile = work.outFile,
                    collection = work.collection,
                    state = State.FAILURE
                )
            )
        }

        override fun onEnded(referenceId: String, work: ExtractWork) {
            logger.info { "\nreferenceId: $referenceId \nWorkId ${work.workId}  \nExtract: Ended\n${work.outFile}" }
            producer.sendMessage(
                KafkaEvents.EVENT_ENCODER_SUBTITLE_FILE_ENDED.event,
                Message(referenceId, Status(statusType = StatusType.SUCCESS), work)
            )
            extractItems.put(
                referenceId, WorkOrderItem(
                    id = referenceId,
                    inputFile = work.inFile,
                    outputFile = work.outFile,
                    collection = work.collection,
                    state = State.ENDED
                )
            )
        }

    }

}
