package no.iktdev.mediaprocessing.shared.common

import no.iktdev.mediaprocessing.shared.common.contract.Events
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
    }


}