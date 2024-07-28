package no.iktdev.mediaprocessing.coordinator.tasksV2.mapping

import no.iktdev.mediaprocessing.shared.common.contract.ffmpeg.AudioArgumentsDto
import no.iktdev.mediaprocessing.shared.common.contract.ffmpeg.VideoArgumentsDto

fun toFfmpegWorkerArguments(
    videoArguments: VideoArgumentsDto?,
    audioArguments: AudioArgumentsDto?
): List<String> {
    val arguments = mutableListOf<String>(
        *videoArguments?.codecParameters?.toTypedArray() ?: arrayOf(),
        *videoArguments?.optionalParameters?.toTypedArray() ?: arrayOf(),
        *audioArguments?.codecParameters?.toTypedArray() ?: arrayOf(),
        *audioArguments?.optionalParameters?.toTypedArray() ?: arrayOf()
    )
    videoArguments?.index?.let {
        arguments.addAll(listOf("-map", "0:v:$it"))
    }
    audioArguments?.index?.let {
        arguments.addAll(listOf("-map", "0:a:$it"))
    }
    return arguments
}