package no.iktdev.mediaprocessing.coordinator.services.scheduled

import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.spyk
import no.iktdev.eventi.models.store.TaskStatus
import no.iktdev.eventi.serialization.ZDS.toEvent
import no.iktdev.files.IFile
import no.iktdev.mediaprocessing.TestBase
import no.iktdev.mediaprocessing.coordinator.services.FileInfoService
import no.iktdev.mediaprocessing.shared.common.effectivePersisted
import no.iktdev.mediaprocessing.shared.common.event_task_contract.events.CompletedCacheDeletedEvent
import no.iktdev.mediaprocessing.shared.common.event_task_contract.events.CompletedEvent
import no.iktdev.mediaprocessing.shared.common.event_task_contract.events.ProcesserEncodeResultEvent
import no.iktdev.mediaprocessing.shared.common.event_task_contract.events.StartFlow
import no.iktdev.mediaprocessing.shared.common.getName
import no.iktdev.mediaprocessing.shared.database.stores.EventStore
import no.iktdev.mediaprocessing.transferModel.coordinatorUi.preference.coordinator.FlowTypes
import no.iktdev.mediaprocessing.transferModel.coordinatorUi.preference.coordinator.Retention
import no.iktdev.mediaprocessing.transferModel.coordinatorUi.preference.coordinator.RetentionUnit
import no.iktdev.mediaprocessing.withCreatedAt
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.time.Duration
import java.time.temporal.ChronoUnit
import io.mockk.*
import no.iktdev.eventi.models.Event
import no.iktdev.mediaprocessing.ffmpeg.util.UtcNow
import no.iktdev.mediaprocessing.shared.common.event_task_contract.events.OperationType
import no.iktdev.mediaprocessing.shared.common.event_task_contract.events.StartData
import no.iktdev.mediaprocessing.shared.common.event_task_contract.events.StartProcessingEvent

class EventProducedDataCleanupServicePerformCleanupTest : TestBase() {

    private val fileInfoService = mockk<FileInfoService>()


    @BeforeEach
    override fun setup() {
        super.setup()

        // FIX: stub mediaPaths used by service()
        every { coordinatorEnv.media } returns mockk {
            every { intermediate } returns "/tmp/intermediate"
            every { scratch } returns "/tmp/scratch"
        }

        mockkObject(EventStore)
        every { EventStore.persist(any()) } answers {
            eventStore.persist(firstArg())
        }

        every { EventStore.getEventSequenceWithLastEventAs(any()) } answers {
            eventStore.getEventSequenceWithLastEventAs(firstArg())
        }



    }

    private fun service() = EventProducedDataCleanupService(
        mediaPaths = coordinatorEnv.media,
        preference = preference,
        fileInfoService = fileInfoService,
    )


    @DisplayName(
        """
    Gitt inputCleanupPreference.enabled = false
    Når cleanupDailyAtMidnight kjøres
    Så:
        deleteFiles skal aldri kalles
    """
    )
    @Test
    fun cleanupDailyAtMidnight_never_deletes_when_disabled() {
        every { preference.getCleanupPreference().inputCleanupPreference.enabled } returns false

        val spy = spyk(service())

        // Selv om disse ville gitt kandidater, skal de aldri brukes
        every { spy.loadEligibleSequencesReadyForDeletion() } returns listOf(mockk())
        every { spy.extractInputFiles(any()) } returns mapOf(IFile("/tmp/inputFiles/fake.bin") to listOf(StartProcessingEvent(data = StartData(
            operation = setOf(OperationType.Encode),
            fileUri = "/tmp/inputFiles/fake.bin",
            flow = StartFlow.Auto
        )).newReferenceId()))
        every { spy.deleteFiles(any()) } just Runs

        spy.cleanupDailyAtMidnight()

        verify(exactly = 0) { spy.deleteFiles(any()) }
        verify(exactly = 0) { spy.extractInputFiles(any()) }
        verify(exactly = 0) { spy.loadEligibleSequencesReadyForDeletion() }
    }


