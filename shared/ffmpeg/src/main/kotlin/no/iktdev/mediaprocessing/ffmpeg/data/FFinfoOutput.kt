package no.iktdev.mediaprocessing.ffmpeg.data

import com.google.gson.JsonObject

data class FFinfoOutput(
    override val success: Boolean,
    val error: String? = null,
    val data: JsonObject? = null
) : FFOutput() {
}