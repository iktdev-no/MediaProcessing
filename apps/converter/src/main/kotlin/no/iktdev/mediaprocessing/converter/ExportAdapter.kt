package no.iktdev.mediaprocessing.converter

import no.iktdev.files.IFile
import no.iktdev.library.subtitle.classes.Dialog
import no.iktdev.library.subtitle.export.Export

class ExportAdapter(
    private val export: Export
) : Exporter {
    override fun write(dialogs: List<Dialog>): MutableList<IFile> {
        return export.write(dialogs).map { IFile(it.absolutePath) } as MutableList<IFile>
    }

    //override fun write(dialogs: List<Dialog>) = export.write(dialogs)
    override fun writeSrt(dialogs: List<Dialog>) = IFile(export.writeSrt(dialogs).absolutePath)
    override fun writeSmi(dialogs: List<Dialog>) = IFile(export.writeSmi(dialogs).absolutePath)
    override fun writeVtt(dialogs: List<Dialog>) = IFile(export.writeVtt(dialogs).absolutePath)
}
