package no.iktdev.mediaprocessing.shared.common.tests

import no.iktdev.mediaprocessing.shared.common.H2DataSource2
import no.iktdev.mediaprocessing.shared.common.datasource.DatabaseConnectionConfig
import no.iktdev.mediaprocessing.shared.common.datasource.withTransaction
import no.iktdev.mediaprocessing.shared.common.persistance.PersistentEventManager
import no.iktdev.mediaprocessing.shared.common.persistance.events
import no.iktdev.mediaprocessing.shared.kafka.core.KafkaEvents
import no.iktdev.mediaprocessing.shared.kafka.dto.Message
import no.iktdev.mediaprocessing.shared.kafka.dto.SimpleMessageData
import no.iktdev.mediaprocessing.shared.kafka.dto.Status
import no.iktdev.mediaprocessing.shared.kafka.dto.events_result.MediaProcessStarted
import org.junit.jupiter.api.Test
import java.util.UUID
import org.assertj.core.api.Assertions.assertThat
import org.jetbrains.exposed.sql.deleteAll


class PersistentEventMangerTestBase {
    val defaultReferenceId = UUID.randomUUID().toString()
    val dataSource = H2DataSource2(DatabaseConnectionConfig(
        address = "",
        username = "",
        password = "",
        databaseName = "test",
        port = null
    ))
    val eventManager: PersistentEventManager = PersistentEventManager(dataSource)

    init {
        val kafkaTables = listOf(
            events, // For kafka
        )
        dataSource.createDatabase()
        dataSource.createTables(*kafkaTables.toTypedArray())
    }

    @Test
    fun testDatabaseIsCreated() {
        val success = dataSource.createDatabase()
        assertThat(success).isNotNull()
    }

    @Test
    fun testDatabaseInit() {
        val referenceId = UUID.randomUUID().toString()
        val mStart = Message<MediaProcessStarted>(
            referenceId = referenceId,
            eventId = UUID.randomUUID().toString(),
            data = MediaProcessStarted(
                status = Status.COMPLETED,
                file = "Nan"
            )
        )
        eventManager.setEvent(KafkaEvents.EventMediaProcessStarted, mStart)
        val stored = eventManager.getEventsWith(referenceId);
        assertThat(stored).isNotEmpty()
    }

    @Test
    fun testSuperseded1() {
        val startEvent = EventToMessage(KafkaEvents.EventMediaProcessStarted, createMessage())
        val oldStack = listOf(
            EventToMessage(KafkaEvents.EventMediaReadStreamPerformed,
                createMessage(eventId = "48c72454-6c7b-406b-b598-fc0a961dabde", derivedFromEventId = startEvent.message.eventId)),
            EventToMessage(KafkaEvents.EventMediaParseStreamPerformed,
                createMessage(eventId = "1d8d995d-a7e4-4d6e-a501-fe82f521cf72", derivedFromEventId ="48c72454-6c7b-406b-b598-fc0a961dabde")),
            EventToMessage(KafkaEvents.EventMediaReadBaseInfoPerformed,
                createMessage(eventId = "f6cae204-7c8e-4003-b598-f7b4e566d03e", derivedFromEventId ="1d8d995d-a7e4-4d6e-a501-fe82f521cf72")),
            EventToMessage(KafkaEvents.EventMediaMetadataSearchPerformed,
                createMessage(eventId = "cbb1e871-e9a5-496d-a655-db719ac4903c", derivedFromEventId = "f6cae204-7c8e-4003-b598-f7b4e566d03e")),
            EventToMessage(KafkaEvents.EventMediaReadOutNameAndType,
                createMessage(eventId = "3f376b72-f55a-4dd7-af87-fb1755ba4ad9", derivedFromEventId = "cbb1e871-e9a5-496d-a655-db719ac4903c")),
            EventToMessage(KafkaEvents.EventMediaReadOutCover,
                createMessage(eventId = "98a39721-41ff-4d79-905e-ced260478524", derivedFromEventId = "cbb1e871-e9a5-496d-a655-db719ac4903c")),

            EventToMessage(KafkaEvents.EventMediaParameterEncodeCreated,
                createMessage(eventId = "9e8f2e04-4950-437f-a203-cfd566203078", derivedFromEventId = "3f376b72-f55a-4dd7-af87-fb1755ba4ad9")),
            EventToMessage(KafkaEvents.EventMediaParameterExtractCreated,
                createMessage(eventId = "af7f2519-0f1d-4679-82bd-0314d1b97b68", derivedFromEventId = "3f376b72-f55a-4dd7-af87-fb1755ba4ad9")),
        )
        eventManager.setEvent(startEvent.event, startEvent.message)
        for (entry in oldStack) {
            eventManager.setEvent(entry.event, entry.message)
        }
        val currentTableWithOldStack = eventManager.getEventsWith(defaultReferenceId)
        assertThat(currentTableWithOldStack).hasSize(oldStack.size +1)

        val supersedingStack = listOf(
            EventToMessage(KafkaEvents.EventMediaReadOutNameAndType,
                createMessage(eventId = "2c3a40bb-2225-4dd4-a8c3-32c6356f8764", derivedFromEventId = "cbb1e871-e9a5-496d-a655-db719ac4903c"))
        ).forEach {entry -> eventManager.setEvent(entry.event, entry.message)}


        // Final check

        val result = eventManager.getEventsWith(defaultReferenceId)
        val idsThatShouldBeRemoved = listOf(
            "9e8f2e04-4950-437f-a203-cfd566203078",
            "af7f2519-0f1d-4679-82bd-0314d1b97b68"
        )
        val search = result.filter { it.eventId in idsThatShouldBeRemoved }
        assertThat(search).isEmpty()


        val expectedInList = listOf(
            startEvent.message.eventId,
            "48c72454-6c7b-406b-b598-fc0a961dabde",
            "1d8d995d-a7e4-4d6e-a501-fe82f521cf72",
            "f6cae204-7c8e-4003-b598-f7b4e566d03e",
            "cbb1e871-e9a5-496d-a655-db719ac4903c",
            "98a39721-41ff-4d79-905e-ced260478524",
            "2c3a40bb-2225-4dd4-a8c3-32c6356f8764"
        )
        val searchForExpected = result.map { it.eventId }
        assertThat(expectedInList).isEqualTo(searchForExpected)
        withTransaction(dataSource) {
            events.deleteAll()
        }
    }

