package no.iktdev.mediaprocessing.coordinator.tasksV2.mapping.store

import no.iktdev.eventi.database.executeOrException
import no.iktdev.eventi.database.withTransaction
import no.iktdev.mediaprocessing.coordinator.getStoreDatabase
import no.iktdev.mediaprocessing.shared.common.contract.reader.SummaryInfo
import no.iktdev.streamit.library.db.query.SummaryQuery

object ContentMetadataStore {

    fun storeSummary(catalogId: Int, summaryInfo: SummaryInfo) {
        val result = executeOrException(getStoreDatabase().database) {
            SummaryQuery(
                cid = catalogId,
                language = summaryInfo.language,
                description = summaryInfo.summary
            ).insert()
        }
    }
}