package no.iktdev.mediaprocessing.ui.service.coordinator

import mu.KotlinLogging
import no.iktdev.mediaprocessing.shared.common.dto.ResetTaskResponse
import no.iktdev.mediaprocessing.shared.common.dto.TaskQuery
import no.iktdev.mediaprocessing.transferModel.coordinatorUi.CoordinatorTaskDto
import no.iktdev.mediaprocessing.ui.dto.Paginated
import no.iktdev.mediaprocessing.ui.dto.UiTask
import no.iktdev.mediaprocessing.ui.dto.passthrough.ProgressUpdate
import org.springframework.core.ParameterizedTypeReference
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono
import java.util.*

@Service
class CoordinatorTaskService(
    private val coordinatorWebClient: WebClient,
) {
    val log = KotlinLogging.logger {}



    fun getPagedTasks(taskQuery: TaskQuery): Mono<Paginated<UiTask>> =
        coordinatorWebClient.get()
            .uri { uri ->
                uri.path("/tasks")
                    .queryParams(taskQuery.toQueryParams())
                    .build()
            }
            .retrieve()
            .bodyToMono(object : ParameterizedTypeReference<Paginated<CoordinatorTaskDto>>() {})
            .map { paginatedDto ->
                Paginated(
                    items = paginatedDto.items.map { UiTask.from(it) },
                    page = paginatedDto.page,
                    size = paginatedDto.size,
                    total = paginatedDto.total
                )
            }


    fun getActiveTasks(): Mono<List<UiTask>> =
        coordinatorWebClient.get()
            .uri("/tasks/active")
            .retrieve()
            .bodyToMono(object : ParameterizedTypeReference<List<CoordinatorTaskDto>>() {})
            .map { it.map { x -> UiTask.from(x) } }

    fun resetTask(taskId: UUID): Mono<ResetTaskResponse> =
        coordinatorWebClient.get()
            .uri("/tasks/${taskId}/reset")
            .retrieve()
            .bodyToMono(object : ParameterizedTypeReference<ResetTaskResponse>() {})

    fun resetTaskForced(taskId: UUID): Mono<ResetTaskResponse> =
        coordinatorWebClient.get()
            .uri("/tasks/${taskId}/reset/force")
            .retrieve()
            .bodyToMono(object : ParameterizedTypeReference<ResetTaskResponse>() {})

    fun getAllProgress(): Mono<List<ProgressUpdate>> =
        coordinatorWebClient.get()
            .uri("/tasks/progress/all")
            .retrieve()
            .bodyToMono(object : ParameterizedTypeReference<List<ProgressUpdate>>() {})


}