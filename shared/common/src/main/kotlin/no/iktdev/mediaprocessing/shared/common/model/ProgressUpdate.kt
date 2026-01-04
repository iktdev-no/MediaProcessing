package no.iktdev.mediaprocessing.shared.common.model

import no.iktdev.mediaprocessing.ffmpeg.decoder.FfmpegDecodedProgress

data class ProgressUpdate(val referenceId: String, val taskId: String, val progress: FfmpegDecodedProgress, val message: String?)
