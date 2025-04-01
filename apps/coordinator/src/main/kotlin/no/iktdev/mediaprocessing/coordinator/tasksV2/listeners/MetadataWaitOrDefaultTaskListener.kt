package no.iktdev.mediaprocessing.coordinator.tasksV2.listeners

import kotlinx.coroutines.*
import mu.KotlinLogging
import no.iktdev.eventi.core.ConsumableEvent
import no.iktdev.eventi.core.WGson
import no.iktdev.eventi.data.*
import no.iktdev.mediaprocessing.coordinator.CoordinatorEventListener
import no.iktdev.mediaprocessing.coordinator.Coordinator
import no.iktdev.eventi.database.toEpochSeconds
import no.iktdev.mediaprocessing.shared.common.contract.Events
import no.iktdev.mediaprocessing.shared.common.contract.data.*
import no.iktdev.mediaprocessing.shared.common.contract.lastOrSuccessOf
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import java.time.Duration
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean

val metadataTimeoutMinutes: Int = System.getenv("METADATA_TIMEOUT")?.toIntOrNull() ?: 0


@Service
class MetadataWaitOrDefaultTaskListener() : CoordinatorEventListener() {

    override fun getProducerName(): String {
        return this::class.java.simpleName
    }

    @Autowired
    override var coordinator: Coordinator? = null

    val log = KotlinLogging.logger {}


    override val produceEvent: Events = Events.MetadataSearchPerformed
    override val listensForEvents: List<Events> = listOf(
        Events.BaseInfoRead,
        Events.MetadataSearchPerformed,
        Events.ProcessCompleted
    )


    val metadataTimeout = metadataTimeoutMinutes * 60

    private val timeoutScope = CoroutineScope(Dispatchers.Default)
    val timeoutJobs = ConcurrentHashMap<String, Job>()

    override fun shouldIHandleFailedEvents(incomingEvent: Event): Boolean {
        return true
    }

    /**
     * This one gets special treatment, since it will only produce a timeout it does not need to use the incoming event
     */
    override fun onEventsReceived(incomingEvent: ConsumableEvent<Event>, events: List<Event>) {
        if (metadataTimeoutMinutes <= 0) {
            return
        }

        val baseInfo = events.findFirstEventOf<BaseInfoEvent>()

        if (baseInfo?.isSuccessful() != true) {
            return
        }

        val digestEvent = incomingEvent.consume() ?: return
        if (digestEvent.eventType == Events.BaseInfoRead) {
            if (!timeoutJobs.containsKey(digestEvent.referenceId())) {
                timeoutScope.launch {
                    val expiryTime = (Instant.now().epochSecond + metadataTimeout)
                    val dateTime = LocalDateTime.ofEpochSecond(expiryTime, 0, ZoneOffset.UTC)
                    val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm", Locale.ENGLISH)
                    log.info { "Sending ${baseInfo.data?.title} to waiting queue. Expiry ${dateTime.format(formatter)}" }
                    delay(Duration.ofSeconds(metadataTimeout.toLong()).toMillis())
                    coordinator!!.produceNewEvent(
                        MediaMetadataReceivedEvent(
                            metadata = EventMetadata(
                                referenceId = digestEvent.referenceId(),
                                derivedFromEventId = digestEvent.eventId(),
                                status = EventStatus.Skipped,
                                source = getProducerName()
                            )
                        )

                    )
                }.also {
                    timeoutJobs[digestEvent.referenceId()] = it
                }
            } else {
                log.error { "Timeout for ${digestEvent.referenceId()} has already been set!" }
            }
        } else {
            timeoutJobs.remove(digestEvent.referenceId())?.cancel()
        }
    }

}