import { useEffect, useState } from "react";
import { apiGet } from "../api/client";
import { getSystemHealth } from "../api/coordinator/health";

// MUI
import {
  Accordion,
  AccordionDetails,
  AccordionSummary,
  Grid,
  Stack,
  Typography,
} from "@mui/material";

import ExpandMoreIcon from "@mui/icons-material/ExpandMore";
import { EventRatePanel } from "../components/dashboard/EventRatePanel";
import { OverdueSequenceCard } from "../components/dashboard/OverdueSequenceCard";
import { StatusHeader } from "../components/dashboard/StatusHeader";
import { StoragePanel } from "../components/dashboard/StoragePanel";
import { TaskOverview } from "../components/dashboard/TaskOverview";
import { useTitle } from "../features/useTitle";
import type {
  SystemHealth,
  DiskInfo,
  EventRate,
  SequenceHealth,
} from "../types/types";

// --- TASK OVERVIEW ---

// --- SEQUENCE CARD ---

function SequenceList({ sequences }: { sequences: SequenceHealth[] }) {
  return (
    <Grid container spacing={2} sx={{ mt: 2 }}>
      {sequences.map((seq) => (
        <Grid size={{ xs: 12, md: 6, lg: 4 }} key={seq.referenceId}>
          <OverdueSequenceCard seq={seq} />
        </Grid>
      ))}
    </Grid>
  );
}

// --- DETAILS ---
function DetailsInspector({ details }: { details: Record<string, unknown> }) {
  return (
    <Accordion sx={{ mt: 3 }}>
      <AccordionSummary expandIcon={<ExpandMoreIcon />}>
        <Typography>Tekniske detaljer</Typography>
      </AccordionSummary>
      <AccordionDetails>
        <Stack spacing={1}>
          {Object.entries(details).map(([key, value]) => (
            <Stack key={key} direction="row" justifyContent="space-between">
              <Typography sx={{ opacity: 0.7 }}>{key}</Typography>
              <Typography>{value === null ? "—" : String(value)}</Typography>
            </Stack>
          ))}
        </Stack>
      </AccordionDetails>
    </Accordion>
  );
}

// --- MAIN PAGE ---
export default function DashboardPage() {
  const [health, setHealth] = useState<SystemHealth | null>(null);
  const [eventRate, setEventRate] = useState<EventRate | null>(null);
  const [storage, setStorage] = useState<DiskInfo[] | null>(null);

  const { setTitle } = useTitle();

  useEffect(() => {
    setTitle("Dashboard");
  }, []);

  // Poll event rate
  useEffect(() => {
    const fetchRate = () =>
      apiGet<EventRate>("/health/events").then(setEventRate);
    fetchRate();
    const interval = setInterval(fetchRate, 5000);
    return () => clearInterval(interval);
  }, []);

  // Poll storage
  useEffect(() => {
    const fetchStorage = () =>
      apiGet<DiskInfo[]>("/health/storage").then(setStorage);
    fetchStorage();
    const interval = setInterval(fetchStorage, 10000);
    return () => clearInterval(interval);
  }, []);

  // Initial health load
  useEffect(() => {
    const fetchHealth = () => getSystemHealth().then(setHealth);
    fetchHealth();
    const interval = setInterval(fetchHealth, 5000);
    return () => clearInterval(interval);
  }, []);

  if (!health) return <div>Laster systemstatus…</div>;

  return (
    <Stack
      spacing={4}
      sx={{
        p: 3,
        height: "100%",
        overflowY: "auto",
        boxSizing: "border-box",
      }}
    >
      <Typography variant="h4">System Health</Typography>

      <StatusHeader status={health.status} lastActivity={health.lastActivity} />

      <TaskOverview
        active={health.activeTasks}
        queued={health.queuedTasks}
        stalled={health.stalledTasks}
        abandoned={health.abandonedTasks}
        failed={health.failedTasks}
        onHold={health.sequencesOnHold}
      />

      {eventRate && <EventRatePanel rate={eventRate} />}

      {storage && <StoragePanel disks={storage} />}

      <Typography variant="h5" sx={{ mt: 4 }}>
        Overdue Sequences
      </Typography>
      <SequenceList sequences={health.overdueSequences} />

      <DetailsInspector details={health.details} />
    </Stack>
  );
}
