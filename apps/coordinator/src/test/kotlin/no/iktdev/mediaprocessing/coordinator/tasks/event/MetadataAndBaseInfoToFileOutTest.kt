package no.iktdev.mediaprocessing.coordinator.tasks.event

import no.iktdev.mediaprocessing.PersistentMessageFromJsonDump
import no.iktdev.mediaprocessing.shared.common.lastOrSuccessOf
import no.iktdev.mediaprocessing.shared.kafka.core.KafkaEvents
import no.iktdev.mediaprocessing.shared.kafka.dto.events_result.BaseInfoPerformed
import no.iktdev.mediaprocessing.shared.kafka.dto.events_result.MetadataPerformed
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class MetadataAndBaseInfoToFileOutTest {

    fun testData(): String {
        return """
            [
            {"type":"header","version":"5.2.1","comment":"Export to JSON plugin for PHPMyAdmin"},
            {"type":"database","name":"eventsV2"},
            {"type":"table","name":"events","database":"eventsV2","data":
            [
            {"id":"9","referenceId":"f015ad8a-8210-4040-993b-bdaa5bd25d80","eventId":"3cea9a98-2e65-4e70-96bc-4b6933c06af7","event":"event:media-read-base-info:performed","data":"{\"status\":\"COMPLETED\",\"title\":\"Psycho-Pass Movie\",\"sanitizedName\":\"Psycho-Pass Movie - Providence\",\"derivedFromEventId\":\"62408248-d457-4f4d-a2c7-9b17e5701336\"}","created":"2024-04-15 22:24:07.406088"},
            {"id":"19","referenceId":"f015ad8a-8210-4040-993b-bdaa5bd25d80","eventId":"0edaa265-fc85-41bc-952a-acb21771feb9","event":"event:media-metadata-search:performed","data":"{\"status\":\"COMPLETED\",\"data\":{\"title\":\"Psycho-Pass Movie: Providence\",\"altTitle\":[],\"cover\":\"https:\/\/cdn.myanimelist.net\/images\/anime\/1244\/134653.jpg\",\"type\":\"movie\",\"summary\":[{\"summary\":\"In 2113, the Ministry of Foreign Affairs (MFA) dissolved their secret paramilitary unit known as the Peacebreakers. However, the squad disappeared, and their activities remained a mystery. Five years later, the Peacebreakers resurface when they murder Milcia Stronskaya, a scientist in possession of highly classified documents essential to the future of the Sybil System—Japan\\u0027s surveillance structure that detects potential criminals in society.  To investigate the incident and prepare for a clash against the Peacebreakers in coordination with the MFA, the chief of the Public Safety Bureau decides to recruit the former Enforcer Shinya Kougami back into the force. Having defected years ago, Kougami currently works for the MFA under Frederica Hanashiro\\u0027s command. Kougami\\u0027s return creates tensions between him and his former colleagues Akane Tsunemori and Nobuchika Ginoza, but they must set aside their past grudges to focus on ensuring the security of the Sybil System.  [Written by MAL Rewrite]\",\"language\":\"eng\"}],\"genres\":[\"Action\",\"Mystery\",\"Sci-Fi\",\"Suspense\"]}}","created":"2024-04-15 22:24:18.339106"}
            ]
            }
            ]
        """.trimIndent()
    }

    @Test
    fun testVideoData() {
        val pmdj = PersistentMessageFromJsonDump(testData())
        val events = pmdj.getPersistentMessages()

        val baseInfo = events.lastOrSuccessOf(KafkaEvents.EventMediaReadBaseInfoPerformed) { it.data is BaseInfoPerformed }?.data as BaseInfoPerformed
        val meta = events.lastOrSuccessOf(KafkaEvents.EventMediaMetadataSearchPerformed) { it.data is MetadataPerformed }?.data as MetadataPerformed?

        val pm = MetadataAndBaseInfoToFileOut.ProcessMediaInfoAndMetadata(baseInfo, meta)


        val vi = pm.getVideoPayload()

        assertThat(vi).isNotNull()
    }

}