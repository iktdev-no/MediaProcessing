package no.iktdev.mediaprocessing.coordinator.services.scheduled

import mu.KotlinLogging
import no.iktdev.eventi.models.Event
import no.iktdev.eventi.models.store.PersistedEvent
import no.iktdev.eventi.serialization.ZDS.toEvent
import no.iktdev.files.IFile
import no.iktdev.mediaprocessing.coordinator.Preference
import no.iktdev.mediaprocessing.coordinator.services.FileInfoService
import no.iktdev.mediaprocessing.shared.common.UtcNow
import no.iktdev.mediaprocessing.shared.common.configs.MediaPaths
import no.iktdev.mediaprocessing.shared.common.effectivePersisted
import no.iktdev.mediaprocessing.shared.common.event_task_contract.events.CompletedCacheDeletedEvent
import no.iktdev.mediaprocessing.shared.common.event_task_contract.events.CompletedEvent
import no.iktdev.mediaprocessing.shared.common.event_task_contract.events.CompletedInputDeletedEvent
import no.iktdev.mediaprocessing.shared.common.event_task_contract.events.ProcesserEncodeResultEvent
import no.iktdev.mediaprocessing.shared.common.event_task_contract.events.ProcesserExtractResultEvent
import no.iktdev.mediaprocessing.shared.common.event_task_contract.events.StartFlow
import no.iktdev.mediaprocessing.shared.common.event_task_contract.events.StartProcessingEvent
import no.iktdev.mediaprocessing.shared.common.getInstanceOf
import no.iktdev.mediaprocessing.shared.common.getInstancesOf
import no.iktdev.mediaprocessing.shared.common.getName
import no.iktdev.mediaprocessing.shared.database.stores.EventStore
import no.iktdev.mediaprocessing.transferModel.coordinatorUi.preference.coordinator.FlowTypes
import no.iktdev.mediaprocessing.transferModel.coordinatorUi.preference.coordinator.toDuration
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import java.time.Duration
import java.time.Instant

