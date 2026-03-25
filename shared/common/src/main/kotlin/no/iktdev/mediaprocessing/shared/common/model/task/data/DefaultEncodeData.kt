package no.iktdev.mediaprocessing.shared.common.model.task.data

import no.iktdev.mediaprocessing.ffmpeg.data.FFmpegInstructions

class DefaultEncodeData(
    val videoInstruction: FFmpegInstructions,
    val audioInstructions: List<FFmpegInstructions>,
    outputFileName: String,
    outputFolderName: String,
    inputFile: String
): EncodeDataBase(inputFile = inputFile, outputFileName = outputFileName, outputFolderName = outputFolderName) {
}