package no.iktdev.mediaprocessing.coordinator.tasksV2.listeners

import no.iktdev.eventi.data.referenceId
import no.iktdev.mediaprocessing.Files
import no.iktdev.mediaprocessing.coordinator.defaultMetadata
import no.iktdev.mediaprocessing.coordinator.defaultStartEvent
import no.iktdev.mediaprocessing.databaseJsonToEvents
import no.iktdev.mediaprocessing.shared.common.contract.Events
import no.iktdev.mediaprocessing.shared.common.contract.data.Event
import no.iktdev.mediaprocessing.shared.common.contract.data.StartEventData
import no.iktdev.mediaprocessing.shared.common.contract.dto.OperationEvents
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test


class ConvertWorkTaskListenerTest {

    @Test
    @DisplayName("""
        When incoming event is of Start Event, and there is multiple operations,
        Validation check should fail
    """)
    fun validate_shouldIProcessAndHandleEvent1() {
        val listener = ConvertWorkTaskListener()
        val events = listOf<Event>(defaultStartEvent())
        val result = listener.shouldIProcessAndHandleEvent(defaultStartEvent(), events)
        assertThat(result).isFalse()
    }
    @Test
    @DisplayName("""
        When incoming event is of Start Event but start is missing form list, and there is multiple operations,
        Validation check should fail
    """)
    fun validate_shouldIProcessAndHandleEvent2() {
        val listener = ConvertWorkTaskListener()
        val result = listener.shouldIProcessAndHandleEvent(defaultStartEvent(), emptyList())
        assertThat(result).isFalse()
    }

    @Test
    @DisplayName("""
        When incoming event is of Start Event and single operation is Convert,
        Validation check should succeed
    """)
    fun validate_shouldIProcessAndHandleEvent3() {
        val listener = ConvertWorkTaskListener()
        val startedEvent = defaultStartEvent().copy(
            data = StartEventData(
                operations = listOf(OperationEvents.CONVERT),
                file = "DummyTestFile.ass"
            )
        )
        val events = listOf<Event>(startedEvent)
        val result = listener.shouldIProcessAndHandleEvent(startedEvent, events)
        assertThat(result).isTrue()
    }



    @Test
    fun validateParsingOfEvents() {
        val content = Files.MultipleLanguageBased.databaseJsonToEvents()
        assertThat(content).isNotEmpty()
        val referenceId = content.firstOrNull()?.referenceId()
        assertThat(referenceId).isNotNull()
    }

    @Test
    fun validateCreationOfConvertTasks() {
        val listener: ConvertWorkTaskListener = ConvertWorkTaskListener()
        val content = Files.MultipleLanguageBased.databaseJsonToEvents().filter { it.eventType in listOf( Events.ExtractTaskCompleted, Events.ProcessStarted, Events.ConvertTaskCreated, Events.ConvertTaskCompleted) }
        assertThat(listener).isNotNull()
        val success = content.map { listener.shouldIProcessAndHandleEvent(it, content) to it }
        assertThat(success.filter { it.first }.size).isGreaterThan(3)
    }
}