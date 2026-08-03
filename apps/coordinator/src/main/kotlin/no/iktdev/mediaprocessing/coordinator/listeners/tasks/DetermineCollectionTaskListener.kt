package no.iktdev.mediaprocessing.coordinator.listeners.tasks

import mu.KotlinLogging
import no.iktdev.eventi.models.Event
import no.iktdev.eventi.models.Task
import no.iktdev.eventi.models.store.TaskStatus
import no.iktdev.eventi.tasks.TaskListener
import no.iktdev.eventi.tasks.TaskType
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
            collection = null
        ).producedFrom(task)
    }

    override fun supports(task: Task) =
        task is DetermineCollectionTask

    override suspend fun onTask(task: Task): Event? {
        val pickedTask = task as? DetermineCollectionTask ?: return null

        // Log input
        logger.info("Determining collection for names: ${pickedTask.names}")

        val collection = try {
            val response = streamitRestTemplate.exchange(
                "/api/meta/title/search/batch",
                HttpMethod.POST,
                HttpEntity(pickedTask.names),
                String::class.java
            )
            response.body
        } catch (e: HttpClientErrorException.NotFound) {
            logger.debug("No collection/title found for batch names (404)")
            null
        } catch (e: Exception) {
            logger.error("Failed to batch search collection", e)
            return DeterminedCollectionTaskResultEvent(
                status = TaskStatus.Failed,
                collection = null
            ).producedFrom(task)
        }

        if (collection.isNullOrEmpty()) {
            logger.info("No valid collection found, returning null collection")
            return DeterminedCollectionTaskResultEvent(
                status = TaskStatus.Completed,
                collection = null
            ).producedFrom(task)
        }

        logger.info("Found collection='$collection' for batch search")

        return DeterminedCollectionTaskResultEvent(
            status = TaskStatus.Completed,
            collection = collection
        ).producedFrom(pickedTask)
    }



}