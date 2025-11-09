package no.iktdev.mediaprocessing.shared.common.model

import com.google.gson.Gson
import com.google.gson.JsonObject

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