package no.iktdev.mediaprocessing.coordinator.listeners.events

import no.iktdev.eventi.models.SignalEvent
import no.iktdev.mediaprocessing.TestBase
import no.iktdev.mediaprocessing.shared.common.event_task_contract.events.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

class PersistContentListenerTest : TestBase() {

    private fun listener() = PersistContentListener(eventStore)

    @Test
    @DisplayName(
        """
        Når PersistContentListener mottar et event
        Hvis ContinuationSummaryEvent mangler i historikken
        Så:
            skal null returneres
        """
    )
    fun missingSummary_returnsNull() {
        val start = defaultStartEvent().addToHistory()
        val result = listener().onEvent(start, history)
        assertNull(result)
    }

    @Test
    @DisplayName(
        """
        Når PersistContentListener mottar første SummaryEvent i manual flow
        Hvis sekvensen ikke er OnHold
        Så:
            skal OnHoldSignalEvent returneres
        """
    )
    fun manualFlow_firstSummary_setsHold() {
        val start = defaultStartEvent(StartFlow.Manual).addToHistory()
        val summary = defaultSummaryEvent().derivedOf(start).addToHistory()

        val result = listener().onEvent(summary, history)

        assertTrue(result is OnHoldSignalEvent)
    }

    @Test
    @DisplayName(
        """
        Når PersistContentListener mottar SummaryEvent
        Hvis sekvensen er OnHold
        Så:
            skal null returneres
        """
    )
    fun manualFlow_onHold_blocksSummary() {
        val start = defaultStartEvent(StartFlow.Manual).addToHistory()
        val summary = defaultSummaryEvent().derivedOf(start).addToHistory()
        OnHoldSignalEvent("manual").derivedOf(summary).addToHistory()

        val result = listener().onEvent(summary, history)

        assertNull(result)
    }

    @Test
    @DisplayName(
        """
        Når PersistContentListener mottar SummaryEvent
        Hvis siste signal er ReleaseHold
        Så:
            skal PersistContentEvent returneres
        """
    )
    fun manualFlow_afterRelease_summaryProducesPersist() {
        val start = defaultStartEvent(StartFlow.Manual).addToHistory()
        val summary = defaultSummaryEvent().derivedOf(start).addToHistory()
        ReleaseHoldSignalEvent("ok").derivedOf(summary).addToHistory()

        val result = listener().onEvent(summary, history)

        assertTrue(result is PersistContentEvent)
    }

    @Test
    @DisplayName(
        """
        Når PersistContentListener mottar PersistContentEvent som input
        Så:
            skal null returneres
        """
    )
    fun persistContentEvent_asInput_isIgnored() {
        val start = defaultStartEvent(StartFlow.Manual).addToHistory()
        val summary = defaultSummaryEvent().derivedOf(start).addToHistory()

        val result = listener().onEvent(PersistContentEvent().derivedOf(summary), history)

        assertNull(result)
    }

    @Test
    @DisplayName(
        """
        Når PersistContentListener mottar SummaryEvent etter PersistContentEvent
        Så:
            skal null returneres (hasPassed)
        """
    )
    fun manualFlow_afterPersistContent_hasPassed() {
        val start = defaultStartEvent(StartFlow.Manual).addToHistory()
        val summary = defaultSummaryEvent().derivedOf(start).addToHistory()
        ReleaseHoldSignalEvent("ok").derivedOf(summary).addToHistory()
        PersistContentEvent().derivedOf(summary).addToHistory()

        val result = listener().onEvent(summary, history)

        assertNull(result)
    }

    @Test
    @DisplayName(
        """
        Når PersistContentListener mottar SummaryEvent i Auto flow
        Så:
            skal PersistContentEvent returneres
        """
    )
    fun autoFlow_summaryProducesPersist() {
        val start = defaultStartEvent(StartFlow.Auto).addToHistory()
        val summary = defaultSummaryEvent().derivedOf(start).addToHistory()

        val result = listener().onEvent(summary, history)

        assertTrue(result is PersistContentEvent)
    }

    @Test
    @DisplayName(
        """
        Når PersistContentListener mottar SummaryEvent i Auto flow
        Hvis PersistContentEvent allerede finnes
        Så:
            skal null returneres
        """
    )
    fun autoFlow_afterPersistContent_hasPassed() {
        val start = defaultStartEvent(StartFlow.Auto).addToHistory()
        val summary = defaultSummaryEvent().derivedOf(start).addToHistory()
        PersistContentEvent().derivedOf(summary).addToHistory()

        val result = listener().onEvent(summary, history)

        assertNull(result)
    }

    @Test
    @DisplayName(
        """
    Når PersistContentListener produserer PersistContentEvent
    Så:
        skal derivedFromId inneholde ContinuationSummaryEvent.eventId
        og ikke inneholde noen signal-eventId
    """
    )
    fun persistContentEvent_isDerivedFromSummary_notSignals() {
        val start = defaultStartEvent(StartFlow.Manual).addToHistory()
        val summary = defaultSummaryEvent().derivedOf(start).addToHistory()

        // Sett hold
        OnHoldSignalEvent("manual").derivedOf(summary).addToHistory()

        // Slipp hold
        ReleaseHoldSignalEvent("ok").derivedOf(summary).addToHistory()

        // Nå kommer SummaryEvent igjen → policy skal produsere PersistContentEvent
        val result = listener().onEvent(summary, history)
        assertNotNull(result)
        assertTrue(result is PersistContentEvent)

        val persist = result as PersistContentEvent

        // derivedFromId skal være et sett som inneholder summary.eventId
        assertTrue(
            persist.metadata.derivedFromId!!.contains(summary.eventId),
            "PersistContentEvent skal være derived fra SummaryEvent"
        )

        // Og settet skal IKKE inneholde signal-eventId’er
        val signalIds = history
            .filterIsInstance<SignalEvent>()
            .map { it.eventId }
            .toSet()

        val persistDerivedEventIds = persist.metadata.derivedFromId
        assertTrue(
            persistDerivedEventIds!!.intersect(signalIds).isEmpty(),
            "PersistContentEvent skal ikke være derived fra signaler"
        )
    }


}
