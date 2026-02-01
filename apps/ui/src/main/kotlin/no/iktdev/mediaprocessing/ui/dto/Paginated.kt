package no.iktdev.mediaprocessing.ui.dto

data class Paginated<T>(
    val items: List<T>,
    val page: Int,
    val size: Int,
    val total: Long,
)
