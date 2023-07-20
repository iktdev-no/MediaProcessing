package no.iktdev.streamit.content.reader.analyzer.contentDeterminator

import no.iktdev.streamit.content.common.dto.Metadata
import no.iktdev.streamit.content.reader.fileWatcher.FileWatcher
import no.iktdev.streamit.library.kafka.KafkaEvents
import no.iktdev.streamit.library.kafka.dto.Message
import no.iktdev.streamit.library.kafka.dto.StatusType
import no.iktdev.streamit.library.kafka.listener.sequential.IMessageDataDeserialization

class Deserializers {

    val fileReceived = object : IMessageDataDeserialization<FileWatcher.FileResult> {
        override fun deserialize(incomingMessage: Message): FileWatcher.FileResult? {
            if (incomingMessage.status.statusType != StatusType.SUCCESS) {
                return null
            }
            return incomingMessage.dataAs(FileWatcher.FileResult::class.java)
        }
    }

    val metadataReceived = object: IMessageDataDeserialization<Metadata> {
        override fun deserialize(incomingMessage: Message): Metadata? {
            if (incomingMessage.status.statusType != StatusType.SUCCESS) {
                return null
            }
            return incomingMessage.dataAs(Metadata::class.java)
        }

    }
    fun getDeserializers(): Map<String, IMessageDataDeserialization<*>> {
        return mutableMapOf(
            KafkaEvents.EVENT_READER_RECEIVED_FILE.event to fileReceived,
            KafkaEvents.EVENT_METADATA_OBTAINED.event to metadataReceived
        )
    }

}