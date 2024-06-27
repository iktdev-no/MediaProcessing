package no.iktdev.mediaprocessing.processer.ffmpeg

import no.iktdev.mediaprocessing.processer.ffmpeg.progress.FfmpegDecodedProgress

interface FfmpegListener {
    fun onStarted(inputFile: String)
    fun onCompleted(inputFile: String, outputFile: String)
    fun onProgressChanged(inputFile: String, progress: FfmpegDecodedProgress)
    fun onError(inputFile: String, message: String) {}
}