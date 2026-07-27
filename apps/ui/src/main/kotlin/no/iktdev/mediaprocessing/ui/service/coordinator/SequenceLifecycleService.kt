package no.iktdev.mediaprocessing.ui.service.coordinator

import no.iktdev.mediaprocessing.ui.controller.passthrough.EventController
import no.iktdev.mediaprocessing.ui.controller.passthrough.TaskController
import no.iktdev.mediaprocessing.transferModel.coordinatorUi.UiEvent
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class SequenceLifecycleService(
    private val eventController: EventController,
    private val taskController: TaskController,
) {

   /* fun getLifecycle(referenceId: UUID) {
        val events: List<UiEvent> = eventController.getEffectiveHistory(referenceId)
        val tasks = taskController.getTasks(referenceId)


    }*/

}