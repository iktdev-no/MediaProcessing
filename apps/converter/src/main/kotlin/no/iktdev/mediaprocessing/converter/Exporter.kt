package no.iktdev.mediaprocessing.converter

import no.iktdev.library.subtitle.classes.Dialog
import java.io.File

interface Exporter {
    fun write(dialogs: List<Dialog>): MutableList<File>
    fun writeSrt(dialogs: List<Dialog>): File
    fun writeSmi(dialogs: List<Dialog>): File
    fun writeVtt(dialogs: List<Dialog>): File
}
