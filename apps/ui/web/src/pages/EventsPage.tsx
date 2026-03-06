import { Box } from "@mui/material"
import { useQuery } from "@tanstack/react-query"
import { useMemo, useState } from "react"
import { getEvents } from "../api/events"
import { EventDialog } from "../components/event/EventDialog"
import { LineageDialog } from "../components/event/EventLinageDialog"
import { EventsTable } from "../components/event/EventsTable"
import { FilterChips } from "../components/FilterChips"
import { Paginator } from "../components/Paginator"
import { eventFilterSchema } from "../features/events/eventFilterSchema"
import { parseEventFilters } from "../features/events/parseEventFilters"
import type { UiEvent } from "../types/types"
import type { EventQuery } from "../types/webTypes"

export default function EventsPage() {
    const [filters, setFilters] = useState<string[]>([])
    const [query, setQuery] = useState<EventQuery>({
        page: 0,
        pageSize: 50,
        sort: "persistedAt",
        order: "DESC"
    })

    // 👇 Her definerer du handleBeforeAdd
    const handleBeforeAdd = (token: string, current: string[]) => {
        // allow multiple keys
        if (token.startsWith("key:")) return current

        // allow multiple eventIds
        if (eventFilterSchema.eventIds.includes(token)) return current

        // date filters override same type
        if (token.startsWith("from:")) {
            return [...current.filter(f => !f.startsWith("from:")), token]
        }
        if (token.startsWith("to:")) {
            return [...current.filter(f => !f.startsWith("to:")), token]
        }

        return current
    }

    const parsedQuery = useMemo(
        () => parseEventFilters(filters, query),
        [filters, query]
    )

    const [selected, setSelected] = useState<UiEvent | null>(null)

    const [lineageOpen, setLineageOpen] = useState(false);
    const [lineageEvent, setLineageEvent] = useState<UiEvent | null>(null);


    function onShowLineage(ev: UiEvent) {
        setLineageEvent(ev);
        setLineageOpen(true);
    }


    const { data, isLoading } = useQuery({
        queryKey: ["events", parsedQuery],
        queryFn: () => getEvents(parsedQuery)
    })

    return (
        <Box sx={{ pb: 1, height: "100%", display: "flex", flexDirection: "column", gap: 2 }}>
            <FilterChips
                value={filters}
                onChange={setFilters}
                onBeforeAdd={handleBeforeAdd}
                suggestions={[
                    ...eventFilterSchema.eventIds,
                    "from:",
                    "to:"
                ]}
                keySuggestions={[]}
                keyLabel="Key"
            />

            <EventsTable
                events={data?.items ?? []}
                loading={isLoading}
                onShowDetails={ev => setSelected(ev)}
                onShowLineage={ev => onShowLineage(ev)}
            />

            {data && (
                <Paginator
                    page={data.page}
                    size={data.size}
                    total={data.total}
                    onPageChange={(page) => {
                        console.log(`New page ${page}`);
                        setQuery(q => ({ ...q, page }))
                    }
                    }
                    onSizeChange={(pageSize) =>
                        setQuery(q => ({ ...q, page: 0, pageSize }))
                    }
                />
            )}

            <EventDialog
                event={selected}
                open={!!selected}
                onClose={() => setSelected(null)}
            />
            <LineageDialog
                open={lineageOpen}
                onClose={() => setLineageOpen(false)}
                referenceId={lineageEvent?.referenceId ?? null}
                selectedEventId={lineageEvent?.eventId ?? null}
            />

        </Box>
    )
}
