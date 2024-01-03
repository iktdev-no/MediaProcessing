package no.iktdev.mediaprocessing.processer.ffmpeg

import java.lang.StringBuilder
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.TimeUnit
import kotlin.math.floor

class FfmpegProgressDecoder {

    data class DecodedProgressData(
        val frame: Int?,
        val fps: Double?,
        val stream_0_0_q: Double?,
        val bitrate: String?,
        val total_size: Int?,
        val out_time_us: Long?,
        val out_time_ms: Long?,
        val out_time: String?,
        val dup_frames: Int?,
        val drop_frames: Int?,
        val speed: Double?,
        val progress: String?
    )

    val expectedKeys = listOf<String>(
        "frame=",
        "fps=",
        "stream_0_0_q=",
        "bitrate=",
        "total_size=",
        "out_time_us=",
        "out_time_ms=",
        "out_time=",
        "dup_frames=",
        "drop_frames=",
        "speed=",
        "progress="
    )
    var duration: Int? = null
        set(value) {
            if (field == null || field == 0)
                field = value
        }
    var durationTime: String = "NA"
    fun parseVideoProgress(lines: List<String>): DecodedProgressData? {
        var frame: Int? = null
        var progress: String? = null
        val metadataMap = mutableMapOf<String, String>()

        try {
            val eqValue = Regex("=")
            for (line in lines) {
                val keyValuePairs = Regex("=\\s*").replace(line, "=").split(" ").filter { it.isNotBlank() }.filter { eqValue.containsMatchIn(it) }
                for (keyValuePair in keyValuePairs) {
                    val (key, value) = keyValuePair.split("=")
                    metadataMap[key] = value
                }

                if (frame == null) {
                    frame = metadataMap["frame"]?.toIntOrNull()
                }

                progress = metadataMap["progress"]
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return if (progress != null) {
            // When "progress" is found, build and return the VideoMetadata object
            DecodedProgressData(
                frame, metadataMap["fps"]?.toDoubleOrNull(), metadataMap["stream_0_0_q"]?.toDoubleOrNull(),
                metadataMap["bitrate"], metadataMap["total_size"]?.toIntOrNull(), metadataMap["out_time_us"]?.toLongOrNull(),
                metadataMap["out_time_ms"]?.toLongOrNull(), metadataMap["out_time"], metadataMap["dup_frames"]?.toIntOrNull(),
                metadataMap["drop_frames"]?.toIntOrNull(), metadataMap["speed"]?.replace("x", "", ignoreCase = true)?.toDoubleOrNull(), progress
            )
        } else {
            null // If "progress" is not found, return null
        }
    }


    fun isDuration(value: String): Boolean {
        return value.contains("Duration", ignoreCase = true)
    }
    fun setDuration(value: String) {
        val results = Regex("Duration:\\s*([^,]+),").find(value)?.groupValues?.firstOrNull()
        durationTime = Regex("[0-9]+:[0-9]+:[0-9]+.[0-9]+").find(results.toString())?.value ?: "NA"
        duration = timeSpanToSeconds(results)
    }

    private fun timeSpanToSeconds(time: String?): Int?
    {
        time ?: return null
        val timeString = Regex("[0-9]+:[0-9]+:[0-9]+.[0-9]+").find(time) ?: return null
        val strippedMS = Regex("[0-9]+:[0-9]+:[0-9]+").find(timeString.value) ?: return null
        val outTime = LocalTime.parse(strippedMS.value, DateTimeFormatter.ofPattern("HH:mm:ss"))
        return outTime.toSecondOfDay()
    }


    fun getProgress(decoded: DecodedProgressData): FfmpegDecodedProgress {
        if (duration == null)
            return FfmpegDecodedProgress(duration = durationTime, time = "NA", speed = "NA")
        val progressTime = timeSpanToSeconds(decoded.out_time) ?: 0
        val progress = floor((progressTime.toDouble() / duration!!.toDouble()) *100).toInt()

        val ect = getEstimatedTimeRemaining(decoded)

        return FfmpegDecodedProgress(
            progress = progress,
            estimatedCompletionSeconds = ect,
            estimatedCompletion = getETA(ect),
            duration = durationTime,
            time = decoded.out_time ?: "NA",
            speed = decoded.speed?.toString() ?: "NA"
        )
    }

    fun getEstimatedTimeRemaining(decoded: DecodedProgressData): Long {
        val position = timeSpanToSeconds(decoded.out_time) ?: 0
        return if(duration == null || decoded.speed == null) -1 else
            Math.round(Math.round(duration!!.toDouble() - position.toDouble()) / decoded.speed)
    }

    fun getECT(time: Long): ECT {
        var seconds = time
        val day = TimeUnit.SECONDS.toDays(seconds)
        seconds -= java.util.concurrent.TimeUnit.DAYS.toSeconds(day)

        val hour = TimeUnit.SECONDS.toHours(seconds)
        seconds -= java.util.concurrent.TimeUnit.HOURS.toSeconds(hour)

        val minute = TimeUnit.SECONDS.toMinutes(seconds)
        seconds -= java.util.concurrent.TimeUnit.MINUTES.toSeconds(minute)

        return ECT(day.toInt(), hour.toInt(), minute.toInt(), seconds.toInt())
    }
    private fun getETA(time: Long): String {
        val etc = getECT(time) ?: return "Unknown"
        val str = StringBuilder()
        if (etc.day > 0) {
            str.append("${etc.day}d").append(" ")
        }
        if (etc.hour > 0) {
            str.append("${etc.hour}h").append(" ")
        }
        if (etc.day == 0 && etc.minute > 0) {
            str.append("${etc.minute}m").append(" ")
        }
        if (etc.hour == 0 && etc.second > 0) {
            str.append("${etc.second}s").append(" ")
        }
        return str.toString().trim()
    }
}