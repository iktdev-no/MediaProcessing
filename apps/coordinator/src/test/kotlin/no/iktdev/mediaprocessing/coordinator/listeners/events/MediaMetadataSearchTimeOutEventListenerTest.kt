package no.iktdev.mediaprocessing.coordinator.listeners.events

import io.mockk.every
import io.mockk.verify
import no.iktdev.eventi.models.store.TaskStatus
import no.iktdev.mediaprocessing.MockData.mediaParsedEvent
import no.iktdev.mediaprocessing.TestBase
import no.iktdev.mediaprocessing.shared.common.event_task_contract.events.MetadataSearchResultEvent
import no.iktdev.mediaprocessing.shared.common.event_task_contract.events.MetadataSearchTaskCreatedEvent
import no.iktdev.mediaprocessing.shared.common.event_task_contract.tasks.MetadataSearchTask
import no.iktdev.mediaprocessing.shared.common.model.MediaType
import no.iktdev.mediaprocessing.shared.database.stores.TaskStore
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

class MediaMetadataSearchTimeOutEventListenerTest : TestBase() {

    private val listener = MediaMetadataSearchTimeOutEventListener()

    @DisplayName(
        """
        Hvis MetadataSearchTaskCreatedEvent mottas
        Og det ikke finnes et MetadataSearchResultEvent
        Når onEvent kalles
        Så:
            Planlegges timeout for tasken
        """
    )
    @Test
    fun `hvis task opprettes saa planlegges timeout`() {
        // Hvis
        val started = defaultStartEvent()

        val parsed = mediaParsedEvent(
            "Baking Bread",
            "Baking Bread - S01E01 - Flour",
            MediaType.Serie
        ).apply { derivedOf(started) }

        val task = MetadataSearchTask(
            MetadataSearchTask.SearchData(
                searchTitles = parsed.data.parsedSearchTitles,
                collection = parsed.data.parsedCollection,
                mediaType = parsed.data.mediaType
            )
        ).derivedOf(parsed)

        val created = MetadataSearchTaskCreatedEvent(
            taskId = task.taskId
        ).derivedOf(parsed)

        val history = listOf(
            started,
            parsed,
            created
        )

        // Når
        val result = listener.onEvent(created, history)

        // Så
        assertThat(result).isNull()
        assertThat(listener.scheduledExpiries)
            .containsKey(task.taskId)
    }

    @DisplayName(
        """
        Hvis MetadataSearchTaskCreatedEvent mottas flere ganger
        Når onEvent kalles
        Så:
            Planlegges kun én timeout
        """
    )
    @Test
    fun `hvis task created mottas flere ganger saa planlegges kun en timeout`() {
        // Hvis
        val started = defaultStartEvent()

        val parsed = mediaParsedEvent(
            "Baking Bread",
            "Baking Bread - S01E01 - Flour",
            MediaType.Serie
        ).apply { derivedOf(started) }

        val task = MetadataSearchTask(
            MetadataSearchTask.SearchData(
                searchTitles = parsed.data.parsedSearchTitles,
                collection = parsed.data.parsedCollection,
                mediaType = parsed.data.mediaType
            )
        ).derivedOf(parsed)

        val created = MetadataSearchTaskCreatedEvent(
            taskId = task.taskId
        ).derivedOf(parsed)

        val history = listOf(
            started,
            parsed,
            created
        )

        // Når
        listener.onEvent(created, history)
        listener.onEvent(created, history)

        // Så
        assertThat(listener.scheduledExpiries)
            .hasSize(1)

        assertThat(listener.scheduledExpiries)
            .containsKey(task.taskId)
    }

    @DisplayName(
        """
        Hvis MetadataSearchTaskCreatedEvent har planlagt timeout
        Og MetadataSearchResultEvent mottas
        Når onEvent kalles
        Så:
            Fjernes timeouten
        """
    )
    @Test
    fun `hvis search result mottas saa fjernes timeout`() {
        // Hvis
        val started = defaultStartEvent()

        val parsed = mediaParsedEvent(
            "Baking Bread",
            "Baking Bread - S01E01 - Flour",
            MediaType.Serie
        ).apply { derivedOf(started) }

        val task = MetadataSearchTask(
            MetadataSearchTask.SearchData(
                searchTitles = parsed.data.parsedSearchTitles,
                collection = parsed.data.parsedCollection,
                mediaType = parsed.data.mediaType
            )
        ).derivedOf(parsed)

        val created = MetadataSearchTaskCreatedEvent(
            taskId = task.taskId
        ).derivedOf(parsed)

        val initialHistory = listOf(
            started,
            parsed,
            created
        )

        listener.onEvent(created, initialHistory)

        assertThat(listener.scheduledExpiries)
            .containsKey(task.taskId)

        val resultEvent = MetadataSearchResultEvent(
            status = TaskStatus.Completed,
            results = emptyList()
        ).producedFrom(task)

        val history = initialHistory + resultEvent

        // Når
        val result = listener.onEvent(resultEvent, history)

        // Så
        assertThat(result).isNull()

        assertThat(listener.scheduledExpiries)
            .doesNotContainKey(task.taskId)
    }

    @DisplayName(
        """
        Hvis MetadataSearchResultEvent allerede finnes
        Når MetadataSearchTaskCreatedEvent behandles
        Så:
            Planlegges ikke timeout
        """
    )
    @Test
    fun `hvis search result allerede finnes saa planlegges ikke timeout`() {
        // Hvis
        val started = defaultStartEvent()

        val parsed = mediaParsedEvent(
            "Baking Bread",
            "Baking Bread - S01E01 - Flour",
            MediaType.Serie
        ).apply { derivedOf(started) }

        val task = MetadataSearchTask(
            MetadataSearchTask.SearchData(
                searchTitles = parsed.data.parsedSearchTitles,
                collection = parsed.data.parsedCollection,
                mediaType = parsed.data.mediaType
            )
        ).derivedOf(parsed)

        val created = MetadataSearchTaskCreatedEvent(
            taskId = task.taskId
        ).derivedOf(parsed)

        val resultEvent = MetadataSearchResultEvent(
            status = TaskStatus.Completed,
            results = emptyList()
        ).producedFrom(task)

        val history = listOf(
            started,
            parsed,
            created,
            resultEvent
        )

        // Når
        val result = listener.onEvent(created, history)

        // Så
        assertThat(result).isNull()

        assertThat(listener.scheduledExpiries)
            .doesNotContainKey(task.taskId)
    }
}