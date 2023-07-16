package no.iktdev.streamit.content.reader.fileWatcher

import java.io.File

interface FileWatcherEvents {
    fun onFileAvailable(file: PendingFile)

    /**
     * If the file is being copied or incomplete, or in case a process currently owns the file, pending should be issued
     */
    fun onFilePending(file: PendingFile)

    /**
     * If the file is either removed or is not a valid file
     */
    fun onFileFailed(file: PendingFile)


    fun onFileRemoved(file: PendingFile)
}