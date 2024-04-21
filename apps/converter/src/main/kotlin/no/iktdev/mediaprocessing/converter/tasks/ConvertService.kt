package no.iktdev.mediaprocessing.converter.tasks

import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import no.iktdev.mediaprocessing.converter.*
import no.iktdev.mediaprocessing.converter.convert.Converter
import no.iktdev.mediaprocessing.shared.common.getComputername
import no.iktdev.mediaprocessing.shared.common.persistance.PersistentProcessDataMessage
import no.iktdev.mediaprocessing.shared.kafka.core.KafkaEvents
import no.iktdev.mediaprocessing.shared.common.helper.DerivedProcessIterationHolder
import no.iktdev.mediaprocessing.shared.kafka.dto.MessageDataWrapper
import no.iktdev.mediaprocessing.shared.kafka.dto.SimpleMessageData
import no.iktdev.mediaprocessing.shared.kafka.dto.events_result.ConvertWorkPerformed
import no.iktdev.mediaprocessing.shared.kafka.dto.events_result.ConvertWorkerRequest
import no.iktdev.mediaprocessing.shared.kafka.dto.isSuccess
import no.iktdev.mediaprocessing.shared.kafka.dto.Status
import no.iktdev.mediaprocessing.shared.kafka.dto.events_result.work.ProcesserExtractWorkPerformed
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import java.util.*


@EnableScheduling
@Service
class ConvertService(@Autowired override var coordinator: ConverterCoordinator) : TaskCreator(coordinator) {
    private val log = KotlinLogging.logger {}
    val serviceId = "${getComputername()}::${this.javaClass.simpleName}::${UUID.randomUUID()}"

    init {
        log.info { "Starting with id: $serviceId" }
    }

    override val listensForEvents: List<KafkaEvents>
        get() = listOf(
            KafkaEvents.EventWorkExtractPerformed,
            KafkaEvents.EventWorkConvertCreated
        )
    override val producesEvent: KafkaEvents
        get() = KafkaEvents.EventWorkConvertPerformed


    fun getRequiredExtractProcessForContinuation(
        referenceId: String,
        requiresEventId: String
    ): PersistentProcessDataMessage? {
        return eventManager.getProcessEventWith(referenceId, requiresEventId)
    }

    fun canConvert(extract: PersistentProcessDataMessage?): Boolean {
        return extract?.consumed == true && extract.data.isSuccess()
    }


    override fun onProcessEvents(
        event: PersistentProcessDataMessage,
        events: List<PersistentProcessDataMessage>
    ): MessageDataWrapper? {

        val waitsForEventId = if (event.event == KafkaEvents.EventWorkConvertCreated) {
            // Do convert check
            val convertRequest = event.data as ConvertWorkerRequest? ?: return null
            convertRequest.requiresEventId

        } else if (event.event == KafkaEvents.EventWorkExtractPerformed) {
            if (event.data is ProcesserExtractWorkPerformed) event.data.derivedFromEventId else return null
        } else null

        val convertData =  if (event.event == KafkaEvents.EventWorkConvertCreated) {
            event.data as ConvertWorkerRequest? ?: return null
        } else {
            val convertEvent = events.find { it.referenceId == event.referenceId && it.event == KafkaEvents.EventWorkConvertCreated } ?: return null
            convertEvent.data as ConvertWorkerRequest? ?: return null
        }




        if (waitsForEventId != null) {
            // Requires the eventId to be defined as consumed
            val requiredEventToBeCompleted = getRequiredExtractProcessForContinuation(
                referenceId = event.referenceId,
                requiresEventId = waitsForEventId
            )
            if (requiredEventToBeCompleted == null) {
                /*log.info { "Sending ${event.eventId} @ ${event.referenceId} to deferred check" }
                val existing = scheduled_deferred_events[event.referenceId]
                val newList = (existing ?: listOf()) + listOf(
                    DerivedProcessIterationHolder(
                        eventId = event.eventId,
                        event = convertEvent
                    )
                )
                scheduled_deferred_events[event.referenceId] = newList*/

                return null
            }
            if (!canConvert(requiredEventToBeCompleted)) {
                // Waiting for required event to be completed
                return null
            }
        }

        val isAlreadyClaimed = eventManager.isProcessEventClaimed(referenceId = event.referenceId, eventId = event.eventId)
        if (isAlreadyClaimed) {
            log.warn { "Process is already claimed!" }
            return null
        }

        val setClaim = eventManager.setProcessEventClaim(
            referenceId = event.referenceId,
            eventId = event.eventId,
            claimer = serviceId
        )
        if (!setClaim) {
            return null
        }

        val converter = Converter(referenceId = event.referenceId, eventId = event.eventId, data = convertData)
        if (!converter.canRead()) {
            // Make claim regardless but push to schedule
            return ConvertWorkPerformed(
                status = Status.ERROR,
                message = "Can't read the file..",
                derivedFromEventId = converter.eventId,
                producedBy = serviceId
            )
        }

        val result = try {
            performConvert(converter)
        } catch (e: Exception) {
            ConvertWorkPerformed(
                status = Status.ERROR, message = e.message,
                derivedFromEventId = converter.eventId,
                producedBy = serviceId
            )
        }

        val consumedIsSuccessful =
            eventManager.setProcessEventCompleted(event.referenceId, event.eventId)
        runBlocking {
            delay(1000)
            if (!consumedIsSuccessful) {
                eventManager.setProcessEventCompleted(event.referenceId, event.eventId)
            }
            delay(1000)
            var readbackIsSuccess = eventManager.isProcessEventCompleted(event.referenceId, event.eventId)

            while (!readbackIsSuccess) {
                delay(1000)
                readbackIsSuccess =
                    eventManager.isProcessEventCompleted(event.referenceId, event.eventId)
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
        } catch (e: Converter.FileIsNullOrEmpty) {
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


    /*val scheduled_deferred_events: MutableMap<String, List<DerivedProcessIterationHolder>> = mutableMapOf()
    @Scheduled(fixedDelay = (300_000))
    fun validatePresenceOfRequiredEvent() {
        val continueDeferral: MutableMap<String, List<DerivedProcessIterationHolder>> = mutableMapOf()

        for ((referenceId, eventList) in scheduled_deferred_events) {
            val keepable = mutableListOf<DerivedProcessIterationHolder>()
            for (event in eventList) {
                val ce = if (event.event.data is ConvertWorkerRequest) event.event.data as ConvertWorkerRequest else null
                try {
                    val requiredEventToBeCompleted = getRequiredExtractProcessForContinuation(
                        referenceId = referenceId,
                        requiresEventId = ce?.requiresEventId!!
                    )
                    if (requiredEventToBeCompleted == null && event.iterated > 4) {
                        throw RuntimeException("Iterated overshot")
                    } else {
                        event.iterated++
                        keepable.add(event)
                        "Iteration ${event.iterated} for event ${event.eventId} in deferred check"
                    }

                } catch (e: Exception) {
                    eventManager.setProcessEventCompleted(referenceId, event.eventId, Status.SKIPPED)
                    log.error { "Canceling event ${event.eventId}\n\t by declaring it as consumed." }
                    producer.sendMessage(
                        referenceId = referenceId,
                        event = producesEvent,
                        data = SimpleMessageData(Status.SKIPPED, "Required event: ${ce?.requiresEventId} is not found. Skipping convert work for referenceId: ${referenceId}", derivedFromEventId = event.eventId)
                    )
                }
            }
            continueDeferral[referenceId] = keepable
        }

        scheduled_deferred_events.clear()
        scheduled_deferred_events.putAll(continueDeferral)

    }*/

}