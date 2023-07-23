package no.iktdev.streamit.content.common.deserializers

import no.iktdev.streamit.content.common.dto.reader.EpisodeInfo
import no.iktdev.streamit.library.kafka.dto.Message
import no.iktdev.streamit.library.kafka.listener.deserializer.IMessageDataDeserialization

class EpisodeInfoDeserializer: IMessageDataDeserialization<EpisodeInfo> {
    override fun deserialize(incomingMessage: Message): EpisodeInfo? {
        return incomingMessage.dataAs(EpisodeInfo::class.java)
    }
}