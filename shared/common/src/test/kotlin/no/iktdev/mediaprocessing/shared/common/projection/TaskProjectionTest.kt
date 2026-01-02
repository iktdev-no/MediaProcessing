package no.iktdev.mediaprocessing.shared.common.projection

import no.iktdev.eventi.models.Event
import no.iktdev.eventi.models.store.TaskStatus
import no.iktdev.mediaprocessing.shared.common.event_task_contract.events.CoordinatorReadStreamsResultEvent
import no.iktdev.mediaprocessing.shared.common.event_task_contract.events.CoordinatorReadStreamsTaskCreatedEvent
import no.iktdev.mediaprocessing.shared.common.event_task_contract.tasks.MediaReadTask
import org.assertj.core.api.AssertionsForInterfaceTypes.assertThat
import org.junit.jupiter.api.Test

class TaskProjectionTest {

    @Test
    fun testChainAcknowledgementNotInitiated1() {
        val events: MutableList<Event> = mutableListOf()

        val projection = TaskProjection(events)
        assertThat(projection.projectStreamReadStatus()).isEqualTo(CollectProjection.TaskStatus.NotInitiated)
    }


    @Test
    fun testChainAcknowledgementSuccess1() {
        val events: MutableList<Event> = mutableListOf()

        val readTask = MediaReadTask(
            fileUri = ""
        ).newReferenceId()

        CoordinatorReadStreamsTaskCreatedEvent(
            taskId = readTask.taskId
        ).usingReferenceId(readTask.referenceId).also {
            events.add(it)
        }

        CoordinatorReadStreamsResultEvent(
            status = TaskStatus.Completed
        ).producedFrom(readTask).also {
            events.add(it)
        }

        val projection = TaskProjection(events)
        assertThat(projection.projectStreamReadStatus()).isEqualTo(CollectProjection.TaskStatus.Completed)
    }

    @Test
    fun testChainAcknowledgementPending1() {
        val events: MutableList<Event> = mutableListOf()

        val readTask = MediaReadTask(
            fileUri = ""
        ).newReferenceId()

        CoordinatorReadStreamsTaskCreatedEvent(
            taskId = readTask.taskId
        ).usingReferenceId(readTask.referenceId).also {
            events.add(it)
        }

        val projection = TaskProjection(events)
        assertThat(projection.projectStreamReadStatus()).isEqualTo(CollectProjection.TaskStatus.Pending)
    }

    @Test
    fun testChainAcknowledgementFailure1() {
        val events: MutableList<Event> = mutableListOf()

        val readTask = MediaReadTask(
            fileUri = ""
        ).newReferenceId()

        CoordinatorReadStreamsTaskCreatedEvent(
            taskId = readTask.taskId
        ).usingReferenceId(readTask.referenceId).also {
            events.add(it)
        }

        CoordinatorReadStreamsResultEvent(
            status = TaskStatus.Failed
        ).producedFrom(readTask).also {
            events.add(it)
        }

        val projection = TaskProjection(events)
        assertThat(projection.projectStreamReadStatus()).isEqualTo(CollectProjection.TaskStatus.Failed)
    }



}