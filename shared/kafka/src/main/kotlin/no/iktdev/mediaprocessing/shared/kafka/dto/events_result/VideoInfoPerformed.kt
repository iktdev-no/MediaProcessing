package no.iktdev.mediaprocessing.shared.kafka.dto.events_result

import com.google.gson.Gson
import com.google.gson.JsonObject
import no.iktdev.mediaprocessing.shared.kafka.core.KafkaBelongsToEvent
import no.iktdev.mediaprocessing.shared.kafka.core.KafkaEvents
import no.iktdev.mediaprocessing.shared.kafka.dto.MessageDataWrapper
import no.iktdev.mediaprocessing.shared.kafka.dto.Status

@KafkaBelongsToEvent(KafkaEvents.EventMediaReadOutNameAndType)
data class VideoInfoPerformed(
    override val status: Status,
    val info: JsonObject,
    val outDirectory: String,
    override val derivedFromEventId: String?
) : MessageDataWrapper(status, derivedFromEventId) {
        fun toValueObject(): VideoInfo? {
            val type = info.get("type").asString
            return when (type) {
                "movie" -> Gson().fromJson(info.toString(), MovieInfo::class.java)
                "serie" -> Gson().fromJson(info.toString(), EpisodeInfo::class.java)
                else -> null
            }
        }
    }


data class EpisodeInfo(
    override val type: String = "serie",
    override val title: String,
    val episode: Int,
    val season: Int,
    val episodeTitle: String?,
    override val fullName: String
): VideoInfo(type, title, fullName)

data class MovieInfo(
    override val type: String = "movie",
    override val title: String,
    override val fullName: String
) : VideoInfo(type, title, fullName)

data class SubtitleInfo(
    val inputFile: String,
    val collection: String,
    val language: String
)

@KafkaBelongsToEvent(KafkaEvents.EventMediaReadOutNameAndType)
open class VideoInfo(
    @Transient open val type: String,
    @Transient open val title: String,
    @Transient open val fullName: String
) {
    fun toJsonObject(): JsonObject {
        return Gson().toJsonTree(this).asJsonObject
    }
}