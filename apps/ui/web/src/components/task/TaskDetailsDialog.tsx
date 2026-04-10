import type { JSX } from "@emotion/react/jsx-runtime";
import ContentCopyIcon from "@mui/icons-material/ContentCopy";
import {
    Box,
    Button,
    Dialog,
    DialogContent,
    IconButton,
    Typography,
} from "@mui/material";
import { useState } from "react";
import { patchTaskOverride } from "../../api/coordinator/tasks";
import type { UiTask } from "../../types/types";
import { JsonViewer } from "../JsonViewer";
import { ProcesserLogDialog } from "../ProcesserLogDialog";
import { TaskActions } from "./TaskActions";

export interface TaskDetailsDialogProps {
  open: boolean;
  onClose: () => void;
  task: UiTask;
  onCopy: () => void;
}

function OpenLogButton({
  task,
  onOpen,
}: {
  task: UiTask;
  onOpen: (path: string) => void;
}): JSX.Element | null {
  if (task.logFiles.length === 0) return null;

  return (
    <>
      {task.logFiles.map((log, index) => (
        <Button key={log} variant="contained" onClick={() => onOpen(log)}>
          Logg {index + 1}
        </Button>
      ))}
    </>
  );
}

export function TaskDetailsDialog({
  open,
  onClose,
  task,
  onCopy,
}: TaskDetailsDialogProps) {
  const [openLog, setOpenLog] = useState(false);
  const [selectedLogPath, setSelectedLogPath] = useState<string | null>(null);

  const handleOpenLog = (path: string) => {
    setSelectedLogPath(path);
    setOpenLog(true);
  };

  const handleCloseLog = () => {
    setOpenLog(false);
    setSelectedLogPath(null);
  };

  const copy = (value: string) => {
    navigator.clipboard.writeText(value);
    onCopy();
  };

  const handleOverrideSelect = async (overrideName: string) => {
    await patchTaskOverride(task.taskId, overrideName);
    onClose(); // reload via parent
  };

  return (
    <Dialog open={open} onClose={onClose} maxWidth="xl" fullWidth>
      <DialogContent
        sx={{
          display: "grid",
          gridTemplateColumns: "1fr 2fr",
          gap: 3,
          height: "80vh", // JSON får full høyde
          overflow: "hidden",
        }}
      >
        {/* LEFT COLUMN */}
        <Box
          sx={{
            display: "flex",
            flexDirection: "column",
            gap: 2,
            overflowY: "auto",
          }}
        >
          {/* Title for left side only */}
          <Typography variant="h6" sx={{ mb: 1 }}>
            {task.task} details
          </Typography>

          {/* Grid matching the card layout */}
          <Box
            sx={{
              display: "grid",
              gridTemplateColumns: "auto 1fr",
              rowGap: 1.5,
              columnGap: 2,
              alignItems: "center",
            }}
          >
            {/* TaskId */}
            <Typography variant="body2" sx={{ fontWeight: 600 }}>
              TaskId
            </Typography>
            <Box display="flex" alignItems="center" gap={1}>
              <Typography variant="body2">{task.taskId}</Typography>
              <IconButton size="small" onClick={() => copy(task.taskId)}>
                <ContentCopyIcon fontSize="small" />
              </IconButton>
            </Box>

            {/* ReferenceId */}
            <Typography variant="body2" sx={{ fontWeight: 600 }}>
              ReferenceId
            </Typography>
            <Box display="flex" alignItems="center" gap={1}>
              <Typography variant="body2">{task.referenceId}</Typography>
              <IconButton size="small" onClick={() => copy(task.referenceId)}>
                <ContentCopyIcon fontSize="small" />
              </IconButton>
            </Box>

            {/* Worker */}
            <Typography variant="body2" sx={{ fontWeight: 600 }}>
              Worker
            </Typography>
            <Typography variant="body2">
              {task.claimedBy ?? "Unclaimed"}
            </Typography>

            {/* Status */}
            <Typography variant="body2" sx={{ fontWeight: 600 }}>
              Status
            </Typography>
            <Typography variant="body2">{task.status}</Typography>

            {/* Claimed */}
            <Typography variant="body2" sx={{ fontWeight: 600 }}>
              Claimed
            </Typography>
            <Typography variant="body2">
              {task.claimed ? "Yes" : "No"}
            </Typography>

            {/* Consumed */}
            <Typography variant="body2" sx={{ fontWeight: 600 }}>
              Consumed
            </Typography>
            <Typography variant="body2">
              {task.consumed ? "Yes" : "No"}
            </Typography>

            {/* Persisted */}
            <Typography variant="body2" sx={{ fontWeight: 600 }}>
              Persisted
            </Typography>
            <Typography variant="body2">{task.persistedAt}</Typography>

            {/* Last check-in */}
            <Typography variant="body2" sx={{ fontWeight: 600 }}>
              Last check-in
            </Typography>
            <Typography variant="body2">{task.lastCheckIn ?? "—"}</Typography>

            {/* Overrides */}
            <Typography variant="body2" sx={{ fontWeight: 600 }}>
              Overrides
            </Typography>

            <Box sx={{ display: "flex", flexDirection: "column", gap: 1 }}>
              {/* Active (read-only) */}
              <Typography variant="body2" sx={{ opacity: 0.7 }}>
                Active:{" "}
                {task.activeOverrides.length > 0
                  ? task.activeOverrides.join(", ")
                  : "None"}
              </Typography>

              {/* Available (clickable) */}
              <Box sx={{ display: "flex", gap: 1, flexWrap: "wrap" }}>
                {task.availableOverrides.map((ov) => (
                  <Button
                    key={ov}
                    size="small"
                    variant="outlined"
                    onClick={() => handleOverrideSelect(ov)}
                  >
                    {ov}
                  </Button>
                ))}
              </Box>
            </Box>
          </Box>

          <TaskActions task={task} reload={onClose} />
          <OpenLogButton task={task} onOpen={handleOpenLog} />
        </Box>

        {/* RIGHT COLUMN — JSON FULL HEIGHT */}
        <Box
          sx={{
            background: "#111",
            borderRadius: 1,
            p: 2,
            overflowY: "auto",
          }}
        >
          <JsonViewer value={task.data} />
        </Box>
      </DialogContent>
      <ProcesserLogDialog
        open={openLog}
        onClose={handleCloseLog}
        logPath={selectedLogPath ?? undefined}
        taskId={selectedLogPath ? undefined : task.taskId}
      />
    </Dialog>
  );
}
