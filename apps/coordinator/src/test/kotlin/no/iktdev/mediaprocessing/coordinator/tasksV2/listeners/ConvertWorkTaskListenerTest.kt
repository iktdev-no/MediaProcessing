package no.iktdev.mediaprocessing.coordinator.tasksV2.listeners

import no.iktdev.eventi.data.referenceId
import no.iktdev.mediaprocessing.Files
import no.iktdev.mediaprocessing.databaseJsonToEvents
import no.iktdev.mediaprocessing.shared.common.contract.Events
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test


class ConvertWorkTaskListenerTest {

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