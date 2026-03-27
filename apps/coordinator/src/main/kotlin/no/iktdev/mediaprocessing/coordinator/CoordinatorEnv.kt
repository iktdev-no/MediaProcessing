package no.iktdev.mediaprocessing.coordinator

import no.iktdev.files.IFile
import no.iktdev.mediaprocessing.coordinator.config.ExecutablesConfig
import no.iktdev.mediaprocessing.shared.common.configs.MediaPaths
import no.iktdev.mediaprocessing.shared.common.configs.StreamItConfig
import org.springframework.stereotype.Service

@Service
class CoordinatorEnv(
    val streamIt: StreamItConfig,
    val exec: ExecutablesConfig,
    val media: MediaPaths
) {
    val streamitAddress = streamIt.address
    val ffprobe = exec.ffprobe

    val scratchFolder = IFile(media.scratch)
    val intermediateFolder = IFile(media.intermediate)
    val outboxFolder = IFile(media.outbox)
    val inboxFolder = IFile(media.inbox)
    val preference: IFile = IFile("/data/config/preference.json")

}
