package no.iktdev.streamit.content.common.deserializers

import no.iktdev.streamit.content.common.dto.reader.work.EncodeWork
import no.iktdev.streamit.library.kafka.dto.Message
import no.iktdev.streamit.library.kafka.listener.deserializer.IMessageDataDeserialization

class EncodeWorkDeserializer: IMessageDataDeserialization<EncodeWork> {
    override fun deserialize(incomingMessage: Message): EncodeWork? {
        return incomingMessage.dataAs(EncodeWork::class.java)
    }
}