    @Test
    fun testSuperseded2() {
        val startEvent = EventToMessage(KafkaEvents.EventMediaProcessStarted, createMessage()).also {
            eventManager.setEvent(it.event, it.message)
        }
        val keepStack = listOf(
            EventToMessage(KafkaEvents.EventMediaReadStreamPerformed,
                createMessage(eventId = "48c72454-6c7b-406b-b598-fc0a961dabde", derivedFromEventId = startEvent.message.eventId)),
            EventToMessage(KafkaEvents.EventMediaParseStreamPerformed,
                createMessage(eventId = "1d8d995d-a7e4-4d6e-a501-fe82f521cf72", derivedFromEventId ="48c72454-6c7b-406b-b598-fc0a961dabde")),
            EventToMessage(KafkaEvents.EventMediaReadBaseInfoPerformed,
                createMessage(eventId = "f6cae204-7c8e-4003-b598-f7b4e566d03e", derivedFromEventId ="1d8d995d-a7e4-4d6e-a501-fe82f521cf72")),
            EventToMessage(KafkaEvents.EventMediaMetadataSearchPerformed,
                createMessage(eventId = "cbb1e871-e9a5-496d-a655-db719ac4903c", derivedFromEventId = "f6cae204-7c8e-4003-b598-f7b4e566d03e")),
            EventToMessage(KafkaEvents.EventMediaReadOutCover,
                createMessage(eventId = "98a39721-41ff-4d79-905e-ced260478524", derivedFromEventId = "cbb1e871-e9a5-496d-a655-db719ac4903c")),
        ).onEach { entry -> eventManager.setEvent(entry.event, entry.message)  }

        val toBeReplaced = listOf(
            EventToMessage(KafkaEvents.EventMediaReadOutNameAndType,
                createMessage(eventId = "3f376b72-f55a-4dd7-af87-fb1755ba4ad9", derivedFromEventId = "cbb1e871-e9a5-496d-a655-db719ac4903c")),
            EventToMessage(KafkaEvents.EventMediaParameterEncodeCreated,
                createMessage(eventId = "9e8f2e04-4950-437f-a203-cfd566203078", derivedFromEventId = "3f376b72-f55a-4dd7-af87-fb1755ba4ad9")),
            EventToMessage(KafkaEvents.EventMediaParameterExtractCreated,
                createMessage(eventId = "af7f2519-0f1d-4679-82bd-0314d1b97b68", derivedFromEventId = "3f376b72-f55a-4dd7-af87-fb1755ba4ad9")),
        ).onEach { entry -> eventManager.setEvent(entry.event, entry.message)  }


        val currentTableWithOldStack = eventManager.getEventsWith(defaultReferenceId)
        assertThat(currentTableWithOldStack).hasSize(keepStack.size + toBeReplaced.size +1)

        val supersedingStack = listOf(
            EventToMessage(KafkaEvents.EventMediaReadOutNameAndType,
                createMessage(eventId = "2c3a40bb-2225-4dd4-a8c3-32c6356f8764", derivedFromEventId = "cbb1e871-e9a5-496d-a655-db719ac4903c"))
        ).onEach { entry -> eventManager.setEvent(entry.event, entry.message)  }


        // Final check

        val result = eventManager.getEventsWith(defaultReferenceId)

        val idsRemoved = toBeReplaced.map { it.message.eventId }
        val search = result.filter { it.eventId in idsRemoved }
        assertThat(search).isEmpty()


        val expectedInList = listOf(startEvent.message.eventId) + keepStack.map { it.message.eventId } + supersedingStack.map { it.message.eventId }
        val searchForExpected = result.map { it.eventId }
        assertThat(expectedInList).isEqualTo(searchForExpected)

        withTransaction(dataSource) {
            events.deleteAll()
        }
    }

