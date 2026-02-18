package no.iktdev.mediaprocessing.shared.common.event_task_contract.progress

import no.iktdev.eventi.models.Progress
import no.iktdev.mediaprocessing.ffmpeg.decoder.FfmpegDecodedProgress

class EncodeProgress(override val progress: Int, val ffmpegDecodedProgress: FfmpegDecodedProgress, override val message: String) : Progress() {
}