package no.iktdev.mediaprocessing.ui.controller

import no.iktdev.mediaprocessing.shared.common.dto.*
import no.iktdev.mediaprocessing.ui.UiSseHub
import no.iktdev.mediaprocessing.ui.dto.Paginated
import no.iktdev.mediaprocessing.ui.dto.UiEvent
import no.iktdev.mediaprocessing.ui.dto.UiTask
import no.iktdev.mediaprocessing.ui.dto.health.CoordinatorHealth
import no.iktdev.mediaprocessing.ui.dto.health.DiskInfo
import no.iktdev.mediaprocessing.ui.dto.rate.EventRate
import no.iktdev.mediaprocessing.ui.dto.requests.ContinueResult
import no.iktdev.mediaprocessing.ui.dto.requests.StartProcessRequest
import no.iktdev.mediaprocessing.ui.dto.status.SystemStatus
import no.iktdev.mediaprocessing.ui.service.CoordinatorClient
import no.iktdev.mediaprocessing.ui.service.MediaPathRewriteService
import no.iktdev.mediaprocessing.ui.service.StatusService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.reactive.function.client.WebClientResponseException
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter
import reactor.core.publisher.Mono
import java.util.*

@RestController
@RequestMapping("/api")
class UiApiController(
    private val coordinator: CoordinatorClient,
    private val statusService: StatusService,
    private val hub: UiSseHub,
    private val mediaPathRewriteService: MediaPathRewriteService,
) {

    @GetMapping("/sequences/active")
    fun getActive(): Mono<List<SequenceSummary>> =
        coordinator.getActiveSequences()

    @GetMapping("/sequences/recent")
    fun getRecent(@RequestParam(defaultValue = "15") limit: Int): Mono<List<SequenceSummary>> =
        coordinator.getRecentSequences(limit)

    @GetMapping("/sequences")
    fun getEventSequences(
        @RequestParam referenceId: UUID,
        @RequestParam(required = false) beforeEventId: UUID?,
        @RequestParam(required = false) afterEventId: UUID?,
        @RequestParam(defaultValue = "50") limit: Int
    ): Mono<List<SequenceEvent>> =
        coordinator.getEventSequence(referenceId, beforeEventId, afterEventId, limit)

    @PostMapping("/sequences/{referenceId}/continue")
    fun continueSequence(@PathVariable referenceId: UUID): ResponseEntity<String> {
        return when (val result = coordinator.continueSequence(referenceId)) {
            is ContinueResult.Success ->
                ResponseEntity.ok("Action accepted!")

            is ContinueResult.Failure ->
                ResponseEntity.status(HttpStatus.BAD_REQUEST).body(result.message)
        }
    }

    @PostMapping("/sequences/{referenceId}/delete")
    fun deleteSequence(@PathVariable referenceId: UUID): ResponseEntity<String> {
        return when (val result = coordinator.deleteSequence(referenceId)) {
            is ContinueResult.Success ->
                ResponseEntity.ok("Action accepted!")

            is ContinueResult.Failure ->
                ResponseEntity.status(HttpStatus.BAD_REQUEST).body(result.message)
        }
    }

    @GetMapping("/events")
    fun getEvents(query: EventQuery): Mono<Paginated<UiEvent>> {
        return coordinator.getPagedEvents(query)
    }

    @GetMapping("/events/history/{referenceId}/effective")
    fun getEffectiveHistory(
        @PathVariable referenceId: UUID,
    ): Mono<List<UiEvent>> {
        return coordinator.getEffectiveHistory(referenceId)
    }

    @GetMapping("/tasks")
    fun getTasks(query: TaskQuery): Mono<Paginated<UiTask>> {
        return coordinator.getPagedTasks(query)
    }

    @GetMapping("/tasks/{taskId}/reset")
    fun resetTask(@PathVariable taskId: UUID): Mono<ResponseEntity<ResetTaskResponse>> {
        return coordinator.resetTask(taskId)
            .map { ResponseEntity.ok(it) }
            .onErrorResume(WebClientResponseException::class.java) { ex ->
                if (ex.statusCode == HttpStatus.CONFLICT) {
                    Mono.just(ResponseEntity.status(HttpStatus.CONFLICT).build())
                } else {
                    Mono.error(ex)
                }
            }
    }

    @GetMapping("/tasks/{taskId}/reset/force")
    fun resetTaskForce(@PathVariable taskId: UUID): Mono<ResponseEntity<ResetTaskResponse>> {
        return coordinator.resetTaskForced(taskId)
            .map { ResponseEntity.ok(it) }
            .onErrorResume(WebClientResponseException::class.java) { ex ->
                if (ex.statusCode == HttpStatus.CONFLICT) {
                    Mono.just(ResponseEntity.status(HttpStatus.CONFLICT).build())
                } else {
                    Mono.error(ex)
                }
            }
    }



    @GetMapping("/tasks/active")
    fun getActiveTasks() = coordinator.getActiveTasks()

    @PostMapping("/operations/start")
    fun startProcess(@RequestBody req: StartProcessRequest): Mono<Map<String, String>> {
        val rewritten = req.copy(fileUri = mediaPathRewriteService.rewrite(req.fileUri))
        return coordinator.startProcess(rewritten)
    }

    @GetMapping("/sse") fun events(): SseEmitter = hub.createEmitter()

    @GetMapping("/status")
    fun getStatus(): SystemStatus {
        return statusService.status
    }

    @GetMapping("/health")
    fun getHealth(): Mono<CoordinatorHealth> {
        return coordinator.getHealth()
    }

    @GetMapping("/health/events")
    fun getHealthEventRate(): Mono<EventRate> {
        return coordinator.getEventRate()
    }

    @GetMapping("/health/storage")
    fun getHealthStorage(): Mono<List<DiskInfo>> {
        return coordinator.getHealthStorage()
    }

}
