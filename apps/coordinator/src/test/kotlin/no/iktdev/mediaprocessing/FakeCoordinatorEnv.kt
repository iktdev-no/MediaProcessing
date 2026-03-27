package no.iktdev.mediaprocessing

import no.iktdev.files.IFile
import no.iktdev.mediaprocessing.coordinator.CoordinatorEnv
import no.iktdev.mediaprocessing.coordinator.config.ExecutablesConfig
import no.iktdev.mediaprocessing.shared.common.configs.MediaPaths
import no.iktdev.mediaprocessing.shared.common.configs.StreamItConfig

// ------------------------------------------------------------
// Fake CoordinatorEnv for testing
// ------------------------------------------------------------
class FakeCoordinatorEnv(prefFile: IFile) : CoordinatorEnv(
    streamIt = StreamItConfig(address = "http://localhost"),
    exec = ExecutablesConfig(ffprobe = "/usr/bin/ffprobe"),
    media = MediaPaths(
        scratch = "/tmp/scratch",
        intermediate = "/tmp/intermediate",
        outbox = "/tmp/outbox",
        inbox = "/tmp/inbox"
    )
) {
    override val preference: IFile = prefFile
}