package no.iktdev.mediaprocessing.shared.database.queries

import no.iktdev.mediaprocessing.shared.common.dto.PagedQuery
import no.iktdev.mediaprocessing.shared.common.dto.Paginated
import no.iktdev.mediaprocessing.shared.common.dto.Sort
import no.iktdev.mediaprocessing.shared.database.withTransaction
import org.jetbrains.exposed.sql.*

fun <T> pagedQuery(
    table: Table,
    query: PagedQuery,
    sortColumns: Map<String, Column<*>>,
    applyFilters: QueryBuilder.() -> Unit,
    mapper: (ResultRow) -> T
): Paginated<T> {

    return withTransaction {

        // 1. Start query
        var base = table.selectAll()

        // 2. Apply filters
        val builder = QueryBuilder(base)
        builder.applyFilters()
        base = builder.build()

        // 3. Count before paging
        val total = base.count()

        // 4. Sorting
        val sortColumn = sortColumns[query.sort] ?: error("Unknown sort: ${query.sort}")
        val sortOrder = if (query.order == Sort.ASC) SortOrder.ASC else SortOrder.DESC

        // 5. Paging + mapping
        val items = base
            .orderBy(sortColumn, sortOrder)
            .limit(query.pageSize)
            .offset((query.page * query.pageSize).toLong())
            .map(mapper)

        Paginated(
            items = items,
            page = query.page,
            size = query.pageSize,
            total = total
        )
    }.getOrDefault(Paginated(emptyList(), query.page, query.pageSize, 0))
}
