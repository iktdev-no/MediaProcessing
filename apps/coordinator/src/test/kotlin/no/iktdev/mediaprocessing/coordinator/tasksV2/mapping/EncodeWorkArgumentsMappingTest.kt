package no.iktdev.mediaprocessing.coordinator.tasksV2.mapping

import no.iktdev.mediaprocessing.shared.common.contract.Events
import no.iktdev.mediaprocessing.shared.common.contract.data.MediaFileStreamsParsedEvent
import no.iktdev.mediaprocessing.shared.common.contract.data.az
import no.iktdev.mediaprocessing.shared.common.contract.ffmpeg.AudioPreference
import no.iktdev.mediaprocessing.shared.common.contract.ffmpeg.EncodingPreference
import no.iktdev.mediaprocessing.shared.common.contract.ffmpeg.ParsedMediaStreams
import no.iktdev.mediaprocessing.shared.common.contract.ffmpeg.VideoPreference
import no.iktdev.mediaprocessing.shared.common.contract.fromJsonWithDeserializer
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.io.File

class EncodeWorkArgumentsMappingTest {

    @Test
    fun parse() {
        val event = data.fromJsonWithDeserializer(Events.EventMediaParseStreamPerformed)
        val parser = EncodeWorkArgumentsMapping(
            "potato.mkv",
            "potato.mp4",
            File(".\\potato.mp4"),
            event.az<MediaFileStreamsParsedEvent>()!!.data!!,
            EncodingPreference(VideoPreference(), AudioPreference())
        )
        val result = parser.getArguments()
    }


}

val data = """
    {"metadata":{"derivedFromEventId":"308267f3-6eee-4250-9dc4-0f54edb1a120","eventId":"51970dfb-790d-48e4-a37e-ae0915bb1b70","referenceId":"d8be4d8f-64c2-4df1-aed1-e66cdc7163b4","status":"Success","created":"2024-07-19T18:45:31.956816095"},"data":{"videoStream":[{"index":0,"codec_name":"hevc","codec_long_name":"H.265 / HEVC (High Efficiency Video Coding)","codec_type":"video","codec_tag_string":"[0][0][0][0]","codec_tag":"0x0000","r_frame_rate":"24000/1001","avg_frame_rate":"24000/1001","time_base":"1/1000","start_pts":0,"start_time":"0.000000","disposition":{"default":1,"dub":0,"original":0,"comment":0,"lyrics":0,"karaoke":0,"forced":0,"hearing_impaired":0,"visual_impaired":0,"clean_effects":0,"attached_pic":0,"timed_thumbnails":0},"tags":{"title":"Presented By EMBER","BPS":"1288407","DURATION":"00:23:40.002000000","NUMBER_OF_FRAMES":"34046","NUMBER_OF_BYTES":"228692681","_STATISTICS_WRITING_APP":"mkvmerge v69.0.0 (\u0027Day And Age\u0027) 64-bit","_STATISTICS_WRITING_DATE_UTC":"2024-01-06 04:19:11","_STATISTICS_TAGS":"BPS DURATION NUMBER_OF_FRAMES NUMBER_OF_BYTES"},"profile":"Main 10","width":1920,"height":1080,"coded_width":1920,"coded_height":1080,"closed_captions":0,"has_b_frames":2,"sample_aspect_ratio":"1:1","display_aspect_ratio":"16:9","pix_fmt":"yuv420p10le","level":120,"color_range":"tv","color_space":"bt709","color_transfer":"bt709","color_primaries":"bt709","refs":1},{"index":18,"codec_name":"png","codec_long_name":"PNG (Portable Network Graphics) image","codec_type":"video","codec_tag_string":"[0][0][0][0]","codec_tag":"0x0000","r_frame_rate":"90000/1","avg_frame_rate":"0/0","time_base":"1/90000","start_pts":0,"start_time":"0.000000","disposition":{"default":0,"dub":0,"original":0,"comment":0,"lyrics":0,"karaoke":0,"forced":0,"hearing_impaired":0,"visual_impaired":0,"clean_effects":0,"attached_pic":1,"timed_thumbnails":0},"tags":{"filename":"cover.png","mimetype":"image/png"},"duration":"1421.100000","duration_ts":127899000,"width":350,"height":197,"coded_width":350,"coded_height":197,"closed_captions":0,"has_b_frames":0,"sample_aspect_ratio":"1:1","display_aspect_ratio":"350:197","pix_fmt":"rgb24","level":-99,"color_range":"pc","refs":1}],"audioStream":[{"index":1,"codec_name":"eac3","codec_long_name":"ATSC A/52B (AC-3, E-AC-3)","codec_type":"audio","codec_tag_string":"[0][0][0][0]","codec_tag":"0x0000","r_frame_rate":"0/0","avg_frame_rate":"0/0","time_base":"1/1000","start_pts":12,"start_time":"0.012000","disposition":{"default":1,"dub":0,"original":0,"comment":0,"lyrics":0,"karaoke":0,"forced":0,"hearing_impaired":0,"visual_impaired":0,"clean_effects":0,"attached_pic":0,"timed_thumbnails":0},"tags":{"BPS":"128000","DURATION":"00:23:41.088000000","NUMBER_OF_FRAMES":"44409","NUMBER_OF_BYTES":"22737408","_STATISTICS_WRITING_APP":"mkvmerge v69.0.0 (\u0027Day And Age\u0027) 64-bit","_STATISTICS_WRITING_DATE_UTC":"2024-01-06 04:19:11","_STATISTICS_TAGS":"BPS DURATION NUMBER_OF_FRAMES NUMBER_OF_BYTES","language":"jpn"},"sample_fmt":"fltp","sample_rate":"48000","channels":2,"bits_per_sample":0}],"subtitleStream":[{"index":2,"codec_name":"ass","codec_long_name":"ASS (Advanced SSA) subtitle","codec_type":"subtitle","codec_tag_string":"[0][0][0][0]","codec_tag":"0x0000","r_frame_rate":"0/0","avg_frame_rate":"0/0","time_base":"1/1000","start_pts":0,"start_time":"0.000000","duration":"1421.100000","duration_ts":1421100,"disposition":{"default":1,"dub":0,"original":0,"comment":0,"lyrics":0,"karaoke":0,"forced":0,"hearing_impaired":0,"visual_impaired":0,"clean_effects":0,"attached_pic":0,"timed_thumbnails":0},"tags":{"BPS":"110","DURATION":"00:21:01.710000000","NUMBER_OF_FRAMES":"255","NUMBER_OF_BYTES":"17392","_STATISTICS_WRITING_APP":"mkvmerge v69.0.0 (\u0027Day And Age\u0027) 64-bit","_STATISTICS_WRITING_DATE_UTC":"2024-01-06 04:19:11","_STATISTICS_TAGS":"BPS DURATION NUMBER_OF_FRAMES NUMBER_OF_BYTES","language":"eng"}}]},"eventType":"EventMediaParseStreamPerformed"}
""".trimIndent()