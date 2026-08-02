package no.iktdev.mediaprocessing.ui.models.contract

import no.iktdev.mediaprocessing.shared.common.dto.Paginated as SPaginated

data class Paginated<T>(
    val items: List<T>,
    val page: Int,
    val size: Int,
    val total: Long,
)


fun <T> SPaginated<T>.toUi(): Paginated<T> {
    return Paginated(
        items = this.items,
        page = this.page,
        size = this.size,
        total = this.total
    )
}