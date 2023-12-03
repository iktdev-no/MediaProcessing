package no.iktdev.mediaprocessing.coordinator.mapping

import no.iktdev.mediaprocessing.shared.dto.MetadataDto
import no.iktdev.mediaprocessing.shared.kafka.core.KafkaEvents
import no.iktdev.mediaprocessing.shared.kafka.dto.MessageDataWrapper
import no.iktdev.mediaprocessing.shared.kafka.dto.events_result.BaseInfoPerformed
import no.iktdev.mediaprocessing.shared.kafka.dto.events_result.MetadataPerformed
import no.iktdev.mediaprocessing.shared.kafka.dto.events_result.pyMetadata
import no.iktdev.mediaprocessing.shared.kafka.dto.isSuccess
import no.iktdev.mediaprocessing.shared.persistance.PersistentMessage
import no.iktdev.streamit.library.kafka.dto.Status



class MetadataMapping(val events: List<PersistentMessage>) {


    fun map(): MetadataDto {
        val baseInfo = events.find { it.data is BaseInfoPerformed }?.data as BaseInfoPerformed?
        val meta = events.find { it.data is MetadataPerformed }?.data as MetadataPerformed?

        if (!baseInfo.isSuccess()) {
            return
        }

        return MetadataDto(
            title = meta.data?.title,
            type = meta.data.type,


        )

    }

}