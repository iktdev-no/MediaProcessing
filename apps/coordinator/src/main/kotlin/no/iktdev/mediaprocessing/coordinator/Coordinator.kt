package no.iktdev.mediaprocessing.coordinator

import com.google.gson.Gson
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import mu.KotlinLogging
import no.iktdev.exfl.coroutines.Coroutines
import no.iktdev.mediaprocessing.coordinator.coordination.EventBasedMessageListener
import no.iktdev.mediaprocessing.shared.common.DatabaseConfig
import no.iktdev.mediaprocessing.shared.common.persistance.PersistentDataReader
import no.iktdev.mediaprocessing.shared.common.persistance.PersistentDataStore
import no.iktdev.mediaprocessing.shared.common.persistance.PersistentMessage
import no.iktdev.mediaprocessing.shared.contract.ProcessType
import no.iktdev.mediaprocessing.shared.kafka.core.CoordinatorProducer
import no.iktdev.mediaprocessing.shared.kafka.core.KafkaEvents
import no.iktdev.mediaprocessing.shared.kafka.core.DefaultMessageListener
import no.iktdev.mediaprocessing.shared.kafka.core.KafkaEnv
import no.iktdev.mediaprocessing.shared.kafka.dto.events_result.*
import no.iktdev.mediaprocessing.shared.kafka.dto.isSuccess
import no.iktdev.streamit.library.kafka.dto.Status
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.io.File
import java.util.UUID
import javax.annotation.PostConstruct

@Service
class Coordinator() {

    @Autowired
    private lateinit var producer: CoordinatorProducer

    @Autowired
    private lateinit var listener: DefaultMessageListener

    private val log = KotlinLogging.logger {}

    val listeners = EventBasedMessageListener()

    private val forwarder = Forwarder()

    public fun startProcess(file: File, type: ProcessType) {
        val processStartEvent = ProcessStarted(
            status = Status.STARTED,
            file = file.absolutePath,
            type = type
        )
        producer.sendMessage(UUID.randomUUID().toString(), KafkaEvents.EVENT_PROCESS_STARTED, processStartEvent)
    }


    val io = Coroutines.io()

    fun readAllUncompletedMessagesInQueue() {
        val messages = PersistentDataReader().getUncompletedMessages()
        io.launch {
            messages.forEach {
                delay(1000)
                listeners.forwardBatchEventMessagesToListeners(it)
            }
        }
    }

    fun readAllMessagesFor(referenceId: String, eventId: String) {
        val messages = PersistentDataReader().getMessagesFor(referenceId)
        if (messages.find { it.eventId == eventId && it.referenceId == referenceId } == null) {
            log.warn { "EventId ($eventId) for ReferenceId ($referenceId) has not been made available in the database yet." }
            io.launch {
                val fixedDelay = 1000L
                delay(fixedDelay)
                var delayed = 0L
                var msc = PersistentDataReader().getMessagesFor(referenceId)
                while (msc.find { it.eventId == eventId } != null || delayed < 1000*60) {
                    delayed += fixedDelay
                    msc = PersistentDataReader().getMessagesFor(referenceId)
                }
                operationToRunOnMessages(referenceId, eventId, msc)
            }
        } else {
            operationToRunOnMessages(referenceId, eventId, messages)
        }
    }

    fun operationToRunOnMessages(referenceId: String, eventId: String, messages: List<PersistentMessage>) {
        createTasksBasedOnEventsAndPersistance(referenceId, eventId, messages)

        io.launch {
            buildModelBasedOnMessagesFor(referenceId, messages)
        }
    }

    fun getProcessStarted(messages: List<PersistentMessage>): ProcessStarted? {
        return messages.find { it.event == KafkaEvents.EVENT_PROCESS_STARTED }?.data as ProcessStarted
    }

    suspend fun buildModelBasedOnMessagesFor(referenceId: String, messages: List<PersistentMessage>) {
        if (messages.any { it.data is ProcessCompleted }) {
            // TODO: Build and insert into database
        }
    }

    fun createTasksBasedOnEventsAndPersistance(referenceId: String, eventId: String, messages: List<PersistentMessage>) {
        val triggered = messages.find { it.eventId == eventId }
        if (triggered == null) {
            log.error { "Could not find $eventId in provided messages" }
            return
        }
        listeners.forwardEventMessageToListeners(triggered, messages)

        if (forwarder.hasAnyRequiredEventToCreateProcesserEvents(messages)) {
            if (getProcessStarted(messages)?.type == ProcessType.FLOW) {
                forwarder.produceAllMissingProcesserEvents(producer = producer, referenceId = referenceId, eventId = eventId, messages = messages)
            } else {
                log.info { "Process for $referenceId was started manually and will require user input for continuation" }
            }
        }
    }

