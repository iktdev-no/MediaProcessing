import {
  Box,
  CircularProgress,
  FormControl,
  InputLabel,
  MenuItem,
  Select,
  Snackbar,
} from "@mui/material";
import { useEffect, useState } from "react";
import { toast } from "react-toastify";
import { cancelTask, getTasks } from "../api/coordinator/tasks";
import { FilterChips } from "../components/FilterChips";
import { Paginator } from "../components/Paginator";
import { TaskCard } from "../components/task/TaskCard";
import { useToast } from "../components/useToast";
import { parseTaskFilters } from "../features/tasks/parseTaskFilters";
import {
  knownTaskNames,
  taskFilterSchema,
} from "../features/tasks/taskFilterSchema";
import { useAutoRefresh } from "../features/useAutoRefresh";
import { useStorage } from "../features/useStorage";
import { useTitle } from "../features/useTitle";
import type { UiTask } from "../types/types";
import type { PagedUiTask, TaskQuery } from "../types/webTypes";

const handleBeforeAdd = (token: string, current: string[]) => {
  // Claimed
  if (token === "claimed") {
    return [...current.filter((f) => f !== "!claimed"), token];
  }
  if (token === "!claimed") {
    return [...current.filter((f) => f !== "claimed"), token];
  }

  // Consumed
  if (token === "consumed") {
    return [...current.filter((f) => f !== "!consumed"), token];
  }
  if (token === "!consumed") {
    return [...current.filter((f) => f !== "consumed"), token];
  }

  return current;
};

export default function TasksPage() {
  const { open, message, showToast, handleClose } = useToast();
  const [refreshInterval, setRefreshInterval] = useStorage<number>(
    "tasks-refresh-interval",
    0,
  );
  const [lastUpdated, setLastUpdated] = useState<Date | null>(null);

  const { setTitle } = useTitle();

  useEffect(() => {
    setTitle("Tasks");
  }, []);

  const [query, setQuery] = useState<TaskQuery>({
    page: 0,
    pageSize: 25,
    sort: "persistedAt",
    order: "DESC",
  });

  const [data, setData] = useState<PagedUiTask | null>(null);
  const [filters, setFilters] = useState<string[]>([]);

  // Parse filters → update query
  useEffect(() => {
    setQuery((prev) => parseTaskFilters(filters, prev));
  }, [filters]);

  // Fetch tasks
  // Fetch tasks når query endres
  useEffect(() => {
    setData(null);
    getTasks(query).then(setData);
  }, [query]);

  // Auto-refresh hvert 3 sekund
  useAutoRefresh(
    () => {
      getTasks(query).then((d) => {
        setData(d);
        setLastUpdated(new Date());
      });
    },
    refreshInterval ? refreshInterval * 1000 : null,
  );

  const onCancelTask = async (taskId: string) => {
    try {
      await cancelTask(taskId, {
        onError: (status) => {
          toast.error(
            `Failed to cancel task ${taskId} with status code ${status}`,
          );
        },
      });
      toast.success("Task cancelled");
    } catch (err: any) {
      if (err.status !== 404) {
        toast.error("Cancellation failed");
      }
    }
  };

  if (!data) {
    return (
      <Box
        display="flex"
        alignItems="center"
        justifyContent="center"
        height="100%"
      >
        <CircularProgress />
      </Box>
    );
  }

  return (
    <>
      <Snackbar
        open={open}
        autoHideDuration={2000}
        onClose={handleClose}
        message={message}
        anchorOrigin={{ vertical: "bottom", horizontal: "center" }}
      />

      <div
        style={{
          display: "flex",
          flexDirection: "column",
          height: "100%",
          overflow: "hidden",
        }}
      >
        {/* Filter + Refresh + Last updated */}
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
          {/* Left: Filter chips (takes all available space) */}
          <Box sx={{ flex: 1, minWidth: 200, alignItems: "center" }}>
            <FilterChips
              value={filters}
              onChange={setFilters}
              onBeforeAdd={handleBeforeAdd}
              suggestions={[
                ...taskFilterSchema.status,
                ...taskFilterSchema.booleans.claimed,
                ...taskFilterSchema.booleans.consumed,
              ]}
              keySuggestions={knownTaskNames}
              keyLabel={taskFilterSchema.keyLabel}
            />
          </Box>

          {/* Right: Refresh selector + Last updated */}
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
                  if (v === 0) setLastUpdated(null);
                }}
              >
                <MenuItem value={0}>Never</MenuItem>
                <MenuItem value={5}>5s</MenuItem>
                <MenuItem value={10}>10s</MenuItem>
                <MenuItem value={15}>15s</MenuItem>
                <MenuItem value={30}>30s</MenuItem>
                <MenuItem value={60}>60s</MenuItem>
              </Select>
            </FormControl>
          </Box>
        </Box>
        {/* Scrollable content */}
        <div
          style={{
            flex: 1,
            overflowY: "auto",
            padding: 16,
            display: "grid",
            gridTemplateColumns: "1fr",
            gap: 12,
            alignItems: "start",
            alignContent: "start",
          }}
        >
          {data.items.map((task: UiTask) => (
            <TaskCard
              key={task.id}
              task={task}
              show="taskId"
              onCopy={() => showToast("Kopiert til utklippstavlen")}
              onReferenceIdClicked={(referenceId) => {
                if (!filters.includes(referenceId)) {
                  setFilters((prev) => [...prev, referenceId]);
                }
              }}
              onCanceltask={(taskId) => onCancelTask(taskId)}
            />
          ))}
        </div>

        {/* Sticky footer paginator */}
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
              onPageChange={(page) => setQuery((q) => ({ ...q, page }))}
              onSizeChange={(itemCount) =>
                setQuery((q) => ({ ...q, page: 0, pageSize: itemCount }))
              }
            />
          )}
        </Box>
      </div>
    </>
  );
}
