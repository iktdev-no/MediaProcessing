import {
  Box,
  CircularProgress,
  FormControl,
  InputLabel,
  MenuItem,
  Select,
  Snackbar,
} from "@mui/material";
import { useEffect, useMemo, useState } from "react";
import { toast } from "react-toastify";
import { cancelTask, getTasks } from "../api/coordinator/tasks";
import { FilterChips } from "../components/FilterChips";
import { Paginator } from "../components/Paginator";
import { RefreshProgressBar } from "../components/RefreshProgressBar";
import { TaskCard } from "../components/task/TaskCard";
import { useToast } from "../features/useToast";
import { parseTaskFilters } from "../features/tasks/parseTaskFilters";
import {
  knownTaskNames,
  taskFilterSchema,
} from "../features/tasks/taskFilterSchema";
import { usePageQuery } from "../features/usePageQuery";
import { useStorage } from "../features/useStorage";
import { useTitle } from "../features/useTitle";
import type { UiTask } from "../types/types";
import type { TaskQuery } from "../types/webTypes";

const handleBeforeAdd = (token: string, current: string[]) => {
  if (token === "claimed")
    return [...current.filter((f) => f !== "!claimed"), token];
  if (token === "!claimed")
    return [...current.filter((f) => f !== "claimed"), token];
  if (token === "consumed")
    return [...current.filter((f) => f !== "!consumed"), token];
  if (token === "!consumed")
    return [...current.filter((f) => f !== "consumed"), token];
  return current;
};

export default function TasksPage() {
  const { open, message, showToast, handleClose } = useToast();
  const [refreshInterval, setRefreshInterval] = useStorage<number>(
    "tasks-refresh-interval",
    0,
  );

  const { setTitle } = useTitle();
  useEffect(() => setTitle("Tasks"), []);

  const [filters, setFilters] = useState<string[]>([]);
  const [query, setQuery] = useState<TaskQuery>({
    page: 0,
    pageSize: 25,
    sort: "persistedAt",
    order: "DESC",
  });

  const parsedQuery = useMemo(
    () => parseTaskFilters(filters, query),
    [filters, query],
  );

  const { data, isLoading, isFetching, lastUpdated } = usePageQuery(
    ["tasks", parsedQuery],
    () => getTasks(parsedQuery),
    refreshInterval,
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
      if (err.status !== 404) toast.error("Cancellation failed");
    }
  };

  // FIRST LOAD ONLY
  if (isLoading && !data) {
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

      <Box
        style={{
          display: "flex",
          flexDirection: "column",
          height: "100%",
          overflow: "hidden",
        }}
      >
        {/* Header */}
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

          <Box
            sx={{
              display: "flex",
              alignSelf: "start",
              alignItems: "center",
              flexDirection: "column",
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
                onChange={(e) => setRefreshInterval(Number(e.target.value))}
              >
                <MenuItem value={0}>Never</MenuItem>
                <MenuItem value={5}>5s</MenuItem>
                <MenuItem value={10}>10s</MenuItem>
                <MenuItem value={15}>15s</MenuItem>
                <MenuItem value={30}>30s</MenuItem>
                <MenuItem value={60}>60s</MenuItem>
              </Select>
            </FormControl>
            <RefreshProgressBar
              refreshInterval={refreshInterval}
              isFetching={isFetching}
              sx={{ marginTop: -2.6, zIndex: 1 }}
            />
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
          {data?.items.map((task: UiTask) => (
            <TaskCard
              key={task.taskId}
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

        {/* Footer */}
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
              onSizeChange={(pageSize) =>
                setQuery((q) => ({ ...q, page: 0, pageSize }))
              }
            />
          )}
        </Box>
      </Box>
    </>
  );
}
