package no.iktdev.mediaprocessing.processer

import no.iktdev.mediaprocessing.shared.common.Defaults
import no.iktdev.mediaprocessing.shared.kafka.core.CoordinatorProducer
import no.iktdev.mediaprocessing.shared.kafka.core.DefaultMessageListener
import no.iktdev.mediaprocessing.shared.kafka.core.KafkaImplementation
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.springframework.web.client.RestTemplate

//@Configuration
//class SocketLocalInit: SocketImplementation()

@Configuration
@Import(CoordinatorProducer::class, DefaultMessageListener::class)
class KafkaLocalInit: KafkaImplementation() {
}

@Configuration
public class DefaultProcesserConfiguration: Defaults() {
}
