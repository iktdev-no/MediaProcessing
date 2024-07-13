package no.iktdev.mediaprocessing.shared.contract.data

import com.google.gson.Gson
import com.google.gson.JsonObject
import no.iktdev.eventi.data.EventMetadata
import no.iktdev.mediaprocessing.shared.contract.Events

data class MediaOutInformationConstructedEvent(
    override val metadata: EventMetadata,
    override val eventType: Events = Events.EventMediaReadOutNameAndType,
    override val data: MediaInfoReceived? = null
) : Event() {
}

data class MediaInfoReceived(
    val info: JsonObject,
    val outDirectory: String,
) {
    fun toValueObject(): MediaInfo? {
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
): MediaInfo(type, title, fullName)

data class MovieInfo(
    override val type: String = "movie",
    override val title: String,
    override val fullName: String
) : MediaInfo(type, title, fullName)

data class SubtitleInfo(
    val inputFile: String,
    val collection: String,
    val language: String
)

open class MediaInfo(
    @Transient open val type: String,
    @Transient open val title: String,
    @Transient open val fullName: String
) {
    fun toJsonObject(): JsonObject {
        return Gson().toJsonTree(this).asJsonObject
    }
}