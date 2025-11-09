package no.iktdev.mediaprocessing.ffmpeg.decoder


data class FfmpegDecodedProgress(
    val progress: Int = -1,
    val time: String,
    val duration: String,
    val speed: String,
    val estimatedCompletionSeconds: Long = -1,
    val estimatedCompletion: String = "Unknown",
) {
    fun toProcessProgress(): ProcesserProgress {
        return ProcesserProgress(
            progress = this.progress,
            speed = this.speed,
            timeWorkedOn = this.time,
            timeLeft = this.estimatedCompletion
        )

    }
}

data class ProcesserProgress(
    val progress: Int = -1,
    val speed: String? = null,
    val timeWorkedOn: String? = null,
    val timeLeft: String? = "Unknown", // HH mm
)

data class ECT(val day: Int = 0, val hour: Int = 0, val minute: Int = 0, val second: Int = 0)