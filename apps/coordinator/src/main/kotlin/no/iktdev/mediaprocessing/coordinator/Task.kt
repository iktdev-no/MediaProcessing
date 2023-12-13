package no.iktdev.mediaprocessing.coordinator

import no.iktdev.mediaprocessing.shared.common.persistance.PersistentMessage
import no.iktdev.mediaprocessing.shared.kafka.core.CoordinatorProducer
import no.iktdev.mediaprocessing.shared.kafka.core.KafkaEvents
import org.springframework.beans.factory.annotation.Autowired

abstract class TaskCreator: TaskCreatorListener {
    @Autowired
    lateinit var producer: CoordinatorProducer
    open fun isPrerequisitesOk(events: List<PersistentMessage>): Boolean {
        return true
    }
}

interface TaskCreatorListener {
    fun onEventReceived(referenceId: String, event: PersistentMessage,  events: List<PersistentMessage>): Unit
}