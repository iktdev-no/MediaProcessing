package no.iktdev.streamit.content.common.deserializers

import no.iktdev.streamit.content.common.dto.reader.FileResult
import no.iktdev.streamit.library.kafka.KafkaEvents
import no.iktdev.streamit.library.kafka.dto.Message
import no.iktdev.streamit.library.kafka.dto.StatusType
import no.iktdev.streamit.library.kafka.listener.deserializer.IMessageDataDeserialization

class FileResultDeserializer: IMessageDataDeserialization<FileResult> {
    override fun deserialize(incomingMessage: Message): FileResult? {
        return incomingMessage.dataAs(FileResult::class.java)
    }
}
