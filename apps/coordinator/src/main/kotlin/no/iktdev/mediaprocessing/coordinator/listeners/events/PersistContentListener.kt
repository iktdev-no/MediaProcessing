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
        val shorthand = event.referenceId.toString().split("-").first()

        // 1. Logg innkommende event
        log.warn("[$shorthand] PERSIST DEBUG >>> Incoming event: ${event::class.simpleName} " +
                "eventId=${event.eventId} ref=${event.referenceId} createdAt=${event.metadata.created} " +
                "derivedFrom=${event.metadata.derivedFromId}")

        // 2. Dump hele historikken
        var loggHistory = "[$shorthand] PERSIST DEBUG >>> History dump (${history.size} events) for ref=${event.referenceId}:\n"
        history.forEachIndexed { idx, e ->
            loggHistory += "\t[$idx] ${e::class.simpleName} eventId=${e.eventId} ref=${e.referenceId} " +
                    "createdAt=${e.metadata.created} derivedFrom=${e.metadata.derivedFromId}\n"
        }
        log.warn(loggHistory)

        // 3. Entry check
        try {
            event.requireQualifiedEntry<ContinuationSummaryEvent>()
            log.warn("[$shorthand] PERSIST DEBUG >>> Passed requireQualifiedEntry")
        } catch (e: Exception) {
            log.error("[$shorthand] PERSIST DEBUG >>> requireQualifiedEntry FAILED: ${e.message}")
            throw e
        }

        // 4. Finn ALLE signaler
        val allSignals = history.filterIsInstance<SignalEvent>()
        val loggBlock = StringBuilder()
        loggBlock.append("[$shorthand] PERSIST DEBUG >>> All signals (${allSignals.size}):\n")
        allSignals.forEach {
            loggBlock.append("\t- ${it::class.simpleName} eventId=${it.eventId} createdAt=${it.metadata.created}\n")
        }
        log.warn(loggBlock.toString())

        // 5. Filtrer relevante signaler
        val relevantSignals = allSignals.filter {
            it is OnHoldSignalEvent || it is ReleaseHoldSignalEvent
        }

        var relevantSignalsLogg = "[$shorthand] PERSIST DEBUG >>> Relevant signals (${relevantSignals.size}):\n"
        relevantSignals.forEach {
            relevantSignalsLogg += "\t- ${it::class.simpleName} eventId=${it.eventId} createdAt=${it.metadata.created}\n"
        }
        log.warn(relevantSignalsLogg)

        // 6. Finn siste signal basert på createdAt
        val lastSignal = relevantSignals.maxByOrNull { it.metadata.created }
        log.warn("[$shorthand] PERSIST DEBUG >>> lastSignal = ${lastSignal?.javaClass?.simpleName} " +
                "eventId=${lastSignal?.eventId} createdAt=${lastSignal?.metadata?.created}")

        // 7. Branching
        if (lastSignal == null) {
            log.warn("[$shorthand] PERSIST DEBUG >>> No signals found → using default implementation")
            return super.onEvent(event, history)
        }

        if (lastSignal is OnHoldSignalEvent) {
            log.warn("[$shorthand] PERSIST DEBUG >>> OnHold detected → returning null")
            return null
        }

        if (lastSignal is ReleaseHoldSignalEvent) {
            log.warn("[$shorthand] PERSIST DEBUG >>> ReleaseHold detected → passthrough to super")
            return super.onEvent(event, history)
        }

        log.warn("[$shorthand] PERSIST DEBUG >>> Unknown signal type → returning null")
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

