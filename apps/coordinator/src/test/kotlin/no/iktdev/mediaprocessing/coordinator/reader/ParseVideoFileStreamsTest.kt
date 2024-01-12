package no.iktdev.mediaprocessing.coordinator.reader
/*
import com.google.gson.Gson
import com.google.gson.JsonObject
import no.iktdev.mediaprocessing.shared.kafka.core.CoordinatorProducer
import no.iktdev.mediaprocessing.shared.kafka.dto.events_result.ReaderPerformed
import no.iktdev.mediaprocessing.shared.kafka.dto.Status
import org.junit.jupiter.api.Test

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Named
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.Mock
import org.skyscreamer.jsonassert.JSONAssert
import org.springframework.beans.factory.annotation.Autowired

class ParseVideoFileStreamsTest {

    @Autowired
    private lateinit var testBase: KafkaTestBase

    @Mock
    lateinit var coordinatorProducer: CoordinatorProducer

    val parseVideoStreams = ParseVideoFileStreams(coordinatorProducer)

    @ParameterizedTest
    @MethodSource("streams")
    fun parseStreams(data: TestInfo) {
        val gson = Gson()
        val converted = gson.fromJson(data.input, JsonObject::class.java)
        val result = parseVideoStreams.parseStreams(ReaderPerformed(
            Status.COMPLETED,
            file = "ignore",
            output = converted
        ))
        JSONAssert.assertEquals(
            data.expected,
            gson.toJson(result),
            false
        )

    }

    data class TestInfo(
        val input: String,
        val expected: String
    )

    companion object {
        @JvmStatic
        fun streams(): List<Named<TestInfo>> {
            return listOf(
                Named.of(
                    "Top Clown streams", TestInfo(
                        """
                            {
                              "streams": [
                                {
                                  "index": 0,
                                  "codec_name": "h264",
                                  "codec_long_name": "H.264 / AVC / MPEG-4 AVC / MPEG-4 part 10",
                                  "profile": "Main",
                                  "codec_type": "video",
                                  "codec_tag_string": "[0][0][0][0]",
                                  "codec_tag": "0x0000",
                                  "width": 1920,
                                  "height": 1080,
                                  "coded_width": 1920,
                                  "coded_height": 1080,
                                  "closed_captions": 0,
                                  "film_grain": 0,
                                  "has_b_frames": 0,
                                  "sample_aspect_ratio": "1:1",
                                  "display_aspect_ratio": "16:9",
                                  "pix_fmt": "yuv420p",
                                  "level": 40,
                                  "chroma_location": "left",
                                  "field_order": "progressive",
                                  "refs": 1,
                                  "is_avc": "true",
                                  "nal_length_size": "4",
                                  "r_frame_rate": "24000/1001",
                                  "avg_frame_rate": "24000/1001",
                                  "time_base": "1/1000",
                                  "start_pts": 0,
                                  "start_time": "0.000000",
                                  "bits_per_raw_sample": "8",
                                  "extradata_size": 55,
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
                                  }
                                },
                                {
                                  "index": 1,
                                  "codec_name": "aac",
                                  "codec_long_name": "AAC (Advanced Audio Coding)",
                                  "profile": "HE-AAC",
                                  "codec_type": "audio",
                                  "codec_tag_string": "[0][0][0][0]",
                                  "codec_tag": "0x0000",
                                  "sample_fmt": "fltp",
                                  "sample_rate": "48000",
                                  "channels": 6,
                                  "channel_layout": "5.1",
                                  "bits_per_sample": 0,
                                  "initial_padding": 0,
                                  "r_frame_rate": "0/0",
                                  "avg_frame_rate": "0/0",
                                  "time_base": "1/1000",
                                  "start_pts": 0,
                                  "start_time": "0.000000",
                                  "extradata_size": 2,
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
                                    "language": "eng"
                                  }
                                }
                              ]
                            }

                        """.trimIndent(),
                        """
                
            """.trimIndent()
                    )
                )
            )
        }
    }
}*/