    @Test
    fun testSuperseded3() {
        val startEvent = EventToMessage(KafkaEvents.EventMediaProcessStarted, createMessage()).also {
            eventManager.setEvent(it.event, it.message)
        }
        val keepStack = listOf(
            EventToMessage(KafkaEvents.EventMediaReadStreamPerformed,
                createMessage(eventId = "48c72454-6c7b-406b-b598-fc0a961dabde", derivedFromEventId = startEvent.message.eventId)),

        ).onEach { entry -> eventManager.setEvent(entry.event, entry.message)  }

        val toBeReplaced = listOf(
            EventToMessage(KafkaEvents.EventMediaParseStreamPerformed,
                createMessage(eventId = "1d8d995d-a7e4-4d6e-a501-fe82f521cf72", derivedFromEventId ="48c72454-6c7b-406b-b598-fc0a961dabde")),
            EventToMessage(KafkaEvents.EventMediaReadBaseInfoPerformed,
                createMessage(eventId = "f6cae204-7c8e-4003-b598-f7b4e566d03e", derivedFromEventId ="1d8d995d-a7e4-4d6e-a501-fe82f521cf72")),
            EventToMessage(KafkaEvents.EventMediaMetadataSearchPerformed,
                createMessage(eventId = "cbb1e871-e9a5-496d-a655-db719ac4903c", derivedFromEventId = "f6cae204-7c8e-4003-b598-f7b4e566d03e")),
            EventToMessage(KafkaEvents.EventMediaReadOutCover,
                createMessage(eventId = "98a39721-41ff-4d79-905e-ced260478524", derivedFromEventId = "cbb1e871-e9a5-496d-a655-db719ac4903c")),
            EventToMessage(KafkaEvents.EventMediaReadOutNameAndType,
                createMessage(eventId = "3f376b72-f55a-4dd7-af87-fb1755ba4ad9", derivedFromEventId = "cbb1e871-e9a5-496d-a655-db719ac4903c")),
            EventToMessage(KafkaEvents.EventMediaParameterEncodeCreated,
                createMessage(eventId = "9e8f2e04-4950-437f-a203-cfd566203078", derivedFromEventId = "3f376b72-f55a-4dd7-af87-fb1755ba4ad9")),
            EventToMessage(KafkaEvents.EventMediaParameterExtractCreated,
                createMessage(eventId = "af7f2519-0f1d-4679-82bd-0314d1b97b68", derivedFromEventId = "3f376b72-f55a-4dd7-af87-fb1755ba4ad9")),
        ).onEach { entry -> eventManager.setEvent(entry.event, entry.message)  }


        val currentTableWithOldStack = eventManager.getEventsWith(defaultReferenceId)
        assertThat(currentTableWithOldStack).hasSize(keepStack.size + toBeReplaced.size +1)

        val supersedingStack = listOf(
            EventToMessage(KafkaEvents.EventMediaParseStreamPerformed,
                createMessage(eventId = "2c3a40bb-2225-4dd4-a8c3-32c6356f8764", derivedFromEventId = "48c72454-6c7b-406b-b598-fc0a961dabde"))
        ).onEach { entry -> eventManager.setEvent(entry.event, entry.message)  }


        // Final check

        val result = eventManager.getEventsWith(defaultReferenceId)

        val idsRemoved = toBeReplaced.map { it.message.eventId }
        val search = result.filter { it.eventId in idsRemoved }
        assertThat(search).isEmpty()


        val expectedInList = listOf(startEvent.message.eventId) + keepStack.map { it.message.eventId } + supersedingStack.map { it.message.eventId }
        val searchForExpected = result.map { it.eventId }
        assertThat(expectedInList).isEqualTo(searchForExpected)

        withTransaction(dataSource) {
            events.deleteAll()
        }
    }