    @DisplayName(
        """
    Gitt cacheCleanupPreference.enabled = false
    Når startCacheCleanup kjøres
    Så:
        performCacheCleanup skal aldri kalles
    """
    )
    @Test
    fun startCacheCleanup_never_runs_cleanup_when_disabled() {
        every { preference.getCleanupPreference().cacheCleanupPreference.enabled } returns false

        val spy = spyk(service())

        every { spy.performCacheCleanup(any(), any()) } just Runs

        spy.startCacheCleanup()

        verify(exactly = 0) { spy.performCacheCleanup(any(), any()) }
        verify(exactly = 0) { EventStore.getEventSequenceWithLastEventAs(any()) }
    }




    @Test
    @DisplayName(
        """
        Gitt en CompletedEvent eldre enn retention
        Når performCacheCleanup kjøres
        Så:
            Skal mappen slettes
            Og størrelsen returneres
            Og CompletedCacheDeletedEvent persisteres
        """
    )
    fun cleanupDeletesFolderAndReturnsSize() {
        every { preference.getCleanupPreference().cacheCleanupPreference.enabled } returns true

        val retention = Duration.ofHours(1)
        val started = defaultStartEvent(flow = StartFlow.Auto)

        // Bruk ABSOLUTT path
        val intermediate = IFile("/tmp/intermediate")
        val folder = intermediate.resolve("abc123")
        folder.mkdirs()

        val file1 = folder.resolve("a.bin")
        val file2 = folder.resolve("b.bin")

        file1.writeBytes(ByteArray(1024))
        file2.writeBytes(ByteArray(2048))

        ProcesserEncodeResultEvent(
            data = ProcesserEncodeResultEvent.EncodeResult(
                cachedOutputFile = file1.absolutePath
            ),
            status = TaskStatus.Completed
        ).derivedOf(started).addToHistory()

        CompletedEvent()
            .derivedOf(started)
            .withCreatedAt(UtcNow().minus(2, ChronoUnit.HOURS))
            .addToHistory()

        val sequences = eventStore.getEventSequenceWithLastEventAs(CompletedEvent::class.getName())

        service().performCacheCleanup(retention, sequences)

        assertFalse(folder.exists(), "Folder should be deleted")

        val deletedEvents =
            eventStore.getEventSequenceWithLastEventAs(CompletedCacheDeletedEvent::class.getName())
        assertTrue(deletedEvents.isNotEmpty(), "CompletedCacheDeletedEvent should be persisted")
    }


    @Test
    @DisplayName(
        """
        Gitt en CompletedEvent som ikke er gammel nok
        Når performCacheCleanup kjøres
        Så:
            Skal ingen mapper slettes
            Og ingen CompletedCacheDeletedEvent persisteres
        """
    )
    fun cleanupSkipsTooYoungSequences() {
        // Arrange
        every { preference.getCleanupPreference().cacheCleanupPreference.enabled } returns true

        val retention = Duration.ofHours(1)
        val started = defaultStartEvent(flow = StartFlow.Auto)


        val intermediate = IFile("./tmp/intermediate")
        val folder = intermediate.resolve("young123")
        folder.mkdirs()

        val file = folder.resolve("x.bin")
        file.writeBytes(ByteArray(500))

        ProcesserEncodeResultEvent(
            data = ProcesserEncodeResultEvent.EncodeResult(
                cachedOutputFile = file.absolutePath
            ),
            status = TaskStatus.Completed
        ).derivedOf(started).addToHistory()

        CompletedEvent()
            .derivedOf(started)
            .withCreatedAt(UtcNow().minus(10, ChronoUnit.MINUTES)) // too young
            .addToHistory()

        val sequences = eventStore.getEventSequenceWithLastEventAs(CompletedEvent::class.getName())

        // Act
        service().performCacheCleanup(retention, sequences)

        // Assert
        assertTrue(folder.exists(), "Folder should NOT be deleted")

        val deletedEvents =
            eventStore.getEventSequenceWithLastEventAs(CompletedCacheDeletedEvent::class.getName())
        assertTrue(deletedEvents.isEmpty(), "No CompletedCacheDeletedEvent should be persisted")
    }

