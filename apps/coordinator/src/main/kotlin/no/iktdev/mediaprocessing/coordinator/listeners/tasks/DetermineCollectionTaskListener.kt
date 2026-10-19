package no.iktdev.mediaprocessing.coordinator.listeners.tasks

import mu.KotlinLogging
import no.iktdev.eventi.models.Event
import no.iktdev.eventi.models.Task
import no.iktdev.eventi.models.store.TaskStatus
import no.iktdev.eventi.tasks.TaskListener
import no.iktdev.eventi.tasks.TaskType
import no.iktdev.mediaprocessing.shared.common.dto.streamit.importing.METADATA_CATALOG_SEARCH
import no.iktdev.mediaprocessing.shared.common.dto.streamit.importing.models.CollectionReference
import no.iktdev.mediaprocessing.shared.common.event_task_contract.events.DeterminedCollectionTaskResultEvent
import no.iktdev.mediaprocessing.shared.common.event_task_contract.tasks.DetermineCollectionTask
import org.springframework.http.HttpEntity
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Component
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.RestTemplate
import java.util.UUID

@Component
class DetermineCollectionTaskListener(
    private var streamitRestTemplate: RestTemplate,
): TaskListener(TaskType.MIXED) {
    private val logger = KotlinLogging.logger {}


    override fun getWorkerId(): String {
        return "${this::class.java.simpleName}-${taskType}-${UUID.randomUUID()}"
    }

    override fun createIncompleteStateTaskEvent(
        task: Task,
        status: TaskStatus,
        exception: Exception?
    ): Event {
        return DeterminedCollectionTaskResultEvent(
            status = status,
            collectionReference = null
        ).producedFrom(task)
    }

    override fun supports(task: Task) =
        task is DetermineCollectionTask

    override suspend fun onTask(task: Task): Event? {
        val pickedTask = task as? DetermineCollectionTask ?: return null

        // Log input
        logger.info("Determining collection for names: ${pickedTask.names}")

        return try {
            val response = streamitRestTemplate.exchange(
                METADATA_CATALOG_SEARCH,
                HttpMethod.POST,
                HttpEntity(pickedTask.names),
                CollectionReference::class.java
            )
            DeterminedCollectionTaskResultEvent(
                status = TaskStatus.Completed,
                collectionReference = response.body
            )
        } catch (e: HttpClientErrorException.NotFound) {
            logger.debug("No collection/title found for batch names (404)")
            DeterminedCollectionTaskResultEvent(
                status = TaskStatus.Completed,
                collectionReference = null
            )
        }.producedFrom(pickedTask)
    }



}