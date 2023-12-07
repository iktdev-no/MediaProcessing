package no.iktdev.mediaprocessing.shared.common

import no.iktdev.exfl.coroutines.Coroutines
import no.iktdev.mediaprocessing.shared.common.kafka.CoordinatorProducer
import no.iktdev.mediaprocessing.shared.kafka.core.DefaultMessageListener
import no.iktdev.mediaprocessing.shared.kafka.dto.MessageDataWrapper
import org.springframework.stereotype.Service

@Service
abstract class ProcessingService(var producer: CoordinatorProducer, var listener: DefaultMessageListener) {
    val io = Coroutines.io()
    abstract fun onResult(referenceId: String, data: MessageDataWrapper)

}