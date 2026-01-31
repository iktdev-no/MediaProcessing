package no.iktdev.mediaprocessing.shared.common.event_task_contract

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.KotlinFeature
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.google.gson.GsonBuilder
import com.google.gson.JsonDeserializer
import com.google.gson.JsonPrimitive
import com.google.gson.JsonSerializer
import no.iktdev.eventi.models.store.TaskStatus
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.reflections.Reflections
import java.time.Instant
import java.util.*
import kotlin.reflect.KClass
import kotlin.reflect.full.primaryConstructor

class TaskResultEventSerializationTest {

    val gson = GsonBuilder()
        .registerTypeAdapter(Instant::class.java, JsonSerializer<Instant> { src, _, _ ->
            JsonPrimitive(src.toString())
        })
        .registerTypeAdapter(Instant::class.java, JsonDeserializer { json, _, _ ->
            Instant.parse(json.asString)
        })
        .create()

    val jackson = ObjectMapper()
        .registerModule(JavaTimeModule())
        .registerModule(
            KotlinModule.Builder()
                .configure(KotlinFeature.NullToEmptyCollection, false)
                .configure(KotlinFeature.NullToEmptyMap, false)
                .configure(KotlinFeature.NullIsSameAsDefault, false)
                .configure(KotlinFeature.StrictNullChecks, true)
                .configure(KotlinFeature.UseJavaDurationConversion, true)
                .build()
        )
        .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)


    @Test
    fun `all TaskResultEvent subclasses must serialize with Gson and Jackson`() {
        val reflections = Reflections("no.iktdev.mediaprocessing") // rotpakken din
        val subclasses = reflections.getSubTypesOf(TaskResultEvent::class.java)

        require(subclasses.isNotEmpty()) {
            "Fant ingen subklasser av TaskResultEvent — er pakken riktig?"
        }

        subclasses.forEach { clazz ->
            assertDoesNotThrow("Serialization failed for ${clazz.simpleName}") {
                val instance = createDummyInstance(clazz)
                val jsonGson = gson.toJson(instance)
                gson.fromJson(jsonGson, clazz)

                val jsonJackson = jackson.writeValueAsString(instance)
                jackson.readValue(jsonJackson, clazz)
            }
        }
    }

    /**
     * Lager en dummy-instans av en TaskResultEvent-subklasse.
     * Forutsetter at alle event-klasser har en primary constructor.
     */
    private fun createDummyInstance(clazz: Class<*>): Any {
        val kClass = clazz.kotlin
        val ctor = kClass.primaryConstructor
            ?: error("Klassen ${clazz.simpleName} mangler primary constructor")

        val args = ctor.parameters.associateWith { param ->
            val kType = param.type
            val classifier = kType.classifier

            // --- 1. Handle concrete known types first ---
            if (classifier == String::class) return@associateWith "dummy"
            if (classifier == Int::class) return@associateWith 1
            if (classifier == Long::class) return@associateWith 1L
            if (classifier == Boolean::class) return@associateWith false
            if (classifier == Double::class) return@associateWith 1.0
            if (classifier == UUID::class) return@associateWith UUID.randomUUID()
            if (classifier == Instant::class) return@associateWith Instant.now()
            if (classifier == TaskStatus::class) return@associateWith TaskStatus.Completed
            if (classifier == Float::class) return@associateWith 1.0f


            // --- 2. Generic enum support ---
            if (classifier is KClass<*> && classifier.java.isEnum) {
                return@associateWith classifier.java.enumConstants.first()
            }

            // --- 3. Lists and maps ---
            if (classifier == List::class) return@associateWith emptyList<Any>()
            if (classifier == Map::class) return@associateWith emptyMap<String, Any>()

            // --- 4. Nested data classes ---
            if (classifier is KClass<*> && classifier.isData) {
                return@associateWith createDummyInstance(classifier.java)
            }

            // --- 5. Fallback ---
            null
        }

        return ctor.callBy(args)
    }




}
