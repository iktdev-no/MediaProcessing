package no.iktdev.mediaprocessing.coordinator

import no.iktdev.eventi.data.EventMetadata
import no.iktdev.eventi.data.EventStatus
import no.iktdev.mediaprocessing.coordinator.tasksV2.listeners.MetadataWaitOrDefaultTaskListener
import no.iktdev.mediaprocessing.shared.common.contract.data.*
import no.iktdev.mediaprocessing.shared.common.contract.dto.OperationEvents
import java.util.UUID

val defaultReferenceId = UUID.randomUUID().toString()

fun defaultStartEvent(): MediaProcessStartEvent {
    return MediaProcessStartEvent(
        metadata = defaultMetadata(),
        data = StartEventData(
            operations = listOf(OperationEvents.ENCODE, OperationEvents.EXTRACT, OperationEvents.CONVERT),
            file = "DummyTestFile.mkv"
        )
    )
}

fun defaultBaseInfoEvent(): BaseInfoEvent {
    return BaseInfoEvent(
        metadata = defaultMetadata(),
        data = BaseInfo(
            title = "Potetmos",
            sanitizedName = "Potetmos mannen",
            searchTitles = listOf("Potetmos mannen")
        )
    )
}

fun metadataSearchTimedOutEvent(): MediaMetadataReceivedEvent {
    return MediaMetadataReceivedEvent(
        metadata = defaultMetadata()
            .copy(status = EventStatus.Skipped)
            .copy(source = MetadataWaitOrDefaultTaskListener::class.java.simpleName),
        data = null
    )
}

fun defaultMetadataSearchEvent(): MediaMetadataReceivedEvent {
    return MediaMetadataReceivedEvent(
        metadata = defaultMetadata(),
        data = pyMetadata(
            title = "Potetmos",
            type = "movie",
        )
    )
}


fun defaultMetadata(): EventMetadata {
    return EventMetadata(
        referenceId = defaultReferenceId,
        status = EventStatus.Success,
        source = "TestData"
    )
}