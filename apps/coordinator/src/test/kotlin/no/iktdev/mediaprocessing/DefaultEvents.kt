package no.iktdev.mediaprocessing

import no.iktdev.eventi.models.store.TaskStatus
import no.iktdev.mediaprocessing.MockData.dummyAudioStream
import no.iktdev.mediaprocessing.MockData.dummyVideoStream
import no.iktdev.mediaprocessing.ffmpeg.data.ParsedMediaStreams
import no.iktdev.mediaprocessing.shared.common.event_task_contract.events.FilePrepareForWorkResultEvent
import no.iktdev.mediaprocessing.shared.common.event_task_contract.events.MediaStreamParsedEvent

fun defaultFilePrepareForWorkResultEvent(): FilePrepareForWorkResultEvent {
    return FilePrepareForWorkResultEvent(TaskStatus.Completed, "build/test-intermediate/Test.mkv")
}

fun defaultMediaStreamParsedEvent(): MediaStreamParsedEvent {
    return MediaStreamParsedEvent(
        ParsedMediaStreams(
            videoStream = listOf(dummyVideoStream(0)),
            audioStream = listOf(dummyAudioStream(1)))
    )
}