@Service
class EventProducedDataCleanupService(
    private val mediaPaths: MediaPaths,
    private val preference: Preference,
    private val fileInfoService: FileInfoService
) {
    private val log = KotlinLogging.logger {}

    //region Cache Clear

    @Scheduled(fixedDelay = 30 * 60 * 1000)
    fun startCacheCleanup() {
        val cacheRetention = preference.getCleanupPreference().cacheCleanupPreference
        log.info { "Cache Cleanup settings: enabled=${cacheRetention.enabled}, retention=${cacheRetention.retention}" }
        if (!cacheRetention.enabled) {
            return
        }
        log.info { "Starting cache cleanup..." }
        val retentionDuration = cacheRetention.retention.toDuration()
        val events = EventStore.getEventSequenceWithLastEventAs(CompletedEvent::class.getName())
            .map { it.effectivePersisted() }

        val sequencesStartTypeTargeting = when (cacheRetention.flows) {
            FlowTypes.Any -> listOf(StartFlow.Auto, StartFlow.Manual)
            FlowTypes.Auto -> listOf(StartFlow.Auto)
            FlowTypes.Manual -> listOf(StartFlow.Manual)
        }

        val eligibleSequences = events.filter { it ->
            val started = it.find { it.event == StartProcessingEvent::class.getName() }?.toEvent() as? StartProcessingEvent
            started != null && sequencesStartTypeTargeting.contains(started.data.flow)
        }

        log.info { "Cache cleanup summary: Found ${eligibleSequences.size} sequences matching flow criteria." }
        if (eligibleSequences.isEmpty()) {
            log.info("No events were ready to have their cache cleared")
            return
        }
        performCacheCleanup(retentionDuration, eligibleSequences)
    }

    fun performCacheCleanup(
        retentionDuration: Duration,
        eventSequences: List<List<PersistedEvent>>
    ) {
        val now = UtcNow()

        val ready = eventSequences.filter { seq ->
            val completed = seq.find { it.event == CompletedEvent::class.getName() }?.toEvent()
                ?: return@filter false

            val created = completed.metadata.created
            val isOldEnough = Duration.between(created, now) >= retentionDuration

            // --- LAGT TIL: Debug om sekvensen i det hele tatt er gammel nok ---
            if (!isOldEnough) {
                log.debug { "Sequence ${completed.referenceId} is not old enough for cache cleanup. Created: $created" }
            }

            isOldEnough
        }

        log.info { "Cache cleanup: Found ${ready.size} sequences ready for physical deletion." }

        ready.forEach { seq ->
            val events = seq.mapNotNull { it.toEvent() }
            val freed = clearCachedDataForSequence(events)

            if (freed > 0) {
                val completed = events.find { it is CompletedEvent }!!
                EventStore.persist(CompletedCacheDeletedEvent().derivedOf(completed))
                log.info("Deleted ${freed.humanReadable()} from cache for referenceId ${completed.referenceId}")
            } else {
                // --- LAGT TIL: Varsling når sekvensen ble valgt, men ingenting ble slettet ---
                val refId = events.find { it is CompletedEvent }?.let { (it as CompletedEvent).referenceId }
                log.warn { "Sequence $refId was selected, but no files were cleared. Check file paths/structure." }
            }
        }
    }

    fun clearCachedDataForSequence(events: List<Event>): Long {
        val encode = events.getInstanceOf<ProcesserEncodeResultEvent>()
        val extract = events.getInstanceOf<ProcesserExtractResultEvent>()

        val outputFile = encode?.data?.cachedOutputFile
            ?: extract?.data?.cachedOutputFile
            ?: run {
                log.debug { "No cachedOutputFile found in events" }
                return 0L
            }

        val output = IFile(outputFile)
        val intermediateRoot = IFile(mediaPaths.intermediate)

        log.debug { "Checking cleanup for path: ${output.absolutePath}" }
        log.debug { "Intermediate root is: ${intermediateRoot.absolutePath}" }

        val folder = output.parentFile.isChildOfAndOneLevelBeneath(intermediateRoot)
            ?: run {
                log.debug { "Path did not match intermediate root requirement. Parent: ${output.parentFile.absolutePath}" }
                return 0L
            }

        if (folder.exists() && folder.isDirectory()) {
            log.info("Deleting ${folder.name} and its contents")
            val size = folder.sizeRecursive()
            folder.deleteRecursively()
            return size
        } else {
            log.debug { "Folder does not exist or is not a directory: ${folder.absolutePath}" }
        }
        return 0L
    }
//endregion


    @Scheduled(cron = "0 0 0 * * *")
    fun cleanupDailyAtMidnight() {
        val pref = preference.getCleanupPreference().inputCleanupPreference
        log.info { "Input Cleanup settings: enabled=${pref.enabled}, retention=${pref.retention}" }
        if (!pref.enabled) {
            log.info { "Starting input file cleanup..." }
            return
        }

        val retention = pref.retention.toDuration()
        val preserved = fileInfoService.getPreservedInputFiles()
            .mapTo(HashSet()) { it.fileUri }

        val sequences = loadEligibleSequencesReadyForDeletion()

        // 1. Ekstraherer til Map<IFile, List<Event>>
        val filesWithEvents = extractInputFiles(sequences)

        // 2. Bruker den dedikerte filtreringsfunksjonen (tilpasset Map)
        val candidates = filterFilesForCleanup(
            filesWithEvents = filesWithEvents,
            preserved = preserved,
            retention = retention
        )
        log.info { "Input Cleanup summary: Found ${filesWithEvents.size} total candidates, ${candidates.size} marked for deletion, ${filesWithEvents.size - candidates.size} ignored." }
        if (candidates.isEmpty()) {
            log.info("No input files eligible for cleanup")
            return
        }

        // 3. Sletter kandidatene
        deleteFiles(candidates)
    }

    internal fun loadEligibleSequencesReadyForDeletion(): List<List<Event>> {
        val classNames = listOf(CompletedEvent::class.getName(), CompletedCacheDeletedEvent::class.getName())

        return classNames.flatMap { className ->
            EventStore.getEventSequenceWithLastEventAs(className)
                .flatMap { it.effectivePersisted().mapNotNull { e -> e.toEvent() } }
        }
            .groupBy { it.referenceId }
            .values
            .map { seq -> seq.sortedBy { it.metadata.created } }
    }

    internal fun extractInputFiles(sequences: List<List<Event>>): Map<IFile, List<Event>> {
        val fileWithLast = sequences.mapNotNull { seq ->
            val started = seq.filterIsInstance<StartProcessingEvent>().firstOrNull() ?: return@mapNotNull null
            val lastEvent = seq.lastOrNull { it is CompletedEvent || it is CompletedCacheDeletedEvent } ?: return@mapNotNull null

            IFile(started.data.fileUri) to lastEvent
        }

        return fileWithLast.groupBy({ it.first }, { it.second })
    }

    internal fun deleteFiles(candidates: Map<IFile, List<Event>>) {
        val existingFiles = candidates.filter { it.key.exists() }

        existingFiles.forEach { (file, lastEvents) ->
            log.info("Deleting old input file: ${file.path}")
            file.delete()

            lastEvents.forEach { lastEvent ->
                val deleteEvent = CompletedInputDeletedEvent()
                    .derivedOf(lastEvent)
                EventStore.persist(deleteEvent)
                log.info("Published CompletedInputDeletedEvent for sequence ending with: ${lastEvent::class.simpleName}")
            }
        }
        val missingFiles = (candidates - existingFiles.keys).keys
        if (missingFiles.isNotEmpty()) {
            log.warn { "Could not find the following files for cleanup: \n${missingFiles.joinToString("\n") { it.path }}" }
        }
    }

    internal fun filterFilesForCleanup(
        filesWithEvents: Map<IFile, List<Event>>,
        preserved: Set<String>,
        retention: Duration
    ): Map<IFile, List<Event>> {
        val now = Instant.now()

        return filesWithEvents.filterKeys { file ->
            // 1. Standard sjekk for eksistens og om filen er eksplisitt bevart
            if (!file.exists() || file.path in preserved) {
                return@filterKeys false
            }

            // 2. Hent ut alle events knyttet til akkurat denne filen
            val relatedEvents = filesWithEvents[file] ?: return@filterKeys false

            // 3. Finn ut om NOEN av eventene er for ferske (innenfor retention)
            val hasRecentActivity = relatedEvents.any { event ->
                val eventCreated = event.metadata.created // Antar dette er et Instant (eller kan mappes til det)
                Duration.between(eventCreated, now) <= retention
            }

            // 4. Vi sletter BARE hvis filen IKKE har fersk aktivitet,
            // og den fysiske filen også er eldre enn retention (just in case)
            val filePhysicalOldEnough = Duration.between(Instant.ofEpochMilli(file.lastModified()), now) > retention

            !hasRecentActivity && filePhysicalOldEnough
        }
    }



    fun IFile.isChildOfAndOneLevelBeneath(file: IFile): IFile? {
        return if (this.parentFile.absolutePath == file.absolutePath) this else null
    }



    fun IFile.sizeRecursive(): Long =
        if (!exists()) 0L
        else if (isFile()) length()
        else listFiles().sumOf { it.sizeRecursive() }

    fun Long.humanReadable(): String {
        if (this < 1024) return "$this B"
        val kb = this / 1024.0
        if (kb < 1024) return "%.2f KB".format(kb)
        val mb = kb / 1024.0
        if (mb < 1024) return "%.2f MB".format(mb)
        val gb = mb / 1024.0
        return "%.2f GB".format(gb)
    }


}