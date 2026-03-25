package no.iktdev.mediaprocessing.processer.linear

import no.iktdev.files.IFile
import no.iktdev.mediaprocessing.processer.config.FileUtil
import no.iktdev.mediaprocessing.processer.context.LinearRunnerContext
import no.iktdev.mediaprocessing.shared.common.event_task_contract.tasks.LinearEncodeTask
import java.io.File

class LinearContextFactory(private val fileUtil: FileUtil) {
    fun createContext(taskData: LinearEncodeTask): LinearRunnerContext {
        val input = IFile(taskData.data.inputFile)

        val intermediateStore = fileUtil.getTemporaryStoreFolder(taskData.data.outputFolderName)
            .apply { if (!this.exists()) mkdirs() }

        val output = intermediateStore.using(taskData.data.outputFileName)
            .apply { if (!this.parentFile.exists()) parentFile.mkdirs() }

        val logDirectory = fileUtil.getLogDirectory()
            .using("encode_segment", taskData.taskId.toString())

        val baseOutputFileName = File(taskData.data.outputFileName).nameWithoutExtension
        val videoCheckpointFile = intermediateStore
            .using("VIDEO_CHECKPOINTS.json")
        val audioCheckpointFile = intermediateStore
            .using("AUDIO_CHECKPOINTS.json")

        return LinearRunnerContext(
            task = taskData,
            input = input,
            output = output,
            intermediateStore = intermediateStore,
            logDirectory = logDirectory,
            audioCheckpointFile = audioCheckpointFile,
            taskStartTime = System.currentTimeMillis(),
            videoInstruction = taskData.data.videoInstruction,
            audioInstructions = taskData.data.audioInstructions,
        )
    }
}