    @Test
    fun testSupersededButKeepWork() {
        val startEventPayload = createMessage()
        val keepStack = listOf(
            EventToMessage(KafkaEvents.EventMediaProcessStarted, startEventPayload),
            EventToMessage(KafkaEvents.EventMediaReadStreamPerformed,
                createMessage(eventId = "48c72454-6c7b-406b-b598-fc0a961dabde", derivedFromEventId = startEventPayload.eventId)),
            EventToMessage(KafkaEvents.EventMediaParseStreamPerformed,
                createMessage(eventId = "1d8d995d-a7e4-4d6e-a501-fe82f521cf72", derivedFromEventId ="48c72454-6c7b-406b-b598-fc0a961dabde")),
            EventToMessage(KafkaEvents.EventMediaReadBaseInfoPerformed,
                createMessage(eventId = "f6cae204-7c8e-4003-b598-f7b4e566d03e", derivedFromEventId ="1d8d995d-a7e4-4d6e-a501-fe82f521cf72")),
            EventToMessage(KafkaEvents.EventMediaMetadataSearchPerformed,
                createMessage(eventId = "cbb1e871-e9a5-496d-a655-db719ac4903c", derivedFromEventId = "f6cae204-7c8e-4003-b598-f7b4e566d03e")),
            EventToMessage(KafkaEvents.EventMediaReadOutCover,
                createMessage(eventId = "98a39721-41ff-4d79-905e-ced260478524", derivedFromEventId = "cbb1e871-e9a5-496d-a655-db719ac4903c")),
            EventToMessage(KafkaEvents.EventMediaReadOutNameAndType,
                createMessage(eventId = "3f376b72-f55a-4dd7-af87-fb1755ba4ad9", derivedFromEventId = "cbb1e871-e9a5-496d-a655-db719ac4903c")),
            EventToMessage(KafkaEvents.EventMediaParameterEncodeCreated,
                createMessage(eventId = "9e8f2e04-4950-437f-a203-cfd566203078", derivedFromEventId = "3f376b72-f55a-4dd7-af87-fb1755ba4ad9")),
            EventToMessage(KafkaEvents.EventMediaParameterExtractCreated,
                createMessage(eventId = "af7f2519-0f1d-4679-82bd-0314d1b97b68", derivedFromEventId = "3f376b72-f55a-4dd7-af87-fb1755ba4ad9")),
            EventToMessage(KafkaEvents.EventWorkExtractCreated,
                createMessage(eventId = "ad93a41a-db08-436b-84e4-55adb4752f38", derivedFromEventId = "af7f2519-0f1d-4679-82bd-0314d1b97b68")),
        ).onEach { entry -> eventManager.setEvent(entry.event, entry.message)  }

        val newEvents = listOf(
            EventToMessage(KafkaEvents.EventWorkExtractCreated,
                createMessage(eventId = "cfeee961-69c1-4eed-8ec5-82ebca01c9e1", derivedFromEventId = "af7f2519-0f1d-4679-82bd-0314d1b97b68")),
            EventToMessage(KafkaEvents.EventWorkExtractCreated,
                createMessage(eventId = "64625872-bbfe-4604-85cd-02f58e904267", derivedFromEventId = "af7f2519-0f1d-4679-82bd-0314d1b97b68")),
            EventToMessage(KafkaEvents.EventWorkExtractCreated,
                createMessage(eventId = "0ab96b32-45a5-4517-b0c0-c03d48145340", derivedFromEventId = "af7f2519-0f1d-4679-82bd-0314d1b97b68")),
            EventToMessage(KafkaEvents.EventWorkExtractCreated,
                createMessage(eventId = "cabd9038-307f-48e4-ac99-88232b1a817c", derivedFromEventId = "af7f2519-0f1d-4679-82bd-0314d1b97b68")),
            EventToMessage(KafkaEvents.EventWorkExtractCreated,
                createMessage(eventId = "10c0fd42-b5be-42b2-a27b-12ecccc51635", derivedFromEventId = "af7f2519-0f1d-4679-82bd-0314d1b97b68")),
            EventToMessage(KafkaEvents.EventWorkExtractCreated,
                createMessage(eventId = "b69fb306-e390-4a9e-8d11-89d0688dff16", derivedFromEventId = "af7f2519-0f1d-4679-82bd-0314d1b97b68")),
            ).onEach { entry -> eventManager.setEvent(entry.event, entry.message)  }

        val result = eventManager.getEventsWith(defaultReferenceId)

        val expected = (keepStack + newEvents).map { it.message.eventId }
        val missing = expected - result.map { it.eventId }
        assertThat(missing).isEmpty()
        assertThat(expected.size).isEqualTo(result.size)

        withTransaction(dataSource) {
            events.deleteAll()
        }
    }

