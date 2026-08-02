package no.iktdev.mediaprocessing.processer

import no.iktdev.eventi.models.Progress
import no.iktdev.eventi.serialization.ZPS.toEnvelope
import no.iktdev.mediaprocessing.ffmpeg.decoder.FfmpegDecodedProgress
import no.iktdev.mediaprocessing.shared.common.model.ProgressUpdate
import no.iktdev.mediaprocessing.shared.common.progress.TaskProgressCache
import org.springframework.stereotype.Component
import java.util.*
import java.util.concurrent.ConcurrentHashMap

@Component
class LocalProgressCache: TaskProgressCache() {
}
