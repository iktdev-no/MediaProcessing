package no.iktdev.mediaprocessing.coordinator.listeners.events

import no.iktdev.eventi.models.Event
import no.iktdev.eventi.models.SignalEvent
import no.iktdev.mediaprocessing.shared.common.event_task_contract.events.*
import no.iktdev.mediaprocessing.shared.common.getInstanceOf
import no.iktdev.mediaprocessing.shared.common.listeners.PolicyGateEventListener
import no.iktdev.mediaprocessing.shared.database.stores.EventStore
import org.springframework.stereotype.Component

@Component
class PersistContentListener(
    eventStore: no.iktdev.eventi.stores.EventStore = EventStore
) : PolicyGateEventListener(eventStore) {

    override fun onEvent(event: Event, history: List<Event>): Event? {
        if (history.any { it is PersistContentEvent })
            return null
        return super.onEvent(event, history)
    }

    override fun isRequiredPrecursorEventPresent(fullHistory: List<Event>): Boolean {
        return fullHistory.getInstanceOf<ContinuationSummaryEvent>() != null
    }

    override fun isOnHold(signalHistory: List<SignalEvent>): Boolean {
        return signalHistory.lastOrNull() is OnHoldSignalEvent
    }

    override fun handleOnHold(
        event: Event,
        fullHistory: List<Event>,
        signalHistory: List<SignalEvent>
    ): Event? {
        // Eneste måte å slippe hold på er ReleaseHoldSignalEvent
        if (event is ReleaseHoldSignalEvent) {
            return event // allerede riktig type, bare returner
        }
        return null
    }

    override fun allowPassthrough(
        event: Event,
        fullHistory: List<Event>,
        signalHistory: List<SignalEvent>
    ): Boolean {
        val start = fullHistory.getInstanceOf<StartProcessingEvent>() ?: return false
        return start.data.flow == StartFlow.Auto
    }

    override fun handlePolicy(
        event: Event,
        fullHistory: List<Event>,
        signalHistory: List<SignalEvent>
    ): Event? {
        val start = fullHistory.getInstanceOf<StartProcessingEvent>() ?: return null

        return when (start.data.flow) {
            StartFlow.Auto -> null
            StartFlow.Manual -> handleManualFlow(event, fullHistory, signalHistory)
        }
    }

    private fun handleManualFlow(
        event: Event,
        fullHistory: List<Event>,
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

    override fun producePassthroughEvent(
        event: Event,
        fullHistory: List<Event>,
        signalHistory: List<SignalEvent>
    ): Event? {
        if (fullHistory.any { it is PersistContentEvent }) {
            return null
        }
        return PersistContentEvent().derivedOf(event)
    }
}

