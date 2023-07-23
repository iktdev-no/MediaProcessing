package no.iktdev.streamit.content.common

import no.iktdev.streamit.content.common.CommonConfig
import no.iktdev.streamit.library.kafka.KafkaEvents
import no.iktdev.streamit.library.kafka.consumers.DefaultConsumer
import no.iktdev.streamit.library.kafka.dto.Message
import no.iktdev.streamit.library.kafka.dto.Status
import no.iktdev.streamit.library.kafka.dto.StatusType
import no.iktdev.streamit.library.kafka.listener.deserializer.IMessageDataDeserialization
import no.iktdev.streamit.library.kafka.listener.sequential.ISequentialMessageEvent
import no.iktdev.streamit.library.kafka.listener.sequential.SequentialMessageListener
import no.iktdev.streamit.library.kafka.producer.DefaultProducer

abstract class SequentialKafkaReader(subId: String): DefaultKafkaReader(subId), ISequentialMessageEvent {

    abstract val accept: KafkaEvents
    abstract val subAccepts: List<KafkaEvents>


}