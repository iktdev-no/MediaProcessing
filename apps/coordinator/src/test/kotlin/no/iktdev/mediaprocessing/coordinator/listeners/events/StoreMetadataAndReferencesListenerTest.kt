package no.iktdev.mediaprocessing.projection

import no.iktdev.eventi.models.Event
import no.iktdev.eventi.models.Metadata
import no.iktdev.eventi.models.MultiTaskIdentity
import no.iktdev.eventi.models.store.TaskStatus
import no.iktdev.mediaprocessing.shared.common.event_task_contract.events.transfer.*
import no.iktdev.mediaprocessing.shared.common.projection.CollectProjection
import no.iktdev.mediaprocessing.shared.common.projection.TaskProjection
import no.iktdev.mediaprocessing.withMetadata
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.util.UUID

class TaskProjectionMigrateStatusTest {

    // ---------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------

    private fun createdEvent(vararg taskIds: UUID): TransferContentTaskCreatedEvent {
        return TransferContentTaskCreatedEvent(
            groupId = UUID.randomUUID(),
            taskIds = taskIds.map { MultiTaskIdentity(it, it.toString()) }.toSet()
        ).newReferenceId() as TransferContentTaskCreatedEvent
    }

    private fun videoResult(parent: Event, taskId: UUID, status: TaskStatus) =
        VideoTransferredResultEvent(
            fileUri = "store:///video.mp4",
            collection = "Test",
            status = status,
            error = if (status == TaskStatus.Failed) "boom" else null
        ).derivedOf(parent).apply {
            withMetadata(metadata.derivedFromEventId(taskId))
        }

    private fun coverResult(parent: Event, taskId: UUID, status: TaskStatus) =
        CoverTransferredResultEvent(
            fileUri = "store:///cover.jpg",
            collection = "Test",
            status = status,
            error = if (status == TaskStatus.Failed) "boom" else null
        ).derivedOf(parent).apply {
            withMetadata(metadata.derivedFromEventId(taskId))

        }

    private fun subtitleResult(parent: Event, taskId: UUID, status: TaskStatus) =
        SubtitleTransferredResultEvent(
            fileUri = "store:///sub.ass",
            collection = "Test",
            language = "eng",
            status = status,
            error = if (status == TaskStatus.Failed) "boom" else null
        ).derivedOf(parent).apply {
            withMetadata(metadata.derivedFromEventId(taskId))

        }

    class DummyEvent : Event()

    // ---------------------------------------------------------
    // Tests
    // ---------------------------------------------------------

    @Test
    @DisplayName("Alle tasks OK → Completed")
    fun allTasksCompleted() {
        val t1 = UUID.randomUUID()
        val t2 = UUID.randomUUID()

        val created = createdEvent(t1, t2)
        val r1 = videoResult(created, t1, TaskStatus.Completed)
        val r2 = coverResult(created, t2, TaskStatus.Completed)

        val projection = TaskProjection(listOf(created, r1, r2))
        val status = projection.projectMigrateContentStatus()

        assertThat(status).isEqualTo(CollectProjection.TaskStatus.Completed)
    }

    @Test
    @DisplayName("Én task feiler → Failed")
    fun oneTaskFails() {
        val t1 = UUID.randomUUID()
        val t2 = UUID.randomUUID()

        val created = createdEvent(t1, t2)
        val r1 = videoResult(created, t1, TaskStatus.Completed)
        val r2 = coverResult(created, t2, TaskStatus.Failed)

        val projection = TaskProjection(listOf(created, r1, r2))
        val status = projection.projectMigrateContentStatus()

        assertThat(status).isEqualTo(CollectProjection.TaskStatus.Failed)
    }

    @Test
    @DisplayName("Alle tasks feiler → Failed")
    fun allTasksFail() {
        val t1 = UUID.randomUUID()
        val t2 = UUID.randomUUID()

        val created = createdEvent(t1, t2)
        val r1 = videoResult(created, t1, TaskStatus.Failed)
        val r2 = coverResult(created, t2, TaskStatus.Failed)

        val projection = TaskProjection(listOf(created, r1, r2))
        val status = projection.projectMigrateContentStatus()

        assertThat(status).isEqualTo(CollectProjection.TaskStatus.Failed)
    }

    @Test
    @DisplayName("Created-event finnes, men ingen resultater → Pending")
    fun noResultsYet() {
        val t1 = UUID.randomUUID()
        val created = createdEvent(t1)

        val projection = TaskProjection(listOf(created))
        val status = projection.projectMigrateContentStatus()

        assertThat(status).isEqualTo(CollectProjection.TaskStatus.Pending)
    }

    @Test
    @DisplayName("Resultater finnes, men created-event mangler → NotInitiated")
    fun resultsWithoutCreatedEvent() {
        val t1 = UUID.randomUUID()
        val fakeParent = DummyEvent().newReferenceId()

        val r1 = videoResult(fakeParent, t1, TaskStatus.Completed)

        val projection = TaskProjection(listOf(r1))
        val status = projection.projectMigrateContentStatus()

        assertThat(status).isEqualTo(CollectProjection.TaskStatus.NotInitiated)
    }

    @Test
    @DisplayName("Mismatch mellom taskIds og derivedFromId → Pending")
    fun mismatchBetweenCreatedAndResults() {
        val t1 = UUID.randomUUID()
        val created = createdEvent(t1)

        val r1 = videoResult(created, UUID.randomUUID(), TaskStatus.Completed) // mismatch

        val projection = TaskProjection(listOf(created, r1))
        val status = projection.projectMigrateContentStatus()

        assertThat(status).isEqualTo(CollectProjection.TaskStatus.Pending)
    }

    @Test
    @DisplayName("Delvis manglende resultater → Pending")
    fun partialResultsPending() {
        val t1 = UUID.randomUUID()
        val t2 = UUID.randomUUID()

        val created = createdEvent(t1, t2)
        val r1 = videoResult(created, t1, TaskStatus.Completed)

        val projection = TaskProjection(listOf(created, r1))
        val status = projection.projectMigrateContentStatus()

        assertThat(status).isEqualTo(CollectProjection.TaskStatus.Pending)
    }
}
