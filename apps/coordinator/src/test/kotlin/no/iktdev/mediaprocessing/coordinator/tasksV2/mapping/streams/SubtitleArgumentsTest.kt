package no.iktdev.mediaprocessing.coordinator.tasksV2.mapping.streams

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import no.iktdev.mediaprocessing.shared.common.contract.ffmpeg.SubtitleStream
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class SubtitleArgumentsTest {
    val type = object : TypeToken<List<SubtitleStream>>() {}.type

    @Test
    fun validate1() {
        val data = Gson().fromJson<List<SubtitleStream>>(multipleSubtitleStreamsWithSameLanguage, type)
        assertThat(data.all { it is SubtitleStream }).isTrue()
        assertThat(data).isNotNull()
    }

    @Test
    fun validate2() {
        val data = Gson().fromJson<List<SubtitleStream>>(multipleSubtitleStreamsWithSameLanguage, type)
        val args = SubtitleArguments(data)
        val selectable = args.excludeLowFrameCount(data)
        assertThat(selectable).hasSize(3)
        assertThat(selectable.find { it.index == 4 })
        assertThat(selectable.find { it.index == 5 })
    }

    @Test
    fun validate3() {
        val data = Gson().fromJson<List<SubtitleStream>>(multipleSubtitleStreamsWithSameLanguage, type)
        val args = SubtitleArguments(data).getSubtitleArguments()
        assertThat(args).hasSize(1)
        assertThat(args.firstOrNull()?.mediaIndex).isEqualTo(4)
    }

    @Test
    fun validate3_2() {
        val data = Gson().fromJson<List<SubtitleStream>>(multipleSubtitleStreamsWithSameLanguageWithDisposition, type)
        val args = SubtitleArguments(data).getSubtitleArguments()
        assertThat(args).hasSize(1)
        assertThat(args.firstOrNull()?.mediaIndex).isEqualTo(4)
    }

    @Test
    fun assertThatCorrectTrackIsSelected() {
        val data = Gson().fromJson<List<SubtitleStream>>(selectCorrectTrack, type)
        val args = SubtitleArguments(data).getSubtitleArguments()
        assertThat(args).hasSize(1)
        assertThat(args.firstOrNull()?.index).isEqualTo(0)
    }


    val multipleSubtitleStreamsWithSameLanguage = """
                [{
                    "index": 3,
                    "codec_name": "ass",
                    "codec_long_name": "ASS (Advanced SSA) subtitle",
                    "codec_type": "subtitle",
                    "codec_tag_string": "[0][0][0][0]",
                    "codec_tag": "0x0000",
                    "r_frame_rate": "0/0",
                    "avg_frame_rate": "0/0",
                    "time_base": "1/1000",
                    "start_pts": 0,
                    "start_time": "0.000000",
                    "duration_ts": 1437083,
                    "duration": "1437.083000",
                    "extradata_size": 1967,
                    "tags": {
                        "language": "eng",
                        "title": "Forced",
                        "BPS": "5",
                        "DURATION": "00:21:42.640000000",
                        "NUMBER_OF_FRAMES": "14",
                        "NUMBER_OF_BYTES": "835",
                        "_STATISTICS_WRITING_APP": "mkvmerge v69.0.0 ('Day And Age') 64-bit",
                        "_STATISTICS_WRITING_DATE_UTC": "2024-10-04 08:12:59",
                        "_STATISTICS_TAGS": "BPS DURATION NUMBER_OF_FRAMES NUMBER_OF_BYTES"
                    }
                },
                {
                    "index": 4,
                    "codec_name": "ass",
                    "codec_long_name": "ASS (Advanced SSA) subtitle",
                    "codec_type": "subtitle",
                    "codec_tag_string": "[0][0][0][0]",
                    "codec_tag": "0x0000",
                    "r_frame_rate": "0/0",
                    "avg_frame_rate": "0/0",
                    "time_base": "1/1000",
                    "start_pts": 0,
                    "start_time": "0.000000",
                    "duration_ts": 1437083,
                    "duration": "1437.083000",
                    "extradata_size": 1791,
                    "tags": {
                        "language": "eng",
                        "BPS": "129",
                        "DURATION": "00:22:26.550000000",
                        "NUMBER_OF_FRAMES": "356",
                        "NUMBER_OF_BYTES": "21787",
                        "_STATISTICS_WRITING_APP": "mkvmerge v69.0.0 ('Day And Age') 64-bit",
                        "_STATISTICS_WRITING_DATE_UTC": "2024-10-04 08:12:59",
                        "_STATISTICS_TAGS": "BPS DURATION NUMBER_OF_FRAMES NUMBER_OF_BYTES"
                    }
                },
                {
                    "index": 5,
                    "codec_name": "subrip",
                    "codec_long_name": "SubRip subtitle",
                    "codec_type": "subtitle",
                    "codec_tag_string": "[0][0][0][0]",
                    "codec_tag": "0x0000",
                    "r_frame_rate": "0/0",
                    "avg_frame_rate": "0/0",
                    "time_base": "1/1000",
                    "start_pts": 790,
                    "start_time": "0.790000",
                    "tags": {
                        "language": "eng",
                        "title": "CC",
                        "BPS": "83",
                        "DURATION": "00:23:56.060000000",
                        "NUMBER_OF_FRAMES": "495",
                        "NUMBER_OF_BYTES": "14954",
                        "_STATISTICS_WRITING_APP": "mkvmerge v69.0.0 ('Day And Age') 64-bit",
                        "_STATISTICS_WRITING_DATE_UTC": "2024-10-04 08:12:59",
                        "_STATISTICS_TAGS": "BPS DURATION NUMBER_OF_FRAMES NUMBER_OF_BYTES"
                    }
                }]
    """.trimIndent()

    //language=json
    val multipleSubtitleStreamsWithSameLanguageWithDisposition = """
                [{
                    "index": 3,
                    "codec_name": "ass",
                    "codec_long_name": "ASS (Advanced SSA) subtitle",
                    "codec_type": "subtitle",
                    "codec_tag_string": "[0][0][0][0]",
                    "codec_tag": "0x0000",
                    "r_frame_rate": "0/0",
                    "avg_frame_rate": "0/0",
                    "time_base": "1/1000",
                    "start_pts": 0,
                    "start_time": "0.000000",
                    "duration_ts": 1437083,
                    "duration": "1437.083000",
                    "extradata_size": 1967,
                    "disposition": {
                        "default": 1,
                        "dub": 0,
                        "original": 0,
                        "comment": 0,
                        "lyrics": 0,
                        "karaoke": 0,
                        "forced": 1,
                        "hearing_impaired": 0,
                        "visual_impaired": 0,
                        "clean_effects": 0,
                        "attached_pic": 0,
                        "timed_thumbnails": 0,
                        "non_diegetic": 0,
                        "captions": 0,
                        "descriptions": 0,
                        "metadata": 0,
                        "dependent": 0,
                        "still_image": 0
                    },
                    "tags": {
                        "language": "eng",
                        "title": "Forced",
                        "BPS": "5",
                        "DURATION": "00:21:42.640000000",
                        "NUMBER_OF_FRAMES": "14",
                        "NUMBER_OF_BYTES": "835",
                        "_STATISTICS_WRITING_APP": "mkvmerge v69.0.0 ('Day And Age') 64-bit",
                        "_STATISTICS_WRITING_DATE_UTC": "2024-10-04 08:12:59",
                        "_STATISTICS_TAGS": "BPS DURATION NUMBER_OF_FRAMES NUMBER_OF_BYTES"
                    }
                },
                {
                    "index": 4,
                    "codec_name": "ass",
                    "codec_long_name": "ASS (Advanced SSA) subtitle",
                    "codec_type": "subtitle",
                    "codec_tag_string": "[0][0][0][0]",
                    "codec_tag": "0x0000",
                    "r_frame_rate": "0/0",
                    "avg_frame_rate": "0/0",
                    "time_base": "1/1000",
                    "start_pts": 0,
                    "start_time": "0.000000",
                    "duration_ts": 1437083,
                    "duration": "1437.083000",
                    "extradata_size": 1791,
                    "disposition": {
                        "default": 0,
                        "dub": 0,
                        "original": 0,
                        "comment": 0,
                        "lyrics": 0,
                        "karaoke": 0,
                        "forced": 0,
                        "hearing_impaired": 0,
                        "visual_impaired": 0,
                        "clean_effects": 0,
                        "attached_pic": 0,
                        "timed_thumbnails": 0,
                        "non_diegetic": 0,
                        "captions": 0,
                        "descriptions": 0,
                        "metadata": 0,
                        "dependent": 0,
                        "still_image": 0
                    },
                    "tags": {
                        "language": "eng",
                        "BPS": "129",
                        "DURATION": "00:22:26.550000000",
                        "NUMBER_OF_FRAMES": "356",
                        "NUMBER_OF_BYTES": "21787",
                        "_STATISTICS_WRITING_APP": "mkvmerge v69.0.0 ('Day And Age') 64-bit",
                        "_STATISTICS_WRITING_DATE_UTC": "2024-10-04 08:12:59",
                        "_STATISTICS_TAGS": "BPS DURATION NUMBER_OF_FRAMES NUMBER_OF_BYTES"
                    }
                },
                {
                    "index": 5,
                    "codec_name": "subrip",
                    "codec_long_name": "SubRip subtitle",
                    "codec_type": "subtitle",
                    "codec_tag_string": "[0][0][0][0]",
                    "codec_tag": "0x0000",
                    "r_frame_rate": "0/0",
                    "avg_frame_rate": "0/0",
                    "time_base": "1/1000",
                    "start_pts": 790,
                    "start_time": "0.790000",
                    "disposition": {
                        "default": 0,
                        "dub": 0,
                        "original": 0,
                        "comment": 0,
                        "lyrics": 0,
                        "karaoke": 0,
                        "forced": 0,
                        "hearing_impaired": 0,
                        "visual_impaired": 0,
                        "clean_effects": 0,
                        "attached_pic": 0,
                        "timed_thumbnails": 0,
                        "non_diegetic": 0,
                        "captions": 0,
                        "descriptions": 0,
                        "metadata": 0,
                        "dependent": 0,
                        "still_image": 0
                    },
                    "tags": {
                        "language": "eng",
                        "title": "CC",
                        "BPS": "83",
                        "DURATION": "00:23:56.060000000",
                        "NUMBER_OF_FRAMES": "495",
                        "NUMBER_OF_BYTES": "14954",
                        "_STATISTICS_WRITING_APP": "mkvmerge v69.0.0 ('Day And Age') 64-bit",
                        "_STATISTICS_WRITING_DATE_UTC": "2024-10-04 08:12:59",
                        "_STATISTICS_TAGS": "BPS DURATION NUMBER_OF_FRAMES NUMBER_OF_BYTES"
                    }
                }]
    """.trimIndent()

    val selectCorrectTrack = """
        [
                {
                    "index": 2,
                    "codec_name": "ass",
                    "codec_long_name": "ASS (Advanced SSA) subtitle",
                    "codec_type": "subtitle",
                    "codec_tag_string": "[0][0][0][0]",
                    "codec_tag": "0x0000",
                    "r_frame_rate": "0/0",
                    "avg_frame_rate": "0/0",
                    "time_base": "1/1000",
                    "start_pts": 0,
                    "start_time": "0.000000",
                    "duration_ts": 1430048,
                    "duration": "1430.048000",
                    "extradata_size": 2185,
                    "disposition": {
                        "default": 1,
                        "dub": 0,
                        "original": 0,
                        "comment": 0,
                        "lyrics": 0,
                        "karaoke": 0,
                        "forced": 0,
                        "hearing_impaired": 0,
                        "visual_impaired": 0,
                        "clean_effects": 0,
                        "attached_pic": 0,
                        "timed_thumbnails": 0,
                        "non_diegetic": 0,
                        "captions": 0,
                        "descriptions": 0,
                        "metadata": 0,
                        "dependent": 0,
                        "still_image": 0
                    },
                    "tags": {
                        "language": "eng",
                        "BPS": "173",
                        "DURATION": "00:23:43.500000000",
                        "NUMBER_OF_FRAMES": "436",
                        "NUMBER_OF_BYTES": "30896",
                        "_STATISTICS_WRITING_APP": "mkvmerge v69.0.0 ('Day And Age') 64-bit",
                        "_STATISTICS_WRITING_DATE_UTC": "2025-01-03 02:19:23",
                        "_STATISTICS_TAGS": "BPS DURATION NUMBER_OF_FRAMES NUMBER_OF_BYTES"
                    }
                },
                {
                    "index": 3,
                    "codec_name": "subrip",
                    "codec_long_name": "SubRip subtitle",
                    "codec_type": "subtitle",
                    "codec_tag_string": "[0][0][0][0]",
                    "codec_tag": "0x0000",
                    "r_frame_rate": "0/0",
                    "avg_frame_rate": "0/0",
                    "time_base": "1/1000",
                    "start_pts": 0,
                    "start_time": "0.000000",
                    "duration_ts": 1430048,
                    "duration": "1430.048000",
                    "disposition": {
                        "default": 0,
                        "dub": 0,
                        "original": 0,
                        "comment": 0,
                        "lyrics": 0,
                        "karaoke": 0,
                        "forced": 0,
                        "hearing_impaired": 0,
                        "visual_impaired": 0,
                        "clean_effects": 0,
                        "attached_pic": 0,
                        "timed_thumbnails": 0,
                        "non_diegetic": 0,
                        "captions": 0,
                        "descriptions": 0,
                        "metadata": 0,
                        "dependent": 0,
                        "still_image": 0
                    },
                    "tags": {
                        "language": "eng",
                        "BPS": "83",
                        "DURATION": "00:23:41.860000000",
                        "NUMBER_OF_FRAMES": "432",
                        "NUMBER_OF_BYTES": "14853",
                        "_STATISTICS_WRITING_APP": "mkvmerge v69.0.0 ('Day And Age') 64-bit",
                        "_STATISTICS_WRITING_DATE_UTC": "2025-01-03 02:19:23",
                        "_STATISTICS_TAGS": "BPS DURATION NUMBER_OF_FRAMES NUMBER_OF_BYTES"
                    }
                }
        ]
    """.trimIndent()
}