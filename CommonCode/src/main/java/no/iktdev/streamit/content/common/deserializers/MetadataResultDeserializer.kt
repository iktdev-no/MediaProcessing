package no.iktdev.streamit.content.common.deserializers

import no.iktdev.streamit.content.common.dto.Metadata
import no.iktdev.streamit.library.kafka.dto.Message
import no.iktdev.streamit.library.kafka.listener.deserializer.IMessageDataDeserialization

class MetadataResultDeserializer: IMessageDataDeserialization<Metadata> {
    override fun deserialize(incomingMessage: Message): Metadata? {
        return incomingMessage.dataAs(Metadata::class.java)
    }
}