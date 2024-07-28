package no.iktdev.mediaprocessing.coordinator.tasksV2.listeners

import mu.KotlinLogging
import no.iktdev.eventi.core.ConsumableEvent
import no.iktdev.eventi.core.WGson
import no.iktdev.eventi.data.EventMetadata
import no.iktdev.eventi.data.EventStatus
import no.iktdev.eventi.data.isSuccessful
import no.iktdev.mediaprocessing.coordinator.CoordinatorEventListener
import no.iktdev.mediaprocessing.coordinator.Coordinator
import no.iktdev.eventi.database.toEpochSeconds
import no.iktdev.mediaprocessing.shared.common.contract.Events
import no.iktdev.mediaprocessing.shared.common.contract.data.BaseInfoEvent
import no.iktdev.mediaprocessing.shared.common.contract.data.Event
import no.iktdev.mediaprocessing.shared.common.contract.data.MediaMetadataReceivedEvent
import no.iktdev.mediaprocessing.shared.common.contract.data.az
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.*

val metadataTimeoutMinutes: Int = System.getenv("METADATA_TIMEOUT")?.toIntOrNull() ?: 10


@Service
@EnableScheduling
class MetadataWaitOrDefaultTaskListener() : CoordinatorEventListener() {

    override fun getProducerName(): String {
        return this::class.java.simpleName
    }

    @Autowired
    override var coordinator: Coordinator? = null

    val log = KotlinLogging.logger {}


    override val produceEvent: Events = Events.EventMediaMetadataSearchPerformed
    override val listensForEvents: List<Events> = listOf(
        Events.EventMediaReadBaseInfoPerformed,
        Events.EventMediaMetadataSearchPerformed
    )


    val metadataTimeout = metadataTimeoutMinutes * 60
    val waitingProcessesForMeta: MutableMap<String, MetadataTriggerData> = mutableMapOf()


    /**
     * This one gets special treatment, since it will only produce a timeout it does not need to use the incoming event
     */
    override fun onEventsReceived(incomingEvent: ConsumableEvent<Event>, events: List<Event>) {
        val hasReadBaseInfo = events.any { it.eventType == Events.EventMediaReadBaseInfoPerformed && it.isSuccessful() }
        val hasMetadataSearched = events.any { it.eventType == Events.EventMediaMetadataSearchPerformed }
        val hasPollerForMetadataEvent = waitingProcessesForMeta.containsKey(incomingEvent.metadata().referenceId)

        if (!hasReadBaseInfo) {
            return
        }

        if (hasPollerForMetadataEvent && hasMetadataSearched) {
            waitingProcessesForMeta.remove(incomingEvent.metadata().referenceId)
            return
        }

        if (!hasMetadataSearched && !hasPollerForMetadataEvent) {
            val consumedIncoming = incomingEvent.consume()
            if (consumedIncoming == null) {
                log.error { "Event is null and should not be available nor provided! ${WGson.gson.toJson(incomingEvent.metadata())}" }
                return
            }

            val baseInfo = events.find { it.eventType ==  Events.EventMediaReadBaseInfoPerformed}?.az<BaseInfoEvent>()?.data
            if (baseInfo == null) {
                log.error { "BaseInfoEvent is null for referenceId: ${consumedIncoming.metadata.referenceId} on eventId: ${consumedIncoming.metadata.eventId}" }
                return
            }

            val estimatedTimeout = LocalDateTime.now().toEpochSeconds() + metadataTimeout
            val dateTime = LocalDateTime.ofEpochSecond(estimatedTimeout, 0, ZoneOffset.UTC)

            val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm", Locale.ENGLISH)

            waitingProcessesForMeta[consumedIncoming.metadata.referenceId] =
                MetadataTriggerData(consumedIncoming.metadata.eventId, LocalDateTime.now())

            log.info { "Sending ${baseInfo.title} to waiting queue. Expiry ${dateTime.format(formatter)}" }
        }
    }


    @Scheduled(fixedDelay = (5_000))
    fun sendErrorMessageForMetadata() {
        val expired = waitingProcessesForMeta.filter {
            LocalDateTime.now().toEpochSeconds() > (it.value.executed.toEpochSeconds() + metadataTimeout)
        }
        expired.forEach {
            log.info { "Producing timeout for ${it.key} ${LocalDateTime.now()}" }
            coordinator?.produceNewEvent(
                MediaMetadataReceivedEvent(
                    metadata = EventMetadata(
                        referenceId = it.key,
                        derivedFromEventId = it.value.eventId,
                        status = EventStatus.Skipped,
                        source = getProducerName()
                    )
                )

            )
            waitingProcessesForMeta.remove(it.key)
        }
        active = expired.isNotEmpty()
    }
    data class MetadataTriggerData(val eventId: String, val executed: LocalDateTime)

}