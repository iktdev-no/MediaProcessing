import { Box, FormControl, InputLabel, MenuItem, Select } from "@mui/material";
import { useEffect, useMemo, useState } from "react";
import { getEvents } from "../api/coordinator/events";
import { EventDialog } from "../components/event/EventDialog";
import { LineageDialog } from "../components/event/EventLinageDialog";
import { EventsTable } from "../components/event/EventsTable";
import { FilterChips } from "../components/FilterChips";
import { Paginator } from "../components/Paginator";
import { eventFilterSchema } from "../features/events/eventFilterSchema";
import { parseEventFilters } from "../features/events/parseEventFilters";
import { useStorage } from "../features/useStorage";
import { useTitle } from "../features/useTitle";
import type { UiEvent } from "../types/types";
import type { EventQuery, PagedUiEvent } from "../types/webTypes";

export default function EventsPage() {
  const { setTitle } = useTitle();
  const [lastUpdated, setLastUpdated] = useState<Date | null>(null);

  const [refreshInterval, setRefreshInterval] = useStorage<number>(
    "events-refresh-interval",
    0,
  );

  useEffect(() => {
    setTitle("Events");
  }, []);

  const [filters, setFilters] = useState<string[]>([]);
  const [query, setQuery] = useState<EventQuery>({
    page: 0,
    pageSize: 50,
    sort: "persistedAt",
    order: "DESC",
    eventTypes: undefined,
  });

  // 👇 Her definerer du handleBeforeAdd
  const handleBeforeAdd = (token: string, current: string[]) => {
    // allow multiple keys
    if (token.startsWith("key:")) return current;

    // allow multiple eventIds
    if (eventFilterSchema.eventIds.includes(token)) return current;

    // date filters override same type
    if (token.startsWith("from:")) {
      return [...current.filter((f) => !f.startsWith("from:")), token];
    }
    if (token.startsWith("to:")) {
      return [...current.filter((f) => !f.startsWith("to:")), token];
    }

    return current;
  };

  const [selected, setSelected] = useState<UiEvent | null>(null);

  const [lineageOpen, setLineageOpen] = useState(false);
  const [lineageEvent, setLineageEvent] = useState<UiEvent | null>(null);

  function onShowLineage(ev: UiEvent) {
    setLineageEvent(ev);
    setLineageOpen(true);
  }

  const [data, setData] = useState<PagedUiEvent | null>(null);
  const [isLoading, setIsLoading] = useState(false);
  // Parse filters → update query
  const parsedQuery = useMemo(
    () => parseEventFilters(filters, query),
    [filters, query],
  );

  // Fetch tasks
  // Fetch tasks når query endres
  useEffect(() => {
    setData(null);
    setIsLoading(true);
    getEvents(query).then((result) => {
      setData(result);
      setIsLoading(false);
    });
  }, [query]);

  return (
    <Box
      sx={{
        height: "100%",
        display: "flex",
        flexDirection: "column",
        gap: 2,
      }}
    >
      <Box
        sx={{
          p: 2,
          borderBottom: "1px solid rgba(0,0,0,0.12)",
          display: "flex",
          alignItems: "center",
          gap: 2,
          flexWrap: "wrap",
        }}
      >
        <Box sx={{ flex: 1, minWidth: 200, alignItems: "center" }}>
          <FilterChips
            value={filters}
            onChange={(next) => {
              setFilters(next);
              setQuery((q) => ({ ...q, page: 0 }));
            }}
            onBeforeAdd={handleBeforeAdd}
            suggestions={[...eventFilterSchema.eventIds, "from:", "to:"]}
            keySuggestions={[]}
            keyLabel="Key"
          />
        </Box>
        <Box
          sx={{
            display: "flex",
            alignSelf: "start",
            alignItems: "center",
            gap: 2,
            whiteSpace: "nowrap",
          }}
        >
          <FormControl size="small" sx={{ minWidth: 140 }}>
            <InputLabel id="refresh-label">Auto refresh</InputLabel>
            <Select
              labelId="refresh-label"
              value={refreshInterval}
              label="Auto refresh"
              onChange={(e) => {
                const v = Number(e.target.value);
                setRefreshInterval(v);
              }}
            >
              <MenuItem value={0}>Never</MenuItem>
              <MenuItem value={5}>5s</MenuItem>
              <MenuItem value={10}>10s</MenuItem>
              <MenuItem value={15}>15s</MenuItem>
              <MenuItem value={30}>30s</MenuItem>
              <MenuItem value={60}>60s</MenuItem>
            </Select>
            {refreshInterval > 0 && (
              <></>
              /*
                <RefreshProgressBar
                  refreshInterval={refreshInterval}
                  isFetching={isFetching}
                  sx={{ marginTop: -0.8, zIndex: -1 }}
                />*/
            )}
          </FormControl>
        </Box>
      </Box>
      <EventsTable
        events={data?.items ?? []}
        loading={isLoading}
        onShowDetails={(ev) => setSelected(ev)}
        onShowLineage={(ev) => onShowLineage(ev)}
      />

      <Box
        position="sticky"
        bottom={0}
        zIndex={10}
        bgcolor="background.paper"
        borderTop="1px solid rgba(0,0,0,0.12)"
        py={1}
        px={2}
      >
        <Box sx={{ fontSize: 12, opacity: 0.6 }}>
          {lastUpdated
            ? `Oppdatert ${lastUpdated.toLocaleTimeString("no-NO", { hour12: false })}`
            : "Oppdatert aldri"}
        </Box>
        {data && (
          <Paginator
            page={data.page}
            size={data.size}
            total={data.total}
            onPageChange={(page) => {
              console.log(`New page ${page}`);
              setQuery((q) => ({ ...q, page }));
            }}
            onSizeChange={(pageSize) =>
              setQuery((q) => ({ ...q, page: 0, pageSize }))
            }
          />
        )}
      </Box>

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
  );
}
