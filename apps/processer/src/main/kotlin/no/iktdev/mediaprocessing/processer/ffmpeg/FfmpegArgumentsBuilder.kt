package no.iktdev.mediaprocessing.processer.ffmpeg

class FfmpegArgumentsBuilder() {
    private val defaultArguments = listOf(
        "-nostdin",
        "-hide_banner"
    )
    private var inputFile: String? = null
    private var outputFile: String? = null
    private var overwrite: Boolean = false
    private var progress: Boolean = false
    private var suppliedArgs: List<String> = emptyList()

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

    fun args(args: List<String>) = apply {
        this.suppliedArgs = args
    }

    fun build(): List<String> {
        val args = mutableListOf<String>()
        val inFile = if (inputFile == null || inputFile?.isBlank() == true) {
            throw RuntimeException("Inputfile is required")
        } else this.inputFile!!
        val outFile: String = if (outputFile == null || outputFile?.isBlank() == true) {
            throw RuntimeException("Outputfile is required")
        } else this.outputFile!!

        if (overwrite) {
            args.add("-y")
        }
        args.addAll(defaultArguments)
        args.addAll(listOf("-i", inFile))
        args.addAll(suppliedArgs)
        args.add(outFile)
        if (progress) {
            args.addAll(listOf("-progress", "pipe:1"))
        }



        return args
    }


}