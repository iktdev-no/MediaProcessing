package no.iktdev.mediaprocessing.coordinator.listeners.tasks

import mu.KotlinLogging
import no.iktdev.eventi.models.Event
import no.iktdev.eventi.models.Task
import no.iktdev.eventi.models.store.TaskStatus
import no.iktdev.eventi.tasks.TaskListener
import no.iktdev.eventi.tasks.TaskType
import no.iktdev.mediaprocessing.shared.common.event_task_contract.events.DeterminedCollectionTaskResultEvent
import no.iktdev.mediaprocessing.shared.common.event_task_contract.tasks.DetermineCollectionTask
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
        )
    }

    override fun supports(task: Task) =
        task is DetermineCollectionTask

    override suspend fun onTask(task: Task): Event? {
        val pickedTask = task as? DetermineCollectionTask ?: return null

        // Log input
        logger.info("Determining collection for names: ${pickedTask.names}")

        val responses = pickedTask.names.map { name ->
            try {
                val response = streamitRestTemplate.exchange(
                    "/api/meta/title/search/$name",
                    HttpMethod.GET,
                    null,
                    String::class.java
                )
                name to response
            } catch (e: HttpClientErrorException.NotFound) {
                logger.debug("Title '{}' not found (404)", name)
                name to ResponseEntity.status(HttpStatus.NOT_FOUND).body<String?>(null)
            } catch (e: Exception) {
                return DeterminedCollectionTaskResultEvent(
                    status = TaskStatus.Failed,
                    collection = null
                )
            }
        }


        // Filtrer ut gyldige svar
        val validBodies = responses
            .mapNotNull { (name, response) ->
                if (response.statusCode == HttpStatus.OK && !response.body.isNullOrEmpty()) {
                    logger.debug("Accepted '$name' with body='${response.body}'")
                    response.body!!
                } else {
                    logger.debug("Rejected '$name' due to invalid status/body")
                    null
                }
            }

        // 0 treff → returner null collection
        if (validBodies.isEmpty()) {
            logger.info("No valid results found, returning null collection")
            return DeterminedCollectionTaskResultEvent(
                status = TaskStatus.Completed,
                collection = null
            )
        }

        // Sjekk om alle body-ene er identiske
        val first = validBodies.first()
        val allIdentical = validBodies.all { it == first }

        if (!allIdentical) {
            logger.error(
                "Multiple results but bodies differ: $validBodies"
            )
            throw IllegalStateException(
                "Flere treff, men body-ene er ikke identiske: $validBodies"
            )
        }

        logger.info("All results identical, returning collection='$first'")

        return DeterminedCollectionTaskResultEvent(
            status = TaskStatus.Completed,
            collection = first
        )
    }



}