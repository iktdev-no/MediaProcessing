package no.iktdev.mediaprocessing.coordinator.tasksV2.listeners

import no.iktdev.mediaprocessing.coordinator.defaultBaseInfoEvent
import no.iktdev.mediaprocessing.coordinator.defaultMetadataSearchEvent
import no.iktdev.mediaprocessing.coordinator.defaultStartEvent
import no.iktdev.mediaprocessing.coordinator.metadataSearchTimedOutEvent
import no.iktdev.mediaprocessing.shared.common.contract.data.Event
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

class MetadataWaitOrDefaultTaskListenerTest {

    @Test
    @DisplayName("""
        When incoming event is of base name, and there is no search performed,
        Validation check should proceed
    """)
    fun validate_shouldIProcessAndHandleEvent1() {
        val listener = MetadataWaitOrDefaultTaskListener()
        val events = listOf<Event>(defaultStartEvent(), defaultBaseInfoEvent())
        val result = listener.shouldIProcessAndHandleEvent(incomingEvent = events.last(), events)
        assertTrue(result)
    }

    @Test
    @DisplayName("""
        When incoming event is of MetadataReceivedEvent,
        And timeout listener is the origin,
        Then validation should abort
    """)
    fun validate_shouldIProcessAndHandleEvent2() {
        val listener = MetadataWaitOrDefaultTaskListener()
        val events = listOf<Event>(defaultStartEvent(), defaultBaseInfoEvent(), metadataSearchTimedOutEvent())
        val result = listener.shouldIProcessAndHandleEvent(incomingEvent = events.last(), events)
        assertFalse(result)
    }

    @Test
    @DisplayName("""
        When incoming event is of MetadataReceivedEvent,
        And metadata service has produced the event,
        Then validation should allow, due to cleanup
    """)
    fun validate_shouldIProcessAndHandleEvent3() {
        val listener = MetadataWaitOrDefaultTaskListener()
        val events = listOf<Event>(defaultStartEvent(), defaultBaseInfoEvent(), defaultMetadataSearchEvent())
        val result = listener.shouldIProcessAndHandleEvent(incomingEvent = events.last(), events)
        assertTrue(result)
    }

}