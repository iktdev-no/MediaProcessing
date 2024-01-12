package no.iktdev.mediaprocessing.coordinator.mapping

import no.iktdev.mediaprocessing.shared.common.persistance.PersistentMessage
import no.iktdev.mediaprocessing.shared.contract.reader.MetadataCoverDto
import no.iktdev.mediaprocessing.shared.contract.reader.MetadataDto
import no.iktdev.mediaprocessing.shared.contract.reader.SummaryInfo
import no.iktdev.mediaprocessing.shared.kafka.dto.events_result.*
import no.iktdev.mediaprocessing.shared.kafka.dto.isSuccess
import java.io.File


class MetadataMapping(val events: List<PersistentMessage>) {


    fun map(): MetadataDto? {
        val baseInfo = events.find { it.data is BaseInfoPerformed }?.data as BaseInfoPerformed?
        val mediaReadOut = events.find { it.data is VideoInfoPerformed }?.data as VideoInfoPerformed?
        val meta = events.find { it.data is MetadataPerformed }?.data as MetadataPerformed?

        val coverDownloadTask = events.find { it.data is CoverInfoPerformed }?.data as CoverInfoPerformed?
        val cover = events.find { it.data is CoverDownloadWorkPerformed }?.data as CoverDownloadWorkPerformed?

        if (!baseInfo.isSuccess()) {
            return null
        }

        val videoInfo = mediaReadOut?.toValueObject()

        val mediaCover = if (coverDownloadTask != null || cover != null) {
            val coverFile = cover?.coverFile?.let { File(it) }
            MetadataCoverDto(
                cover = coverFile?.name,
                coverFile = cover?.coverFile,
                coverUrl = coverDownloadTask?.url
            )
        } else null

        return if (meta != null || videoInfo != null) {
            MetadataDto(
                title = meta?.data?.title ?: videoInfo?.fullName ?: return null,
                collection = baseInfo?.title ?: return null,
                cover = mediaCover,
                type = meta?.data?.type ?: videoInfo?.type ?: return null,
                summary = meta?.data?.summary?.filter {it.summary != null }?.map { SummaryInfo(language = it.language, summary = it.summary!! ) } ?: emptyList(),
                genres = meta?.data?.genres ?: emptyList(),
            )
        } else null
    }

    fun getCollection(): String? {
        val baseInfo = events.find { it.data is BaseInfoPerformed }?.data as BaseInfoPerformed?
        if (!baseInfo.isSuccess()) {
            return null
        }
        return baseInfo?.title
    }

}