package no.iktdev.mediaprocessing.coordinator

import no.iktdev.mediaprocessing.shared.common.socket.SocketImplementation
import no.iktdev.mediaprocessing.shared.kafka.core.CoordinatorProducer
import no.iktdev.mediaprocessing.shared.kafka.core.DefaultMessageListener
import no.iktdev.mediaprocessing.shared.kafka.core.KafkaImplementation
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import

@Configuration
class SocketLocalInit: SocketImplementation() {

}

@Configuration
@Import(CoordinatorProducer::class, DefaultMessageListener::class)
class KafkaLocalInit: KafkaImplementation() {
}