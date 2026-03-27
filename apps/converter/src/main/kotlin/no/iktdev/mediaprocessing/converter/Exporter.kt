package no.iktdev.mediaprocessing.converter

import no.iktdev.files.IFile
import no.iktdev.library.subtitle.classes.Dialog

interface Exporter {
    fun write(dialogs: List<Dialog>): MutableList<IFile>
    fun writeSrt(dialogs: List<Dialog>): IFile
    fun writeSmi(dialogs: List<Dialog>): IFile
    fun writeVtt(dialogs: List<Dialog>): IFile
}