    @Test
    @DisplayName(
        """
        Gitt en sekvens uten cachedOutputFile eller extractFile
        Når performCacheCleanup kjøres
        Så:
            Skal ingenting slettes
        """
    )
    fun cleanupSkipsSequencesWithoutFiles() {
        // Arrange
        val started = defaultStartEvent(flow = StartFlow.Auto)

        every { preference.getCleanupPreference().cacheCleanupPreference.enabled } returns true

        val retention = Duration.ofHours(1)

        CompletedEvent()
            .derivedOf(started)
            .withCreatedAt(UtcNow().minus(2, ChronoUnit.HOURS))
            .addToHistory()

        val sequences = eventStore.getEventSequenceWithLastEventAs(CompletedEvent::class.getName())

        // Act
        service().performCacheCleanup(retention, sequences)

        // Assert
        val deletedEvents =
            eventStore.getEventSequenceWithLastEventAs(CompletedCacheDeletedEvent::class.getName())
        assertTrue(deletedEvents.isEmpty(), "No cleanup should occur")
    }


    @Test
    @DisplayName(
        """
        Gitt FlowTypes.Auto
        Når startCacheCleanup kjøres
        Så:
            Skal kun Auto-sekvenser slettes
        """
    )
    fun startCleanupDeletesOnlyAuto() {
        every { preference.getCleanupPreference().cacheCleanupPreference.enabled } returns true
        every { preference.getCleanupPreference().cacheCleanupPreference.retention } returns Retention(
            1,
            RetentionUnit.Hours
        )
        every { preference.getCleanupPreference().cacheCleanupPreference.flows } returns FlowTypes.Auto

        // Auto
        val autoStart = defaultStartEvent(flow = StartFlow.Auto)
            .addToHistory()
        val autoFolder = IFile("/tmp/intermediate/auto123").apply { mkdirs() }
        val autoFile = autoFolder.resolve("a.bin").apply { writeBytes(ByteArray(10)) }

        ProcesserEncodeResultEvent(
            data = ProcesserEncodeResultEvent.EncodeResult(autoFile.absolutePath),
            status = TaskStatus.Completed
        ).derivedOf(autoStart).addToHistory()

        CompletedEvent()
            .derivedOf(autoStart)
            .withCreatedAt(UtcNow().minus(2, ChronoUnit.HOURS))
            .addToHistory()

        // Manual
        val manualStart = defaultStartEvent(flow = StartFlow.Manual)
            .addToHistory()

        val manualFolder = IFile("/tmp/intermediate/manual123").apply { mkdirs() }
        val manualFile = manualFolder.resolve("b.bin").apply { writeBytes(ByteArray(10)) }

        ProcesserEncodeResultEvent(
            data = ProcesserEncodeResultEvent.EncodeResult(manualFile.absolutePath),
            status = TaskStatus.Completed
        ).derivedOf(manualStart).addToHistory()

        CompletedEvent()
            .derivedOf(manualStart)
            .withCreatedAt(UtcNow().minus(2, ChronoUnit.HOURS))
            .addToHistory()

        // Act
        service().startCacheCleanup()

        // Assert
        assertFalse(autoFolder.exists(), "Auto-folder should be deleted")
        assertTrue(manualFolder.exists(), "Manual-folder should NOT be deleted")

        val deletedEvents =
            eventStore.getEventSequenceWithLastEventAs(CompletedCacheDeletedEvent::class.getName())

        assertTrue(deletedEvents.size == 1, "Only Auto should produce a deletion event")
    }

