package no.iktdev.mediaprocessing.coordinator.listeners.events

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

    override fun onEvent(event: Event, history: List<Event>): Event? {
        event.requireQualifiedEntry<ContinuationSummaryEvent>()

        val relevantSignals = history.getInstancesOf<SignalEvent>()
            .filter { it::class in listOf(OnHoldSignalEvent::class, ReleaseHoldSignalEvent::class)  }

        val lastSignal = relevantSignals.lastOrNull() ?: return super.onEvent(event, history)

        if (lastSignal is OnHoldSignalEvent) {
            return null
        }

        if (lastSignal is ReleaseHoldSignalEvent) {
            return super.onEvent(event, history)
        }

        return null
    }

    override fun isRequiredPrecursorEventPresent(history: List<Event>): Boolean {
        return history.getInstanceOf<ContinuationSummaryEvent>() != null
    }

    override fun isOnHold(signalHistory: List<SignalEvent>): Boolean {
        return signalHistory.lastOrNull() is OnHoldSignalEvent
    }

    override fun hasPassed(history: List<Event>): Boolean {
        return history.any { it is PersistContentEvent }
    }

    override fun handlePolicy(
        event: Event,
        history: List<Event>,
        signalHistory: List<SignalEvent>
    ): Event? {
        val start = history.getInstanceOf<StartProcessingEvent>() ?: return null

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
        return null
    }

    private fun handleAutoFlow(event: Event, history: List<Event>): Event {
        return PersistContentEvent().derivedOf(event)
    }
}

