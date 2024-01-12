package no.iktdev.mediaprocessing.shared.kafka.dto.events_result

import com.google.gson.Gson
import com.google.gson.JsonObject
import no.iktdev.mediaprocessing.shared.kafka.dto.MessageDataWrapper
import no.iktdev.mediaprocessing.shared.kafka.dto.Status

data class VideoInfoPerformed(
    override val status: Status,
    val info: JsonObject,
    val outDirectory: String
)
    : MessageDataWrapper(status) {
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
    val title: String,
    val episode: Int,
    val season: Int,
    val episodeTitle: String?,
    override val fullName: String
): VideoInfo(type, fullName)

data class MovieInfo(
    override val type: String = "movie",
    val title: String,
    override val fullName: String
) : VideoInfo(type, fullName)

data class SubtitleInfo(
    val inputFile: String,
    val collection: String,
    val language: String
)

open class VideoInfo(
    @Transient open val type: String,
    @Transient open val fullName: String
) {
    fun toJsonObject(): JsonObject {
        return Gson().toJsonTree(this).asJsonObject
    }
}