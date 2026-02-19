package no.iktdev.mediaprocessing.ffmpeg.arguments

import java.io.File

class MpegArgument {
    private val defaultArguments = listOf(
        "-nostdin",
        "-nostats",
        "-hide_banner"
    )
    var inputFile: String? = null
        private set
    var outputFile: String? = null
        private set
    private var overwrite: Boolean = false
    private var progress: Boolean = false
    private var preSuppliedArgs: List<String> = emptyList()
    private var suppliedArgs: List<String> = emptyList()
    var outputCacheFile: Boolean = true
        private set

    fun inputFile(inputFile: String) = apply {
        this.inputFile = inputFile
    }

    fun outputFile(outputFile: String) = apply {
        this.outputFile = outputFile
    }

    fun allowOverwrite(allowOverwrite: Boolean) = apply {
        this.overwrite = allowOverwrite
    }

    fun withProgress(withProgress: Boolean) = apply {
        this.progress = withProgress
    }

    fun preArgs(args: List<String>) = apply {
        this.preSuppliedArgs = args
    }

    fun preArgs(vararg args: String) = apply {
        this.preSuppliedArgs = args.toList()
    }

    fun args(args: List<String>) = apply {
        this.suppliedArgs = args
    }

    fun args(vararg args: String) = apply {
        this.suppliedArgs = args.toList()
    }

    fun useCacheFile(useCacheFile: Boolean) = apply {
        this.outputCacheFile = useCacheFile
    }

    fun getCacheOutputFile(): String {
        return if (outputCacheFile) {
            File(outputFile!!).let {
                File(it.parentFile.absoluteFile, "${it.nameWithoutExtension}.work.${it.extension}")
            }.absolutePath
        } else {
            this.outputFile!!
        }
    }

    fun getOutputFileUsed(): String {
        if (outputFile == null || outputFile?.isBlank() == true) {
            throw RuntimeException("Outputfile is required")
        }
        return if (outputCacheFile) {
            File(outputFile!!).let {
                File(it.parentFile.absoluteFile, "${it.nameWithoutExtension}.work.${it.extension}")
            }.absolutePath
        } else {
            this.outputFile!!
        }
    }

    fun build(): List<String> {
        val args = mutableListOf<String>()
        val inFile = if (inputFile == null || inputFile?.isBlank() == true) {
            throw RuntimeException("Inputfile is required")
        } else this.inputFile!!
        val outFile: String = getOutputFileUsed()
        if (overwrite) {
            args.add("-y")
        }
        args.addAll(defaultArguments)
        args.addAll(preSuppliedArgs)
        args.addAll(listOf("-i", inFile))
        args.addAll(suppliedArgs)
        args.add(outFile)
        if (progress) {
            args.addAll(listOf("-progress", "pipe:1"))
        }
        return args
    }
}