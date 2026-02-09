package no.iktdev.mediaprocessing.processer.segment;

import java.io.File;

class SegmentPlanner(
        private val segmentLength: Double = 60.0
) {
    fun plan(input: File, totalDuration: Double, outputDir: File): List<Segment> {
        val segments = mutableListOf<Segment>()
        var start = 0.0
        var index = 0

        while (start < totalDuration) {
            val dur = minOf(segmentLength, totalDuration - start)
            segments += Segment(
                    index = index,
                    start = start,
                    duration = dur,
                    output = File(outputDir, "segment_${index}.mkv")
            )
            start += dur
            index++
        }

        return segments
    }
}
