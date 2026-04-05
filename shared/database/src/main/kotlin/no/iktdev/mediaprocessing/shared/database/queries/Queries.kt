package no.iktdev.mediaprocessing.shared.database.queries

import no.iktdev.mediaprocessing.shared.common.dto.PagedQuery
import no.iktdev.mediaprocessing.shared.common.dto.Paginated
import no.iktdev.mediaprocessing.shared.common.dto.Sort
import no.iktdev.mediaprocessing.shared.database.withTransaction
import org.jetbrains.exposed.sql.*

fun <T> pagedQuery(
    table: Table,
    query: PagedQuery,
    sortColumns: Map<String, Column<*>>? = null,
    applyFilters: QueryBuilder.() -> Unit,
    mapper: (ResultRow) -> T
): Paginated<T> {

    return withTransaction {

        // 1. Base query
        val base = table.selectAll()

        // 2. Filters
        val builder = QueryBuilder(base)
        builder.applyFilters()
        val filtered = builder.build()

        // 3. Count
        val total = filtered.count()

        // 4. Sorting (optional)
        val sorted = if (sortColumns != null) {
            val sortColumn = sortColumns[query.sort]
                ?: error("Unknown sort: ${query.sort}")

            val sortOrder = when (query.order) {
                Sort.ASC -> SortOrder.ASC
                Sort.DESC -> SortOrder.DESC
            }

            filtered.orderBy(sortColumn, sortOrder)
        } else {
            filtered
        }

        // 5. Paging
        val paged = sorted
            .limit(query.pageSize)
            .offset((query.page * query.pageSize).toLong())

        // 6. Map
        val items = paged.map(mapper)

        Paginated(
            items = items,
            page = query.page,
            size = query.pageSize,
            total = total
        )
    }.getOrDefault(Paginated(emptyList(), query.page, query.pageSize, 0))
}
