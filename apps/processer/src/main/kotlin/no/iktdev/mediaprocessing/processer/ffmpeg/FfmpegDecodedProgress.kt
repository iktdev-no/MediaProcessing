package no.iktdev.mediaprocessing.processer.ffmpeg

import no.iktdev.mediaprocessing.shared.contract.dto.ProcesserProgress

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

data class ECT(val day: Int = 0, val hour: Int = 0, val minute: Int = 0, val second: Int = 0)