package no.iktdev.mediaprocessing.converter.tasks

import com.google.gson.Gson
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import no.iktdev.mediaprocessing.converter.ConverterCoordinator
import no.iktdev.mediaprocessing.converter.convert.Converter
import no.iktdev.mediaprocessing.converter.flow.ProcesserTaskCreator
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
class ConvertService(@Autowired override var coordinator: ConverterCoordinator) : ProcesserTaskCreator(coordinator) {
    private val log = KotlinLogging.logger {}
    val serviceId = "${getComputername()}::${this.javaClass.simpleName}::${UUID.randomUUID()}"

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
        if (event.data !is ConvertWorkerRequest)
            return null
        log.info { Gson().toJson(event) }

        val isAlreadyClaimed = PersistentDataReader().isProcessEventAlreadyClaimed(referenceId = event.referenceId, eventId = event.eventId)
        if (isAlreadyClaimed) {
            log.warn {  "Process is already claimed!" }
            return null
        }


        val payload = event.data as ConvertWorkerRequest
        val requiresEventId: String? = payload.requiresEventId

        val awaitingFor: PersistentProcessDataMessage? = if (requiresEventId != null) {
            val existing = getRequiredExtractProcessForContinuation(referenceId = event.referenceId, requiresEventId = requiresEventId)
            if (existing == null) {
                skipConvertEvent(event, requiresEventId)
                return null
            }
            existing
        } else null

        val converter = if (requiresEventId.isNullOrBlank() || canConvert(awaitingFor)) {
            Converter(referenceId = event.referenceId, eventId = event.eventId, data = payload)
        } else null


        val setClaim = PersistentDataStore().setProcessEventClaim(referenceId = event.referenceId, eventId = event.eventId, claimedBy = serviceId)
        if (!setClaim) {
            return null
        }

        if (converter == null || !converter.canRead()) {
            // Make claim regardless but push to schedule
            return null
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

    fun skipConvertEvent(event: PersistentProcessDataMessage, requiresEventId: String) {
        if (event.event == KafkaEvents.EVENT_WORK_CONVERT_CREATED)
            return
        val producesPayload = SimpleMessageData(status = Status.COMPLETED, message = "Convert event contains a payload stating that it waits for eventId: $requiresEventId with referenceId: ${event.referenceId}")
        coordinator.producer.sendMessage(
            referenceId = event.referenceId,
            event = KafkaEvents.EVENT_WORK_CONVERT_SKIPPED,
            data = producesPayload
        )
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