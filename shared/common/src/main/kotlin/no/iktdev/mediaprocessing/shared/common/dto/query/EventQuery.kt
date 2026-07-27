package no.iktdev.mediaprocessing.shared.common.dto.query

import org.springframework.util.LinkedMultiValueMap
import org.springframework.util.MultiValueMap
import java.time.Instant

data class EventQuery(
    val referenceId: String? = null,
    val eventId: String? = null,
    val from: Instant? = null,
    val to: Instant? = null,
    val eventTypes: List<String>? = null,
    override val sort: String = "persistedAt",
    override val order: Sort = Sort.DESC,
    override val page: Int = 0,
    override val pageSize: Int = 50,
    override val key: List<String>?
) : PagedQuery {

    fun toQueryParams(): MultiValueMap<String, String> {
        val params = LinkedMultiValueMap<String, String>()
        key?.forEach { params.add("key", it) }
        referenceId?.let { params.add("referenceId", it) }
        eventId?.let { params.add("eventId", it) }
        from?.let { params.add("from", it.toString()) }
        to?.let { params.add("to", it.toString()) }
        params.add("sort", sort)
        params.add("order", order.name)
        params.add("page", page.toString())
        params.add("pageSize", pageSize.toString())
        eventTypes?.forEach { params.add("eventTypes", it) }
        return params
    }
}

