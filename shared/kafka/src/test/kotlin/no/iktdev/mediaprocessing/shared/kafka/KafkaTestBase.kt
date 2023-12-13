package no.iktdev.mediaprocessing.shared.kafka

import no.iktdev.mediaprocessing.shared.kafka.core.DefaultMessageListener
import org.apache.kafka.clients.admin.AdminClient
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.springframework.kafka.core.KafkaTemplate

@ExtendWith(MockitoExtension::class)
class KafkaTestBase {

    @Mock
    lateinit var kafkaTemplate: KafkaTemplate<String, String>

    @Mock
    lateinit var adminClient: AdminClient

    /*@InjectMocks
    lateinit var defaultProducer: DefaultProducer

    @InjectMocks
    lateinit var defaultConsumer: DefaultConsumer*/

    @InjectMocks
    lateinit var defaultListener: DefaultMessageListener

}