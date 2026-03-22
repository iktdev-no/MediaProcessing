package no.iktdev.mediaprocessing.processer

import no.iktdev.eventi.models.Task
import no.iktdev.files.FakeFile
import no.iktdev.mediaprocessing.ffmpeg.FFmpeg
import no.iktdev.mediaprocessing.processer.config.ExecutablesConfig
import no.iktdev.mediaprocessing.processer.context.FfProvider
import no.iktdev.mediaprocessing.processer.segment.SegmentedProgressListener
import no.iktdev.mediaprocessing.processer.segment.SegmentedRunnerContext
import no.iktdev.files.IFile
import no.iktdev.mediaprocessing.ffmpeg.data.FFmpegInstructions
import no.iktdev.mediaprocessing.ffmpeg.dsl.AudioCodec
import no.iktdev.mediaprocessing.ffmpeg.dsl.VideoCodec
import no.iktdev.mediaprocessing.ffmpeg.dsl.args.section.InputSection
import no.iktdev.mediaprocessing.ffmpeg.dsl.args.section.OutputSection
import no.iktdev.mediaprocessing.shared.common.event_task_contract.tasks.SegmentedEncodeTask
import no.iktdev.mediaprocessing.shared.common.model.task.data.SegmentEncodeData
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import java.io.File

open class TestBase {
    val workFolder = FakeFile("build").using("tests")

    val mockExecConfig = ExecutablesConfig(ffmpeg = "ffmpeg", ffprobe = "ffprobe")
    val logDirectory = File(workFolder.parent, "logs")

    val mockFfProvider = object : FfProvider {
        override fun getExecutableFfprobe(): String {
            return "ffprobe"
        }

        override fun getFfmpeg(
            listener: FFmpeg.Listener?,
            logDirectory: IFile
        ): FFmpeg {
            return object : FFmpeg("ffmpeg", logDirectory) {}
        }
    }

    fun defaultTestSegmentedProgressListener(task: Task): SegmentedProgressListener {
        return SegmentedProgressListener(task, null)
    }

    fun fakeVideoInstruction(input: IFile, outputName: String = "video.mkv") =
        FFmpegInstructions(
            inputs = InputSection().apply {
                file(input.absolutePath) {
                    video(0) {
                        map = true
                        codec = VideoCodec.Copy
                    }
                }
            },
            output = OutputSection(outputName).apply {
                overwrite = true
                useWorkFile = true
            },
        )


    fun fakeAudioInstruction(input: IFile, outputName: String) =
        FFmpegInstructions(
            inputs = InputSection().apply {
                file(input.absolutePath) {
                    audio(0) {
                        map = true
                        codec = AudioCodec.Copy
                    }
                }
            },
            output = OutputSection(outputName).apply {
                overwrite = true
                useWorkFile = true
            },
        )



    fun fakeContext(
        input: IFile = FakeFile("/input.mp4", exists = true),
        output: IFile = FakeFile("/output.mp4"),
        intermediate: IFile = FakeFile("/intermediate", directory = true),
        logs: IFile = FakeFile("/logs", directory = true),
        videoCp: IFile = FakeFile("/video_cp.json"),
        audioCp: IFile = FakeFile("/audio_cp.json"),
        taskStart: Long = System.currentTimeMillis(),
        videoInstruction: FFmpegInstructions = FFmpegInstructions(
            inputs = InputSection().apply {
                file(input.absolutePath) {
                    video(0) {
                        map = true
                        codec = VideoCodec.Copy
                    }
                }
            },
            output = OutputSection(output.name).apply {
                overwrite = true
                useWorkFile = true
            },
        ),
        audioInstructions: List<FFmpegInstructions> = emptyList()
    ): SegmentedRunnerContext {

        val data = SegmentEncodeData(
            videoInstruction = videoInstruction,
            audioInstructions = audioInstructions,
            outputFileName = output.name,
            outputFolderName = output.parent ?: "/",
            inputFile = input.path
        )

        val task = SegmentedEncodeTask(data = data)

        return SegmentedRunnerContext(
            task = task,
            input = input,
            output = output,
            intermediateStore = intermediate,
            logDirectory = logs,
            videoCheckpointFile = videoCp,
            audioCheckpointFile = audioCp,
            taskStartTime = taskStart,
            videoInstruction = videoInstruction,
            audioInstructions = audioInstructions
        )
    }

    @BeforeEach
    fun cleanup() {
        FakeFile.fileRegistry.clear()
    }


    fun IFile.asFake(): FakeFile? = this as? FakeFile

    companion object {
        @JvmStatic
        @BeforeAll
        fun setup(): Unit {
            IFile.factory = { path -> FakeFile(path, exists = true) }
        }
    }

}