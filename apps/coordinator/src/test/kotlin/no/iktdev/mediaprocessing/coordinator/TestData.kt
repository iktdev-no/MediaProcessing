package no.iktdev.mediaprocessing.coordinator

import no.iktdev.eventi.data.EventMetadata
import no.iktdev.eventi.data.EventStatus
import no.iktdev.mediaprocessing.shared.common.contract.data.MediaProcessStartEvent
import no.iktdev.mediaprocessing.shared.common.contract.data.StartEventData
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

fun defaultMetadata(): EventMetadata {
    return EventMetadata(
        referenceId = defaultReferenceId,
        status = EventStatus.Success,
        source = "TestData"
    )
}