package no.iktdev.mediaprocessing.coordinator.tasksV2.mapping.store

import no.iktdev.mediaprocessing.coordinator.getStoreDatabase
import no.iktdev.mediaprocessing.shared.common.contract.reader.SummaryInfo
import no.iktdev.streamit.library.db.executeOrException
import no.iktdev.streamit.library.db.query.SummaryQuery

object ContentMetadataStore {

    fun storeSummary(catalogId: Int, summaryInfo: SummaryInfo) {
        val result = executeOrException(getStoreDatabase().database, block =  {
            SummaryQuery(
                cid = catalogId,
                language = summaryInfo.language,
                description = summaryInfo.summary
            ).insert()
        })
    }
}