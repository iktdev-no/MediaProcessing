package no.iktdev.mediaprocessing.coordinator

import com.google.gson.Gson
import kotlinx.coroutines.launch
import mu.KotlinLogging
import no.iktdev.exfl.coroutines.Coroutines
import no.iktdev.mediaprocessing.shared.common.SharedConfig
import no.iktdev.mediaprocessing.shared.common.kafka.CoordinatorProducer
import no.iktdev.mediaprocessing.shared.common.persistance.PersistentDataReader
import no.iktdev.mediaprocessing.shared.common.persistance.PersistentDataStore
import no.iktdev.mediaprocessing.shared.common.persistance.PersistentMessage
import no.iktdev.mediaprocessing.shared.contract.ProcessType
import no.iktdev.mediaprocessing.shared.kafka.core.KafkaEvents
import no.iktdev.mediaprocessing.shared.kafka.core.DefaultMessageListener
import no.iktdev.mediaprocessing.shared.kafka.dto.events_result.*
import no.iktdev.mediaprocessing.shared.kafka.dto.isSuccess
import no.iktdev.streamit.library.kafka.dto.Status
import org.springframework.stereotype.Service
import java.io.File
import java.util.UUID

@Service
class Coordinator() {
    val producer = CoordinatorProducer()
    private val log = KotlinLogging.logger {}


    private val listeners: MutableList<TaskCreatorListener> = mutableListOf()
    fun addListener(listener: TaskCreatorListener) {
        listeners.add(listener)
    }


    public fun startProcess(file: File, type: ProcessType) {
        val processStartEvent = ProcessStarted(
            status = Status.STARTED,
            file = file.absolutePath,
            type = type
        )
        producer.sendMessage(UUID.randomUUID().toString(), KafkaEvents.EVENT_PROCESS_STARTED, processStartEvent)
    }

    fun produceEncodeWork(message: PersistentMessage) {
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
                    createdRequest)
            }
        }
    }

    fun produceExtractWork(message: PersistentMessage) {
        if (message.event != KafkaEvents.EVENT_MEDIA_EXTRACT_PARAMETER_CREATED) {
            throw RuntimeException("Incorrect event passed ${message.event}")
        }
        if (message.data !is FfmpegWorkerArgumentsCreated) {
            throw RuntimeException("Invalid data passed:\n${Gson().toJson(message)}")
        }
        val data = message.data as FfmpegWorkerArgumentsCreated
        data.entries.forEach {
            val eventId = UUID.randomUUID().toString()
            FfmpegWorkRequestCreated(
                inputFile = data.inputFile,
                arguments = it.arguments,
                outFile = it.outputFile
            ).let { createdRequest ->
                producer.sendMessage(message.eventId,
                    KafkaEvents.EVENT_WORK_EXTRACT_CREATED,
                    eventId,
                    createdRequest)
            }
            val outFile = File(it.outputFile)
            ConvertWorkerRequest(
                requiresEventId = eventId,
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


    val io = Coroutines.io()
    private val listener = DefaultMessageListener(SharedConfig.kafkaTopic) { event ->
        val success = PersistentDataStore().storeMessage(event.key.event, event.value)
        if (!success) {
            log.error { "Unable to store message: ${event.key.event} in database!" }
        } else
            readAllMessagesFor(event.value.referenceId, event.value.eventId)
    }

    fun readAllMessagesFor(referenceId: String, eventId: String) {
        io.launch {
            val messages = PersistentDataReader().getMessagesFor(referenceId)
            createTasksBasedOnEventsAndPersistance(referenceId, eventId, messages)
            buildModelBasedOnMessagesFor(referenceId, messages)
        }
    }

    suspend fun buildModelBasedOnMessagesFor(referenceId: String, messages: List<PersistentMessage>) {
        if (messages.any { it.data is ProcessCompleted }) {
            // TODO: Build and insert into database
        }
    }

    fun createTasksBasedOnEventsAndPersistance(referenceId: String, eventId: String, messages: List<PersistentMessage>) {
        io.launch {
            val triggered = messages.find { it.eventId == eventId } ?: return@launch
            listeners.forEach { it.onEventReceived(referenceId, triggered, messages) }
            if (listOf(KafkaEvents.EVENT_MEDIA_ENCODE_PARAMETER_CREATED, KafkaEvents.EVENT_MEDIA_EXTRACT_PARAMETER_CREATED).contains(triggered.event) && triggered.data.isSuccess()) {
                val processStarted = messages.find { it.event == KafkaEvents.EVENT_PROCESS_STARTED }?.data as ProcessStarted
                if (processStarted.type == ProcessType.FLOW) {
                    log.info { "Process for $referenceId was started from flow and will be processed" }
                    if (triggered.event == KafkaEvents.EVENT_MEDIA_ENCODE_PARAMETER_CREATED) {
                        produceEncodeWork(triggered)
                    } else if (triggered.event == KafkaEvents.EVENT_MEDIA_EXTRACT_PARAMETER_CREATED) {
                        produceExtractWork(triggered)
                    }
                } else {
                    log.info { "Process for $referenceId was started manually and will require user input for continuation" }
                }
            }
        }
    }



    init {
        io.launch { listener.listen() }
    }
}


abstract class TaskCreator: TaskCreatorListener {
    val producer = CoordinatorProducer()
    open fun isPrerequisitesOk(events: List<PersistentMessage>): Boolean {
        return true
    }
}

interface TaskCreatorListener {
    fun onEventReceived(referenceId: String, event: PersistentMessage,  events: List<PersistentMessage>): Unit
}