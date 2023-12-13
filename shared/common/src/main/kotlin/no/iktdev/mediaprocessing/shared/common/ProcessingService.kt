package no.iktdev.mediaprocessing.shared.common

import no.iktdev.exfl.coroutines.Coroutines
import no.iktdev.mediaprocessing.shared.kafka.core.CoordinatorProducer
import no.iktdev.mediaprocessing.shared.kafka.core.DefaultMessageListener
import no.iktdev.mediaprocessing.shared.kafka.dto.MessageDataWrapper
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Import
import org.springframework.stereotype.Service
import javax.annotation.PostConstruct

@Service
@Import(DefaultMessageListener::class)
abstract class ProcessingService() {
    val io = Coroutines.io()

    @Autowired
    lateinit var producer: CoordinatorProducer

    abstract fun onResult(referenceId: String, data: MessageDataWrapper)
    @PostConstruct
    abstract fun onReady(): Unit
}