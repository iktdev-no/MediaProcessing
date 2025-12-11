package no.iktdev.mediaprocessing.ffmpeg

import com.github.pgreze.process.ProcessResult
import com.github.pgreze.process.Redirect
import com.github.pgreze.process.process
import com.google.gson.Gson
import com.google.gson.JsonObject
import no.iktdev.mediaprocessing.ffmpeg.data.FFinfoOutput

abstract class FFprobe {
    open val defaultArguments: List<String> = listOf("-v", "quiet")
    abstract val executable: String

    open suspend fun readJsonStreams(inputFile: String): FFinfoOutput {
        var error: String? = null
        val output = mutableListOf<String>()
        val args = defaultArguments + listOf("-print_format", "json", "-show_format", "-show_streams", inputFile)
        val processResult = execute(args) { output.add(it) }

        val success = processResult.resultCode == 0
        val longString = output.joinToString(" ")
        val json = try {
            Gson().fromJson(longString, JsonObject::class.java)
        } catch (e: Exception) {
            error = "Could not parse ffinfo output to JSON: ${e.message}"
            null
        }
        return FFinfoOutput(
            success = success,
            data = json,
            error = error
        )
    }

    private suspend fun execute(arguments: List<String>, output: (String) -> Unit): ProcessResult {
        return process(executable, *arguments.toTypedArray(),
            stderr = Redirect.CAPTURE,
            stdout = Redirect.CAPTURE,
            consumer = {
                output(it)
            })
    }
}