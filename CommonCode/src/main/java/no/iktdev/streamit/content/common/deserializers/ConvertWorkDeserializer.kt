package no.iktdev.streamit.content.common.deserializers

import no.iktdev.streamit.content.common.dto.reader.work.ConvertWork
import no.iktdev.streamit.content.common.dto.reader.work.EncodeWork
import no.iktdev.streamit.content.common.dto.reader.work.ExtractWork
import no.iktdev.streamit.library.kafka.dto.Message
import no.iktdev.streamit.library.kafka.listener.deserializer.IMessageDataDeserialization

class ConvertWorkDeserializer: IMessageDataDeserialization<ConvertWork> {
    override fun deserialize(incomingMessage: Message): ConvertWork? {
        return incomingMessage.dataAs(ConvertWork::class.java)
    }
}