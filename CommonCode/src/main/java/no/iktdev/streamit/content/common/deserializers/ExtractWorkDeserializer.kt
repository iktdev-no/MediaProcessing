package no.iktdev.streamit.content.common.deserializers

import no.iktdev.streamit.content.common.dto.reader.work.EncodeWork
import no.iktdev.streamit.content.common.dto.reader.work.ExtractWork
import no.iktdev.streamit.library.kafka.dto.Message
import no.iktdev.streamit.library.kafka.listener.deserializer.IMessageDataDeserialization

class ExtractWorkDeserializer: IMessageDataDeserialization<ExtractWork> {
    override fun deserialize(incomingMessage: Message): ExtractWork? {
        return incomingMessage.dataAs(ExtractWork::class.java)
    }
}