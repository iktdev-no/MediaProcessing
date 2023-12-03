package no.iktdev.mediaprocessing.shared.kafka.dto

import org.apache.kafka.common.header.Headers
import org.apache.kafka.common.record.TimestampType

data class DeserializedConsumerRecord<K, V>(
    val topic: String,
    val partition: Int,
    val offset: Long,
    val timestamp: Long,
    val timestampType: TimestampType,
    val headers: Headers,
    val key: K,
    val value: V,
    val leaderEpoch: Int?
)
