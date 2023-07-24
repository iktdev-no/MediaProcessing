package no.iktdev.streamit.content.reader.analyzer.encoding.helpers

import no.iktdev.exfl.using
import no.iktdev.streamit.content.common.CommonConfig
import no.iktdev.streamit.content.common.dto.reader.work.EncodeWork
import no.iktdev.streamit.content.common.dto.reader.work.ExtractWork
import no.iktdev.streamit.content.common.streams.*
import no.iktdev.streamit.content.reader.analyzer.encoding.dto.AudioEncodeArguments
import no.iktdev.streamit.content.reader.analyzer.encoding.dto.SubtitleEncodeArguments
import no.iktdev.streamit.content.reader.analyzer.encoding.dto.VideoEncodeArguments
import no.iktdev.streamit.content.reader.preference

class EncodeArgumentSelector(val collection: String, val inputFile: String, val streams: MediaStreams, val outFileName: String) {
    var defaultSelectedVideo: VideoStream? = defaultSelectedVideo()
    var defaultSelectedAudio: AudioStream? = defaultSelectedAudio()

    private fun obtainAudioStreams() = streams.streams.filterIsInstance<AudioStream>()
    private fun obtainVideoStreams() = streams.streams.filterIsInstance<VideoStream>()


    private fun defaultSelectedVideo(): VideoStream? {
        return obtainVideoStreams().filter { (it.duration_ts ?: 0) > 0 }.maxByOrNull { it.duration_ts!! } ?: obtainVideoStreams().minByOrNull { it.index }
    }

    private fun defaultSelectedAudio(): AudioStream? {
        return obtainAudioStreams().filter { (it.duration_ts ?: 0) > 0 }.maxByOrNull { it.duration_ts!! } ?: obtainAudioStreams().minByOrNull { it.index }
    }

    /**
     * @return VideoStream based on preference or defaultSelectedVideo
     */
    /*private fun getSelectedVideoBasedOnPreference(): VideoStream {
        val
    }*/

    /**
     * @return AudioStrem based on preference or defaultSelectedAudio
     */
    private fun getSelectedAudioBasedOnPreference(): AudioStream? {
        val languageFiltered = obtainAudioStreams().filter { it.tags.language == preference.audio.language }
        val channeledAndCodec = languageFiltered.find { it.channels >= (preference.audio.channels ?: 2) && it.codec_name == preference.audio.codec.lowercase() }
        return channeledAndCodec ?: return languageFiltered.minByOrNull { it.index } ?: defaultSelectedAudio
    }


    fun getVideoAndAudioArguments(): EncodeWork? {
        val selectedVideo = defaultSelectedVideo
        val selectedAudio = getSelectedAudioBasedOnPreference() ?: defaultSelectedAudio
        return if (selectedVideo == null || selectedAudio == null) return null
        else {
            val outFileName = "$outFileName.mp4"
            val outFile = CommonConfig.outgoingContent.using(collection, outFileName)
            val audioIndex = obtainAudioStreams().indexOf(selectedAudio)
            val videoIndex = obtainVideoStreams().indexOf(selectedVideo)
            EncodeWork(
                collection = collection,
                inFile = inputFile,
                arguments = VideoEncodeArguments(selectedVideo, videoIndex).getVideoArguments() +
                        AudioEncodeArguments(selectedAudio, audioIndex).getAudioArguments(),
                outFile = outFile.absolutePath
            )
        }
    }

    fun getSubtitleArguments(): List<ExtractWork> {
        val availableSubtitleStreams = streams.streams.filterIsInstance<SubtitleStream>()
        val subtitleStreams = SubtitleStreamSelector(availableSubtitleStreams)

        val conversionCandidates = subtitleStreams.getCandidateForConversion()

        return subtitleStreams.getDesiredStreams().map {
            val args = SubtitleEncodeArguments(it, availableSubtitleStreams.indexOf(it))
            val language = it.tags.language ?: "eng"
            val outFileName = "$outFileName.${subtitleStreams.getFormatToCodec(it.codec_name)}"
            val outFile = CommonConfig.outgoingContent.using(collection, "sub", language, outFileName)

            ExtractWork(
                collection = collection,
                language = language,
                inFile = inputFile,
                outFile = outFile.absolutePath,
                arguments = args.getSubtitleArguments(),
                produceConvertEvent = conversionCandidates.contains(it)
            )
        }

    }
}