    @PostConstruct
    fun onReady() {
        io.launch {
            listener.onMessageReceived = { event ->
                val success = PersistentDataStore().storeEventDataMessage(event.key.event, event.value)
                if (!success) {
                    log.error { "Unable to store message: ${event.key.event} in database ${DatabaseConfig.database}" }
                } else
                    io.launch {
                        delay(500) // Give the database a few sec to update
                        readAllMessagesFor(event.value.referenceId, event.value.eventId)
                    }
            }
            listener.listen(KafkaEnv.kafkaTopic)
        }
        readAllUncompletedMessagesInQueue()
    }


    class Forwarder() {
        val forwardOnEventReceived = listOf(
            KafkaEvents.EVENT_MEDIA_ENCODE_PARAMETER_CREATED, KafkaEvents.EVENT_MEDIA_EXTRACT_PARAMETER_CREATED
        )

        fun hasAnyRequiredEventToCreateProcesserEvents(messages: List<PersistentMessage>): Boolean {
            return messages.filter { forwardOnEventReceived.contains(it.event) && it.data.isSuccess() }.map { it.event }.isNotEmpty()
        }

        fun isMissingEncodeWorkCreated(messages: List<PersistentMessage>): Boolean {
            val existingWorkEncodeCreated = messages.filter { it.event == KafkaEvents.EVENT_WORK_ENCODE_CREATED }
            return existingWorkEncodeCreated.isEmpty() && existingWorkEncodeCreated.none { it.data.isSuccess() }
        }
        fun isMissingExtractWorkCreated(messages: List<PersistentMessage>): Boolean {
            val existingWorkCreated = messages.filter { it.event == KafkaEvents.EVENT_WORK_EXTRACT_CREATED }
            return existingWorkCreated.isEmpty() && existingWorkCreated.none { it.data.isSuccess() }
        }

        fun produceAllMissingProcesserEvents(producer: CoordinatorProducer, referenceId: String, eventId: String, messages: List<PersistentMessage>) {
            val currentMessage = messages.find { it.eventId == eventId }
            if (!currentMessage?.data.isSuccess()) {
                return
            }
            when (currentMessage?.event) {
                KafkaEvents.EVENT_MEDIA_ENCODE_PARAMETER_CREATED -> {
                    if (isMissingEncodeWorkCreated(messages)) {
                        produceEncodeWork(producer, currentMessage)
                    }
                }
                KafkaEvents.EVENT_MEDIA_EXTRACT_PARAMETER_CREATED -> {
                    if (isMissingExtractWorkCreated(messages)) {
                        produceExtractWork(producer, currentMessage)
                    }
                }
                else -> {}
            }
        }


        fun produceEncodeWork(producer: CoordinatorProducer, message: PersistentMessage) {
            if (message.event != KafkaEvents.EVENT_MEDIA_ENCODE_PARAMETER_CREATED) {
                throw RuntimeException("Incorrect event passed ${message.event}")
            }
            if (message.data !is FfmpegWorkerArgumentsCreated) {
                throw RuntimeException("Invalid data passed:\n${Gson().toJson(message)}")
            }
            val data = message.data as FfmpegWorkerArgumentsCreated
            data.entries.forEach {
                FfmpegWorkRequestCreated(
                    inputFile = data.inputFile,
                    arguments = it.arguments,
                    outFile = it.outputFile
                ).let {  createdRequest ->
                    producer.sendMessage(message.referenceId,
                        KafkaEvents.EVENT_WORK_ENCODE_CREATED,
                        eventId = message.eventId,
                        createdRequest)
                }
            }
        }

        fun produceExtractWork(producer: CoordinatorProducer, message: PersistentMessage) {
            if (message.event != KafkaEvents.EVENT_MEDIA_EXTRACT_PARAMETER_CREATED) {
                throw RuntimeException("Incorrect event passed ${message.event}")
            }
            if (message.data !is FfmpegWorkerArgumentsCreated) {
                throw RuntimeException("Invalid data passed:\n${Gson().toJson(message)}")
            }
            val data = message.data as FfmpegWorkerArgumentsCreated
            data.entries.forEach {
                FfmpegWorkRequestCreated(
                    inputFile = data.inputFile,
                    arguments = it.arguments,
                    outFile = it.outputFile
                ).let { createdRequest ->
                    producer.sendMessage(message.referenceId,
                        KafkaEvents.EVENT_WORK_EXTRACT_CREATED,
                        eventId = message.eventId,
                        createdRequest
                    )
                }
                val outFile = File(it.outputFile)
                ConvertWorkerRequest(
                    requiresEventId = message.eventId,
                    inputFile = it.outputFile,
                    true,
                    outFileBaseName = outFile.nameWithoutExtension,
                    outDirectory = outFile.parentFile.absolutePath
                ).let { createdRequest ->
                    producer.sendMessage(message.referenceId, KafkaEvents.EVENT_WORK_CONVERT_CREATED,
                        createdRequest)
                }
            }
        }
    }
}


