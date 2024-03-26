package no.iktdev.mediaprocessing.converter.tasks

import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import no.iktdev.mediaprocessing.converter.ConverterCoordinator
import no.iktdev.mediaprocessing.converter.TaskCreator
import no.iktdev.mediaprocessing.converter.convert.Converter
import no.iktdev.mediaprocessing.shared.common.getComputername
import no.iktdev.mediaprocessing.shared.common.persistance.PersistentDataReader
import no.iktdev.mediaprocessing.shared.common.persistance.PersistentDataStore
import no.iktdev.mediaprocessing.shared.common.persistance.PersistentProcessDataMessage
import no.iktdev.mediaprocessing.shared.kafka.core.KafkaEvents
import no.iktdev.mediaprocessing.shared.kafka.dto.MessageDataWrapper
import no.iktdev.mediaprocessing.shared.kafka.dto.SimpleMessageData
import no.iktdev.mediaprocessing.shared.kafka.dto.events_result.ConvertWorkPerformed
import no.iktdev.mediaprocessing.shared.kafka.dto.events_result.ConvertWorkerRequest
import no.iktdev.mediaprocessing.shared.kafka.dto.isSuccess
import no.iktdev.mediaprocessing.shared.kafka.dto.Status
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.util.*


@Service
class ConvertService(@Autowired override var coordinator: ConverterCoordinator) : TaskCreator(coordinator) {
    private val log = KotlinLogging.logger {}
    val serviceId = "${getComputername()}::${this.javaClass.simpleName}::${UUID.randomUUID()}"

    init {
        log.info { "Starting with id: $serviceId" }
    }

    override val listensForEvents: List<KafkaEvents>
        get() = listOf(
            KafkaEvents.EVENT_WORK_EXTRACT_PERFORMED,
            KafkaEvents.EVENT_WORK_CONVERT_CREATED
        )
    override val producesEvent: KafkaEvents
        get() = KafkaEvents.EVENT_WORK_CONVERT_PERFORMED


    fun getRequiredExtractProcessForContinuation(referenceId: String, requiresEventId: String): PersistentProcessDataMessage? {
        return PersistentDataReader().getProcessEvent(referenceId, requiresEventId)
    }
    fun canConvert(extract: PersistentProcessDataMessage?): Boolean {
        return extract?.consumed == true && extract.data.isSuccess()
    }


    override fun onProcessEvents(
        event: PersistentProcessDataMessage,
        events: List<PersistentProcessDataMessage>
    ): MessageDataWrapper? {
        val convertEvent = events.find { it.event == KafkaEvents.EVENT_WORK_CONVERT_CREATED && it.data is ConvertWorkerRequest }
        if (convertEvent == null) {
            // No convert here..
            return null
        }
        val convertRequest = convertEvent.data as ConvertWorkerRequest? ?: return null
        val requiredEventId = convertRequest.requiresEventId
        if (requiredEventId != null) {
            // Requires the eventId to be defined as consumed
            val requiredEventToBeCompleted =
                getRequiredExtractProcessForContinuation(referenceId = event.referenceId, requiresEventId = requiredEventId)
                    ?: return SimpleMessageData(Status.SKIPPED, "Required event: $requiredEventId is not found. Skipping convert work for referenceId: ${event.referenceId}")
            if (!canConvert(requiredEventToBeCompleted)) {
                // Waiting for required event to be completed
                return null
            }
        }

        val isAlreadyClaimed = PersistentDataReader().isProcessEventAlreadyClaimed(referenceId = event.referenceId, eventId = event.eventId)
        if (isAlreadyClaimed) {
            log.warn {  "Process is already claimed!" }
            return null
        }

        val setClaim = PersistentDataStore().setProcessEventClaim(referenceId = event.referenceId, eventId = event.eventId, claimedBy = serviceId)
        if (!setClaim) {
            return null
        }

        val payload = event.data as ConvertWorkerRequest
        val converter = Converter(referenceId = event.referenceId, eventId = event.eventId, data = payload)
        if (!converter.canRead()) {
            // Make claim regardless but push to schedule
            return SimpleMessageData(Status.ERROR, "Can't read the file..")
        }

        val result = try {
            performConvert(converter)
        } catch (e: Exception) {
            SimpleMessageData(status = Status.ERROR, message = e.message)
        }

        val consumedIsSuccessful = PersistentDataStore().setProcessEventCompleted(event.referenceId, event.eventId, serviceId)
        runBlocking {
            delay(1000)
            if (!consumedIsSuccessful) {
                PersistentDataStore().setProcessEventCompleted(event.referenceId, event.eventId, serviceId)
            }
            delay(1000)
            var readbackIsSuccess = PersistentDataReader().isProcessEventDefinedAsConsumed(event.referenceId, event.eventId, serviceId)

            while (!readbackIsSuccess) {
                delay(1000)
                readbackIsSuccess = PersistentDataReader().isProcessEventDefinedAsConsumed(event.referenceId, event.eventId, serviceId)
            }
        }
        return result
    }


    fun performConvert(converter: Converter): ConvertWorkPerformed {
        return try {
            val result = converter.execute()
            ConvertWorkPerformed(
                status = Status.COMPLETED,
                producedBy = serviceId,
                derivedFromEventId = converter.eventId,
                outFiles = result.map { it.absolutePath }
            )
        } catch (e: Converter.FileUnavailableException) {
            e.printStackTrace()
            ConvertWorkPerformed(
                status = Status.ERROR,
                message = e.message,
                producedBy = serviceId,
                derivedFromEventId = converter.eventId,
                outFiles = emptyList()
            )
        } catch (e : Converter.FileIsNullOrEmpty) {
            e.printStackTrace()
            ConvertWorkPerformed(
                status = Status.ERROR,
                message = e.message,
                producedBy = serviceId,
                derivedFromEventId = converter.eventId,
                outFiles = emptyList()
            )
        }
    }
}