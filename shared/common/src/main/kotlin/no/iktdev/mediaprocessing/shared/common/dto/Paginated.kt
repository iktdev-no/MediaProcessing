package no.iktdev.mediaprocessing.shared.common.dto

data class Paginated<T>(
    val items: List<T>,
    val page: Int,
    val size: Int,
    val total: Long
)

fun <T, R> Paginated<T>.map(transform: (T) -> R): Paginated<R> {
    return Paginated(
        items = items.map(transform),
        page = page,
        size = size,
        total = total
    )
}
