package no.iktdev.mediaprocessing.shared.common

import no.iktdev.mediaprocessing.shared.common.contract.Events
import no.iktdev.mediaprocessing.shared.common.contract.data.EpisodeInfo
import no.iktdev.mediaprocessing.shared.common.contract.data.MediaMetadataReceivedEvent
import no.iktdev.mediaprocessing.shared.common.contract.data.MediaOutInformationConstructedEvent
import no.iktdev.mediaprocessing.shared.common.contract.data.StartEventData
import no.iktdev.mediaprocessing.shared.common.contract.jsonToEvent
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class DatabaseDeserializerTest {

    @Test
    fun validateParsingOfStartEvent() {
        //language=json
        val data = """{"metadata":{"eventId":"45e92856-37a6-4266-81cd-a8cf16dd7eb2","referenceId":"a42bf0a4-d31d-4715-8e4d-1432d52f9786","status":"Success","created":"2025-02-10T21:35:52.437791666","source":"Coordinator"},"data":{"type":"FLOW","operations":["ENCODE","EXTRACT","CONVERT"],"file":"/potato"},"eventType":"EventMediaProcessStarted"}"""
        val result = data.jsonToEvent("event:media-process:started")
        assertThat(result.data!!.javaClass).hasSameClassAs(StartEventData::class.java)
        assertThat(result.eventType).isNotNull()
        assertThat(result.eventType).isEqualTo(Events.ProcessStarted)
    }

    @Test
    fun validateMediaInfo() {
        //language=json
        val data = """
            {
                "metadata": {
                    "derivedFromEventId": "c2ec1424-3d8f-444c-ad85-ce04e6c583fd",
                    "eventId": "0b164f37-fa23-4b43-a31e-edfa56325eb2",
                    "referenceId": "920e67cc-1f07-43f0-b121-e5ae87195122",
                    "status": "Success",
                    "created": "2025-02-24T00:39:16.057643776",
                    "source": "MediaOutInformationTaskListener"
                },
                "eventType": "ReadOutNameAndType",
                "data": {
                    "info": {
                        "type": "serie",
                        "title": "The Potato",
                        "episode": 1,
                        "season": 2,
                        "episodeTitle": "",
                        "fullName": "The Bit potato"
                    }
                }
            }
        """.trimIndent()
        val result = data.jsonToEvent("event:media-read-out-name-and-type:performed")
        assertThat(result.data!!.javaClass).hasSameClassAs(MediaOutInformationConstructedEvent::class.java)
        assertThat(result.eventType).isNotNull()
        val serieInfo = (result as  MediaOutInformationConstructedEvent).data?.toValueObject()
        assertThat(serieInfo).isNotNull()
        assertThat(serieInfo!!.javaClass).hasSameClassAs(EpisodeInfo::class.java)
    }

    @Test
    fun validateMetadataRead() {
        //language=json
        val data = """
            {
                "metadata": {
                    "derivedFromEventId": "855b6de0-38f1-4ac9-9397-1ca7fc83fa4d",
                    "eventId": "c2ec1424-3d8f-444c-ad85-ce04e6c583fd",
                    "referenceId": "920e67cc-1f07-43f0-b121-e5ae87195122",
                    "status": "Success",
                    "created": "2025-02-24T00:39:15.674278",
                    "source": "metadataApp"
                },
                "eventType": "EventMediaMetadataSearchPerformed",
                "data": {
                    "title": "Cabbage",
                    "altTitle": [
                        "Cabbage man"
                    ],
                    "cover": "https://cabbageman.co",
                    "banner": null,
                    "type": "serie",
                    "summary": [
                        {
                            "summary": "Forced to becoma a cabbage farmer after getting their passport confiscated",
                            "language": "eng"
                        }
                    ],
                    "genres": [
                        "Drama",
                        "Mystery"
                    ],
                    "source": "yt"
                }
            }
        """.trimIndent()
        val result = data.jsonToEvent("event:media-metadata-search:performed")
        assertThat(result.data!!.javaClass).hasSameClassAs(MediaMetadataReceivedEvent::class.java)
        assertThat(result.eventType).isNotNull()
        assertThat(result.data).isNotNull()
    }

}