package no.iktdev.streamit.content.encode.progress

import no.iktdev.streamit.content.common.dto.reader.work.WorkBase
import java.io.File
import java.lang.StringBuilder
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.TimeUnit
import kotlin.math.floor

class ProgressDecoder(val workBase: WorkBase) {
    var duration: Int? = null
        set(value) {
            if (field == null || field == 0)
                field = value
        }
    fun parseVideoProgress(lines: List<String>): DecodedProgressData? {
        var frame: Int? = null
        var progress: String? = null
        val metadataMap = mutableMapOf<String, String>()

        for (line in lines) {
            val keyValuePairs = Regex("=\\s*").replace(line, "=").split(" ").filter { it.isNotBlank() }
            for (keyValuePair in keyValuePairs) {
                val (key, value) = keyValuePair.split("=")
                metadataMap[key] = value
            }

            if (frame == null) {
                frame = metadataMap["frame"]?.toIntOrNull()
            }

            progress = metadataMap["progress"]
        }

        return if (progress != null) {
            // When "progress" is found, build and return the VideoMetadata object
            DecodedProgressData(
                frame, metadataMap["fps"]?.toDoubleOrNull(), metadataMap["stream_0_0_q"]?.toDoubleOrNull(),
                metadataMap["bitrate"], metadataMap["total_size"]?.toIntOrNull(), metadataMap["out_time_us"]?.toLongOrNull(),
                metadataMap["out_time_ms"]?.toLongOrNull(), metadataMap["out_time"], metadataMap["dup_frames"]?.toIntOrNull(),
                metadataMap["drop_frames"]?.toIntOrNull(), metadataMap["speed"]?.toDoubleOrNull(), progress
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


    private fun getProgressTime(time: Long?): Long {
        if (time == null) return 0
        return time / 1000L
    }
    fun getProgress(decoded: DecodedProgressData): Progress {
        if (duration == null) return Progress(workId = workBase.workId, outFileName = File(workBase.outFile).name)
        val diff = getProgressTime(decoded.out_time_ms).toDouble() / duration!!.toDouble()
        val progress = floor(diff*100).toInt()

        val ect = getEstimatedTimeRemaining(decoded)

        return Progress(
            workId = workBase.workId, outFileName = File(workBase.outFile).name,
            progress = progress,
            estimatedCompletion = getETA(ect)
        )
    }

    fun getEstimatedTimeRemaining(decoded: DecodedProgressData): Long {
        val position = getProgressTime(decoded.out_time_ms)
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