package no.iktdev.mediaprocessing.processer.segment;

import no.iktdev.files.IFile

class SegmentPlanner(
        private val segmentLength: Double = 60.0
) {
    fun plan(totalDuration: Double, outputDir: IFile): List<Segment> {
        val segments = mutableListOf<Segment>()
        var start = 0.0
        var index = 0

        while (start < totalDuration) {
            val dur = minOf(segmentLength, totalDuration - start)
            segments += Segment(
                    index = index,
                    start = start,
                    duration = dur,
                    output = outputDir.using("segment_${index}.mkv")
            )
            start += dur
            index++
        }

        return segments
    }
}