    @Test
    fun testSupersededWork() {
        val startEventPayload = createMessage()
        val keepStack = listOf(
            EventToMessage(KafkaEvents.EventMediaProcessStarted, startEventPayload),
            EventToMessage(KafkaEvents.EventMediaReadStreamPerformed,
                createMessage(eventId = "48c72454-6c7b-406b-b598-fc0a961dabde", derivedFromEventId = startEventPayload.eventId)),
            EventToMessage(KafkaEvents.EventMediaParseStreamPerformed,
                createMessage(eventId = "1d8d995d-a7e4-4d6e-a501-fe82f521cf72", derivedFromEventId ="48c72454-6c7b-406b-b598-fc0a961dabde")),
            EventToMessage(KafkaEvents.EventMediaReadBaseInfoPerformed,
                createMessage(eventId = "f6cae204-7c8e-4003-b598-f7b4e566d03e", derivedFromEventId ="1d8d995d-a7e4-4d6e-a501-fe82f521cf72")),
            EventToMessage(KafkaEvents.EventMediaMetadataSearchPerformed,
                createMessage(eventId = "cbb1e871-e9a5-496d-a655-db719ac4903c", derivedFromEventId = "f6cae204-7c8e-4003-b598-f7b4e566d03e")),
            EventToMessage(KafkaEvents.EventMediaReadOutCover,
                createMessage(eventId = "98a39721-41ff-4d79-905e-ced260478524", derivedFromEventId = "cbb1e871-e9a5-496d-a655-db719ac4903c")),
            EventToMessage(KafkaEvents.EventMediaReadOutNameAndType,
                createMessage(eventId = "3f376b72-f55a-4dd7-af87-fb1755ba4ad9", derivedFromEventId = "cbb1e871-e9a5-496d-a655-db719ac4903c")),
            EventToMessage(KafkaEvents.EventMediaParameterEncodeCreated,
                createMessage(eventId = "9e8f2e04-4950-437f-a203-cfd566203078", derivedFromEventId = "3f376b72-f55a-4dd7-af87-fb1755ba4ad9")),
        ).onEach { entry -> eventManager.setEvent(entry.event, entry.message)  }

        val newEvents = listOf(
            EventToMessage(KafkaEvents.EventMediaParameterExtractCreated,
                createMessage(eventId = "af7f2519-0f1d-4679-82bd-0314d1b97b68", derivedFromEventId = "3f376b72-f55a-4dd7-af87-fb1755ba4ad9")),
            EventToMessage(KafkaEvents.EventWorkExtractCreated,
                createMessage(eventId = "ad93a41a-db08-436b-84e4-55adb4752f38", derivedFromEventId = "af7f2519-0f1d-4679-82bd-0314d1b97b68")),
            EventToMessage(KafkaEvents.EventWorkExtractCreated,
                createMessage(eventId = "cfeee961-69c1-4eed-8ec5-82ebca01c9e1", derivedFromEventId = "af7f2519-0f1d-4679-82bd-0314d1b97b68")),
            EventToMessage(KafkaEvents.EventWorkExtractCreated,
                createMessage(eventId = "64625872-bbfe-4604-85cd-02f58e904267", derivedFromEventId = "af7f2519-0f1d-4679-82bd-0314d1b97b68")),
            EventToMessage(KafkaEvents.EventWorkExtractCreated,
                createMessage(eventId = "0ab96b32-45a5-4517-b0c0-c03d48145340", derivedFromEventId = "af7f2519-0f1d-4679-82bd-0314d1b97b68")),
            EventToMessage(KafkaEvents.EventWorkExtractCreated,
                createMessage(eventId = "cabd9038-307f-48e4-ac99-88232b1a817c", derivedFromEventId = "af7f2519-0f1d-4679-82bd-0314d1b97b68")),
            EventToMessage(KafkaEvents.EventWorkExtractCreated,
                createMessage(eventId = "10c0fd42-b5be-42b2-a27b-12ecccc51635", derivedFromEventId = "af7f2519-0f1d-4679-82bd-0314d1b97b68")),
        ).onEach { entry -> eventManager.setEvent(entry.event, entry.message)  }

        val replacedWith = listOf(
            EventToMessage(KafkaEvents.EventMediaParameterExtractCreated,
                createMessage(eventId = "e40b2096-2e6f-4672-9c5a-6c81fe8fc302", derivedFromEventId = "3f376b72-f55a-4dd7-af87-fb1755ba4ad9")),
            EventToMessage(KafkaEvents.EventWorkExtractCreated,
                createMessage(eventId = "b69fb306-e390-4a9e-8d11-89d0688dff16", derivedFromEventId = "e40b2096-2e6f-4672-9c5a-6c81fe8fc302")),
        ).onEach { entry -> eventManager.setEvent(entry.event, entry.message)  }

        val result = eventManager.getEventsWith(defaultReferenceId)

        val expected = (keepStack + replacedWith).map { it.message.eventId }
        val missing = expected - result.map { it.eventId }
        assertThat(missing).isEmpty()
        assertThat(expected.size).isEqualTo(result.size)

        withTransaction(dataSource) {
            events.deleteAll()
        }
    }

