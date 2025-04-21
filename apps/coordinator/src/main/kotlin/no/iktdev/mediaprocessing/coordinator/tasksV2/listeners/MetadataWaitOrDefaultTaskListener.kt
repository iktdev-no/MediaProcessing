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

val metadataTimeoutMinutes: Int = System.getenv("METADATA_TIMEOUT")?.toIntOrNull() ?: -1


@Service
class MetadataWaitOrDefaultTaskListener() : CoordinatorEventListener() {

    override fun onReady() {
        super.onReady()
        if (metadataTimeoutMinutes == 0) {
            log.warn { "Metadata timeout is set to 0 minutes.. This will block proceeding until metadata is found.." }
        }
    }

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


    override fun shouldIProcessAndHandleEvent(incomingEvent: Event, events: List<Event>): Boolean {
        if (!isOfEventsIListenFor(incomingEvent))
            return false

        val childOf = events.filter { it.derivedFromEventId() == incomingEvent.eventId() }
        val haveListenerProduced = childOf.any { it.eventType == produceEvent }
        if (haveListenerProduced)
            return false

        val metadataEvent = events.findEventOf<MediaMetadataReceivedEvent>()
        val metadataSource = metadataEvent?.metadata?.source

        if (events.any { it.eventType == produceEvent } && !canProduceMultipleEvents() && metadataSource == getProducerName()) {
            return false
        }

        if (!havProducedDerivedEventOnIncomingEvent(incomingEvent, events) && canProduceMultipleEvents()) {
            return true
        }

        if (haveProducedExpectedMessageBasedOnEvent(incomingEvent, events))
            return false

        return (events.any { it.eventType == Events.BaseInfoRead })
    }

    /**
     * This one gets special treatment, since it will only produce a timeout it does not need to use the incoming event
     */
    override fun onEventsReceived(incomingEvent: ConsumableEvent<Event>, events: List<Event>) {
        if (metadataTimeoutMinutes <= -1) {
            log.info { "Metadata has no timeout, a timeout will be created.." }
            val meta = incomingEvent.metadata()
            onProduceEvent(MediaMetadataReceivedEvent(
                metadata = meta.copy(
                    status = EventStatus.Failed,
                    source = getProducerName()
                ),
                data = null
            ))
            return
        }


        val searchPerformedEvent: MediaMetadataReceivedEvent? = events.findEventOf<MediaMetadataReceivedEvent>()

        if (searchPerformedEvent != null) {
            if (timeoutJobs.containsKey(searchPerformedEvent.referenceId())) {
                val job = timeoutJobs.remove(searchPerformedEvent.referenceId())
                job?.cancel()
            }
        }


        val baseInfo = events.findFirstEventOf<BaseInfoEvent>()
        if (baseInfo?.isSuccessful() != true) {
            return
        }

        if (incomingEvent.isOfEvent(Events.BaseInfoRead)) {
            if (timeoutJobs.containsKey(incomingEvent.metadata().referenceId))
                return
            val ttsc = timeoutScope.launch {
                createTimeout(incomingEvent.metadata().referenceId, incomingEvent.metadata().eventId, baseInfo)
            }
            timeoutJobs[incomingEvent.metadata().referenceId] = ttsc
        }
    }

    override fun produceFailure(incomingEvent: Event) {
        onProduceEvent(MediaMetadataReceivedEvent(
            metadata = incomingEvent.makeDerivedEventInfo(EventStatus.Failed, getProducerName()),
            data = null
        ))
    }

    suspend fun createTimeout(referenceId: String, eventId: String, baseInfo: BaseInfoEvent) {
        val expiryTime = (Instant.now().epochSecond + metadataTimeout)
        val dateTime = LocalDateTime.ofEpochSecond(expiryTime, 0, ZoneOffset.UTC)
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm", Locale.ENGLISH)
        log.info { "Sending ${baseInfo.data?.title} to waiting queue. Expiry ${dateTime.format(formatter)}" }
        delay(Duration.ofSeconds(metadataTimeout.toLong()).toMillis())
        if (!this.isActive()) {
            return
        }
        coordinator!!.produceNewEvent(
            MediaMetadataReceivedEvent(
                metadata = EventMetadata(
                    referenceId = referenceId,
                    derivedFromEventId = eventId,
                    status = EventStatus.Skipped,
                    source = getProducerName()
                )
            )

        )
    }

}