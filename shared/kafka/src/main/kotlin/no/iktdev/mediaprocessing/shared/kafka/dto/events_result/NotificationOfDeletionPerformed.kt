package no.iktdev.mediaprocessing.shared.kafka.dto.events_result

import no.iktdev.mediaprocessing.shared.kafka.core.KafkaBelongsToEvent
import no.iktdev.mediaprocessing.shared.kafka.core.KafkaEvents
import no.iktdev.mediaprocessing.shared.kafka.dto.MessageDataWrapper
import no.iktdev.mediaprocessing.shared.kafka.dto.Status

@KafkaBelongsToEvent(KafkaEvents.EventNotificationOfWorkItemRemoval)
data class NotificationOfDeletionPerformed(
    override val status: Status = Status.COMPLETED,
    override val message: String? = null,
    override val derivedFromEventId: String? = null, // Skal aldri settes derived
    val deletedEventId: String,
    val deletedEvent: KafkaEvents
): MessageDataWrapper()
