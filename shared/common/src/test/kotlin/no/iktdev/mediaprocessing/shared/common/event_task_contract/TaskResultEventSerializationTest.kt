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
import no.iktdev.eventi.models.Event
import no.iktdev.eventi.models.Metadata
import no.iktdev.eventi.models.store.TaskStatus
import no.iktdev.mediaprocessing.shared.common.TestBase
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.reflections.Reflections
import java.lang.reflect.Modifier
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
        val reflections = Reflections("no.iktdev.mediaprocessing")
        val subclasses = reflections.getSubTypesOf(TaskResultEvent::class.java)

        require(subclasses.isNotEmpty()) {
            "Fant ingen subklasser av TaskResultEvent — er pakken riktig?"
        }

        // Filtrer ut klasser som er abstrakte eller interfaces
        val concreteSubclasses = subclasses.filter { clazz ->
            val modifiers = clazz.modifiers
            !Modifier.isAbstract(modifiers) && !Modifier.isInterface(modifiers)
        }

        concreteSubclasses.forEach { clazz ->
            assertDoesNotThrow("Serialization failed for ${clazz.simpleName}") {
                val instance = createDummyInstance(clazz)

                // Gson test
                val jsonGson = gson.toJson(instance)
                gson.fromJson(jsonGson, clazz)

                // Jackson test
                val jsonJackson = jackson.writeValueAsString(instance)
                jackson.readValue(jsonJackson, clazz)
            }
        }
    }

    @Test
    fun `all TaskResultEvent subclasses must correctly copy via newStatus and create a new object reference`() {
        val reflections = Reflections("no.iktdev.mediaprocessing")
        val subclasses = reflections.getSubTypesOf(TaskResultEvent::class.java)

        require(subclasses.isNotEmpty()) {
            "Fant ingen subklasser av TaskResultEvent — er pakken riktig?"
        }

        val concreteSubclasses = subclasses.filter { clazz ->
            val modifiers = clazz.modifiers
            !Modifier.isAbstract(modifiers) && !Modifier.isInterface(modifiers)
        }

        concreteSubclasses.forEach { clazz ->
            assertDoesNotThrow("newStatus test failed for ${clazz.simpleName}") {
                // 1. Opprett original dummy-instans og tving inn referenceId
                val original = (createDummyInstance(clazz) as TaskResultEvent).apply {
                    this.newReferenceId()
                    this.derivedOf(TestBase.DummyEvent().usingReferenceId(this.referenceId))
                }
                original.newReferenceId()

                val originalId = original.eventId
                val originalRefId = original.referenceId
                val originalDerivedIds = original.metadata?.derivedFromId

                // 2. Kall newStatus for å lage et nytt objekt
                val newEvent = original.newStatus(TaskStatus.Skipped)

                // 3. Verifiser at det er en ekte minnekopi (nytt objekt, ikke samme referanse)
                org.junit.jupiter.api.Assertions.assertNotSame(
                    original,
                    newEvent,
                    "${clazz.simpleName}.newStatus() returnerte samme objektreferanse (ingen minnekopi)!"
                )

                // 4. Verifiser at typen bevares (f.eks. ikke kuttet ned til baseklassen)
                org.junit.jupiter.api.Assertions.assertEquals(
                    clazz,
                    newEvent.javaClass,
                    "${clazz.simpleName}.newStatus() endret klassetype til ${newEvent.javaClass.simpleName}"
                )

                // 5. Verifiser at status ble oppdatert
                org.junit.jupiter.api.Assertions.assertEquals(
                    TaskStatus.Skipped,
                    newEvent.status,
                    "${clazz.simpleName} fikk ikke oppdatert status til SKIPPED"
                )

                // 6. Verifiser at eventId er ny (auto-generert unik ID)
                org.junit.jupiter.api.Assertions.assertNotEquals(
                    originalId,
                    newEvent.eventId,
                    "${clazz.simpleName} beholdt den gamle eventId-en"
                )

                // 7. Verifiser at referenceId ble med videre
                org.junit.jupiter.api.Assertions.assertEquals(
                    originalRefId,
                    newEvent.referenceId,
                    "${clazz.simpleName} mistet referenceId"
                )

                // 8. Verifiser at derivedFromId blir med uendret over (og at originalen IKKE er lagt til som avledet fra seg selv)
                val newDerivedIds = newEvent.metadata?.derivedFromId
                org.junit.jupiter.api.Assertions.assertEquals(
                    originalDerivedIds,
                    newDerivedIds,
                    "${clazz.simpleName} beholdt ikke nøyaktig samme derivedFromId-er fra originalen"
                )

                if (newDerivedIds != null) {
                    org.junit.jupiter.api.Assertions.assertFalse(
                        newDerivedIds.contains(originalId),
                        "${clazz.simpleName} la urettmessig til sin egen original-eventId i derivedFromId"
                    )
                }
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
        val instance = ctor.callBy(args)
        if (instance is Event) { instance.newReferenceId() }
        return instance
    }
}