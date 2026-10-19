package no.iktdev.mediaprocessing.coordinator.listeners.tasks

import kotlinx.coroutines.test.runTest
import no.iktdev.eventi.models.Event
import no.iktdev.eventi.models.Progress
import no.iktdev.eventi.models.Task
import no.iktdev.eventi.models.store.TaskStatus
import no.iktdev.eventi.tasks.Result
import no.iktdev.eventi.tasks.TaskReporter
import no.iktdev.mediaprocessing.TestBase
import no.iktdev.mediaprocessing.shared.database.stores.TaskStore
import io.mockk.every
import no.iktdev.mediaprocessing.shared.common.event_task_contract.events.DeterminedCollectionTaskResultEvent
import no.iktdev.mediaprocessing.shared.common.event_task_contract.tasks.DetermineCollectionTask
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.whenever
import org.springframework.http.HttpMethod
import org.springframework.web.client.RestTemplate
import java.util.UUID

@ExtendWith(MockitoExtension::class)
class DetermineCollectionTaskListenerTest : TestBase() {

    private class FakeTaskReporter : TaskReporter {
        val events = mutableListOf<Event>()
        var completed = false
        var failed = false

        override fun markClaimed(taskId: UUID, workerId: String): Result = Result.Success
        override fun updateLastSeen(taskId: UUID): Result = Result.Success
        override fun markCompleted(taskId: UUID): Result { completed = true; return Result.Success }
        override fun markFailed(referenceId: UUID, taskId: UUID): Result {
            failed = true
            TaskStore.markConsumed(taskId, TaskStatus.Failed)
            return Result.Success
        }
        override fun markCancelled(referenceId: UUID, taskId: UUID): Result = Result.Success
        override fun updateProgress(referenceId: UUID, taskId: UUID, payload: Progress): Result = Result.Success
        override fun log(taskId: UUID, message: String) {}
        override fun publishEvent(event: Event): Result { events.add(event); return Result.Success }
    }

    @Mock
    lateinit var restTemplate: RestTemplate

    @Test
    @DisplayName(
        """
        Gitt at RestTemplate kaster exception
        Når tasken kjøres gjennom accept()
        Så:
            publiseres et Failed-resultatevent uten collectionReference
            og tasken markeres som Failed
        """
    )
    fun accept_publishesFailedResultAndMarksTaskFailed() = runTest {
        val listener = DetermineCollectionTaskListener(restTemplate)
        val reporter = FakeTaskReporter()
        val task = DetermineCollectionTask(names = listOf("Example Collection")).newReferenceId()
        taskStore.persist(task)
        every { TaskStore.markConsumed(task.taskId, TaskStatus.Failed) } answers {
            taskStore.markConsumed(task.taskId, TaskStatus.Failed)
        }

        whenever(
            restTemplate.exchange(
                any<String>(),
                eq(HttpMethod.POST),
                any(),
                eq(no.iktdev.mediaprocessing.shared.common.dto.streamit.importing.models.CollectionReference::class.java)
            )
        ).thenThrow(RuntimeException("boom"))

        listener.accept(task, reporter)
        listener.currentJob?.join()

        val event = reporter.events.single() as DeterminedCollectionTaskResultEvent
        assertTrue(reporter.failed)
        assertEquals(TaskStatus.Failed, event.status)
        assertNull(event.collectionReference)
        assertEquals(TaskStatus.Failed, taskStore.findByTaskId(task.taskId)?.status)
    }
}
