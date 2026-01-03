package no.iktdev.mediaprocessing.converter

import no.iktdev.library.subtitle.classes.Dialog
import no.iktdev.library.subtitle.export.Export

class ExportAdapter(
    private val export: Export
) : Exporter {

    override fun write(dialogs: List<Dialog>) = export.write(dialogs)
    override fun writeSrt(dialogs: List<Dialog>) = export.writeSrt(dialogs)
    override fun writeSmi(dialogs: List<Dialog>) = export.writeSmi(dialogs)
    override fun writeVtt(dialogs: List<Dialog>) = export.writeVtt(dialogs)
}
