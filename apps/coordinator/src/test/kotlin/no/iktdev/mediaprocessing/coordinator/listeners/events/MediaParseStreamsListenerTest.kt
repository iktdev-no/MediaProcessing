package no.iktdev.mediaprocessing.coordinator.listeners.events

import org.junit.jupiter.api.Assertions.*

import com.google.gson.JsonParser
import no.iktdev.eventi.models.Event
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

class MediaParseStreamsListenerTest {

    private val listener = MediaParseStreamsListener()

    class DummyEvent(): Event() {}

    @Test
    @DisplayName("""
    Hvis JSON inneholder video, audio og subtitle streams
    Når parseStreams kalles
    Så:
        Alle tre typer havner i riktig liste
    """)
    fun testparseMappingCorrectly() {
        val json = """
            {
              "streams": [
                {
                  "codec_name":"h264",
                  "codec_long_name":"H.264 / AVC / MPEG-4 AVC / MPEG-4 part 10",
                  "codec_type":"video",
                  "codec_tag_string":"avc1",
                  "codec_tag":"0x31637661",
                  "r_frame_rate":"25/1",
                  "avg_frame_rate":"25/1",
                  "time_base":"1/90000",
                  "start_pts":0,
                  "start_time":"0.000000",
                  "disposition": { "default":1,"dub":0,"original":0,"comment":0,"lyrics":0,"karaoke":0,"forced":0,"hearing_impaired":0,"captions":0,"visual_impaired":0,"clean_effects":0,"attached_pic":0,"timed_thumbnails":0 },
                  "tags": { "title":"Main Video","language":"eng" },
                  "profile":"High",
                  "width":1920,
                  "height":1080,
                  "coded_width":1920,
                  "coded_height":1080,
                  "closed_captions":0,
                  "has_b_frames":2,
                  "sample_aspect_ratio":"1:1",
                  "display_aspect_ratio":"16:9",
                  "pix_fmt":"yuv420p",
                  "level":40,
                  "color_range":"tv",
                  "color_space":"bt709",
                  "color_transfer":"bt709",
                  "color_primaries":"bt709",
                  "chroma_location":"left",
                  "refs":1
                },
                {
                  "codec_name":"aac",
                  "codec_long_name":"AAC (Advanced Audio Coding)",
                  "codec_type":"audio",
                  "codec_tag_string":"mp4a",
                  "codec_tag":"0x6134706d",
                  "r_frame_rate":"0/0",
                  "avg_frame_rate":"0/0",
                  "time_base":"1/48000",
                  "start_pts":0,
                  "start_time":"0.000000",
                  "disposition": { "default":1,"dub":0,"original":0,"comment":0,"lyrics":0,"karaoke":0,"forced":0,"hearing_impaired":0,"captions":0,"visual_impaired":0,"clean_effects":0,"attached_pic":0,"timed_thumbnails":0 },
                  "tags": { "title":"Stereo Track","language":"eng" },
                  "profile":"LC",
                  "sample_fmt":"fltp",
                  "sample_rate":"48000",
                  "channels":2,
                  "channel_layout":"stereo",
                  "bits_per_sample":0
                },
                {
                  "codec_name":"ass",
                  "codec_long_name":"ASS (Advanced SSA Subtitle)",
                  "codec_type":"subtitle",
                  "codec_tag_string":"[0][0][0][0]",
                  "codec_tag":"0x0000",
                  "r_frame_rate":"0/0",
                  "avg_frame_rate":"0/0",
                  "time_base":"1/1000",
                  "start_pts":0,
                  "start_time":"0.000000",
                  "disposition": { "default":1,"dub":0,"original":0,"comment":0,"lyrics":0,"karaoke":0,"forced":0,"hearing_impaired":0,"captions":0,"visual_impaired":0,"clean_effects":0,"attached_pic":0,"timed_thumbnails":0 },
                  "tags": { "title":"English Subs","language":"eng" },
                  "subtitle_tags": { "language":"eng","filename":"subs.ass","mimetype":"text/x-ssa" }
                }
              ]
            }
        """.trimIndent()

        val parsed = listener.parseStreams(JsonParser.parseString(json).asJsonObject)

        assertEquals(1, parsed.videoStream.size)
        assertEquals("h264", parsed.videoStream[0].codec_name)

        assertEquals(1, parsed.audioStream.size)
        assertEquals("aac", parsed.audioStream[0].codec_name)

        assertEquals(1, parsed.subtitleStream.size)
        assertEquals("ass", parsed.subtitleStream[0].codec_name)
    }



    @Test
    @DisplayName("""
    Hvis event ikke er MediaStreamReadEvent
    Når onEvent kalles
    Så:
        Returneres null
    """)
    fun testOnEventNonMediaStreamReadEvent() {
        val result = listener.onEvent(DummyEvent(), emptyList())
        assertNull(result)
    }

    @Test
    @DisplayName("""
    Hvis JSON inneholder video, audio og subtitle streams
    Når parseStreams kalles
    Så:
        Alle tre typer havner i riktig liste
    """)
    fun testParseStreamsMapsCorrectly() {
        val json = """
        {
          "streams": [
            {"codec_name":"h264","codec_type":"video"},
            {"codec_name":"aac","codec_type":"audio"},
            {"codec_name":"ass","codec_type":"subtitle"}
          ]
        }
    """.trimIndent()

        val parsed = listener.parseStreams(JsonParser.parseString(json).asJsonObject)

        assertEquals(1, parsed.videoStream.size)
        assertEquals("h264", parsed.videoStream[0].codec_name)

        assertEquals(1, parsed.audioStream.size)
        assertEquals("aac", parsed.audioStream[0].codec_name)

        assertEquals(1, parsed.subtitleStream.size)
        assertEquals("ass", parsed.subtitleStream[0].codec_name)
    }

    @Test
    @DisplayName("""
    Hvis JSON inneholder codec_name png og mjpeg
    Når parseStreams kalles
    Så:
        Disse ignoreres og videoStream blir tom
    """)
    fun testParseStreamsIgnoresPngAndMjpeg() {
        val json = """
        {
          "streams": [
            {"codec_name":"png","codec_type":"video"},
            {"codec_name":"mjpeg","codec_type":"video"}
          ]
        }
    """.trimIndent()

        val parsed = listener.parseStreams(JsonParser.parseString(json).asJsonObject)
        assertTrue(parsed.videoStream.isEmpty())
    }

    @Test
    @DisplayName("""
    Hvis JSON mangler streams array
    Når parseStreams kalles
    Så:
        Kastes Exception
    """)
    fun testParseStreamsThrowsOnInvalidJson() {
        val json = """{}"""
        assertThrows(Exception::class.java) {
            listener.parseStreams(JsonParser.parseString(json).asJsonObject)
        }
    }
}
