package no.iktdev.mediaprocessing.ui.models

import no.iktdev.mediaprocessing.shared.common.model.FailingReason as SailingReason
import no.iktdev.mediaprocessing.ui.models.contract.DiskInfo
import no.iktdev.mediaprocessing.ui.models.contract.sequence.FailingReason
import no.iktdev.mediaprocessing.shared.common.dto.DiskInfo as SharedDiskInfo

fun SharedDiskInfo.translate() = DiskInfo(
    mount = this.mount,
    device = this.device,
    totalBytes = this.totalBytes,
    freeBytes = this.freeBytes,
    usedBytes = this.usedBytes,
    usedPercent = this.usedPercent,
)

fun SailingReason.translate(): FailingReason {
    return FailingReason.valueOf(this.name)
}