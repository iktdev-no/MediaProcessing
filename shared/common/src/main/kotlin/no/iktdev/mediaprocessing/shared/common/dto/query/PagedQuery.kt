package no.iktdev.mediaprocessing.shared.common.dto.query

interface PagedQuery {
    val page: Int
    val pageSize: Int
    val sort: String
    val order: Sort
    val key: List<String>?
}
