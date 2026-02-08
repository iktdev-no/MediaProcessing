package no.iktdev.mediaprocessing.ui.dto.passthrough


data class ProgressUpdate(val referenceId: String, val taskId: String, val progress: FfmpegDecodedProgress, val message: String?)
