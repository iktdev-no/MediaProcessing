package no.iktdev.streamit.content.common.deserializers

import no.iktdev.streamit.content.common.dto.reader.MovieInfo
import no.iktdev.streamit.library.kafka.dto.Message
import no.iktdev.streamit.library.kafka.listener.deserializer.IMessageDataDeserialization

class MovieInfoDeserializer: IMessageDataDeserialization<MovieInfo> {
    override fun deserialize(incomingMessage: Message): MovieInfo? {
        return incomingMessage.dataAs(MovieInfo::class.java)
    }
}