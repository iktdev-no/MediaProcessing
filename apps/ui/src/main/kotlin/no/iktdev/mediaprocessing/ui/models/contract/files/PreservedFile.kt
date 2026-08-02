package no.iktdev.mediaprocessing.ui.models.contract.files

import no.iktdev.mediaprocessing.shared.common.dto.files.PreservedFile as SPF

import java.time.Instant
import java.util.UUID

data class PreservedFile(
    val filePath: String,
    val fileName: String,
    val preserved: Boolean,
    val persistedAt: Instant? = null,
    val usedInReferences: List<UUID> = emptyList() // Fylles inn ved behov fra Event Store
)

fun SPF.translate() = PreservedFile(
    filePath = filePath,
    fileName = fileName,
    preserved = preserved,
    persistedAt = persistedAt,
    usedInReferences = usedInReferences
)