    @Test
    @DisplayName(
        """
        Gitt FlowTypes.Manual
        Når startCacheCleanup kjøres
        Så:
            Skal kun Manual-sekvenser slettes
        """
    )
    fun startCleanupDeletesOnlyManual() {
        every { preference.getCleanupPreference().cacheCleanupPreference.enabled } returns true
        every { preference.getCleanupPreference().cacheCleanupPreference.retention } returns Retention(1, RetentionUnit.Hours)
        every { preference.getCleanupPreference().cacheCleanupPreference.flows } returns FlowTypes.Manual

        // Auto
        val autoStart = defaultStartEvent(flow = StartFlow.Auto)
            .addToHistory()
        val autoFolder = IFile("/tmp/intermediate/auto123").apply { mkdirs() }
        val autoFile = autoFolder.resolve("a.bin").apply { writeBytes(ByteArray(10)) }

        ProcesserEncodeResultEvent(
            data = ProcesserEncodeResultEvent.EncodeResult(autoFile.absolutePath),
            status = TaskStatus.Completed
        ).derivedOf(autoStart).addToHistory()

        CompletedEvent()
            .derivedOf(autoStart)
            .withCreatedAt(UtcNow().minus(2, ChronoUnit.HOURS))
            .addToHistory()

        // Manual
        val manualStart = defaultStartEvent(flow = StartFlow.Manual)
            .addToHistory()
        val manualFolder = IFile("/tmp/intermediate/manual123").apply { mkdirs() }
        val manualFile = manualFolder.resolve("b.bin").apply { writeBytes(ByteArray(10)) }

        ProcesserEncodeResultEvent(
            data = ProcesserEncodeResultEvent.EncodeResult(manualFile.absolutePath),
            status = TaskStatus.Completed
        ).derivedOf(manualStart).addToHistory()

        CompletedEvent()
            .derivedOf(manualStart)
            .withCreatedAt(UtcNow().minus(2, ChronoUnit.HOURS))
            .addToHistory()

        // Act
        service().startCacheCleanup()

        // Assert
        assertTrue(autoFolder.exists(), "Auto-folder should NOT be deleted")
        assertFalse(manualFolder.exists(), "Manual-folder should be deleted")

        val deletedEvents =
            eventStore.getEventSequenceWithLastEventAs(CompletedCacheDeletedEvent::class.getName())

        assertTrue(deletedEvents.size == 1, "Only Manual should produce a deletion event")
    }

    @Test
    @DisplayName(
        """
        Gitt FlowTypes.Any
        Når startCacheCleanup kjøres
        Så:
            Skal både Auto og Manual slettes
        """
    )
    fun startCleanupDeletesBoth() {
        every { preference.getCleanupPreference().cacheCleanupPreference.enabled } returns true
        every { preference.getCleanupPreference().cacheCleanupPreference.retention } returns Retention(1, RetentionUnit.Hours)
        every { preference.getCleanupPreference().cacheCleanupPreference.flows } returns FlowTypes.Any

        // Auto
        val autoStart = defaultStartEvent(flow = StartFlow.Auto)
            .addToHistory()
        val autoFolder = IFile("/tmp/intermediate/auto123").apply { mkdirs() }
        val autoFile = autoFolder.resolve("a.bin").apply { writeBytes(ByteArray(10)) }

        ProcesserEncodeResultEvent(
            data = ProcesserEncodeResultEvent.EncodeResult(autoFile.absolutePath),
            status = TaskStatus.Completed
        ).derivedOf(autoStart).addToHistory()

        CompletedEvent()
            .derivedOf(autoStart)
            .withCreatedAt(UtcNow().minus(2, ChronoUnit.HOURS))
            .addToHistory()

        // Manual
        val manualStart = defaultStartEvent(flow = StartFlow.Manual)
            .addToHistory()
        val manualFolder = IFile("/tmp/intermediate/manual123").apply { mkdirs() }
        val manualFile = manualFolder.resolve("b.bin").apply { writeBytes(ByteArray(10)) }

        ProcesserEncodeResultEvent(
            data = ProcesserEncodeResultEvent.EncodeResult(manualFile.absolutePath),
            status = TaskStatus.Completed
        ).derivedOf(manualStart).addToHistory()

        CompletedEvent()
            .derivedOf(manualStart)
            .withCreatedAt(UtcNow().minus(2, ChronoUnit.HOURS))
            .addToHistory()

        // Act
        service().startCacheCleanup()

        // Assert
        assertFalse(autoFolder.exists(), "Auto-folder should be deleted")
        assertFalse(manualFolder.exists(), "Manual-folder should be deleted")

        val deletedEvents =
            eventStore.getEventSequenceWithLastEventAs(CompletedCacheDeletedEvent::class.getName())

        assertTrue(deletedEvents.size == 2, "Both flows should produce deletion events")
    }


