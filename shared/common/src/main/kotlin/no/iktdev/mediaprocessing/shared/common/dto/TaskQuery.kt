package no.iktdev.mediaprocessing.shared.common.dto

import org.springframework.util.LinkedMultiValueMap
import org.springframework.util.MultiValueMap
import java.time.Instant

data class TaskQuery(
    val status: List<String>? = null,
    val claimed: Boolean? = null,
    val consumed: Boolean? = null,
    val referenceId: String? = null,
    val from: Instant? = null,
    val to: Instant? = null,
    override val sort: String = "persistedAt",
    override val order: Sort = Sort.DESC,
    override val page: Int = 0,
    override val pageSize: Int = 50,
    override val key: String? = null
): PagedQuery {
    fun toQueryParams(): MultiValueMap<String, String> {
        val params = LinkedMultiValueMap<String, String>()

        key?.let { params.add("key", it) }
        status?.forEach { params.add("status", it) }
        claimed?.let { params.add("claimed", it.toString()) }
        consumed?.let { params.add("consumed", it.toString()) }
        referenceId?.let { params.add("referenceId", it) }
        from?.let { params.add("from", it.toString()) }
        to?.let { params.add("to", it.toString()) }
        params.add("sort", sort)
        params.add("order", order.name)
        params.add("page", page.toString())
        params.add("pageSize", pageSize.toString())

        return params
    }

}

