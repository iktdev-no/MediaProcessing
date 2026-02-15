package no.iktdev.mediaprocessing

import no.iktdev.eventi.models.Event
import no.iktdev.eventi.models.store.TaskStatus
import no.iktdev.mediaprocessing.TestBase.DummyTask
import no.iktdev.mediaprocessing.ffmpeg.data.*
import no.iktdev.mediaprocessing.shared.common.event_task_contract.events.*
import no.iktdev.mediaprocessing.shared.common.model.MediaType
import no.iktdev.mediaprocessing.shared.common.model.SubtitleItem
import no.iktdev.mediaprocessing.shared.common.model.SubtitleType
import java.util.*

object MockData {

    fun mediaParsedEvent(
        collection: String,
        fileName: String,
        mediaType: MediaType
    ) = MediaParsedInfoEvent(
        data = MediaParsedInfoEvent.ParsedData(
            parsedCollection = collection,
            parsedFileName = fileName,
            parsedSearchTitles = listOf(collection, fileName),
            mediaType = mediaType
        )
    )

    fun metadataEvent(
        derivedFrom: Event,
        source: String = "potetland",
        coverUrl: String = "cover.jpg"
    ): List<Event> {

        val dummyTask = DummyTask().derivedOf(derivedFrom)
        val create = MetadataSearchTaskCreatedEvent(dummyTask.taskId).derivedOf(derivedFrom)

        val result = MetadataSearchResultEvent(
            results = listOf(
                MetadataSearchResultEvent.SearchResult(
                    searchTitles = listOf("MyCollection"),
                    similarity = 95,
                    prefix = 10,
                    keywordScore = 20.0,
                    typeScore = 80.0,
                    completenessScore = 15.0,
                    sourceScore = 5.0,
                    totalScore = 225.0,
                    metadata = MetadataSearchResultEvent.SearchResult.MetadataResult(
                        source = source,
                        title = "MyCollection",
                        alternateTitles = listOf("Alt1", "Alt2"),
                        cover = coverUrl,
                        bannerImage = null,
                        type = MediaType.Movie,
                        summary = listOf(
                            MetadataSearchResultEvent.SearchResult.MetadataResult.Summary(
                                language = "en",
                                description = "desc"
                            )
                        ),
                        genres = listOf("Drama")
                    )
                )
            ),
            recommended = null,
            status = TaskStatus.Completed
        ).producedFrom(dummyTask)

        return listOf(create, result)
    }


    fun encodeEvent(cachedFile: String, derivedFrom: Event, status: TaskStatus = TaskStatus.Completed,): List<Event> {
        val dummyTask = DummyTask().derivedOf(derivedFrom)
        val create = ProcesserEncodeTaskCreatedEvent(dummyTask.taskId)
            .derivedOf(derivedFrom)

        val result = ProcesserEncodeResultEvent(
            data = ProcesserEncodeResultEvent.EncodeResult(
                cachedOutputFile = cachedFile
            ),
            status = status
        ).producedFrom(dummyTask)
        return listOf(create, result)
    }

    fun extractEvent(language: String, cachedFile: String, derivedFrom: Event): List<Event> {
        val dummyTask = DummyTask().derivedOf(derivedFrom)
        val create = ProcesserExtractTaskCreatedEvent(listOf(dummyTask.taskId) as MutableList<UUID>)
            .derivedOf(derivedFrom)

        val result = ProcesserExtractResultEvent(
            status = TaskStatus.Completed,
            data = ProcesserExtractResultEvent.ExtractResult(
                language = language,
                cachedOutputFile = cachedFile
            )
        ).producedFrom(dummyTask)
        return listOf(create, result)
    }

    fun convertEvent(
        language: String,
        baseName: String,
        outputFiles: List<String>,
        derivedFrom: Event
    ): List<Event> {
        val dummyTask = DummyTask().derivedOf(derivedFrom)
        val createdTaskEvent = ConvertTaskCreatedEvent(
            taskId = dummyTask.taskId
        ).derivedOf(derivedFrom)

        val resultTask = ConvertTaskResultEvent(
            data = ConvertTaskResultEvent.ConvertedData(
                language = language,
                baseName = baseName,
                outputFiles = outputFiles
            ),
            status = TaskStatus.Completed
        ).producedFrom(dummyTask)
        return listOf(createdTaskEvent, resultTask)
    }

    fun coverEvent(cacheFile: String, derivedFrom: Event, source: String = "test"): List<Event> {
        val dummyTask = DummyTask().derivedOf(derivedFrom)
        val start = CoverDownloadTaskCreatedEvent(listOf(dummyTask.taskId)).derivedOf(derivedFrom)

        val result = CoverDownloadResultEvent(
            data = CoverDownloadResultEvent.CoverDownloadedData(
                source = source,
                outputFile = cacheFile
            ),
            status = TaskStatus.Completed
        ).producedFrom(dummyTask)
        return listOf(start, result)
    }

