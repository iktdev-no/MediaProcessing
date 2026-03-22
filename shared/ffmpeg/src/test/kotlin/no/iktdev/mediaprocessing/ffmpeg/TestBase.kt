package no.iktdev.mediaprocessing.ffmpeg

import no.iktdev.files.FakeFile
import no.iktdev.mediaprocessing.ffmpeg.FFmpeg
import no.iktdev.files.IFile
import no.iktdev.mediaprocessing.ffmpeg.data.FFmpegInstructions
import no.iktdev.mediaprocessing.ffmpeg.dsl.AudioCodec
import no.iktdev.mediaprocessing.ffmpeg.dsl.VideoCodec
import no.iktdev.mediaprocessing.ffmpeg.dsl.args.section.InputSection
import no.iktdev.mediaprocessing.ffmpeg.dsl.args.section.OutputSection
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import java.io.File

open class TestBase {
    val workFolder = FakeFile("build").using("tests")

    val logDirectory = File(workFolder.parent, "logs")


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


    @BeforeEach
    fun cleanup() {
        FakeFile.wipe()
    }


    fun IFile.asFake(): FakeFile? = this as? FakeFile

    companion object {
        @JvmStatic
        @BeforeAll
        fun setup(): Unit {
            IFile.factory = { path -> FakeFile(path) }
        }
    }

}