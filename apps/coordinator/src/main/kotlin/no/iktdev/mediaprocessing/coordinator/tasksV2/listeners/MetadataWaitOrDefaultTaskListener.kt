package no.iktdev.mediaprocessing.coordinator.tasksV2.listeners

import mu.KotlinLogging
import no.iktdev.eventi.data.EventMetadata
import no.iktdev.eventi.data.EventStatus
import no.iktdev.mediaprocessing.coordinator.CoordinatorEventListener
import no.iktdev.mediaprocessing.coordinator.Coordinator
import no.iktdev.mediaprocessing.shared.common.datasource.toEpochSeconds
import no.iktdev.mediaprocessing.shared.contract.Events
import no.iktdev.mediaprocessing.shared.contract.data.BaseInfoEvent
import no.iktdev.mediaprocessing.shared.contract.data.Event
import no.iktdev.mediaprocessing.shared.contract.data.MediaMetadataReceivedEvent
import no.iktdev.mediaprocessing.shared.contract.data.az
import no.iktdev.mediaprocessing.shared.kafka.core.KafkaEnv
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.*

@Service
@EnableScheduling
class MetadataWaitOrDefaultTaskListener() : CoordinatorEventListener() {
    @Autowired
    override var coordinator: Coordinator? = null

    val log = KotlinLogging.logger {}


    override val produceEvent: Events = Events.EventMediaMetadataSearchPerformed
    override val listensForEvents: List<Events> = listOf(
        Events.EventMediaReadBaseInfoPerformed,
        Events.EventMediaMetadataSearchPerformed
    )


    val metadataTimeout = KafkaEnv.metadataTimeoutMinutes * 60
    val waitingProcessesForMeta: MutableMap<String, MetadataTriggerData> = mutableMapOf()


    override fun onEventsReceived(incomingEvent: Event, events: List<Event>) {
        if (incomingEvent.eventType == Events.EventMediaReadBaseInfoPerformed &&
            events.none { it.eventType ==  Events.EventMediaMetadataSearchPerformed }) {
            val baseInfo = incomingEvent.az<BaseInfoEvent>()?.data
            if (baseInfo == null) {
                log.error { "BaseInfoEvent is null for referenceId: ${incomingEvent.metadata.referenceId} on eventId: ${incomingEvent.metadata.eventId}" }
                return
            }

            val estimatedTimeout = LocalDateTime.now().toEpochSeconds() + metadataTimeout
            val dateTime = LocalDateTime.ofEpochSecond(estimatedTimeout, 0, ZoneOffset.UTC)

            val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm", Locale.ENGLISH)
            log.info { "Sending ${baseInfo.title} to waiting queue. Expiry ${dateTime.format(formatter)}" }
            if (!waitingProcessesForMeta.containsKey(incomingEvent.metadata.referenceId)) {
                waitingProcessesForMeta[incomingEvent.metadata.referenceId] =
                    MetadataTriggerData(incomingEvent.metadata.eventId, LocalDateTime.now())
            }
        }

        if (incomingEvent.eventType == Events.EventMediaMetadataSearchPerformed) {
            if (waitingProcessesForMeta.containsKey(incomingEvent.metadata.referenceId)) {
                waitingProcessesForMeta.remove(incomingEvent.metadata.referenceId)
            }
        }
    }


    @Scheduled(fixedDelay = (1_000))
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
                        status = EventStatus.Skipped
                    )
                )

            )
            waitingProcessesForMeta.remove(it.key)
        }
    }
    data class MetadataTriggerData(val eventId: String, val executed: LocalDateTime)

}