    fun dummyAudioStream(
        index: Int,
        language: String = "eng",
        channels: Int = 2,
        durationTs: Long = 1000,
        codec: String = "aac",
        disposition: Disposition = dummyDisposition(),
        tags: Tags = dummyTags(language)
    ): AudioStream {
        return AudioStream(
            index = index,
            codec_name = codec,
            codec_long_name = "AAC",
            codec_type = "audio",
            codec_tag_string = "",
            codec_tag = "",
            r_frame_rate = "0/0",
            avg_frame_rate = "0/0",
            time_base = "1/1000",
            start_pts = 0,
            start_time = "0",
            duration = null,
            duration_ts = durationTs,
            disposition = disposition,
            tags = tags,
            profile = "LC",
            sample_fmt = "fltp",
            sample_rate = "48000",
            channels = channels,
            channel_layout = "stereo",
            bits_per_sample = 16,
            bit_rate = 48000
        )
    }


    fun dummyVideoStream(
        index: Int,
        durationTs: Long = 1000,
        codec: String = "h264",
        disposition: Disposition = dummyDisposition(),
        tags: Tags = dummyTags("eng")
    ): VideoStream {
        return VideoStream(
            index = index,
            codec_name = codec,
            codec_long_name = "H.264",
            codec_type = "video",
            codec_tag_string = "",
            codec_tag = "",
            r_frame_rate = "25/1",
            avg_frame_rate = "25/1",
            time_base = "1/1000",
            start_pts = 0,
            start_time = "0",
            duration = null,
            duration_ts = durationTs,
            disposition = disposition,
            tags = tags,
            profile = "main",
            width = 1920,
            height = 1080,
            coded_width = 1920,
            coded_height = 1080,
            closed_captions = 0,
            has_b_frames = 0,
            sample_aspect_ratio = "1:1",
            display_aspect_ratio = "16:9",
            pix_fmt = "yuv420p",
            level = 30,
            color_range = "tv",
            color_space = "bt709",
            color_transfer = "bt709",
            color_primaries = "bt709",
            chroma_location = "left",
            refs = 1
        )
    }


    fun dummySubtitleStream(index: Int, language: String?, disposition: Disposition? = null): SubtitleStream {
        return SubtitleStream(
            index = index,
            codec_name = "ass",
            codec_long_name = "ASS",
            codec_type = "subtitle",
            codec_tag_string = "",
            codec_tag = "",
            r_frame_rate = "0/0",
            avg_frame_rate = "0/0",
            time_base = "1/1000",
            start_pts = 0,
            start_time = "0",
            duration = null,
            duration_ts = 1000,
            disposition = disposition,
            tags = Tags(
                title = null, BPS = null, DURATION = null, NUMBER_OF_FRAMES = 0,
                NUMBER_OF_BYTES = null, _STATISTICS_WRITING_APP = null, _STATISTICS_WRITING_DATE_UTC = null,
                _STATISTICS_TAGS = null, language = language, filename = null, mimetype = null
            )
        )
    }

    fun dummySubtitleItem(index: Int, language: String?, type: SubtitleType): SubtitleItem {
        val stream = dummySubtitleStream(index, language)
        return SubtitleItem(stream = stream, type = type)
    }

    fun dummyTags(language: String? = "eng") = Tags(
        title = null,
        BPS = null,
        DURATION = null,
        NUMBER_OF_FRAMES = 0,
        NUMBER_OF_BYTES = null,
        _STATISTICS_WRITING_APP = null,
        _STATISTICS_WRITING_DATE_UTC = null,
        _STATISTICS_TAGS = null,
        language = language,
        filename = null,
        mimetype = null
    )


    fun dummyDisposition(
        block: DispositionBuilder.() -> Unit = {}
    ): Disposition {
        val builder = DispositionBuilder().apply(block)
        return builder.build()
    }

    class DispositionBuilder {
        var default: Boolean = false
        var dub: Boolean = false
        var original: Boolean = false
        var comment: Boolean = false
        var lyrics: Boolean = false
        var karaoke: Boolean = false
        var forced: Boolean = false
        var hearingImpaired: Boolean = false
        var captions: Boolean = false
        var visualImpaired: Boolean = false
        var cleanEffects: Boolean = false
        var attachedPic: Boolean = false
        var timedThumbnails: Boolean = false

        fun build() = Disposition(
            default = default.toInt(),
            dub = dub.toInt(),
            original = original.toInt(),
            comment = comment.toInt(),
            lyrics = lyrics.toInt(),
            karaoke = karaoke.toInt(),
            forced = forced.toInt(),
            hearing_impaired = hearingImpaired.toInt(),
            captions = captions.toInt(),
            visual_impaired = visualImpaired.toInt(),
            clean_effects = cleanEffects.toInt(),
            attached_pic = attachedPic.toInt(),
            timed_thumbnails = timedThumbnails.toInt()
        )

        private fun Boolean.toInt() = if (this) 1 else 0
    }







}