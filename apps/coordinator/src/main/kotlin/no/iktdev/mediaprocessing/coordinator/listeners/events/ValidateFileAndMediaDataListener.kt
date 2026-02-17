package no.iktdev.mediaprocessing.coordinator.listeners.events

import com.google.gson.Gson
import no.iktdev.eventi.events.EventListener
import no.iktdev.eventi.models.Event
import no.iktdev.eventi.models.store.TaskStatus
import no.iktdev.mediaprocessing.ffmpeg.data.FFprobeFormat
import no.iktdev.mediaprocessing.shared.common.dto.files.IFile
import no.iktdev.mediaprocessing.shared.common.event_task_contract.events.CoordinatorReadStreamsResultEvent
import no.iktdev.mediaprocessing.shared.common.event_task_contract.events.MediaStreamParsedEvent
import no.iktdev.mediaprocessing.shared.common.event_task_contract.events.OperationType
import no.iktdev.mediaprocessing.shared.common.event_task_contract.events.StartProcessingEvent
import no.iktdev.mediaprocessing.shared.common.event_task_contract.events.ValidateFileAndMediaDataEvent
import no.iktdev.mediaprocessing.shared.common.requireQualifiedEntry
import org.springframework.stereotype.Component
import java.io.File
import java.net.URI

@Component
class ValidateFileAndMediaDataListener() : EventListener() {

    override fun onEvent(event: Event, history: List<Event>): Event? {
        val parsed = event.requireQualifiedEntry<MediaStreamParsedEvent>()

        val start = findStartEvent(history)
            ?: return reject("Missing StartProcessingEvent")

        val jsonEvent = findJsonEvent(history)
            ?: return reject("Missing CoordinatorReadStreamsResultEvent")

        val format = parseFormat(jsonEvent)
            ?: return reject("Failed to parse ffprobe format")

        val warnings = mutableListOf<String>()

        // 1) Funksjonell validering
        validateFunctional(start.data.operation, parsed)
            ?.let { return it.derivedOf(event) }

        // 2) Teknisk validering
        validateTechnical(format, warnings)
            ?.let { return it.derivedOf(event) }

        // 3) Filstørrelses-konsistens
        validateFileSizeConsistency(start.data.fileUri, format, warnings)
            ?.let { return it.derivedOf(event) }

        // 4) Stream-konsistens
        validateStreamConsistency(parsed, format, warnings)

        return ok(warnings).derivedOf(event)
    }

    // ---------------------------------------------------------
    //  Helpers: Event lookups
    // ---------------------------------------------------------

    private fun findStartEvent(history: List<Event>): StartProcessingEvent? =
        history.filterIsInstance<StartProcessingEvent>().firstOrNull()

    private fun findJsonEvent(history: List<Event>): CoordinatorReadStreamsResultEvent? =
        history.filterIsInstance<CoordinatorReadStreamsResultEvent>().firstOrNull()
            ?.takeIf { it.status == TaskStatus.Completed }

    // ---------------------------------------------------------
    //  Helpers: Parsing
    // ---------------------------------------------------------

    private fun parseFormat(jsonEvent: CoordinatorReadStreamsResultEvent): FFprobeFormat? {
        val json = jsonEvent.data ?: return null
        val formatObj = json.getAsJsonObject("format") ?: return null

        return try {
            val parsed = Gson().fromJson(formatObj, FFprobeFormat::class.java)

            // Kritiske felter må være tilstede
            if (parsed.duration == null ||
                parsed.size == null ||
                parsed.bit_rate == null ||
                parsed.nb_streams == null ||
                parsed.probe_score == null
            ) {
                return null
            }

            parsed
        } catch (e: Exception) {
            null
        }
    }


    // ---------------------------------------------------------
    // 1) Functional validation
    // ---------------------------------------------------------

    private fun validateFunctional(
        ops: Set<OperationType>,
        parsed: MediaStreamParsedEvent
    ): ValidateFileAndMediaDataEvent? {

        val video = parsed.data.videoStream.size
        val audio = parsed.data.audioStream.size
        val subs = parsed.data.subtitleStream.size

        val requiresEncode = OperationType.Encode in ops
        val requiresSubs = OperationType.ExtractSubtitles in ops

        if (requiresEncode) {
            if (video == 0) return reject("Encode requires at least one video stream")
            if (audio == 0) return reject("Encode requires at least one audio stream")
        }

        if (requiresSubs) {
            if (subs == 0) return reject("ExtractSubtitles requires at least one subtitle stream")
        }

        return null
    }

