package no.iktdev.mediaprocessing.shared.database.queries

import no.iktdev.mediaprocessing.shared.common.dto.PagedQuery
import no.iktdev.mediaprocessing.shared.common.dto.Paginated
import no.iktdev.mediaprocessing.shared.common.dto.Sort
import no.iktdev.mediaprocessing.shared.database.withTransaction
import org.jetbrains.exposed.sql.*

data class ColumnSort(val priority: Int, val column: Column<*>)

fun <T> pagedQuery(
    table: Table,
    query: PagedQuery,
    sortColumns: Map<String, ColumnSort>? = null,
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

        // 4. Sorting with priority
        val sorted = if (sortColumns != null) {

            val sortOrder = if (query.order == Sort.ASC) SortOrder.ASC else SortOrder.DESC

            // primær sortering (brukerens valg)
            val primary = sortColumns[query.sort]
                ?: error("Unknown sort: ${query.sort}")

            // bygg prioritert liste: primær først, så resten etter priority
            val ordered = listOf(primary) +
                    sortColumns.values
                        .filter { it != primary }
                        .sortedBy { it.priority }

            // konverter til Exposed-par
            val orderPairs = ordered.map { sort ->
                sort.column to sortOrder
            }

            filtered.orderBy(*orderPairs.toTypedArray())

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