    @Test
    fun testConvertBatchFromExtract() {
        val startEventPayload = createMessage()
        val keepStack = listOf(
            EventToMessage(KafkaEvents.EventMediaProcessStarted, startEventPayload),
            EventToMessage(KafkaEvents.EventMediaReadStreamPerformed,
                createMessage(eventId = "48c72454-6c7b-406b-b598-fc0a961dabde", derivedFromEventId = startEventPayload.eventId)),
            EventToMessage(KafkaEvents.EventMediaParseStreamPerformed,
                createMessage(eventId = "1d8d995d-a7e4-4d6e-a501-fe82f521cf72", derivedFromEventId ="48c72454-6c7b-406b-b598-fc0a961dabde")),
            EventToMessage(KafkaEvents.EventMediaReadBaseInfoPerformed,
                createMessage(eventId = "f6cae204-7c8e-4003-b598-f7b4e566d03e", derivedFromEventId ="1d8d995d-a7e4-4d6e-a501-fe82f521cf72")),
            EventToMessage(KafkaEvents.EventMediaMetadataSearchPerformed,
                createMessage(eventId = "cbb1e871-e9a5-496d-a655-db719ac4903c", derivedFromEventId = "f6cae204-7c8e-4003-b598-f7b4e566d03e")),
            EventToMessage(KafkaEvents.EventMediaReadOutCover,
                createMessage(eventId = "98a39721-41ff-4d79-905e-ced260478524", derivedFromEventId = "cbb1e871-e9a5-496d-a655-db719ac4903c")),
            EventToMessage(KafkaEvents.EventMediaReadOutNameAndType,
                createMessage(eventId = "3f376b72-f55a-4dd7-af87-fb1755ba4ad9", derivedFromEventId = "cbb1e871-e9a5-496d-a655-db719ac4903c")),
            EventToMessage(KafkaEvents.EventMediaParameterEncodeCreated,
                createMessage(eventId = "9e8f2e04-4950-437f-a203-cfd566203078", derivedFromEventId = "3f376b72-f55a-4dd7-af87-fb1755ba4ad9")),
            EventToMessage(KafkaEvents.EventMediaParameterExtractCreated,
                createMessage(eventId = "af7f2519-0f1d-4679-82bd-0314d1b97b68", derivedFromEventId = "3f376b72-f55a-4dd7-af87-fb1755ba4ad9")),

        ).onEach { entry -> eventManager.setEvent(entry.event, entry.message)  }

        val convertEvents = mutableListOf<PersistentEventMangerTestBase.EventToMessage>();

        val extractEvents = listOf(
            EventToMessage(KafkaEvents.EventWorkExtractCreated,
                createMessage(eventId = "ad93a41a-db08-436b-84e4-55adb4752f38", derivedFromEventId = "af7f2519-0f1d-4679-82bd-0314d1b97b68")),
            EventToMessage(KafkaEvents.EventWorkExtractCreated,
                createMessage(eventId = "cfeee961-69c1-4eed-8ec5-82ebca01c9e1", derivedFromEventId = "af7f2519-0f1d-4679-82bd-0314d1b97b68")),
            EventToMessage(KafkaEvents.EventWorkExtractCreated,
                createMessage(eventId = "64625872-bbfe-4604-85cd-02f58e904267", derivedFromEventId = "af7f2519-0f1d-4679-82bd-0314d1b97b68")),
            EventToMessage(KafkaEvents.EventWorkExtractCreated,
                createMessage(eventId = "0ab96b32-45a5-4517-b0c0-c03d48145340", derivedFromEventId = "af7f2519-0f1d-4679-82bd-0314d1b97b68")),
            EventToMessage(KafkaEvents.EventWorkExtractCreated,
                createMessage(eventId = "cabd9038-307f-48e4-ac99-88232b1a817c", derivedFromEventId = "af7f2519-0f1d-4679-82bd-0314d1b97b68")),
            EventToMessage(KafkaEvents.EventWorkExtractCreated,
                createMessage(eventId = "10c0fd42-b5be-42b2-a27b-12ecccc51635", derivedFromEventId = "af7f2519-0f1d-4679-82bd-0314d1b97b68")),
            EventToMessage(KafkaEvents.EventWorkExtractCreated,
                createMessage(eventId = "b69fb306-e390-4a9e-8d11-89d0688dff16", derivedFromEventId = "af7f2519-0f1d-4679-82bd-0314d1b97b68")),
        ).onEach { entry ->
            run {
                eventManager.setEvent(entry.event, entry.message)
                convertEvents.add(EventToMessage(KafkaEvents.EventWorkConvertCreated,
                    createMessage(derivedFromEventId = entry.message.eventId)))
            }
        }

        val simpleCascade = eventManager.getEventsWith(defaultReferenceId)
        assertThat(simpleCascade.size).isEqualTo(keepStack.size+extractEvents.size)

        assertThat(convertEvents.size).isEqualTo(extractEvents.size)
        convertEvents.forEach {
            eventManager.setEvent(it.event, it.message)
        }

        val result = eventManager.getEventsWith(defaultReferenceId)

        assertThat(result.size).isEqualTo(keepStack.size+extractEvents.size+convertEvents.size)

        withTransaction(dataSource) {
            events.deleteAll()
        }
    }