    // ---------------------------------------------------------
    // 2) Technical validation
    // ---------------------------------------------------------

    private fun validateTechnical(
        format: FFprobeFormat,
        warnings: MutableList<String>
    ): ValidateFileAndMediaDataEvent? {

        val duration = format.duration?.toDoubleOrNull() ?: return reject("Invalid duration: ${format.duration}")
        if (duration <= 0.0) return reject("Invalid duration: ${format.duration}")

        val size = format.size?.toLongOrNull() ?: return reject("Invalid file size: ${format.size}")
        if (size <= 0L) return reject("Invalid file size: ${format.size}")

        val bitrate = format.bit_rate?.toLongOrNull() ?: -1L
        if (bitrate <= 0L) warnings += "Bitrate reported as ${format.bit_rate}"

        val streams = format.nb_streams ?: return reject("No streams detected in container")
        if (streams <= 0) return reject("No streams detected in container")

        val probe = format.probe_score ?: return reject("Invalid probe_score")  // non-null by design
        when {
            probe < 25 ->
                return reject("Low probe_score ($probe) indicates corruption")

            probe < 50 ->
                warnings += "Low probe_score ($probe)"
        }

        return null
    }


    // ---------------------------------------------------------
    // 3) File size consistency validation
    // ---------------------------------------------------------

    private fun validateFileSizeConsistency(
        fileUri: String,
        format: FFprobeFormat,
        warnings: MutableList<String>
    ): ValidateFileAndMediaDataEvent? {

        val file = try {
            IFile(fileUri)
        } catch (e: Exception) {
            return reject("Invalid file path: $fileUri")
        }

        if (!file.exists()) {
            return reject("File does not exist at $fileUri")
        }

        val actualSize = file.length()
        if (actualSize <= 0L) return reject("Actual file size is zero")

        val ffprobeSize = format.size?.toLongOrNull()
            ?: return reject("Invalid ffprobe size: ${format.size}")

        if (ffprobeSize != actualSize) {
            warnings += "ffprobe size ($ffprobeSize) differs from actual file size ($actualSize)"
        }

        val ratio = actualSize.toDouble() / ffprobeSize.toDouble()
        if (ratio !in 0.95..1.05) {
            return reject("File size mismatch >5% (ffprobe=$ffprobeSize, actual=$actualSize)")
        }

        return null
    }


    // ---------------------------------------------------------
    // 4) Stream consistency validation
    // ---------------------------------------------------------

    private fun validateStreamConsistency(
        parsed: MediaStreamParsedEvent,
        format: FFprobeFormat,
        warnings: MutableList<String>
    ) {
        val duration = format.duration?.toDoubleOrNull() ?: return

        fun checkStreamDuration(type: String, index: Int, dur: String?) {
            if (dur == null || dur == "0.000000") {
                warnings += "$type stream $index has zero duration"
            }
        }

        parsed.data.videoStream.forEach { checkStreamDuration("Video", it.index, it.duration) }
        parsed.data.audioStream.forEach { checkStreamDuration("Audio", it.index, it.duration) }
        parsed.data.subtitleStream.forEach { checkStreamDuration("Subtitle", it.index, it.duration) }

        val streamDurations = (
                parsed.data.videoStream.mapNotNull { it.duration?.toDoubleOrNull() } +
                        parsed.data.audioStream.mapNotNull { it.duration?.toDoubleOrNull() }
                )

        if (streamDurations.isNotEmpty()) {
            val avg = streamDurations.average()
            val diff = kotlin.math.abs(avg - duration)

            if (diff > duration * 0.10) {
                warnings += "Container/stream duration mismatch (avg=$avg, container=$duration)"
            }
        }
    }

    // ---------------------------------------------------------
    //  Event builders
    // ---------------------------------------------------------

    private fun ok(warnings: List<String>) =
        ValidateFileAndMediaDataEvent(
            validationStatus = ValidateFileAndMediaDataEvent.ValidationStatus.Ok,
            warnings = warnings,
            severity = if (warnings.isEmpty())
                ValidateFileAndMediaDataEvent.ValidationSeverity.None
            else
                ValidateFileAndMediaDataEvent.ValidationSeverity.Warning
        )

    private fun reject(reason: String) =
        ValidateFileAndMediaDataEvent(
            validationStatus = ValidateFileAndMediaDataEvent.ValidationStatus.Rejected,
            rejectionReason = reason,
            severity = ValidateFileAndMediaDataEvent.ValidationSeverity.Critical
        )
}

