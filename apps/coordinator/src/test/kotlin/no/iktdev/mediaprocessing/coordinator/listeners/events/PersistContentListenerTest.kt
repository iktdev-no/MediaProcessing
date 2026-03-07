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
        Hvis CollectedEvent mangler i historikken
        Så:
            skal null returneres
        """
    )
    fun missingCollectedEvent_returnsNull() {
        val start = defaultStartEvent().addToHistory()
        val result = listener().onEvent(start, history)
        assertNull(result)
    }

    @Test
    @DisplayName(
        """
        Når PersistContentListener mottar første event i manual flow
        Hvis sekvensen ikke er OnHold
        Så:
            skal OnHoldSignalEvent returneres
        """
    )
    fun manualFlow_firstEvent_setsHold() {
        val start = defaultStartEvent(StartFlow.Manual).addToHistory()
        val summaryEvent = defaultSummaryEvent().derivedOf(start).addToHistory()

        val result = listener().onEvent(summaryEvent, history)

        assertNotNull(result)
        assertTrue(result is OnHoldSignalEvent)
    }

    @Test
    @DisplayName(
        """
        Når PersistContentListener mottar et event
        Hvis sekvensen er OnHold
        Og eventet ikke er ReleaseHoldSignalEvent
        Så:
            skal null returneres
        """
    )
    fun manualFlow_onHold_blocksEvents() {
        val start = defaultStartEvent(StartFlow.Manual).addToHistory()
        val collected = CollectedEvent(setOf(start.eventId)).derivedOf(start).addToHistory()
        OnHoldSignalEvent("manual").derivedOf(collected).addToHistory()

        val nextEvent = PersistContentEvent().derivedOf(collected)
        val result = listener().onEvent(nextEvent, history)

        assertNull(result)
    }

    @Test
    @DisplayName(
        """
        Når PersistContentListener mottar ReleaseHoldSignalEvent
        Hvis sekvensen er OnHold
        Så:
            skal ReleaseHoldSignalEvent returneres
        """
    )
    fun manualFlow_onHold_releaseHold() {
        val start = defaultStartEvent(StartFlow.Manual).addToHistory()
        val summaryEvent = defaultSummaryEvent().derivedOf(start).addToHistory()
        OnHoldSignalEvent("manual").derivedOf(summaryEvent).addToHistory()

        val release = ReleaseHoldSignalEvent("ok").derivedOf(summaryEvent)
        val result = listener().onEvent(release, history)

        assertNotNull(result)
        assertTrue(result is ReleaseHoldSignalEvent)
    }

    @Test
    @DisplayName(
        """
        Når PersistContentListener mottar et event etter ReleaseHoldSignalEvent
        Hvis StartFlow er Manual
        Så:
            skal PersistContentEvent returneres
        """
    )
    fun manualFlow_afterRelease_passthrough() {
        val start = defaultStartEvent(StartFlow.Manual).addToHistory()
        val summaryEvent = defaultSummaryEvent().derivedOf(start).addToHistory()
        ReleaseHoldSignalEvent("ok").derivedOf(summaryEvent).addToHistory()

        val nextEvent = PersistContentEvent().derivedOf(summaryEvent)
        val result = listener().onEvent(nextEvent, history)

        assertNotNull(result)
        assertTrue(result is PersistContentEvent)
    }

    @Test
    @DisplayName(
        """
        Når PersistContentListener mottar flere events etter ReleaseHoldSignalEvent
        Hvis StartFlow er Manual
        Så:
            skal OnHoldSignalEvent aldri genereres på nytt
        """
    )
    fun manualFlow_afterRelease_neverReintroduceHold() {
        val start = defaultStartEvent(StartFlow.Manual).addToHistory()
        val summaryEvent = defaultSummaryEvent().derivedOf(start).addToHistory()
        ReleaseHoldSignalEvent("ok").derivedOf(summaryEvent).addToHistory()

        repeat(3) {
            val event = PersistContentEvent().derivedOf(summaryEvent)
            val result = listener().onEvent(event, history)
            assertTrue(result is PersistContentEvent)
        }
    }

    @Test
    @DisplayName(
        """
        Når PersistContentListener mottar flere events i manual flow
        Hvis OnHoldSignalEvent allerede finnes i historikken
        Så:
            skal det ikke genereres flere OnHoldSignalEvent
        """
    )
    fun manualFlow_doesNotGenerateMultipleOnHold() {
        val start = defaultStartEvent(StartFlow.Manual).addToHistory()
        val summaryEvent = defaultSummaryEvent().derivedOf(start).addToHistory()

        val first = listener().onEvent(summaryEvent, history)
        assertTrue(first is OnHoldSignalEvent)

        (first as SignalEvent).addToHistory()

        val second = listener().onEvent(summaryEvent, history)
        assertNull(second)
    }

    @Test
    @DisplayName(
        """
        Når PersistContentListener mottar event i Auto flow
        Hvis precursor finnes
        Så:
            skal PersistContentEvent returneres
        """
    )
    fun autoFlow_passthrough() {
        val start = defaultStartEvent(StartFlow.Auto).addToHistory()
        val summaryEvent = defaultSummaryEvent().derivedOf(start).addToHistory()


        val result = listener().onEvent(summaryEvent, history)

        assertNotNull(result)
        assertTrue(result is PersistContentEvent)
    }
}