    @Test
    fun testSomeAreSingleSomeAreNot() {
        val startEventPayload = createMessage()
        val keepStack = listOf(
            EventToMessage(KafkaEvents.EventMediaProcessStarted, startEventPayload),
            EventToMessage(KafkaEvents.EventMediaReadStreamPerformed,
                createMessage(eventId = "48c72454-6c7b-406b-b598-fc0a961dabde", derivedFromEventId = startEventPayload.eventId)),
            EventToMessage(KafkaEvents.EventMediaParseStreamPerformed,
                createMessage(eventId = "1d8d995d-a7e4-4d6e-a501-fe82f521cf72", derivedFromEventId ="48c72454-6c7b-406b-b598-fc0a961dabde")),
            EventToMessage(KafkaEvents.EventMediaReadBaseInfoPerformed,
                createMessage(eventId = "f6cae204-7c8e-4003-b598-f7b4e566d03e", derivedFromEventId ="1d8d995d-a7e4-4d6e-a501-fe82f521cf72")),
            EventToMessage(KafkaEvents.EventMediaMetadataSearchPerformed,
                createMessage(eventId = "cbb1e871-e9a5-496d-a655-db719ac4903c", derivedFromEventId = "f6cae204-7c8e-4003-b598-f7b4e566d03e")),
            EventToMessage(KafkaEvents.EventMediaReadOutCover,
                createMessage(eventId = "98a39721-41ff-4d79-905e-ced260478524", derivedFromEventId = "cbb1e871-e9a5-496d-a655-db719ac4903c")),
            EventToMessage(KafkaEvents.EventMediaReadOutNameAndType,
                createMessage(eventId = "3f376b72-f55a-4dd7-af87-fb1755ba4ad9", derivedFromEventId = "cbb1e871-e9a5-496d-a655-db719ac4903c")),
            EventToMessage(KafkaEvents.EventMediaParameterEncodeCreated,
                createMessage(eventId = "9e8f2e04-4950-437f-a203-cfd566203078", derivedFromEventId = "3f376b72-f55a-4dd7-af87-fb1755ba4ad9")),
            EventToMessage(KafkaEvents.EventMediaParameterExtractCreated,
                createMessage(eventId = "af7f2519-0f1d-4679-82bd-0314d1b97b68", derivedFromEventId = "3f376b72-f55a-4dd7-af87-fb1755ba4ad9")),
            EventToMessage(KafkaEvents.EventWorkExtractCreated,
                createMessage(eventId = "ad93a41a-db08-436b-84e4-55adb4752f38", derivedFromEventId = "af7f2519-0f1d-4679-82bd-0314d1b97b68")),
        ).onEach { entry -> eventManager.setEvent(entry.event, entry.message)  }

        val newEvents = listOf(
            EventToMessage(KafkaEvents.EventWorkExtractCreated,
                createMessage(eventId = "cfeee961-69c1-4eed-8ec5-82ebca01c9e1", derivedFromEventId = "af7f2519-0f1d-4679-82bd-0314d1b97b68")),
            EventToMessage(KafkaEvents.EventWorkExtractCreated,
                createMessage(eventId = "64625872-bbfe-4604-85cd-02f58e904267", derivedFromEventId = "af7f2519-0f1d-4679-82bd-0314d1b97b68")),
            EventToMessage(KafkaEvents.EventWorkExtractCreated,
                createMessage(eventId = "0ab96b32-45a5-4517-b0c0-c03d48145340", derivedFromEventId = "af7f2519-0f1d-4679-82bd-0314d1b97b68")),
            EventToMessage(KafkaEvents.EventWorkExtractCreated,
                createMessage(eventId = "cabd9038-307f-48e4-ac99-88232b1a817c", derivedFromEventId = "af7f2519-0f1d-4679-82bd-0314d1b97b68")),
            EventToMessage(KafkaEvents.EventMediaProcessCompleted,
                createMessage(eventId = "10c0fd42-b5be-42b2-a27b-12ecccc51635", derivedFromEventId = "cabd9038-307f-48e4-ac99-88232b1a817c")),
            EventToMessage(KafkaEvents.EventMediaProcessCompleted,
                createMessage(eventId = "3519af2e-0767-4dbb-b0c5-f19cb926900d", derivedFromEventId = "cabd9038-307f-48e4-ac99-88232b1a817c")),

            EventToMessage(KafkaEvents.EventCollectAndStore,
                createMessage(eventId = "b69fb306-e390-4a9e-8d11-89d0688dff16", derivedFromEventId = "3519af2e-0767-4dbb-b0c5-f19cb926900d")),
            EventToMessage(KafkaEvents.EventCollectAndStore,
                createMessage(eventId = "4e6d3a6a-ab89-4627-9158-3c3f92ff7b4c", derivedFromEventId = "3519af2e-0767-4dbb-b0c5-f19cb926900d")),
            EventToMessage(KafkaEvents.EventCollectAndStore,
                createMessage(eventId = "4e6d3a6a-ab89-4627-9158-3c3f92ff7b4c", derivedFromEventId = "3519af2e-0767-4dbb-b0c5-f19cb926900d")),
        ).onEach { entry -> eventManager.setEvent(entry.event, entry.message)  }

        val result = eventManager.getEventsWith(defaultReferenceId)
        val singles = result.filter { it.event != KafkaEvents.EventWorkExtractCreated }
        singles.forEach {
            val instancesOfMe = singles.filter { sit -> it.event == sit.event }
            assertThat(instancesOfMe).hasSize(1)
        }
        assertThat(result.filter { it.event == KafkaEvents.EventCollectAndStore }).hasSize(1)



        withTransaction(dataSource) {
            events.deleteAll()
        }
    }

    @Test
    fun testDerivedOrphanNotInserted() {
        val startEvent = EventToMessage(KafkaEvents.EventMediaProcessStarted, createMessage()).also {
            eventManager.setEvent(it.event, it.message)
        }
        val result = eventManager.setEvent(KafkaEvents.EventMediaReadStreamPerformed,
            createMessage(derivedFromEventId = UUID.randomUUID().toString()))
        assertThat(result).isFalse()
    }

    data class EventToMessage(val event: KafkaEvents, val message: Message<*>)

    private fun createMessage(referenceId: String = defaultReferenceId, eventId: String = UUID.randomUUID().toString(), derivedFromEventId: String? = null): Message<SimpleMessageData>{
        return Message<SimpleMessageData>(
            referenceId = referenceId,
            eventId = eventId,
            data = SimpleMessageData(
                status = Status.COMPLETED,
                message = "Potato",
                derivedFromEventId = derivedFromEventId
            )
        )
    }

}