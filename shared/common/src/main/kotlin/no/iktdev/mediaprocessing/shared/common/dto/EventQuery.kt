package no.iktdev.mediaprocessing.shared.common.dto

import java.time.Instant

data class EventQuery(
    val referenceId: String? = null,
    val eventId: String? = null,
    val event: String? = null,
    val from: Instant? = null,
    val to: Instant? = null,
    override val sort: String = "persistedAt",
    override val order: Sort = Sort.DESC,
    override val page: Int = 0,
    override val pageSize: Int = 50
) : PagedQuery

