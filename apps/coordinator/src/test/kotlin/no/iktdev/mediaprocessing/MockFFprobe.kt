package no.iktdev.mediaprocessing

import com.google.gson.JsonObject
import kotlinx.coroutines.delay
import no.iktdev.mediaprocessing.ffmpeg.FFprobe
import no.iktdev.mediaprocessing.ffmpeg.data.FFinfoOutput

class MockFFprobe(
    private val delayMillis: Long = 0,
    private val result: FFinfoOutput? = null,
    private val throwException: Boolean = false
) : FFprobe("") {

    var lastInputFile: String? = null

    override suspend fun readJsonStreams(inputFile: String): FFinfoOutput {
        lastInputFile = inputFile
        if (delayMillis > 0) delay(delayMillis)
        if (throwException) throw RuntimeException("Simulated ffprobe failure")
        return result ?: FFinfoOutput(success = false, data = null, error = "No result configured")
    }

    companion object {
        fun success(json: JsonObject, delay: Long = 0) = MockFFprobe(
            result = FFinfoOutput(success = true, data = json, error = null),
            delayMillis = delay
        )
        fun failure(errorMsg: String) = MockFFprobe(
            result = FFinfoOutput(success = false, data = null, error = errorMsg)
        )
        fun exception() = MockFFprobe(throwException = true)
    }
}