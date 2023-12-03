import com.fasterxml.jackson.databind.ObjectMapper
import com.google.gson.Gson
import no.iktdev.mediaprocessing.shared.kafka.core.DefaultConsumer
import no.iktdev.mediaprocessing.shared.kafka.dto.Message
import no.iktdev.mediaprocessing.shared.kafka.dto.MessageDataWrapper
import no.iktdev.streamit.library.kafka.dto.Status
import org.junit.jupiter.api.Test
import org.assertj.core.api.Assertions.assertThat


class SerializationTest {

    @Test
    fun serialize() {
        val gson = Gson()
        val message = Message(
            "d2fb1472-ebdd-4fce-9ffd-7202a1ad911d",
            "01e4420d-f7ab-49b5-ac5b-8b0f4f4a600e",
            data = MockData(
            Status.COMPLETED,
            "Test"
        ))

        val json = gson.toJson(message)
        val objectMapper = ObjectMapper()
        val result = objectMapper.readValue(json, Message::class.java)
        assertThat(result.data).isInstanceOf(MockData::class.java)


    }

    @Test
    fun getAnnotatedClasses() {
        val serializer = DefaultConsumer.GsonDeserializer()
        val result = serializer.getAnnotatedClasses()
        assertThat(result).isNotEmpty()
    }



}

data class MockData(
    override val status: Status,
    val tekst: String

): MessageDataWrapper(status)