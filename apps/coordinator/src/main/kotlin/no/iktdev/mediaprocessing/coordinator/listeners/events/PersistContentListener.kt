package no.iktdev.mediaprocessing.coordinator.listeners.events

import mu.KotlinLogging
import no.iktdev.eventi.models.Event
import no.iktdev.eventi.models.SignalEvent
import no.iktdev.mediaprocessing.shared.common.event_task_contract.events.*
import no.iktdev.mediaprocessing.shared.common.getInstanceOf
import no.iktdev.mediaprocessing.shared.common.getInstancesOf
import no.iktdev.mediaprocessing.shared.common.listeners.PolicyGateEventListener
import no.iktdev.mediaprocessing.shared.common.requireQualifiedEntry
import no.iktdev.mediaprocessing.shared.database.stores.EventStore
import org.springframework.stereotype.Component

@Component
class PersistContentListener(
    eventStore: no.iktdev.eventi.stores.EventStore = EventStore
) : PolicyGateEventListener(eventStore) {
    private val log = KotlinLogging.logger {}


    override fun onEvent(event: Event, history: List<Event>): Event? {
        event.requireQualifiedEntry<ContinuationSummaryEvent>()

        val relevantSignals = history.getInstancesOf<SignalEvent>()
            .filter { it::class in listOf(OnHoldSignalEvent::class, ReleaseHoldSignalEvent::class)
            }

        val lastSignal = relevantSignals.lastOrNull()
        if (lastSignal == null) {
            log.info { "No signals found ${event.referenceId}, processing using default implementation" }
            return super.onEvent(event, history)
        }

        if (lastSignal is OnHoldSignalEvent) {
            log.debug("{} is onHolding {}", event.referenceId, lastSignal.referenceId)
            return null
        }

        if (lastSignal is ReleaseHoldSignalEvent) {
            log.debug("{} is onRelease {}", event.referenceId, lastSignal.referenceId)
            return super.onEvent(event, history)
        }

        return null
    }

    override fun isRequiredPrecursorEventPresent(history: List<Event>): Boolean {
        val precursorIsPresent =  history.getInstanceOf<ContinuationSummaryEvent>() != null
        if (!precursorIsPresent) {
            log.warn { "${history.single().referenceId} Precursor is missing!" }
        }
        return precursorIsPresent
    }

    override fun hasPassed(history: List<Event>): Boolean {
        return history.any { it is PersistContentEvent }
    }

    override fun handlePolicy(
        event: Event,
        history: List<Event>,
        signalHistory: List<SignalEvent>
    ): Event? {
        val start = history.getInstanceOf<StartProcessingEvent>() ?: run {
            log.error { "${event.referenceId} Does not contain a StartProcessingEvent" }
            return null
        }

        return when (start.data.flow) {
            StartFlow.Auto -> handleAutoFlow(event, history)
            StartFlow.Manual -> handleManualFlow(event, signalHistory)
        }
    }

    private fun handleManualFlow(
        event: Event,
        signalHistory: List<SignalEvent>
    ): Event? {

        val lastSignal = signalHistory.lastOrNull()

        // 1. Hvis vi nettopp slapp hold → passthrough
        if (lastSignal is ReleaseHoldSignalEvent) {
            return PersistContentEvent().derivedOf(event)
        }

        // 2. Hvis vi aldri har vært på hold → sett hold
        if (lastSignal !is OnHoldSignalEvent) {
            return OnHoldSignalEvent("Manual flow").derivedOf(event)
        }

        // 3. Hvis vi er på hold → handleOnHold() vil ta seg av ReleaseHold
        log.debug { "${event.referenceId} is onManual flow received, but no signal received or handled" }
        return null
    }

    private fun handleAutoFlow(event: Event, history: List<Event>): Event {
        return PersistContentEvent().derivedOf(event)
    }
}

