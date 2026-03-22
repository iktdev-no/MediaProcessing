package no.iktdev.mediaprocessing.processer.segment

import no.iktdev.files.IFile

data class Segment(val index: Int, val start: Double, val duration: Double, val output: IFile)