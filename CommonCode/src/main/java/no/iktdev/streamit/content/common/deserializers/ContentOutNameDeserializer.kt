package no.iktdev.streamit.content.common.deserializers

import no.iktdev.streamit.content.common.dto.ContentOutName
import no.iktdev.streamit.library.kafka.dto.Message
import no.iktdev.streamit.library.kafka.listener.deserializer.IMessageDataDeserialization

class ContentOutNameDeserializer: IMessageDataDeserialization<ContentOutName> {
    override fun deserialize(incomingMessage: Message): ContentOutName? {
        return incomingMessage.dataAs(ContentOutName::class.java)
    }
}