package no.iktdev.mediaprocessing.processer.processors.segment

import no.iktdev.files.IFile
import no.iktdev.mediaprocessing.processer.config.FileUtil
import no.iktdev.mediaprocessing.processer.context.SegmentedRunnerContext
import no.iktdev.mediaprocessing.shared.common.event_task_contract.tasks.SegmentedEncodeTask

class SegmentedContextFactory(private val fileUtil: FileUtil) {
    fun createContext(taskData: SegmentedEncodeTask): SegmentedRunnerContext {
        val input = IFile(taskData.data.inputFile)

        val intermediateStore = fileUtil.getTemporaryStoreFolder(taskData.data.outputFolderName)
            .apply { if (!this.exists()) mkdirs() }

        val output = intermediateStore.using(taskData.data.outputFileName)
            .apply { if (!this.parentFile.exists()) parentFile.mkdirs() }

        val logDirectory = fileUtil.getLogDirectory()
            .using("encode_segment", taskData.taskId.toString())

        val baseOutputFileName = IFile(taskData.data.outputFileName).nameWithoutExtension
        val videoCheckpointFile = intermediateStore
            .using("VIDEO_CHECKPOINTS.json")
        val audioCheckpointFile = intermediateStore
            .using("AUDIO_CHECKPOINTS.json")

        return SegmentedRunnerContext(
            task = taskData,
            input = input,
            output = output,
            intermediateStore = intermediateStore,
            logDirectory = logDirectory,
            videoCheckpointFile = videoCheckpointFile,
            audioCheckpointFile = audioCheckpointFile,
            taskStartTime = System.currentTimeMillis(),
            videoInstruction = taskData.data.videoInstruction,
            audioInstructions = taskData.data.audioInstructions,
        )
    }
}