    @Test
    @DisplayName(
        """
    Gitt ingen events
    Når cleanupDailyAtMidnight kjøres
    Så:
        Skal ikke slette noen filer
    """
    )
    fun cleanupDailyAtMidnight_skips_when_no_events() {
        every { preference.getCleanupPreference().inputCleanupPreference.enabled } returns true
        every { preference.getCleanupPreference().inputCleanupPreference.retention } returns Retention(1, RetentionUnit.Hours)

        val spy = spyk(service())

        // Viktig: stub fileInfoService ETTER spyk
        every { fileInfoService.getPreservedInputFiles() } returns emptyList()

        every { spy.loadEligibleSequencesReadyForDeletion() } returns emptyList()
        every { spy.extractInputFiles(any()) } returns emptyMap()
        every { spy.deleteFiles(any()) } just Runs

        spy.cleanupDailyAtMidnight()

        verify(exactly = 1) { spy.loadEligibleSequencesReadyForDeletion() }
        verify(exactly = 1) { spy.extractInputFiles(emptyList()) }
        verify(exactly = 0) { spy.deleteFiles(any()) }
    }


    @DisplayName(
        """
    Gitt preserved filer
    Når cleanupDailyAtMidnight kjøres
    Så:
        Skal ikke slette preserved filer
    """
    )
    @Test
    fun cleanupDailyAtMidnight_skips_preserved_files() {
        every { preference.getCleanupPreference().inputCleanupPreference.enabled } returns true
        every { preference.getCleanupPreference().inputCleanupPreference.retention } returns Retention(1, RetentionUnit.Hours)

        val started = defaultStartEvent(flow = StartFlow.Auto)

        val folder = IFile("/tmp/inputFiles").apply { mkdirs() }
        val file = folder.resolve("x.bin").apply { writeBytes(ByteArray(10)) }

        val startedEvent = StartProcessingEvent(data = StartData(
            operation = setOf(OperationType.Encode),
            fileUri = file.absolutePath,
            flow = StartFlow.Auto
        )).derivedOf(started).addToHistory()

        // Sørger for at dette eventet har en tid som matcher "old" (2 timer gammel, retention er 1 time)
        val completed = CompletedEvent()
            .derivedOf(started)
            .withCreatedAt(UtcNow().minus(2, ChronoUnit.HOURS))
            .addToHistory()

        val lastEvent = CompletedCacheDeletedEvent()
            .derivedOf(completed)
            .withCreatedAt(UtcNow().minus(2, ChronoUnit.HOURS)) // Sørg for at denne også har riktig tidspunkt for filteret
            .addToHistory()

        val spy = spyk(service())

        // 1. Vi lar den ekte 'loadEligibleSequencesReadyForDeletion()' hente fra historikken over,
        // eller mock det som en List<List<Event>> hvis du absolutt må isolere det:
        every { spy.loadEligibleSequencesReadyForDeletion() } returns listOf(
            listOf(startedEvent, completed, lastEvent)
        )

        // 2. Oppdatert til å returnere Map<IFile, List<Event>> i stedet for List<IFile>
        every { spy.extractInputFiles(any()) } returns mapOf(file to listOf(lastEvent))

        every { fileInfoService.getPreservedInputFiles() } returns listOf(
            FileInfoService.PreservedFile(fileUri = file.path, fileName = file.name)
        )

        // 3. Oppdatert 'every' og 'verify' til å forvente et Map
        every { spy.deleteFiles(any<Map<IFile, List<Event>>>()) } just Runs

        spy.cleanupDailyAtMidnight()

        assert(folder.exists())

        // Verifiserer at ingen filer ble sendt til sletting (siden filen er i preserved)
        verify(exactly = 0) { spy.deleteFiles(any<Map<IFile, List<Event>>>()) }
    }

