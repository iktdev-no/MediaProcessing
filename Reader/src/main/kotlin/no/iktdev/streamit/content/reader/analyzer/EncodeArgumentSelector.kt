package no.iktdev.streamit.content.reader.analyzer

import no.iktdev.streamit.content.common.streams.AudioStream
import no.iktdev.streamit.content.common.streams.MediaStreams
import no.iktdev.streamit.content.common.streams.SubtitleStream
import no.iktdev.streamit.content.common.streams.VideoStream
import no.iktdev.streamit.content.reader.analyzer.encoding.AudioEncodeArguments
import no.iktdev.streamit.content.reader.analyzer.encoding.EncodeInformation
import no.iktdev.streamit.content.reader.analyzer.encoding.SubtitleEncodeArguments
import no.iktdev.streamit.content.reader.analyzer.encoding.VideoEncodeArguments
import no.iktdev.streamit.content.reader.preference

class EncodeArgumentSelector(val inputFile: String, val streams: MediaStreams, val outFileName: String) {
    var defaultSelectedVideo: VideoStream? = getDefaultSelectedVideo()
    var defaultSelectedAudio: AudioStream? = getDefaultSelectedAudio()

    private fun getAudioStreams() = streams.streams.filterIsInstance<AudioStream>()
    private fun getVideoStreams() = streams.streams.filterIsInstance<VideoStream>()


    private fun getDefaultSelectedVideo(): VideoStream? {
        return getVideoStreams().filter { (it.duration_ts ?: 0) > 0 }.maxByOrNull { it.duration_ts!! } ?: getVideoStreams().minByOrNull { it.index }
    }

    private fun getDefaultSelectedAudio(): AudioStream? {
        return getAudioStreams().filter { (it.duration_ts ?: 0) > 0 }.maxByOrNull { it.duration_ts!! } ?: getAudioStreams().minByOrNull { it.index }
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
        val languageFiltered = getAudioStreams().filter { it.tags.language == preference.audio.language }
        val channeledAndCodec = languageFiltered.find { it.channels >= (preference.audio.channels ?: 2) && it.codec_name == preference.audio.codec.lowercase() }
        return channeledAndCodec ?: return languageFiltered.minByOrNull { it.index } ?: defaultSelectedAudio
    }


    fun getVideoAndAudioArguments(): EncodeInformation? {
        val selectedVideo = defaultSelectedVideo
        val selectedAudio = getSelectedAudioBasedOnPreference() ?: defaultSelectedAudio
        return if (selectedVideo == null || selectedAudio == null) return null
        else {
            EncodeInformation(
                inputFile = inputFile,
                outFileName = "$outFileName.mp4",
                language = selectedAudio.tags.language ?: "eng",
                arguments = VideoEncodeArguments(selectedVideo).getVideoArguments() +
                        AudioEncodeArguments(selectedAudio).getAudioArguments()
            )
        }
    }

    fun getSubtitleArguments(): List<EncodeInformation> {
        return streams.streams.filterIsInstance<SubtitleStream>().map {
            val subArgs = SubtitleEncodeArguments(it)
            EncodeInformation(
                inputFile = inputFile,
                outFileName = "$outFileName.${subArgs.getFormatToCodec()}",
                language = it.tags.language ?: "eng",
                arguments = subArgs.getSubtitleArguments()
            )
        }
    }
}