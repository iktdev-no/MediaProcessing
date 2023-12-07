package no.iktdev.mediaprocessing.shared.common.runner

import com.github.pgreze.process.Redirect
import com.github.pgreze.process.process

data class CodeToOutput(
    val statusCode: Int,
    val output: List<String>
)

suspend fun getOutputUsing(executable: String, vararg arguments: String): CodeToOutput {
    val result: MutableList<String> = mutableListOf()
    val code = process(executable, *arguments,
        stderr = Redirect.CAPTURE,
        stdout = Redirect.CAPTURE,
        consumer = {
            result.add(it)
        }).resultCode
    return CodeToOutput(statusCode = code, result)
}