    @DisplayName(
        """
    Gitt retention
    Når cleanupDailyAtMidnight kjøres
    Så:
        Skal kun gamle filer slettes
    """
    )
    @Test
    fun cleanupDailyAtMidnight_deletes_only_old_files() {
        every { preference.getCleanupPreference().inputCleanupPreference.enabled } returns true
        every { preference.getCleanupPreference().inputCleanupPreference.retention } returns Retention(1, RetentionUnit.Hours)

        val folder = IFile("/tmp/inputFiles").apply { mkdirs() }

        val oldFile = folder.resolve("old.bin").apply { writeBytes(ByteArray(10)) }.apply {
            setLastModified(UtcNow().minus(2, ChronoUnit.HOURS).toEpochMilli())
        }

        val newFile = folder.resolve("new.bin").apply { writeBytes(ByteArray(10)) }.apply {
            setLastModified(UtcNow().minus(10, ChronoUnit.MINUTES).toEpochMilli())
        }

        // Lag StartProcessingEvents som cleanupDailyAtMidnight forventer
        val oldStartEvent = StartProcessingEvent(
            data = StartData(
                operation = setOf(OperationType.Encode),
                fileUri = oldFile.absolutePath,
                flow = StartFlow.Auto
            )
        ).newReferenceId()

        val newStartEvent = StartProcessingEvent(
            data = StartData(
                operation = setOf(OperationType.Encode),
                fileUri = newFile.absolutePath,
                flow = StartFlow.Auto
            )
        ).newReferenceId()

        val oldCompleted = CompletedEvent()
            .derivedOf(oldStartEvent)
            .withCreatedAt(UtcNow().minus(2, ChronoUnit.HOURS))

        val newCompleted = CompletedEvent()
            .derivedOf(newStartEvent)
            .withCreatedAt(UtcNow().minus(10, ChronoUnit.MINUTES))

        // Husk å sette riktig tidspunkt i metadata på de siste eventene også,
        // siden filterFilesForCleanup sjekker eventets opprettelsestid
        val oldCacheDeleted = CompletedCacheDeletedEvent()
            .derivedOf(oldCompleted)
            .withCreatedAt(UtcNow().minus(2, ChronoUnit.HOURS))

        val newCacheDeleted = CompletedCacheDeletedEvent()
            .derivedOf(newCompleted)
            .withCreatedAt(UtcNow().minus(10, ChronoUnit.MINUTES))

        val spy = spyk(service())

        // 1. loadEligibleSequencesReadyForDeletion → returner liste av sekvenser (List<List<Event>>)
        every { spy.loadEligibleSequencesReadyForDeletion() } returns listOf(
            listOf(oldStartEvent, oldCompleted, oldCacheDeleted),
            listOf(newStartEvent, newCompleted, newCacheDeleted)
        )

        // 2. extractInputFiles → returner et Map<IFile, List<Event>>
        every { spy.extractInputFiles(any()) } returns mapOf(
            oldFile to listOf(oldCacheDeleted),
            newFile to listOf(newCacheDeleted)
        )

        every { fileInfoService.getPreservedInputFiles() } returns emptyList()

        // 3. deleteFiles → answers må hente ut keys fra Map-argumentet for å slette fysisk
        every { spy.deleteFiles(any<Map<IFile, List<Event>>>()) } answers {
            val candidatesMap = firstArg<Map<IFile, List<Event>>>()
            candidatesMap.keys.forEach { it.delete() }
        }

        spy.cleanupDailyAtMidnight()

        assertFalse(oldFile.exists(), "Old file should be deleted")
        assertTrue(newFile.exists(), "New file should NOT be deleted")

        // 4. Verifiser at kun oldFile var en del av slettekandidatene (sjekker keys i mappet)
        verify {
            spy.deleteFiles(match<Map<IFile, List<Event>>> { map ->
                map.containsKey(oldFile) && !map.containsKey(newFile)
            })
        }
    }

