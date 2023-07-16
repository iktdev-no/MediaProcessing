package no.iktdev.streamit.content.common.streams

data class MediaStreams(
    val streams: List<Stream>
)

sealed class Stream(
    @Transient open val index: Int,
    @Transient open val codec_name: String,
    @Transient open val codec_long_name: String,
    @Transient open val codec_type: String,
    @Transient open val codec_tag_string: String,
    @Transient open val codec_tag: String,
    @Transient open val r_frame_rate: String,
    @Transient open val avg_frame_rate: String,
    @Transient open val time_base: String,
    @Transient open val start_pts: Long,
    @Transient open val start_time: String,
    @Transient open val duration_ts: Long? = null,
    @Transient open val duration: String? = null,
    @Transient open val disposition: Disposition,
    @Transient open val tags: Tags
)

data class VideoStream(
    override val index: Int,
    override val codec_name: String,
    override val codec_long_name: String,
    override val codec_type: String,
    override val codec_tag_string: String,
    override val codec_tag: String,
    override val r_frame_rate: String,
    override val avg_frame_rate: String,
    override val time_base: String,
    override val start_pts: Long,
    override val start_time: String,
    override val disposition: Disposition,
    override val tags: Tags,
    override val duration: String?,
    override val duration_ts: Long?,
    val profile: String,
    val width: Int,
    val height: Int,
    val coded_width: Int,
    val coded_height: Int,
    val closed_captions: Int,
    val has_b_frames: Int,
    val sample_aspect_ratio: String,
    val display_aspect_ratio: String,
    val pix_fmt: String,
    val level: Int,
    val color_range: String,
    val color_space: String,
    val color_transfer: String,
    val color_primaries: String,
    val chroma_location: String,
    val refs: Int
) : Stream(
    index,
    codec_name,
    codec_long_name,
    codec_type,
    codec_tag_string,
    codec_tag,
    r_frame_rate,
    avg_frame_rate,
    time_base,
    start_pts,
    start_time,
    duration_ts,
    duration,
    disposition,
    tags
)

data class AudioStream(
    override val index: Int,
    override val codec_name: String,
    override val codec_long_name: String,
    override val codec_type: String,
    override val codec_tag_string: String,
    override val codec_tag: String,
    override val r_frame_rate: String,
    override val avg_frame_rate: String,
    override val time_base: String,
    override val start_pts: Long,
    override val start_time: String,
    override val duration: String?,
    override val duration_ts: Long?,
    override val disposition: Disposition,
    override val tags: Tags,
    val profile: String,
    val sample_fmt: String,
    val sample_rate: String,
    val channels: Int,
    val channel_layout: String,
    val bits_per_sample: Int
) : Stream(
    index,
    codec_name,
    codec_long_name,
    codec_type,
    codec_tag_string,
    codec_tag,
    r_frame_rate,
    avg_frame_rate,
    time_base,
    start_pts,
    start_time,
    duration_ts,
    duration,
    disposition,
    tags
)

data class SubtitleStream(
    override val index: Int,
    override val codec_name: String,
    override val codec_long_name: String,
    override val codec_type: String,
    override val codec_tag_string: String,
    override val codec_tag: String,
    override val r_frame_rate: String,
    override val avg_frame_rate: String,
    override val time_base: String,
    override val start_pts: Long,
    override val start_time: String,
    override val duration: String?,
    override val duration_ts: Long?,
    override val disposition: Disposition,
    override val tags: Tags,
    val subtitle_tags: SubtitleTags
) : Stream(
    index,
    codec_name,
    codec_long_name,
    codec_type,
    codec_tag_string,
    codec_tag,
    r_frame_rate,
    avg_frame_rate,
    time_base,
    start_pts,
    start_time,
    duration_ts,
    duration,
    disposition,
    tags
)

data class Disposition(
    val default: Int,
    val dub: Int,
    val original: Int,
    val comment: Int,
    val lyrics: Int,
    val karaoke: Int,
    val forced: Int,
    val hearing_impaired: Int,
    val visual_impaired: Int,
    val clean_effects: Int,
    val attached_pic: Int,
    val timed_thumbnails: Int
)

data class Tags(
    val title: String?,
    val BPS: String?,
    val DURATION: String?,
    val NUMBER_OF_FRAMES: String?,
    val NUMBER_OF_BYTES: String?,
    val _STATISTICS_WRITING_APP: String?,
    val _STATISTICS_WRITING_DATE_UTC: String?,
    val _STATISTICS_TAGS: String?,
    val language: String?,
    val filename: String?,
    val mimetype: String?
)

data class SubtitleTags(
    val language: String?,
    val filename: String?,
    val mimetype: String?
)
