package no.iktdev.mediaprocessing.shared.kafka

import com.google.gson.Gson
import no.iktdev.mediaprocessing.shared.contract.ProcessType
import no.iktdev.mediaprocessing.shared.kafka.core.DeserializingRegistry
import no.iktdev.mediaprocessing.shared.kafka.core.KafkaEvents
import no.iktdev.mediaprocessing.shared.kafka.dto.Message
import no.iktdev.mediaprocessing.shared.kafka.dto.MessageDataWrapper
import no.iktdev.mediaprocessing.shared.kafka.dto.events_result.MediaProcessStarted
import no.iktdev.mediaprocessing.shared.kafka.dto.Status
import org.junit.jupiter.api.Test
import org.assertj.core.api.Assertions.assertThat


class SerializationTest {

    @Test
    fun serialize() {
        val gson = Gson()
        val message = Message(
            "d2fb1472-ebdd-4fce-9ffd-7202a1ad911d",
            "01e4420d-f7ab-49b5-ac5b-8b0f4f4a600e",
            data = MediaProcessStarted(
            Status.COMPLETED,
                ProcessType.MANUAL,
                file = "Potato.mp4"
        ))

        val json = gson.toJson(message)
        val deserializer = DeserializingRegistry()
        val result = deserializer.deserialize(KafkaEvents.EventMediaProcessStarted, json)
        assertThat(result.data).isInstanceOf(MediaProcessStarted::class.java)


    }




}

data class MockData(
    override val status: Status,
    val tekst: String

): MessageDataWrapper(status)