    @DisplayName(
        """
    Gitt en fil knyttet til to sekvenser, der en er gammel og en er innenfor retention
    Når cleanupDailyAtMidnight kjøres
    Så:
        Skal filen IKKE slettes fordi den nyeste hendelsen beskytter den
    """
    )
    @Test
    fun cleanupDailyAtMidnight_does_not_delete_file_with_mixed_age_events() {
        every { preference.getCleanupPreference().inputCleanupPreference.enabled } returns true
        every { preference.getCleanupPreference().inputCleanupPreference.retention } returns Retention(1, RetentionUnit.Hours)

        val folder = IFile("/tmp/inputFiles").apply { mkdirs() }
        val sharedFile = folder.resolve("shared.bin").apply { writeBytes(ByteArray(10)) }

        // 1. Gammel sekvens (Kvalifiserer isolert sett til sletting)
        val oldStartEvent = StartProcessingEvent(
            data = StartData(
                operation = setOf(OperationType.Encode),
                fileUri = sharedFile.absolutePath,
                flow = StartFlow.Auto
            )
        ).newReferenceId()

        val oldCompleted = CompletedEvent()
            .derivedOf(oldStartEvent)
            .withCreatedAt(UtcNow().minus(2, ChronoUnit.HOURS))

        val oldCacheDeleted = CompletedCacheDeletedEvent()
            .derivedOf(oldCompleted)
            .withCreatedAt(UtcNow().minus(2, ChronoUnit.HOURS))

        // 2. Ny sekvens (Innenfor retention - skal blokkere sletting)
        val newStartEvent = StartProcessingEvent(
            data = StartData(
                operation = setOf(OperationType.Encode),
                fileUri = sharedFile.absolutePath,
                flow = StartFlow.Auto
            )
        ).newReferenceId()

        val newCompleted = CompletedEvent()
            .derivedOf(newStartEvent)
            .withCreatedAt(UtcNow().minus(10, ChronoUnit.MINUTES))

        val newCacheDeleted = CompletedCacheDeletedEvent()
            .derivedOf(newCompleted)
            .withCreatedAt(UtcNow().minus(10, ChronoUnit.MINUTES))

        val spy = spyk(service())

        // loadEligibleSequencesReadyForDeletion returnerer begge sekvensene
        every { spy.loadEligibleSequencesReadyForDeletion() } returns listOf(
            listOf(oldStartEvent, oldCompleted, oldCacheDeleted),
            listOf(newStartEvent, newCompleted, newCacheDeleted)
        )

        // extractInputFiles mapper slettemeldingene fra BEGGE sekvensene til den samme filen
        every { spy.extractInputFiles(any()) } returns mapOf(
            sharedFile to listOf(oldCacheDeleted, newCacheDeleted)
        )

        every { fileInfoService.getPreservedInputFiles() } returns emptyList()

        every { spy.deleteFiles(any<Map<IFile, List<Event>>>()) } answers {
            val candidatesMap = firstArg<Map<IFile, List<Event>>>()
            candidatesMap.keys.forEach { it.delete() }
        }

        spy.cleanupDailyAtMidnight()

        // Siden den ene hendelsen er fersk (10 min), skal filen overleve sjekken
        assertTrue(sharedFile.exists(), "Shared file should NOT be deleted since one of its events is too new")

        // Verifiserer at deleteFiles aldri ble kalt med denne filen i mappet
        verify(exactly = 0) {
            spy.deleteFiles(match<Map<IFile, List<Event>>> { map ->
                map.containsKey(sharedFile)
            })
        }
    }

}
