package no.iktdev.mediaprocessing.processer.context

import no.iktdev.files.IFile
import no.iktdev.mediaprocessing.ffmpeg.FFmpeg

interface FfProvider {
    fun getExecutableFfprobe(): String

    fun getFfmpeg(listener: FFmpeg.Listener? = null, logDirectory: IFile): FFmpeg
}
