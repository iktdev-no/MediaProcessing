package no.iktdev.mediaprocessing.shared.common.model

import no.iktdev.mediaprocessing.ffmpeg.data.SubtitleStream

data class SubtitleItem(val stream: SubtitleStream, val type: SubtitleType)