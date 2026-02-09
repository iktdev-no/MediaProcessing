package no.iktdev.mediaprocessing.processer.segment

import java.io.File

data class Segment(val index: Int, val start: Double, val duration: Double, val output: File)