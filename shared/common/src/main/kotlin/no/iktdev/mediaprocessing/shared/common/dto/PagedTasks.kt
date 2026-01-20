package no.iktdev.mediaprocessing.shared.common.dto

import no.iktdev.eventi.models.store.PersistedTask

data class PagedTasks(
    val content: List<PersistedTask>,
    val page: Int,
    val size: Int,
    val total: Long
)