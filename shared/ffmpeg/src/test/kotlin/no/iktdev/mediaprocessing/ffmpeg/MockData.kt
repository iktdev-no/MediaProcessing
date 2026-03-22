package no.iktdev.mediaprocessing.ffmpeg

import no.iktdev.mediaprocessing.ffmpeg.data.AudioStream
import no.iktdev.mediaprocessing.ffmpeg.data.Disposition
import no.iktdev.mediaprocessing.ffmpeg.data.Tags
import no.iktdev.mediaprocessing.ffmpeg.data.VideoStream

class MockData {
    fun mockVideoStream(
        index: Int = 0,
        codec: String = "h264",
        width: Int = 1920,
        height: Int = 1080,
        disposition: Disposition,
        tags: Tags
    ) = VideoStream(
        index = index,
        codec_name = codec,
        codec_long_name = "H.264 / AVC / MPEG-4 AVC / MPEG-4 part 10",
        codec_type = "video",
        codec_tag_string = "avc1",
        codec_tag = "0x31637661",
        r_frame_rate = "25/1",
        avg_frame_rate = "25/1",
        time_base = "1/90000",
        start_pts = 0,
        start_time = "0.000000",
        disposition = disposition,
        tags = tags,
        duration = "60.0",
        duration_ts = 54000,
        profile = "High",
        width = width,
        height = height,
        coded_width = width,
        coded_height = height,
        closed_captions = 0,
        has_b_frames = 2,
        sample_aspect_ratio = "1:1",
        display_aspect_ratio = "16:9",
        pix_fmt = "yuv420p",
        level = 40,
        color_range = "tv",
        color_space = "bt709",
        color_transfer = "bt709",
        color_primaries = "bt709",
        chroma_location = "left",
        refs = 1
    )

    fun mockAudioStream(
        index: Int = 0,
        codec: String = "aac",
        channels: Int = 2,
        profile: String = "LC",
        disposition: Disposition,
        bitRate: Long = 48000,
        tags: Tags
    ) = AudioStream(
        index = index,
        codec_name = codec,
        codec_long_name = "AAC (Advanced Audio Coding)",
        codec_type = "audio",
        codec_tag_string = "mp4a",
        codec_tag = "0x6134706d",
        r_frame_rate = "0/0",
        avg_frame_rate = "0/0",
        time_base = "1/48000",
        start_pts = 0,
        start_time = "0.000000",
        duration = "60.0",
        duration_ts = 2880000,
        disposition = disposition,
        tags = tags,
        profile = profile,
        sample_fmt = "fltp",
        sample_rate = "48000",
        channels = channels,
        channel_layout = "stereo",
        bits_per_sample = 0,
        bit_rate = bitRate
    )

    fun mockDisposition(
        default: Int = 1,
        forced: Int = 0
    ) = Disposition(
        default = default,
        dub = 0,
        original = 0,
        comment = 0,
        lyrics = 0,
        karaoke = 0,
        forced = forced,
        hearing_impaired = 0,
        captions = 0,
        visual_impaired = 0,
        clean_effects = 0,
        attached_pic = 0,
        timed_thumbnails = 0
    )

    fun mockTags(
        language: String? = "eng",
        title: String? = null,
        filename: String? = null
    ) = Tags(
        title = title,
        BPS = null,
        DURATION = null,
        NUMBER_OF_FRAMES = 0,
        NUMBER_OF_BYTES = null,
        _STATISTICS_WRITING_APP = null,
        _STATISTICS_WRITING_DATE_UTC = null,
        _STATISTICS_TAGS = null,
        language = language,
        filename = filename,
        mimetype = null
    )
}